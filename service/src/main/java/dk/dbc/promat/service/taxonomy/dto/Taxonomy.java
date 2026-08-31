package dk.dbc.promat.service.taxonomy.dto;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// This class predates tonight's Kafka/database work (only setRoot(...) and searchList(...)
// below are new) - it's the in-memory representation of the whole taxonomy tree that
// TaxonomyCache holds onto and TaxonomyService serves over REST. Structurally it's just a
// nested Map<String, Object>: each key is a category name, and each value is either another
// nested Map (a sub-category) or a List (a leaf category's actual subjects) - a generic,
// JSON-shaped tree with no dedicated "Category"/"Node" class of its own.
public class Taxonomy  implements Serializable {
    private static final JSONBContext JSONB_CONTEXT =  new JSONBContext();
    private Map<String, Object> root = new LinkedHashMap<>();

    // Historically, this constructor was the ONLY way the tree's structure got built - every
    // category name was hardcoded directly in Java, by hand, matching what used to be the
    // taxonomy's one fixed shape. Now that the structure can also come from the database (see
    // DbTaxonomyBuilder, which calls setRoot(...) below to replace this hardcoded skeleton
    // entirely), this constructor mostly matters as: (a) the default/fallback shape if no
    // TaxonomyBuilder is configured at all, and (b) what TaxonomyCache starts with before its
    // very first successful refresh.
    public Taxonomy() {

        // Settings (Ramme)
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("handlingens tid udtrykt i ord", new ArrayList<>());
        settings.put("handlingens tid udtrykt i tal", new ArrayList<>());
        settings.put("geografisk sted", new ArrayList<>());
        settings.put("fiktivt sted", new ArrayList<>());
        settings.put("miljø", new ArrayList<>());
        settings.put("genre", new ArrayList<>());
        settings.put("univers", new ArrayList<>());

        // Action (Handling)
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("handler om", new ArrayList<Map<String, Object>>());
        action.put("navngivet hovedperson", new ArrayList<Map<String, Object>>());

        Map<String, Object> mainCharacterDescription = new LinkedHashMap<>();
        mainCharacterDescription.put("om hovedpersonen", new ArrayList<>());
        mainCharacterDescription.put("hovedpersonens karaktertræk", new ArrayList<>());
        mainCharacterDescription.put("hovedpersonens konflikt", new ArrayList<>());

        action.put("hovedperson(er) - beskrivelse", mainCharacterDescription);

        // Narrative technique (Fortælleteknik)
        Map<String, Object> narrative = new LinkedHashMap<>();
        narrative.put("skrivestil og struktur", new ArrayList<>());
        narrative.put("fortællerstemme", new ArrayList<>());
        narrative.put("tempo", new ArrayList<>());

        // Mood (stemning)
        Map<String, Object> mood = new LinkedHashMap<>();
        mood.put("positiv", new ArrayList<>());
        mood.put("humoristisk", new ArrayList<>());
        mood.put("romantisk", new ArrayList<>());
        mood.put("erotisk", new ArrayList<>());
        mood.put("dramatisk", new ArrayList<>());
        mood.put("trist", new ArrayList<>());
        mood.put("uhyggelig", new ArrayList<>());
        mood.put("fantasifuld", new ArrayList<>());
        mood.put("tankevækkende", new ArrayList<>());

        // Consolidate
        root.put("ramme", settings);
        root.put("handling", action);
        root.put("fortælleteknik", narrative);
        root.put("stemning", mood);
    }

    public Map<String, Object> getRoot() {
        return root;
    }

    // Added for DbTaxonomyBuilder: lets that class throw away the hardcoded structure built
    // by the constructor above and replace it wholesale with one derived from the
    // taxonomy_category table instead. A plain setter, nothing Jakarta-specific about it -
    // it's just how a builder that constructs the tree from a different source (the database
    // instead of hardcoded Java) gets to install its result.
    public void setRoot(Map<String, Object> root) {
        this.root = root;
    }

    public Map<String, Object> getStructure() {
        return stripSubjects(root);
    }

    // Overload #1: varargs convenience - lets callers write put(subject, "handling", "handler om")
    // instead of building a List themselves. It just wraps the array as a List and delegates
    // to overload #2.
    public void put(Subject subject, String... path) {
        put(subject, Arrays.asList(path));
    }

    // Overload #2: the actual logic - find the leaf list this path points at, and append the
    // subject's plain-Map representation (Subject.toHashMap()) to it. Subjects are stored as
    // LinkedHashMaps, not Subject objects, throughout this tree - see getList()'s comment
    // below for why that matters when reading them back out.
    public void put(Subject subject, List<String> path) {
        getList(path).add(subject.toHashMap());
    }

    public Subject get(String... path) {
        List<String> pathList =  new ArrayList<>(Arrays.asList(path));
        // List.removeLast() (Java 21+): removes and returns the final element - used here to
        // split the given path into "the category to look inside" (everything but the last
        // segment) and "which subject's title to look for there" (the last segment itself).
        String key = pathList.removeLast();
        List<LinkedHashMap<String, Object>> s = getList(pathList);
        return s.stream().map(Subject::of)
                .filter(subject -> key.equals(subject.getTitle()))
                .findFirst()
                .orElse(null);
    }

    // Returns everything under a path as proper Subject objects. Note this ALWAYS
    // re-materializes every entry in the target list via Subject::of (a method reference to
    // Subject's static factory method - see Subject.java), every single call - fine for small
    // categories, but for one with thousands of subjects (like "handling->handler om"), that
    // means fully rebuilding thousands of Subject objects just to serve one request. This is
    // exactly why searchList() below deliberately does its filtering BEFORE calling
    // Subject::of, rather than reusing this method and filtering the result afterwards.
    public List<Subject> getList(String... path) {
        List<LinkedHashMap<String, Object>> list = getList(new ArrayList<>(Arrays.asList(path)));
        return list.stream().map(Subject::of).toList();
    }

    /**
     * Case-insensitive substring search over the titles of subjects at the given path,
     * sourced from the same in-memory cache as {@link #getList}, so it works regardless
     * of which {@code TaxonomyBuilder} populated it.
     */
    public List<Subject> searchList(String[] path, String query, int limit) {
        List<LinkedHashMap<String, Object>> list = getList(new ArrayList<>(Arrays.asList(path)));
        String lowerQuery = query.toLowerCase();
        // Deliberately filters (and limits) the raw LinkedHashMap entries FIRST, and only
        // calls Subject::of on whatever's left afterwards - the expensive
        // map-to-Subject-object conversion only happens for the (at most `limit`) actual
        // matches, not for every entry in a potentially huge list. `entry.get("title")
        // instanceof String title` is a Java 16+ "pattern variable" - it checks the type AND
        // binds a usable, already-cast variable (`title`) in one expression, instead of a
        // separate instanceof check followed by an explicit cast.
        return list.stream()
                .filter(entry -> entry.get("title") instanceof String title && title.toLowerCase().contains(lowerQuery))
                .limit(limit)
                .map(Subject::of)
                .toList();
    }

    // @SuppressWarnings("unchecked") silences the compiler's warning about the unchecked cast
    // to Map<String, Object> below - Java's generics are "erased" at runtime (a List<String>
    // and a List<Integer> are indistinguishable to the JVM once compiled), so casting an
    // Object that's merely known to be *some* Map into a Map<String, Object> can't actually be
    // verified by the compiler; this annotation is how you tell the compiler "I've reasoned
    // about this and it's fine", rather than leaving a warning in every build's output.
    @SuppressWarnings("unchecked")
    private List<LinkedHashMap<String, Object>> getList(List<String> path) {
        Map<String, Object> current = root;
        // Walks every path segment except the LAST one, descending one Map level per segment
        // (this is why put()/getList() throw IllegalArgumentException for an unknown path -
        // see TaxonomyService, which turns that into an HTTP 404 - if a segment along the way
        // isn't actually a nested Map).
        for (int i = 0; i < path.size() - 1; i++) {
            String key = path.get(i);
            Object next = current.get(key);
            if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                throw new IllegalArgumentException("Invalid path: expected Map at " + key);
            }
        }

        Object target = current.get(path.getLast());
        if (target instanceof List) {
            return (List<LinkedHashMap<String, Object>>) target;
        } else {
            throw new IllegalArgumentException("Invalid path: expected List at '" + path.getLast() + "' ");
        }
    }

    public static Taxonomy of(String taxonomyString) throws JSONBException {
        Taxonomy taxonomy = new Taxonomy();
        taxonomy.root = JSONB_CONTEXT.unmarshall(taxonomyString, LinkedHashMap.class);
        return taxonomy;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> stripSubjects(Map<String, Object> source) {
        Map<String, Object> structure = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            // `instanceof Map<?, ?> childMap` - another pattern-variable instanceof (see
            // searchList() above): checks the type and binds `childMap` in one step. `<?, ?>`
            // is a "wildcard" generic type meaning "a Map of some unknown key/value types" -
            // needed because you can't check `instanceof Map<String, Object>` directly (generic
            // type parameters are erased at runtime, as mentioned above, so the JVM has no way
            // to verify them at an instanceof check).
            if (value instanceof Map<?, ?> childMap) {
                structure.put(entry.getKey(), stripSubjects((Map<String, Object>) childMap));
            } else if (value instanceof List<?>) {
                structure.put(entry.getKey(), new ArrayList<>());
            } else {
                throw new IllegalArgumentException("Invalid taxonomy structure at '" + entry.getKey() + "'");
            }
        }
        return structure;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Taxonomy taxonomy = (Taxonomy) o;
        return Objects.equals(root, taxonomy.root);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(root);
    }
}
