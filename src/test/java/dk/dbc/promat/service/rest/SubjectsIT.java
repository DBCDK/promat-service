package dk.dbc.promat.service.rest;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.SubjectList;
import dk.dbc.promat.service.persistence.Subject;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;


public class SubjectsIT extends ContainerTest {

    private final JSONBContext jsonbContext = new JSONBContext();
    private static final Logger LOGGER = LoggerFactory.getLogger(SubjectsIT.class);


    @Test
    public void test_referential_hierarchy() throws JSONBException {
        SubjectList expected = new SubjectList().withSubjects(
                List.of(
                        new Subject().withId(1).withName("Voksen"),
                        new Subject().withId(2).withName("Roman").withParentId(1),
                        new Subject().withId(3).withName("Eventyr, fantasy").withParentId(2),
                        new Subject().withId(4).withName("Digte").withParentId(1),
                        new Subject().withId(5).withName("Multimedie")
                ));

        SubjectList actual = jsonbContext
                .unmarshall(get(String.format("%s/%s", promatServiceBaseUrl, "v1/api/subjects")), SubjectList.class);
        assertThat("Service returns all persisted subjects", actual.toString(), is(expected.toString()));
    }
}

