package dk.dbc.promat.service.batch;

import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.TaxonomyCategory;
import dk.dbc.promat.service.persistence.TaxonomySubject;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// @Stateless is a lighter-weight EJB stereotype than @Singleton (see
// ScheduledTaxonomyKafkaSync.java for that one): the container maintains a *pool* of
// interchangeable instances rather than exactly one, handing out whichever is free to
// service each call and recycling it afterwards. There's no meaningful "state" carried
// between calls (hence the name) - every method here starts fresh from its arguments and the
// injected EntityManager, so pooling multiple instances is perfectly safe and lets several
// callers use this bean concurrently without contention.
@Stateless
public class TaxonomyKafkaPersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaxonomyKafkaPersistence.class);

    // @PromatEntityManager is this project's own custom CDI qualifier (not a standard
    // Jakarta annotation) that disambiguates *which* EntityManager to inject, since a
    // project can have more than one persistence unit (see persistence.xml: promatPU vs
    // promatITPU). The EntityManager itself is JPA's central API for talking to the
    // database - every query, persist(), find() etc. in this class goes through it.
    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    // If a sync would delete more than this fraction of existing subjects, treat it as
    // a bad/incomplete Kafka read rather than an intentional bulk removal and skip the
    // delete step entirely (upserts still apply - they're safe either way).
    @Inject
    @ConfigProperty(name = "TAXONOMY_SUBJECT_DELETE_THRESHOLD_PERCENT", defaultValue = "15")
    int deleteThresholdPercent;

    // @TransactionAttribute(REQUIRES_NEW) tells the EJB container "always start a brand new
    // transaction for this method, suspending any transaction the caller might already be
    // in". The caller here (ScheduledTaxonomyKafkaSync.run()) deliberately opts OUT of
    // container transactions entirely (NOT_SUPPORTED) because it spends most of its time on
    // non-transactional Kafka I/O - REQUIRES_NEW is what makes sure the actual database work
    // in THIS method still gets a proper transaction (auto-committed on successful return,
    // auto-rolled-back if an exception escapes), independent of whatever the caller does.
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public PersistenceResult applyToDatabase(Collection<ScheduledTaxonomyKafkaSync.KafkaTaxonomyItem> seenSubjects) {
        Map<CategoryKey, TaxonomyCategory> categoryCache = loadCategoryCache();
        Set<Integer> touchedSubjectIds = new HashSet<>();
        int createdCategories = 0;
        int insertedSubjects = 0;
        int updatedSubjects = 0;
        int skippedSubjects = 0;

        for (ScheduledTaxonomyKafkaSync.KafkaTaxonomyItem item : seenSubjects) {
            if (item.getPath() == null || item.getPath().isEmpty()) {
                skippedSubjects++;
                // Present in the feed but unprocessable - preserve whatever's already stored for
                // this id rather than treating it as absent-and-therefore-deleted.
                touchedSubjectIds.add(item.getId());
                LOGGER.warn("Skipping taxonomy subject '{}' ({}) because path is missing", item.getTitle(), item.getId());
                continue;
            }

            ResolveCategoryResult categoryResult = resolveOrCreateCategory(item.getPath(), categoryCache);
            createdCategories += categoryResult.createdCount();
            TaxonomyCategory category = categoryResult.category();

            if (category == null) {
                skippedSubjects++;
                touchedSubjectIds.add(item.getId());
                continue;
            }

            UpsertOutcome outcome = upsertSubject(item, category);
            if (outcome == UpsertOutcome.INSERTED) {
                insertedSubjects++;
            } else if (outcome == UpsertOutcome.UPDATED) {
                updatedSubjects++;
            }
            touchedSubjectIds.add(item.getId());
        }

        DeletionOutcome deletionOutcome = deleteMissingSubjects(touchedSubjectIds);
        // entityManager.flush() forces any pending persist()/update changes to actually be
        // sent to the database as SQL right now, rather than whenever JPA would otherwise get
        // around to it (normally, at the end of the transaction). It's called once here,
        // after the whole batch, rather than after each individual subject - that's a
        // deliberate efficiency choice (fewer round-trips), with the tradeoff that a
        // constraint violation from any single bad row only surfaces at this one flush,
        // rolling back the whole method's writes together rather than isolating just that
        // row (see the parse-time validation added in ScheduledTaxonomyKafkaSync for how
        // most realistic bad-row cases are caught earlier instead, which sidesteps this).
        entityManager.flush();
        return new PersistenceResult(createdCategories, insertedSubjects, updatedSubjects,
                deletionOutcome.deletedCount(), skippedSubjects,
                deletionOutcome.thresholdExceeded(), deletionOutcome.existingCount(), deletionOutcome.wouldBeDeletedCount());
    }

    private Map<CategoryKey, TaxonomyCategory> loadCategoryCache() {
        // JPQL ("Jakarta Persistence Query Language") looks like SQL but queries over entity
        // classes and their fields, not table/column names - "SELECT c FROM TaxonomyCategory c"
        // means "give me TaxonomyCategory objects", not raw rows; EclipseLink translates this
        // into whatever real SQL Postgres needs, so this code has zero SQL string literals for
        // it and stays valid even if the underlying table/column names change later (as long
        // as the entity mapping is updated to match).
        Map<CategoryKey, TaxonomyCategory> cache = new HashMap<>();
        List<TaxonomyCategory> categories = entityManager
                .createQuery("SELECT c FROM TaxonomyCategory c", TaxonomyCategory.class)
                .getResultList();
        for (TaxonomyCategory category : categories) {
            cache.put(CategoryKey.of(category.getParent(), category.getName()), category);
        }
        return cache;
    }

    private ResolveCategoryResult resolveOrCreateCategory(List<String> path, Map<CategoryKey, TaxonomyCategory> categoryCache) {
        TaxonomyCategory parent = null;
        int createdCount = 0;

        for (int i = 0; i < path.size(); i++) {
            String name = path.get(i);
            boolean isLeaf = i == path.size() - 1;
            CategoryKey key = CategoryKey.of(parent, name);
            TaxonomyCategory category = categoryCache.get(key);

            if (category == null) {
                category = new TaxonomyCategory()
                        .withParent(parent)
                        .withName(name)
                        .withIsLeaf(isLeaf)
                        .withActive(false);
                // entityManager.persist(...) schedules an INSERT - it doesn't necessarily hit
                // the database immediately (JPA is allowed to batch writes up until the next
                // flush/commit), which is why the explicit .flush() right after is needed
                // here specifically: the freshly-created category's auto-generated `id` (see
                // TaxonomyCategory's @GeneratedValue) is only assigned once the INSERT has
                // actually executed, and the very next loop iteration might need to use this
                // category as a *parent*, which requires a real, already-assigned id.
                entityManager.persist(category);
                entityManager.flush();
                categoryCache.put(CategoryKey.of(parent, name), category);
                createdCount++;
            } else if (isLeaf && !category.isLeaf()) {
                LOGGER.warn("Skipping taxonomy path {} because existing category '{}' is not a leaf", path, name);
                return new ResolveCategoryResult(null, createdCount);
            } else if (!isLeaf && category.isLeaf()) {
                LOGGER.warn("Skipping taxonomy path {} because existing category '{}' is a leaf but used as a parent", path, name);
                return new ResolveCategoryResult(null, createdCount);
            }

            parent = category;
        }

        return new ResolveCategoryResult(parent, createdCount);
    }

    private UpsertOutcome upsertSubject(ScheduledTaxonomyKafkaSync.KafkaTaxonomyItem item, TaxonomyCategory category) {
        // entityManager.find(EntityClass, primaryKey) is JPA's direct-by-id lookup - the JPA
        // equivalent of `SELECT * FROM taxonomy_subject WHERE id = ?`, but returning an
        // actual managed TaxonomySubject object (or null) instead of a raw row.
        TaxonomySubject existing = entityManager.find(TaxonomySubject.class, item.getId());
        String[] note = item.getNote().toArray(String[]::new);

        if (existing == null) {
            TaxonomySubject subject = new TaxonomySubject()
                    .withId(item.getId())
                    .withTitle(item.getTitle())
                    .withNote(note)
                    .withOftenUsed(item.isOftenUsed())
                    .withRef(item.getRef())
                    .withCategory(category)
                    .withSourceRecordId(item.getSourceRecordId())
                    .withUpdatedAt(LocalDateTime.now());
            entityManager.persist(subject);
            return UpsertOutcome.INSERTED;
        }

        if (isUnchanged(existing, item, category, note)) {
            return UpsertOutcome.UNCHANGED;
        }

        // Note there's no entityManager.merge()/update() call here - `existing` is a
        // "managed" entity, meaning JPA is already tracking it (it came from find(), inside
        // this transaction). Simply calling its setters is enough; JPA automatically detects
        // the change and includes an UPDATE for it in the next flush - this is JPA's "dirty
        // checking" mechanism, and it's why entities fetched inside a transaction behave
        // differently from a plain Java object you construct yourself.
        existing.setTitle(item.getTitle());
        existing.setNote(note);
        existing.setOftenUsed(item.isOftenUsed());
        existing.setRef(item.getRef());
        existing.setCategory(category);
        existing.setSourceRecordId(item.getSourceRecordId());
        existing.setUpdatedAt(LocalDateTime.now());
        return UpsertOutcome.UPDATED;
    }

    private boolean isUnchanged(TaxonomySubject existing,
                                ScheduledTaxonomyKafkaSync.KafkaTaxonomyItem item,
                                TaxonomyCategory category,
                                String[] note) {
        return Objects.equals(existing.getTitle(), item.getTitle())
                && Arrays.equals(existing.getNote(), note)
                && existing.isOftenUsed() == item.isOftenUsed()
                && Objects.equals(existing.getRef(), item.getRef())
                && Objects.equals(existing.getCategory().getId(), category.getId())
                && Objects.equals(existing.getSourceRecordId(), item.getSourceRecordId());
    }

    // NOT IN with a Set<Integer> binds one parameter per element rather than a single array
    // bind. Fine at current scale (~11k subjects); if this set grows enough to approach
    // Postgres's per-statement bind-parameter limit, switch to a native `<> ALL(?)` array
    // bind instead - deliberately not done now, to avoid an untested type-binding change
    // (this file already hit two of those tonight: EclipseLink/PGobject and Instant/bytea).
    private DeletionOutcome deleteMissingSubjects(Set<Integer> touchedSubjectIds) {
        long existingCount = countAllSubjects();
        long wouldBeDeleted = countSubjectsNotIn(touchedSubjectIds);

        if (existingCount > 0 && wouldBeDeleted > existingCount * (deleteThresholdPercent / 100.0)) {
            LOGGER.error("Refusing to delete {} of {} existing taxonomy subjects ({}%, exceeds the {}% safety threshold) - " +
                            "this looks like a bad or incomplete Kafka read rather than an intentional bulk removal. " +
                            "Leaving existing subjects untouched; upserts from this run were still applied.",
                    wouldBeDeleted, existingCount, Math.round(100.0 * wouldBeDeleted / existingCount), deleteThresholdPercent);
            return new DeletionOutcome(0, true, existingCount, wouldBeDeleted);
        }

        // A JPQL "bulk" DELETE (no `entityManager.find` + `.remove()` per row) - this issues
        // one DELETE statement straight to the database, without loading matching rows into
        // memory as entities first. Much cheaper for "delete potentially thousands of rows",
        // at the cost of bypassing per-entity lifecycle callbacks (not used by this entity
        // anyway) and the persistence context's normal change tracking.
        int deleted = touchedSubjectIds.isEmpty()
                ? entityManager.createQuery("DELETE FROM TaxonomySubject s").executeUpdate()
                : entityManager.createQuery("DELETE FROM TaxonomySubject s WHERE s.id NOT IN :ids")
                        .setParameter("ids", touchedSubjectIds)
                        .executeUpdate();
        return new DeletionOutcome(deleted, false, existingCount, wouldBeDeleted);
    }

    private long countAllSubjects() {
        return entityManager.createQuery("SELECT COUNT(s) FROM TaxonomySubject s", Long.class).getSingleResult();
    }

    private long countSubjectsNotIn(Set<Integer> touchedSubjectIds) {
        if (touchedSubjectIds.isEmpty()) {
            return countAllSubjects();
        }
        return entityManager.createQuery("SELECT COUNT(s) FROM TaxonomySubject s WHERE s.id NOT IN :ids", Long.class)
                .setParameter("ids", touchedSubjectIds)
                .getSingleResult();
    }

    // A Java "record" (introduced in Java 16): a compact way to declare an immutable data
    // carrier. `record PersistenceResult(int createdCategories, ...)` implicitly generates a
    // constructor taking all these fields, a getter for each with the SAME name as the field
    // (result.createdCategories(), not getCreatedCategories() - a deliberate departure from
    // the JavaBeans getter convention used elsewhere in this codebase, e.g. TaxonomyCategory),
    // plus equals()/hashCode()/toString() implementations, all without writing any of that
    // boilerplate by hand. Records are a natural fit for "just a bundle of values to return
    // from a method", which is exactly what this and the other three records below are for.
    public record PersistenceResult(int createdCategories,
                                    int insertedSubjects,
                                    int updatedSubjects,
                                    int deletedSubjects,
                                    int skippedSubjects,
                                    boolean deletionThresholdExceeded,
                                    long existingSubjectCountBeforeDelete,
                                    long subjectsThatWouldHaveBeenDeleted) {
    }

    private record DeletionOutcome(int deletedCount, boolean thresholdExceeded, long existingCount, long wouldBeDeletedCount) {
    }

    private record ResolveCategoryResult(TaxonomyCategory category, int createdCount) {
    }

    // A record can have its own methods too, not just the auto-generated ones - `of(...)`
    // here is a small static "factory method" (a common alternative to calling `new` directly,
    // useful when construction needs a little logic, here: safely reading a possibly-null
    // parent's id) that produces a CategoryKey. CategoryKey itself exists so a (parentId, name)
    // pair can be used as a HashMap key in loadCategoryCache()/resolveOrCreateCategory() above -
    // records automatically get a correct equals()/hashCode() (comparing all their components),
    // which is exactly what's needed for two CategoryKeys with the same values to be treated
    // as the same map key.
    private record CategoryKey(Integer parentId, String name) {
        private static CategoryKey of(TaxonomyCategory parent, String name) {
            return new CategoryKey(parent == null ? null : parent.getId(), name);
        }
    }

    // A plain Java enum - three named constants representing the possible outcomes of
    // upsertSubject(...). Preferred over e.g. returning a String ("INSERTED") or an int code,
    // because the compiler enforces that only these three values ever exist, and a switch or
    // if/else comparing against them (see applyToDatabase() above) can't typo a value the way
    // a raw string comparison could.
    private enum UpsertOutcome {
        INSERTED,
        UPDATED,
        UNCHANGED
    }
}
