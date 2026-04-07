package dk.dbc.promat.service.taxonomy;

import com.fasterxml.jackson.databind.SerializationFeature;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.promat.service.taxonomy.dto.PathSubject;
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
            jsonbContext.getObjectMapper().setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
    }

    @Test
    public void simpleTestOfTaxonomyTree() throws JSONBException, IOException {
        Taxonomy actual = new Taxonomy();
        Subject s1 = new Subject()
                .withId(10)
                .withNote("This is a note 1").withTitle("Some title 1");

        Subject s2 = new Subject()
                .withId(11)
                .withNote("Note 2")
                .withTitle("Some title 2");

        actual.put(s1, "ramme", "genre");
        actual.put(s2, "handling", "hovedperson(er) - beskrivelse", "hovedpersonens karaktertræk");

        Taxonomy expected = Taxonomy.of(getResource("/taxonomy/simpletree.json"));
        assertThat(expected.get("ramme", "genre", "Some title 1"), is(s1));
        assertThat(expected.get("handling", "hovedperson(er) - beskrivelse", "hovedpersonens karaktertræk", "Some title 2"), is(s2));

        assertThat(actual, is(expected));

        assertThat(actual.get("ramme", "genre", "Some title 1"), is(s1));
        assertThat(actual.get("handling", "hovedperson(er) - beskrivelse", "hovedpersonens karaktertræk", "Some title 2"), is(s2));

        PathSubject pathSubject = new PathSubject()
                .withPath(List.of("handling", "navngivet hovedperson"));
        pathSubject.withNote("Some note")
                .withId(12)
                .withTitle("Tiny O'Mara");
        actual.put(pathSubject, pathSubject.getPath());

        Subject expectedSubject = new Subject()
                .withId(12)
                .withNote("Some note")
                .withTitle("Tiny O'Mara");

        assertThat(actual.get("handling", "navngivet hovedperson", "Tiny O'Mara"), is(expectedSubject));

        pathSubject = new PathSubject()
                .withPath(List.of("ramme", "handlingens tid udtrykt i tal"));
        pathSubject.withNote("Some note")
                .withId(13)
                .withTitle("1999 - 20000");
        actual.put(pathSubject, pathSubject.getPath());

        expectedSubject = new Subject()
                .withId(13)
                .withNote("Some note")
                .withTitle("1999 - 20000");

        assertThat(actual.get("ramme", "handlingens tid udtrykt i tal", "1999 - 20000"), is(expectedSubject));
    }

}
