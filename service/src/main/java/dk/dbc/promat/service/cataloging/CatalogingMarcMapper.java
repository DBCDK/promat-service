package dk.dbc.promat.service.cataloging;

import dk.dbc.promat.service.taskdata.BuggiSelectionEntry;
import dk.dbc.promat.service.taskdata.MetakompasTaskData;
import dk.dbc.promat.service.taxonomy.dto.PathTranslator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.dbc.promat.service.cataloging.MarcXchangeBuilder.subfield;

public class CatalogingMarcMapper {
    // Metakompas category path to MARC 665 subfield: the first letter of PathTranslator's code, lower case, which maps the
    // other way, from 665 subfields to the taxonomy's categories (I0/I1 = *i, na..ni = the moods in *n)
    private static final Map<List<String>, String> METAKOMPAS_PATH_TO_SUBFIELD = Arrays.stream(PathTranslator.values())
            .collect(Collectors.toMap(PathTranslator::getPathValue, code -> code.name().substring(0, 1).toLowerCase(Locale.ROOT)));

    // Subfields metakompasset writes in a 665 of their own, with their category: moods and named main character
    private static final Set<String> SUBFIELDS_REQUIRING_OWN_FIELD = Set.of("n", "v");

    public String metakompasToMarc(String libraryId, String localIdentifier, MetakompasTaskData selection) {
        MarcXchangeBuilder builder = baseRecord(libraryId, localIdentifier);
        // Mirrored from metakompasset's toDanMARC2: one 665 for the work holding every selected subject and
        // suggestion, except moods and the named main character, which each get a 665 of their own with their
        // category in *& (SUBFIELD_REQUIRES_OWN_FIELD). The own fields come before the work field. Subfields are
        // sorted by code when written, so suggestions (metakompasset's "comments") only come before the selected
        // subjects among subfields with the same code. Each suggestion is one value - Promat's UI takes one word
        // per suggestion, so it isn't split on ";" as in metakompasset.
        List<MarcXchangeBuilder.Subfield> workSubfields = new ArrayList<>();
        for(MetakompasTaskData.Suggestion suggestion : selection.getSuggestions() == null ? List.<MetakompasTaskData.Suggestion>of() : selection.getSuggestions()) {
            addMetakompasValue(builder, workSubfields, suggestion.path(), suggestion.title());
        }
        for(MetakompasTaskData.Entry entry : selection.getEntries() == null ? List.<MetakompasTaskData.Entry>of() : selection.getEntries()) {
            addMetakompasValue(builder, workSubfields, entry.path(), entry.title());
        }
        if(!workSubfields.isEmpty()) {
            workSubfields.add(subfield("&", "lektor"));
            builder.addField("665", workSubfields);
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

    private void addMetakompasValue(MarcXchangeBuilder builder, List<MarcXchangeBuilder.Subfield> workSubfields,
                                    List<String> path, String value) {
        if(value == null || value.isBlank()) {
            return;
        }
        String subfield = METAKOMPAS_PATH_TO_SUBFIELD.get(normalizePath(path));
        if(subfield == null) {
            throw new IllegalArgumentException("No Metakompas MARC mapping for path: " + path);
        }
        if(SUBFIELDS_REQUIRING_OWN_FIELD.contains(subfield)) {
            // The category is the path's second level, as in metakompasset: "positiv" for stemning->positiv
            builder.addField("665", List.of(
                    subfield(subfield, value.trim()),
                    subfield("&", path.get(1).trim()),
                    subfield("&", "lektor")));
        } else {
            workSubfields.add(subfield(subfield, value.trim()));
        }
    }

    private List<String> normalizePath(List<String> path) {
        return path == null ? List.of() : path.stream()
                .map(level -> level == null ? "" : level.toLowerCase(Locale.ROOT).trim())
                .toList();
    }
}
