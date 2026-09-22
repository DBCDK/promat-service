package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.BuggiOption;
import dk.dbc.promat.service.dto.BuggiOptionGroup;
import dk.dbc.promat.service.dto.BuggiSelectionRequest;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.taskdata.BuggiSelectionEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BuggiVocabulary {
    // IDs are part of the API contract used by PUT /tasks/{taskId}/buggi.
    // Keep existing IDs stable; add new options with new IDs instead of renumbering.
    private static final List<Option> OPTIONS = List.of(
            new Option(1, "Læsbarhed", "s", false, "let/svær"),
            new Option(2, "Læsbarhed", "s", false, "tekst/tegninger"),
            new Option(3, "Læsbarhed", "s", false, "kort/lang"),
            new Option(4, "Fantasi/virkelighed", "u", false, "virkelig/fantasi"),
            new Option(5, "Stemning", "n", true, "rar"),
            new Option(6, "Stemning", "n", true, "sjov"),
            new Option(7, "Stemning", "n", true, "romantisk"),
            new Option(8, "Stemning", "n", true, "spændende"),
            new Option(9, "Stemning", "n", true, "trist"),
            new Option(10, "Stemning", "n", true, "uhyggelig"),
            new Option(11, "Stemning", "n", true, "tankevækkende"),
            new Option(12, "Tema", "e", true, "dyr"),
            new Option(13, "Tema", "e", true, "sport"),
            new Option(14, "Tema", "e", true, "venskaber"),
            new Option(15, "Tema", "e", true, "mit liv"),
            new Option(16, "Tema", "e", true, "ud i fremtiden"),
            new Option(17, "Tema", "e", true, "skæve karakterer"),
            new Option(18, "Tema", "e", true, "den store verden"),
            new Option(19, "Tema", "e", true, "fantasy"),
            new Option(20, "Tema", "e", true, "gys"),
            new Option(21, "Tema", "e", true, "eventyrlig"),
            new Option(22, "Tema", "e", true, "action"),
            new Option(23, "Tema", "e", true, "gaming")
    );

    private static final Map<Integer, Option> OPTIONS_BY_ID = OPTIONS.stream()
            .collect(LinkedHashMap::new, (map, option) -> map.put(option.id(), option), Map::putAll);

    private BuggiVocabulary() {
    }

    public static List<BuggiOptionGroup> groups() {
        Map<String, BuggiOptionGroup> groups = new LinkedHashMap<>();
        for(Option option : OPTIONS) {
            BuggiOptionGroup group = groups.computeIfAbsent(option.group(), groupName ->
                    new BuggiOptionGroup(groupName, option.subfieldCode(), option.requiresNonzeroValue(), new ArrayList<>()));
            group.getOptions().add(new BuggiOption(option.id(), option.name()));
        }
        return List.copyOf(groups.values());
    }

    public static BuggiSelectionEntry resolve(BuggiSelectionRequest request) throws ServiceErrorException {
        Option option = request == null ? null : OPTIONS_BY_ID.get(request.getId());
        if(option == null) {
            throw new ServiceErrorException(String.format("Buggi option id %s does not exist", request == null ? null : request.getId()))
                    .withHttpStatus(400)
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withCause("Invalid buggi option");
        }
        if(request.getValue() == null || request.getValue() < 0 || request.getValue() > 5) {
            throw new ServiceErrorException(String.format("Buggi option %s has invalid value %s", request.getId(), request.getValue()))
                    .withHttpStatus(400)
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withCause("Invalid buggi value");
        }
        return new BuggiSelectionEntry(option.id(), option.name(), option.subfieldCode(), option.requiresNonzeroValue(), request.getValue());
    }

    private record Option(int id, String group, String subfieldCode, boolean requiresNonzeroValue, String name) {
    }
}
