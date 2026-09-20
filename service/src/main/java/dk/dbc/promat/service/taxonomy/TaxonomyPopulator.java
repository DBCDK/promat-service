package dk.dbc.promat.service.taxonomy;

import dk.dbc.promat.service.taxonomy.dto.Subject;
import dk.dbc.promat.service.taxonomy.dto.Taxonomy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

// Places subjects into Taxonomy's hardcoded category skeleton via taxonomy.put(subject, path).
public class TaxonomyPopulator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaxonomyPopulator.class);

    private TaxonomyPopulator() {
    }

    public static void populate(Taxonomy taxonomy, Collection<Subject> subjects) {
        int placed = 0;
        int skippedMissingPath = 0;
        int skippedDuplicateId = 0;
        // One line per distinct unresolvable path, not per subject - a single category can
        // cover thousands of subjects.
        Set<String> unimplementedCategories = new LinkedHashSet<>();

        for (Subject item : subjects) {
            // Checked explicitly since an empty/null path would NPE in taxonomy.put(...)
            // instead of raising the IllegalArgumentException the catch below expects.
            if (item.getPath() == null || item.getPath().isEmpty()) {
                skippedMissingPath++;
                continue;
            }

            // id (MARC x09$q, curator-assigned) is expected to be globally unique across the
            // whole taxonomy. Keep the first-seen subject for a given id, skip and log the rest,
            // so one bad/duplicate record can't take down a sync.
            if (taxonomy.getById(item.getId()) != null) {
                LOGGER.warn("Skipping subject with duplicate taxonomy id {} ('{}') - id is expected to be globally unique",
                        item.getId(), item.getTitle());
                skippedDuplicateId++;
                continue;
            }

            try {
                taxonomy.put(item, item.getPath());
                placed++;
            } catch (IllegalArgumentException e) {
                // Path doesn't resolve into Taxonomy's hardcoded skeleton - an unimplemented
                // category. Skipped; see the summary log below.
                unimplementedCategories.add(String.join(" > ", item.getPath()));
            }
        }

        unimplementedCategories.forEach(path -> LOGGER.warn("Found new category not implemented: {}", path));

        int skippedUnimplementedCategory = subjects.size() - placed - skippedMissingPath - skippedDuplicateId;
        LOGGER.info("Built taxonomy: {} subjects placed, {} skipped (missing path), {} skipped (duplicate id), " +
                        "{} skipped (unimplemented category, across {} distinct paths)",
                placed, skippedMissingPath, skippedDuplicateId, skippedUnimplementedCategory, unimplementedCategories.size());
    }
}
