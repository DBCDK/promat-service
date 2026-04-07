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

public class Taxonomy  implements Serializable {
    private static final JSONBContext JSONB_CONTEXT =  new JSONBContext();
    private Map<String, Object> root = new LinkedHashMap<>();
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

    public void put(Subject subject, String... path) {
        put(subject, Arrays.asList(path));
    }

    public void put(Subject subject, List<String> path) {
        getList(path).add(subject.toHashMap());
    }

    public Subject get(String... path) {
        List<String> pathList =  new ArrayList<>(Arrays.asList(path));
        String key = pathList.removeLast();
        List<LinkedHashMap<String, Object>> s = getList(pathList);
        return s.stream().map(Subject::of)
                .filter(subject -> key.equals(subject.getTitle()))
                .findFirst()
                .orElse(null);
    }

    public List<Subject> getList(String... path) {
        List<LinkedHashMap<String, Object>> list = getList(new ArrayList<>(Arrays.asList(path)));
        return list.stream().map(Subject::of).toList();
    }

    @SuppressWarnings("unchecked")
    private List<LinkedHashMap<String, Object>> getList(List<String> path) {
        Map<String, Object> current = root;
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

