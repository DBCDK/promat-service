package dk.dbc.promat.service.taxonomy;

import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.TaxonomyCategory;
import dk.dbc.promat.service.persistence.TaxonomySubject;
import dk.dbc.promat.service.taxonomy.dto.Subject;
import dk.dbc.promat.service.taxonomy.dto.Taxonomy;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// One of two implementations of the TaxonomyBuilder interface (see that file) - this one
// builds the in-memory Taxonomy tree from this project's own database tables, populated by
// ScheduledTaxonomyKafkaSync/TaxonomyKafkaPersistence, rather than fetching it live over HTTP
// like the older DM2Builder does. Note this is a PLAIN class, with no @Stateless/@Singleton -
// it's not itself a CDI/EJB-managed bean, just a regular object that TaxonomyBuilderProducer's
// @Produces method constructs with `new` and hands out.
public class DbTaxonomyBuilder implements TaxonomyBuilder {
    // Comparator.comparing(...).thenComparing(...): builds a Comparator by chaining two
    // sorting criteria - primarily by displayOrder (categories with no explicit order sort
    // last, via the ternary substituting Integer.MAX_VALUE for null), then alphabetically by
    // name as a tie-breaker. A method reference (TaxonomyCategory::getName) is used for the
    // second step since no special null-handling is needed there, whereas the first step
    // needs a full lambda to handle the possibly-null displayOrder.
    private static final Comparator<TaxonomyCategory> CATEGORY_COMPARATOR = Comparator
            .comparing((TaxonomyCategory category) -> category.getDisplayOrder() == null ? Integer.MAX_VALUE : category.getDisplayOrder())
            .thenComparing(TaxonomyCategory::getName);

    private final EntityManager entityManager;

    // Constructor injection isn't happening here in the CDI sense (no @Inject on this
    // constructor) - TaxonomyBuilderProducer just calls `new DbTaxonomyBuilder(entityManager)`
    // directly, passing along the EntityManager it received from ITS OWN @Inject. The
    // @PromatEntityManager annotation on the parameter is inert here (it only has meaning at
    // an actual CDI injection point) - it's left on purely as documentation of what kind of
    // EntityManager this expects.
    public DbTaxonomyBuilder(@PromatEntityManager EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void buildTaxonomy(Taxonomy taxonomy) {
        List<TaxonomyCategory> activeCategories = entityManager
                .createQuery("SELECT c FROM TaxonomyCategory c WHERE c.active = true", TaxonomyCategory.class)
                .getResultList();

        // Two lookup maps built from one query result, rather than querying the database
        // again for each category's children: categoriesById lets resolvePath() below walk
        // "parent of parent of..." without a query per step, and childrenByParentId groups
        // categories by their parent's id (null key = root categories) so buildStructure()
        // can recurse down the tree using only in-memory lookups.
        Map<Integer, TaxonomyCategory> categoriesById = new HashMap<>();
        Map<Integer, List<TaxonomyCategory>> childrenByParentId = new HashMap<>();
        for (TaxonomyCategory category : activeCategories) {
            categoriesById.put(category.getId(), category);
            Integer parentId = category.getParent() == null ? null : category.getParent().getId();
            // computeIfAbsent(key, fn): "if this key isn't in the map yet, compute a value for
            // it using fn and store that; either way, return the value now associated with the
            // key" - a common one-liner for "get-or-create-then-use" that avoids a separate
            // containsKey() check followed by a put().
            childrenByParentId.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(category);
        }
        childrenByParentId.values().forEach(children -> children.sort(CATEGORY_COMPARATOR));

        taxonomy.setRoot(buildStructure(childrenByParentId, null));

        // A JPQL "JOIN FETCH": without it, accessing subject.getCategory() for each of these
        // subjects would trigger a SEPARATE database query per subject the first time its
        // category is touched (JPA's "lazy loading"), potentially thousands of extra
        // round-trips. JOIN FETCH tells JPA to load each subject's category in the SAME query
        // that loads the subjects themselves - this is the standard fix for what's commonly
        // called the "N+1 query problem".
        //
        // Java's triple-quote """ ... """ syntax is a "text block" (Java 15+) - a way to write
        // a multi-line string literal without \n and string-concatenation noise; useful here
        // purely for readability of a longer JPQL query.
        List<TaxonomySubject> subjects = entityManager
                .createQuery("""
                        SELECT s
                        FROM TaxonomySubject s
                        JOIN FETCH s.category c
                        WHERE c.active = true
                          AND c.isLeaf = true
                        ORDER BY s.title
                        """, TaxonomySubject.class)
                .getResultList();

        for (TaxonomySubject subject : subjects) {
            taxonomy.put(toDto(subject), resolvePath(subject.getCategory(), categoriesById));
        }
    }

    // Recursive method: calls itself for each non-leaf category, walking one level deeper
    // into the tree each time, until it bottoms out at leaf categories (which get an empty
    // list instead of a further nested map). This mirrors the tree's own recursive shape -
    // each level of Postgres rows becomes one level of nested Map in the returned structure.
    private Map<String, Object> buildStructure(Map<Integer, List<TaxonomyCategory>> childrenByParentId, Integer parentId) {
        Map<String, Object> node = new LinkedHashMap<>();
        for (TaxonomyCategory category : childrenByParentId.getOrDefault(parentId, List.of())) {
            if (category.isLeaf()) {
                node.put(category.getName(), new ArrayList<>());
            } else {
                node.put(category.getName(), buildStructure(childrenByParentId, category.getId()));
            }
        }
        return node;
    }

    // Walks from a leaf category up to its root, collecting names along the way, to produce
    // e.g. ["handling", "handler om"] from a "handler om" TaxonomyCategory row. LinkedList is
    // used specifically because of addFirst(...): each step discovers one more ANCESTOR
    // (walking upward), but the desired output order is root-to-leaf (top-down) - addFirst
    // builds the list in the right final order without needing a separate reverse() step
    // afterwards. (ArrayList has no efficient addFirst; LinkedList does.)
    private List<String> resolvePath(TaxonomyCategory category, Map<Integer, TaxonomyCategory> categoriesById) {
        LinkedList<String> path = new LinkedList<>();
        TaxonomyCategory current = category;
        while (current != null) {
            path.addFirst(current.getName());
            current = current.getParent() == null ? null : categoriesById.get(current.getParent().getId());
        }
        return path;
    }

    // Converts a JPA entity (TaxonomySubject, tied to the database/persistence context) into
    // a plain DTO (Subject, see taxonomy/dto/Subject.java) that Taxonomy actually stores and
    // serves over the REST API. Keeping these as two separate classes means the API's response
    // shape doesn't have to match the database schema exactly, and the DTO can be handed
    // around/serialized freely without dragging a live database connection along with it the
    // way a JPA entity implicitly can.
    private Subject toDto(TaxonomySubject subject) {
        Subject dto = new Subject()
                .withId(subject.getId())
                .withTitle(subject.getTitle())
                .withOftenUsed(subject.isOftenUsed())
                .withRef(subject.getRef());
        dto.withNote(subject.getNote());
        return dto;
    }
}
