package dk.dbc.promat.service.taxonomy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import dk.dbc.promat.service.batch.ScheduledTaxonomyKafkaSync.KafkaTaxonomyItem;
import dk.dbc.promat.service.persistence.JsonMapperProvider;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.TaxonomySnapshot;
import dk.dbc.promat.service.taxonomy.dto.Subject;
import dk.dbc.promat.service.taxonomy.dto.Taxonomy;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// One of two implementations of the TaxonomyBuilder interface (see that file) - builds the
// Taxonomy tree the same way DM2Builder does: start from Taxonomy's own hardcoded category
// skeleton and add subjects into it one at a time via taxonomy.put(subject, path). The only
// difference is where the subjects come from - this project's own taxonomy_snapshot table
// (the last successful read of the taxonomy Kafka topic, see ScheduledTaxonomyKafkaSync)
// instead of a live HTTP call to rawrepo-record-service.
public class DbTaxonomyBuilder implements TaxonomyBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(DbTaxonomyBuilder.class);
    private static final ObjectMapper OBJECT_MAPPER = new JsonMapperProvider().getObjectMapper();
    private static final CollectionType SUBJECT_LIST_TYPE = OBJECT_MAPPER.getTypeFactory()
            .constructCollectionType(List.class, KafkaTaxonomyItem.class);

    // Fixed id of the single row in taxonomy_snapshot - there's only ever one, so no lookup
    // by anything else is needed.
    private static final int SNAPSHOT_ID = 1;

    private final EntityManager entityManager;

    public DbTaxonomyBuilder(@PromatEntityManager EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void buildTaxonomy(Taxonomy taxonomy) throws TaxonomyException {
        TaxonomySnapshot snapshot = entityManager.find(TaxonomySnapshot.class, SNAPSHOT_ID);
        if (snapshot == null) {
            LOGGER.warn("No taxonomy snapshot in the database yet - serving an empty taxonomy until the next successful Kafka sync");
            return;
        }

        List<KafkaTaxonomyItem> subjects;
        try {
            subjects = OBJECT_MAPPER.readValue(snapshot.getData(), SUBJECT_LIST_TYPE);
        } catch (Exception e) {
            TaxonomyException taxonomyException = new TaxonomyException("Could not parse taxonomy snapshot: " + e.getMessage());
            taxonomyException.initCause(e);
            throw taxonomyException;
        }

        int placed = 0;
        int skippedMissingPath = 0;
        // Paths, not subjects: a single new/unimplemented category can be shared by thousands
        // of subjects (e.g. "handling > handler om" alone accounts for over half the real
        // topic), so logging per-subject would spam the log with the same finding repeated
        // thousands of times. One line per distinct unresolvable path instead.
        Set<String> unimplementedCategories = new LinkedHashSet<>();

        for (KafkaTaxonomyItem item : subjects) {
            // Seen in practice on the real topic: a subject with a null/missing path. Checked
            // explicitly (rather than letting taxonomy.put(...) below fail on it) since an
            // empty/null path isn't a Map-lookup mismatch - it would NPE instead of raising the
            // IllegalArgumentException the catch below expects.
            if (item.getPath() == null || item.getPath().isEmpty()) {
                skippedMissingPath++;
                continue;
            }

            Subject dto = new Subject()
                    .withId(item.getId())
                    .withTitle(item.getTitle())
                    .withOftenUsed(item.isOftenUsed())
                    .withRef(item.getRef())
                    .withNote(item.getNote().toArray(String[]::new));
            try {
                taxonomy.put(dto, item.getPath());
                placed++;
            } catch (IllegalArgumentException e) {
                // A subject whose path doesn't resolve into Taxonomy's hardcoded skeleton -
                // i.e. Kafka is asking for a category this project hasn't implemented. Skipped
                // rather than failing the whole build; see the summary log below for how many
                // distinct categories/subjects this affected.
                unimplementedCategories.add(String.join(" > ", item.getPath()));
            }
        }

        unimplementedCategories.forEach(path -> LOGGER.warn("Found new category not implemented: {}", path));

        int skippedUnimplementedCategory = subjects.size() - placed - skippedMissingPath;
        LOGGER.info("Built taxonomy from snapshot: {} subjects placed, {} skipped (missing path), " +
                        "{} skipped (unimplemented category, across {} distinct paths)",
                placed, skippedMissingPath, skippedUnimplementedCategory, unimplementedCategories.size());
    }
}
