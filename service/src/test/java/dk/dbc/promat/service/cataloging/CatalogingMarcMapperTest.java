package dk.dbc.promat.service.cataloging;

import dk.dbc.promat.service.taskdata.BuggiSelectionEntry;
import dk.dbc.promat.service.taskdata.MetakompasTaskData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
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
        assertThat(marc, containsString("<marcx:subfield code=\"g\">cozy crime</marcx:subfield>"));
        assertThat(marc, containsString("<marcx:subfield code=\"g\">nordic noir</marcx:subfield>"));
        assertThat(marc, containsString("<marcx:subfield code=\"&amp;\">lektor</marcx:subfield>"));
    }

    @Test
    public void mapsBuggiTagsToMarc664AndSkipsZeroValuedMoodAndThemeTags() throws Exception {
        String marc = mapper.buggiToMarc("870970", "12345678", List.of(
                new BuggiSelectionEntry(1, "let/svær", "s", false, 0),
                new BuggiSelectionEntry(5, "rar", "n", true, 0),
                new BuggiSelectionEntry(19, "fantasy", "e", true, 4)));

        assertThat(marc, containsString("<marcx:subfield code=\"s\">let/svær</marcx:subfield>"));
        assertThat(marc, containsString("<marcx:subfield code=\"e\">fantasy</marcx:subfield>"));
        assertThat(marc, containsString("<marcx:subfield code=\"y\">4</marcx:subfield>"));
        assertThat(marc, not(containsString(">rar<")));
    }
}
