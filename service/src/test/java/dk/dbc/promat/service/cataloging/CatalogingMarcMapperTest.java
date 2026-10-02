package dk.dbc.promat.service.cataloging;

import dk.dbc.promat.service.taskdata.BuggiSelectionEntry;
import dk.dbc.promat.service.taskdata.MetakompasTaskData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class CatalogingMarcMapperTest {
    private final CatalogingMarcMapper mapper = new CatalogingMarcMapper();

    @Test
    public void mapsMetakompasEntriesAndSuggestionsToMarc665() throws Exception {
        MetakompasTaskData selection = new MetakompasTaskData()
                .withEntries(List.of(new MetakompasTaskData.Entry(
                        List.of("ramme", "genre"), 1, "krimi", null, null, null)))
                .withSuggestions(List.of(new MetakompasTaskData.Suggestion(
                        List.of("ramme", "genre"), "cozy crime; nordic noir")));

        String marc = mapper.metakompasToMarc("870970", "12345678", selection);

        assertThat(marc, containsString("<marcx:subfield code=\"a\">12345678</marcx:subfield>"));
        assertThat(marc, containsString("<marcx:subfield code=\"b\">870970</marcx:subfield>"));
        assertThat(marc, containsString("<marcx:subfield code=\"g\">krimi</marcx:subfield>"));
        // A suggestion is one value, also when it contains ";"
        assertThat(marc, containsString("<marcx:subfield code=\"g\">cozy crime; nordic noir</marcx:subfield>"));
        assertThat(marc, containsString("<marcx:subfield code=\"&amp;\">lektor</marcx:subfield>"));
    }

    @Test
    public void writesMoodsAndNamedMainCharacterInOwnFieldsWithCategory() {
        MetakompasTaskData selection = new MetakompasTaskData()
                .withEntries(List.of(
                        new MetakompasTaskData.Entry(List.of("stemning", "positiv"), 1, "hyggelig", null, null, null),
                        new MetakompasTaskData.Entry(List.of("stemning", "positiv"), 2, "tryg", null, null, null),
                        new MetakompasTaskData.Entry(List.of("stemning", "humoristisk"), 3, "fjollet", null, null, null),
                        new MetakompasTaskData.Entry(List.of("ramme", "genre"), 4, "krimi", null, null, null)))
                .withSuggestions(List.of(new MetakompasTaskData.Suggestion(
                        List.of("handling", "navngivet hovedperson"), "Anders And")));

        String marc = mapper.metakompasToMarc("870970", "12345678", selection);

        // As in records registered by metakompasset: one 665 per mood/named main character, category in *&
        // (MarcXchangeBuilder writes subfields sorted by code, so *& comes first)
        assertThat(marc, containsString(field665("&amp;", "positiv", "&amp;", "lektor", "n", "hyggelig")));
        assertThat(marc, containsString(field665("&amp;", "positiv", "&amp;", "lektor", "n", "tryg")));
        assertThat(marc, containsString(field665("&amp;", "humoristisk", "&amp;", "lektor", "n", "fjollet")));
        assertThat(marc, containsString(field665("&amp;", "navngivet hovedperson", "&amp;", "lektor", "v", "Anders And")));
        // Other subjects keep their shared field
        assertThat(marc, containsString(field665("&amp;", "lektor", "g", "krimi")));
        assertThat(marc.split("tag=\"665\"").length - 1, is(5));
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
