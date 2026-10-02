package dk.dbc.promat.service.cataloging;

import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.taskdata.BuggiSelectionEntry;
import dk.dbc.promat.service.taskdata.MetakompasTaskData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static dk.dbc.promat.service.cataloging.MarcXchangeBuilder.subfield;

public class CatalogingMarcMapper {
    // Metakompas category-path to MARC 665 subfield mapping, based on the existing
    // registration mapping in metakompasset's danmarc2.utils.js.
    private static final Map<String, String> METAKOMPAS_PATH_TO_SUBFIELD = Map.ofEntries(
            Map.entry("ramme->handlingens tid i ord", "i"),
            Map.entry("ramme->handlingens tid i tal", "i"),
            Map.entry("ramme->handlingens tid udtrykt i tal", "i"),
            Map.entry("ramme->handlingens tid udtrykt i ord", "i"),
            Map.entry("ramme->geografisk sted", "q"),
            Map.entry("ramme->fiktivt sted", "p"),
            Map.entry("ramme->miljø", "m"),
            Map.entry("ramme->genre", "g"),
            Map.entry("ramme->univers", "u"),
            Map.entry("handling->handler om", "e"),
            Map.entry("handling->navngivet hovedperson", "v"),
            Map.entry("fortælleteknik->skrivestil og struktur", "s"),
            Map.entry("fortælleteknik->fortællerstemme", "r"),
            Map.entry("fortælleteknik->tempo", "t"),
            Map.entry("stemning->positiv", "n"),
            Map.entry("stemning->humoristisk", "n"),
            Map.entry("stemning->romantisk", "n"),
            Map.entry("stemning->erotisk", "n"),
            Map.entry("stemning->dramatisk", "n"),
            Map.entry("stemning->trist", "n"),
            Map.entry("stemning->uhyggelig", "n"),
            Map.entry("stemning->fantasifuld", "n"),
            Map.entry("stemning->tankevækkende", "n")
    );

    // Subfields metakompasset writes in a 665 of their own, with their category: moods and named main character
    private static final Set<String> SUBFIELDS_REQUIRING_OWN_FIELD = Set.of("n", "v");

    public String toMarc(TaskFieldType taskFieldType, String libraryId, String localIdentifier,
                         MetakompasTaskData metakompasSelectionData, List<BuggiSelectionEntry> buggiEntries) {
        if(taskFieldType == TaskFieldType.READING_EXPERIENCE_ADULT) {
            return metakompasToMarc(libraryId, localIdentifier, metakompasSelectionData);
        }
        if(taskFieldType == TaskFieldType.READING_EXPERIENCE_CHILD) {
            return buggiToMarc(libraryId, localIdentifier, buggiEntries);
        }
        throw new IllegalArgumentException("Unsupported task type: " + taskFieldType);
    }

    public String metakompasToMarc(String libraryId, String localIdentifier, MetakompasTaskData selection) {
        MarcXchangeBuilder builder = baseRecord(libraryId, localIdentifier);
        Map<String, MetakompasField> valuesByPath = new LinkedHashMap<>();

        // Mirrored from metakompasset: selected taxonomy subjects and free-text
        // suggestions for the same category path are merged into the same MARC field.
        // Each suggestion is one value - Promat's UI takes one word per suggestion.
        for(MetakompasTaskData.Entry entry : selection.getEntries() == null ? List.<MetakompasTaskData.Entry>of() : selection.getEntries()) {
            addMetakompasValue(valuesByPath, entry.path(), entry.title());
        }
        for(MetakompasTaskData.Suggestion suggestion : selection.getSuggestions() == null ? List.<MetakompasTaskData.Suggestion>of() : selection.getSuggestions()) {
            addMetakompasValue(valuesByPath, suggestion.path(), suggestion.title());
        }

        for(MetakompasField field : valuesByPath.values()) {
            if(SUBFIELDS_REQUIRING_OWN_FIELD.contains(field.subfield())) {
                // Mirrored from metakompasset (SUBFIELD_REQUIRES_OWN_FIELD): each mood and named main
                // character in a 665 of its own, with its category in *& - e.g. *n hyggelig *& positiv
                for(String value : field.values()) {
                    builder.addField("665", List.of(
                            subfield(field.subfield(), value),
                            subfield("&", field.category()),
                            subfield("&", "lektor")));
                }
                continue;
            }
            List<MarcXchangeBuilder.Subfield> subfields = new ArrayList<>();
            for(String value : field.values()) {
                subfields.add(subfield(field.subfield(), value));
            }
            subfields.add(subfield("&", "lektor"));
            builder.addField("665", subfields);
        }
        return builder.toXml();
    }

    public String buggiToMarc(String libraryId, String localIdentifier, List<BuggiSelectionEntry> entries) {
        MarcXchangeBuilder builder = baseRecord(libraryId, localIdentifier);
        for(BuggiSelectionEntry entry : entries == null ? List.<BuggiSelectionEntry>of() : entries) {
            String subfield = entry.marcSubfieldCode();
            if(subfield == null) {
                throw new IllegalArgumentException("No MARC subfield code for Buggi tag: " + entry.name());
            }
            int value = entry.value() == null ? 0 : entry.value();
            // 0 = no value, never registered
            if(value == 0) {
                continue;
            }
            builder.addField("664", List.of(
                    subfield(subfield, entry.name()),
                    subfield("y", String.valueOf(value))
            ));
        }
        return builder.toXml();
    }

    private MarcXchangeBuilder baseRecord(String libraryId, String localIdentifier) {
        return new MarcXchangeBuilder()
                .addField("001", List.of(
                        subfield("a", localIdentifier),
                        subfield("b", libraryId)))
                .addField("004", List.of(
                        subfield("a", "e"),
                        subfield("r", "n")));
    }

    private void addMetakompasValue(Map<String, MetakompasField> valuesByPath, List<String> path, String value) {
        if(value == null || value.isBlank()) {
            return;
        }
        String normalizedPath = normalizePath(path);
        String subfield = METAKOMPAS_PATH_TO_SUBFIELD.get(normalizedPath);
        if(subfield == null) {
            throw new IllegalArgumentException("No Metakompas MARC mapping for path: " + path);
        }
        // The category is the path's second level, as in metakompasset: "positiv" for stemning->positiv
        String category = path.size() > 1 ? path.get(1).trim() : null;
        valuesByPath.computeIfAbsent(normalizedPath, ignored -> new MetakompasField(subfield, category, new ArrayList<>()))
                .values()
                .add(value.trim());
    }

    private String normalizePath(List<String> path) {
        return path == null ? "" : normalize(String.join("->", path));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private record MetakompasField(String subfield, String category, List<String> values) {}
}
