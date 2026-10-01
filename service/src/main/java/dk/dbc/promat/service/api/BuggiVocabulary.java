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
    // The value range a reviewer can choose from per group, as in metakompasset: the scales (Læsbarhed,
    // Fantasi/virkelighed) 1-5, moods and themes 0-5. On save, 0 is allowed for every option and means
    // "no value": not chosen for moods and themes, not filled in yet for the scales - so a draft can be
    // saved. 0 is never registered; the scales must have a value of at least minValue before the
    // selection is registered.
    private static final Group READABILITY = new Group("Læsbarhed", "s", 1, 5);
    private static final Group FANTASY_REALITY = new Group("Fantasi/virkelighed", "u", 1, 5);
    private static final Group MOOD = new Group("Stemning", "n", 0, 5);
    private static final Group THEME = new Group("Tema", "e", 0, 5);

    // IDs are part of the API contract used by PUT /tasks/{taskId}/reading-experience/child.
    // Keep existing IDs stable; add new options with new IDs instead of renumbering.
    private static final List<Option> OPTIONS = List.of(
            new Option(1, READABILITY, "let/svær"),
            new Option(2, READABILITY, "tekst/tegninger"),
            new Option(3, READABILITY, "kort/lang"),
            new Option(4, FANTASY_REALITY, "virkelig/fantasi"),
            new Option(5, MOOD, "rar"),
            new Option(6, MOOD, "sjov"),
            new Option(7, MOOD, "romantisk"),
            new Option(8, MOOD, "spændende"),
            new Option(9, MOOD, "trist"),
            new Option(10, MOOD, "uhyggelig"),
            new Option(11, MOOD, "tankevækkende"),
            new Option(12, THEME, "dyr"),
            new Option(13, THEME, "sport"),
            new Option(14, THEME, "venskaber"),
            new Option(15, THEME, "mit liv"),
            new Option(16, THEME, "ud i fremtiden"),
            new Option(17, THEME, "skæve karakterer"),
            new Option(18, THEME, "den store verden"),
            new Option(19, THEME, "fantasy"),
            new Option(20, THEME, "gys"),
            new Option(21, THEME, "eventyrlig"),
            new Option(22, THEME, "action"),
            new Option(23, THEME, "gaming")
    );

    private static final Map<Integer, Option> OPTIONS_BY_ID = OPTIONS.stream()
            .collect(LinkedHashMap::new, (map, option) -> map.put(option.id(), option), Map::putAll);

    private BuggiVocabulary() {
    }

    public static List<BuggiOptionGroup> groups() {
        Map<Group, List<BuggiOption>> optionsByGroup = new LinkedHashMap<>();
        for(Option option : OPTIONS) {
            optionsByGroup.computeIfAbsent(option.group(), group -> new ArrayList<>())
                    .add(new BuggiOption(option.id(), option.name()));
        }
        return optionsByGroup.entrySet().stream()
                .map(entry -> new BuggiOptionGroup(entry.getKey().name(), entry.getKey().subfieldCode(),
                        entry.getKey().minValue(), entry.getKey().maxValue(), List.copyOf(entry.getValue())))
                .toList();
    }

    public static BuggiSelectionEntry resolve(BuggiSelectionRequest request) throws ServiceErrorException {
        Option option = request == null ? null : OPTIONS_BY_ID.get(request.id());
        if(option == null) {
            throw new ServiceErrorException(String.format("Buggi option id %s does not exist", request == null ? null : request.id()))
                    .withHttpStatus(400)
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withCause("Invalid buggi option");
        }
        Group group = option.group();
        // 0 = no value, allowed on save for every group (see the ranges above)
        if(request.value() == null || request.value() < 0 || request.value() > group.maxValue()) {
            throw new ServiceErrorException(String.format("Buggi option %s has invalid value %s - must be 0-%d",
                    request.id(), request.value(), group.maxValue()))
                    .withHttpStatus(400)
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withCause("Invalid buggi value");
        }
        // Fragile due to name and subfieldCode both being String: the record's positional
        // constructor gives the compiler no way to catch the two being swapped here.
        return new BuggiSelectionEntry(option.id(), option.name(), group.subfieldCode(), request.value());
    }

    private record Group(String name, String subfieldCode, int minValue, int maxValue) {
    }

    private record Option(int id, Group group, String name) {
    }
}
