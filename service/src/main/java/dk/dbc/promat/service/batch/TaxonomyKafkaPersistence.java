package dk.dbc.promat.service.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.promat.service.persistence.JsonMapperProvider;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.TaxonomySnapshot;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Collection;

// Writes the Kafka sync's result as a single-row snapshot (see TaxonomySnapshot) rather than
// one row per subject - the consumer already re-reads the whole topic every run (see
// ScheduledTaxonomyKafkaSync), so there's no incremental-update case to optimize for, and
// nothing queries individual subjects at the SQL level (TaxonomyService/TaxonomyCache only
// ever read the whole tree back out of memory).
@Stateless
public class TaxonomyKafkaPersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaxonomyKafkaPersistence.class);
    private static final ObjectMapper OBJECT_MAPPER = new JsonMapperProvider().getObjectMapper();
    private static final int SNAPSHOT_ID = 1;

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    // If a sync would replace the snapshot with drastically fewer subjects than it already
    // holds, treat it as a bad/incomplete Kafka read rather than an intentional bulk removal,
    // and skip the write entirely - the previous, larger snapshot keeps being served instead.
    @Inject
    @ConfigProperty(name = "TAXONOMY_SUBJECT_DELETE_THRESHOLD_PERCENT", defaultValue = "15")
    int deleteThresholdPercent;

    // REQUIRES_NEW: the caller opts out of container transactions entirely (NOT_SUPPORTED,
    // since it spends most of its time on non-transactional Kafka I/O) - this is what gives
    // the actual database write below its own real transaction regardless.
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public PersistenceResult applyToDatabase(Collection<ScheduledTaxonomyKafkaSync.KafkaTaxonomyItem> seenSubjects) {
        TaxonomySnapshot existing = entityManager.find(TaxonomySnapshot.class, SNAPSHOT_ID);
        int previousCount = existing == null ? 0 : existing.getSubjectCount();
        int newCount = seenSubjects.size();

        if (exceedsDeleteThreshold(previousCount, newCount)) {
            LOGGER.error("Refusing to replace taxonomy snapshot: new read has {} subjects, down from {} ({}% drop, exceeds the {}% safety threshold) - " +
                            "this looks like a bad or incomplete Kafka read rather than an intentional bulk removal. Snapshot left untouched.",
                    newCount, previousCount, percentDrop(previousCount, newCount), deleteThresholdPercent);
            return new PersistenceResult(0, true, previousCount, newCount);
        }

        String data;
        try {
            data = OBJECT_MAPPER.writeValueAsString(seenSubjects);
        } catch (Exception e) {
            LOGGER.error("Could not serialize taxonomy snapshot; database was not updated", e);
            return new PersistenceResult(0, false, previousCount, newCount);
        }

        writeSnapshot(existing, data, newCount);
        return new PersistenceResult(newCount, false, previousCount, newCount);
    }

    private boolean exceedsDeleteThreshold(int previousCount, int newCount) {
        return previousCount > 0 && newCount < previousCount * (1 - deleteThresholdPercent / 100.0);
    }

    private static long percentDrop(int previousCount, int newCount) {
        return Math.round(100.0 * (previousCount - newCount) / previousCount);
    }

    private void writeSnapshot(TaxonomySnapshot existing, String data, int subjectCount) {
        if (existing == null) {
            entityManager.persist(new TaxonomySnapshot()
                    .withId(SNAPSHOT_ID)
                    .withData(data)
                    .withSubjectCount(subjectCount)
                    .withUpdatedAt(LocalDateTime.now()));
            return;
        }
        // No merge()/persist() call needed here - `existing` is managed (came from find() in
        // this transaction), so JPA's dirty checking picks up these setters on its own.
        existing.setData(data);
        existing.setSubjectCount(subjectCount);
        existing.setUpdatedAt(LocalDateTime.now());
    }

    public record PersistenceResult(int writtenSubjects,
                                     boolean thresholdExceeded,
                                     int previousSubjectCount,
                                     int newSubjectCount) {
    }
}
