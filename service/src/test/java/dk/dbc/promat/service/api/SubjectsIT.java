package dk.dbc.promat.service.api;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.SubjectList;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;


public class SubjectsIT extends ContainerTest {

    private final JSONBContext jsonbContext = new JSONBContext();
    private static final Logger LOGGER = LoggerFactory.getLogger(SubjectsIT.class);


    @Test
    public void test_referential_hierarchy() throws JSONBException {
        SubjectList actual = jsonbContext
                .unmarshall(get(String.format("%s/%s", PROMATSERVICE_BASE_URL, "v1/api/subjects")), SubjectList.class);

        // Must contain 5 mock subjects with id 1-6 for testing and 475 production values from id 57 and up
        assertThat("Service returns all persisted subjects", actual.getSubjects().size(), is(484));

        // Check a few production values
        assertThat("contains subject 'Voksen'", (int) actual.getSubjects().stream()
                .filter(s -> s.getId() == 57 && Objects.equals(s.getName(), "Voksen")).count(), is(1));
        assertThat("contains subject '26 Kirkens institutioner'", (int) actual.getSubjects().stream()
                .filter(s -> s.getId() == 357 && Objects.equals(s.getName(), "26 Kirkens institutioner")).count(), is(1));
        assertThat("contains subject 'Fagfilm/børn'", (int) actual.getSubjects().stream()
                .filter(s -> s.getId() == 1691 && Objects.equals(s.getName(), "Fagfilm/børn")).count(), is(1));
    }
}
