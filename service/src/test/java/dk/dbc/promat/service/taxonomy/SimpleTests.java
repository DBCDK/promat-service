package dk.dbc.promat.service.taxonomy;

import com.fasterxml.jackson.databind.SerializationFeature;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.promat.service.taxonomy.dto.Subject;
import dk.dbc.promat.service.taxonomy.dto.Taxonomy;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class SimpleTests extends TestBase {
    static final JSONBContext jsonbContext = new JSONBContext();
    static {
        jsonbContext.getObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Test
    public void simpleTestOfTaxonomyTree() throws JSONBException, IOException {
        Taxonomy actual = new Taxonomy();
        Subject s1 = new Subject()
                .withId(10)
                .withNote(List.of("This is a note 1")).withTitle("Some title 1");

        Subject s2 = new Subject()
                .withId(11)
                .withNote(List.of("Note 2"))
                .withTitle("Some title 2");

        actual.put(s1, "ramme", "genre");
        actual.put(s2, "handling", "hovedperson(er) - beskrivelse", "hovedpersonens karaktertræk");

        Taxonomy expected = jsonbContext.unmarshall(getResource("/taxonomy/simpletree.json"), Taxonomy.class);
        assertThat(expected.get("ramme", "genre", "Some title 1"), is(s1));
        assertThat(expected.get("handling", "hovedperson(er) - beskrivelse", "hovedpersonens karaktertræk", "Some title 2"), is(s2));

        assertThat(actual, is(expected));

        assertThat(actual.get("ramme", "genre", "Some title 1"), is(s1));
        assertThat(actual.get("handling", "hovedperson(er) - beskrivelse", "hovedpersonens karaktertræk", "Some title 2"), is(s2));

    }
}
