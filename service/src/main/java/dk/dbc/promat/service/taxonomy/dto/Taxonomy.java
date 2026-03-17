package dk.dbc.promat.service.taxonomy.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Taxonomy extends LinkedHashMap<String, Object> implements Serializable {

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
        put("ramme", settings);
        put("handling", action);
        put("fortælleteknik", narrative);
        put("stemning", mood);
    }

    public void put(Subject subject, String... path) {
        List<String> pathList = Arrays.asList(path);
        getList(pathList).add(subject.toHashMap());
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

    @SuppressWarnings("unchecked")
    private List<LinkedHashMap<String, Object>> getList(List<String> path) {
        Map<String, Object> current = this;
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
            throw new IllegalArgumentException("Invalid path: expected List at " + path.getLast());
        }
    }

}

