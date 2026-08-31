package dk.dbc.promat.service.api;

import dk.dbc.promat.service.persistence.BuggiOption;
import dk.dbc.promat.service.persistence.BuggiOptionGroup;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// A much smaller JAX-RS resource than TaxonomyService - see that class for the general
// @Stateless/@Path/@GET/@Produces explanations, which all apply identically here. This one
// queries the database directly on every request instead of going through an in-memory cache
// like TaxonomyCache - a deliberate choice given how small and rarely-changing this data is
// (a handful of groups, a few dozen options total, edited by hand in the database rather than
// synced from Kafka), where the extra complexity of a cache wouldn't pay for itself.
@Stateless
@Path("buggi")
public class BuggiOptionsService {

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @GET
    @Path("options")
    @Produces("application/json")
    public Response getOptions() {
        // SELECT DISTINCT ... LEFT JOIN FETCH: similar in spirit to DbTaxonomyBuilder's JOIN
        // FETCH (avoiding a separate query per group to load its options), but LEFT rather
        // than plain JOIN so a group with zero options still comes back (a plain JOIN would
        // silently exclude it, since there'd be no matching row to join against). DISTINCT is
        // needed because a JOIN FETCH duplicates the "one" side of a one-to-many for each
        // matching row on the "many" side - without it, a group with 5 options would appear 5
        // times in the raw result before JPA's post-processing collapses those into one
        // BuggiOptionGroup with a 5-element list; DISTINCT here operates at the JPQL/object
        // level (not just a SQL-level DISTINCT keyword) and prevents that duplication.
        List<BuggiOptionGroup> groups = entityManager.createQuery(
                        "SELECT DISTINCT g FROM BuggiOptionGroup g " +
                                "LEFT JOIN FETCH g.options " +
                                "WHERE g.active = true " +
                                "ORDER BY g.displayOrder", BuggiOptionGroup.class)
                .getResultList();

        // The Java Stream API: .stream() turns the List into a pipeline you can chain
        // transformations over. .map(fn) applies fn to each element, producing a new stream
        // of the results (here: each BuggiOptionGroup becomes a Map via toResponseEntry).
        // .collect(Collectors.toList()) runs the pipeline and gathers the results back into a
        // concrete List. This is a functional-programming-flavoured alternative to writing an
        // explicit for-loop that builds up a result list by hand - see toResponseEntry's own
        // stream usage below for a second example, filtering as well as mapping.
        List<Map<String, Object>> result = groups.stream()
                .map(BuggiOptionsService::toResponseEntry)
                .collect(Collectors.toList());

        return Response.ok().entity(result).build();
    }

    // Builds the plain Map (later serialized to JSON by JAX-RS/Jackson, same as elsewhere)
    // representing one group in the response. Using a LinkedHashMap rather than a plain
    // HashMap here matters for output readability: LinkedHashMap remembers insertion order,
    // so the JSON fields come out as "name", "subfieldCode", "requiresNonzeroValue", "options"
    // in that order every time - a plain HashMap gives no ordering guarantee at all.
    private static Map<String, Object> toResponseEntry(BuggiOptionGroup group) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", group.getName());
        entry.put("subfieldCode", group.getSubfieldCode());
        entry.put("requiresNonzeroValue", group.isRequiresNonzeroValue());
        // A longer stream pipeline: start from the group's options, .filter(...) keeps only
        // the ones matching a condition (active ones - a method reference to the isActive()
        // getter, used as a shorthand for `option -> option.isActive()`), .sorted(...) orders
        // what's left, .map(...) transforms each remaining BuggiOption down to just its name
        // (a String, since that's all the frontend needs), and .collect(...) gathers the
        // final result into a List<String>.
        entry.put("options", group.getOptions().stream()
                .filter(BuggiOption::isActive)
                .sorted(Comparator.comparingInt(o -> o.getDisplayOrder() == null ? 0 : o.getDisplayOrder()))
                .map(BuggiOption::getName)
                .collect(Collectors.toList()));
        return entry;
    }
}
