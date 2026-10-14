package dk.dbc.promat.service.cataloging;

import dk.dbc.promat.service.taskdata.BuggiSelectionEntry;
import dk.dbc.promat.service.taskdata.MetakompasTaskData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CatalogingMarcMapperTest {
    private final CatalogingMarcMapper mapper = new CatalogingMarcMapper();

    @Test
    public void writesSubjectsAndSuggestionsInOneWorkField() {
        MetakompasTaskData selection = new MetakompasTaskData()
                .withEntries(List.of(
                        new MetakompasTaskData.Entry(List.of("ramme", "genre"), 1, "krimi", null, null, null),
                        new MetakompasTaskData.Entry(List.of("handling", "handler om"), 2, "venskab", null, null, null),
                        new MetakompasTaskData.Entry(List.of("fortælleteknik", "tempo"), 3, "højt", null, null, null)))
                .withSuggestions(List.of(new MetakompasTaskData.Suggestion(
                        List.of("ramme", "genre"), "cozy crime; nordic noir")));

        String marc = mapper.metakompasToMarc("870970", "12345678", selection);

        assertThat(marc, containsString("<marcx:subfield code=\"a\">12345678</marcx:subfield>"));
        assertThat(marc, containsString("<marcx:subfield code=\"b\">870970</marcx:subfield>"));
        // As metakompasset: one 665 for the work, subfields sorted by code (*& first), suggestions before the
        // selected subjects with the same code.
        // A suggestion is one value, also when it contains ";"
        assertThat(marc, containsString(field665("&amp;", "lektor", "e", "venskab", "g", "cozy crime; nordic noir",
                "g", "krimi", "t", "højt")));
        assertThat(count665(marc), is(1));
    }

    @Test
    public void writesMoodsAndNamedMainCharacterInOwnFieldsBeforeTheWorkField() {
        MetakompasTaskData selection = new MetakompasTaskData()
                .withEntries(List.of(
                        new MetakompasTaskData.Entry(List.of("stemning", "positiv"), 1, "hyggelig", null, null, null),
                        new MetakompasTaskData.Entry(List.of("ramme", "genre"), 4, "krimi", null, null, null),
                        new MetakompasTaskData.Entry(List.of("stemning", "positiv"), 2, "tryg", null, null, null),
                        new MetakompasTaskData.Entry(List.of("stemning", "humoristisk"), 3, "fjollet", null, null, null)))
                .withSuggestions(List.of(new MetakompasTaskData.Suggestion(
                        List.of("handling", "navngivet hovedperson"), "Anders And")));

        String marc = mapper.metakompasToMarc("870970", "12345678", selection);

        // As metakompasset: a 665 per mood/named main character with its category in *&, in the order they're
        // met (suggestions first), then the work field with the remaining subjects - subfields sorted by code
        String expected = field665("&amp;", "navngivet hovedperson", "&amp;", "lektor", "v", "Anders And")
                + field665("&amp;", "positiv", "&amp;", "lektor", "n", "hyggelig")
                + field665("&amp;", "positiv", "&amp;", "lektor", "n", "tryg")
                + field665("&amp;", "humoristisk", "&amp;", "lektor", "n", "fjollet")
                + field665("&amp;", "lektor", "g", "krimi");
        assertThat(marc, containsString(expected));
        assertThat(count665(marc), is(5));
    }

    @Test
    public void writesNoWorkFieldWithOnlyMoods() {
        MetakompasTaskData selection = new MetakompasTaskData()
                .withEntries(List.of(new MetakompasTaskData.Entry(List.of("stemning", "trist"), 1, "melankolsk", null, null, null)));

        String marc = mapper.metakompasToMarc("870970", "12345678", selection);

        assertThat(count665(marc), is(1));
        assertThat(marc, containsString(field665("&amp;", "trist", "&amp;", "lektor", "n", "melankolsk")));
    }

    @Test
    public void mapsTimeOfActionToSubfieldI() {
        MetakompasTaskData selection = new MetakompasTaskData()
                .withEntries(List.of(
                        new MetakompasTaskData.Entry(List.of("ramme", "handlingens tid udtrykt i ord"), 1, "middelalderen", null, null, null),
                        new MetakompasTaskData.Entry(List.of("Ramme", "Handlingens tid udtrykt i tal "), 2, "1200-tallet", null, null, null)));

        String marc = mapper.metakompasToMarc("870970", "12345678", selection);

        assertThat(marc, containsString(field665("&amp;", "lektor", "i", "middelalderen", "i", "1200-tallet")));
    }

    @Test
    public void rejectsPathWithoutMarcMapping() {
        MetakompasTaskData selection = new MetakompasTaskData()
                .withEntries(List.of(new MetakompasTaskData.Entry(List.of("ramme", "ny kategori"), 1, "noget nyt", null, null, null)));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                mapper.metakompasToMarc("870970", "12345678", selection));

        assertThat(exception.getMessage(), containsString("ny kategori"));
    }

    @Test
    public void mapsBuggiTagsToMarc664AndSkipsZeroValues() throws Exception {
        String marc = mapper.buggiToMarc("870970", "12345678", List.of(
                new BuggiSelectionEntry(1, "let/svær", "s", 3),
                new BuggiSelectionEntry(5, "rar", "n", 0),
                new BuggiSelectionEntry(19, "fantasy", "e", 4)));

        assertThat(marc, containsString("<marcx:subfield code=\"s\">let/svær</marcx:subfield>"));
        assertThat(marc, containsString("<marcx:subfield code=\"e\">fantasy</marcx:subfield>"));
        assertThat(marc, containsString("<marcx:subfield code=\"y\">3</marcx:subfield>"));
        assertThat(marc, containsString("<marcx:subfield code=\"y\">4</marcx:subfield>"));
        assertThat(marc, not(containsString(">rar<")));
    }

    private static int count665(String marc) {
        return marc.split("tag=\"665\"").length - 1;
    }

    // A 665 datafield as MarcXchangeBuilder writes it, from code/value pairs
    private static String field665(String... codesAndValues) {
        StringBuilder field = new StringBuilder("  <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"665\">\n");
        for(int i = 0; i < codesAndValues.length; i += 2) {
            field.append("    <marcx:subfield code=\"").append(codesAndValues[i]).append("\">")
                    .append(codesAndValues[i + 1]).append("</marcx:subfield>\n");
        }
        return field.append("  </marcx:datafield>\n").toString();
    }
}
