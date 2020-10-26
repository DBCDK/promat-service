package dk.dbc.promat.service.rest;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.SubjectList;
import dk.dbc.promat.service.entity.Subject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;


public class SubjectsIT extends ContainerTest {

    private final JSONBContext jsonbContext = new JSONBContext();
    private static final Logger LOGGER = LoggerFactory.getLogger(SubjectsIT.class);

    @BeforeAll
    private static void setUp() throws SQLException, IOException, URISyntaxException {
        Connection connection = connectToPromatDB();
        executeScript(connection, SubjectsIT.class.getResource("/dk/dbc/promat/service/db/subjects/subjectsdump.sql"));
    }

    @Test
    public void test_referential_hierarchy() throws JSONBException {
        SubjectList expected = new SubjectList().withSubjects(
                List.of(
                        new Subject().withId(1).withName("Voksen"),
                        new Subject().withId(2).withName("Roman").withParentId(1),
                        new Subject().withId(3).withName("Eventyr, fantasy").withParentId(2),
                        new Subject().withId(4).withName("Digte").withParentId(1)
                ));

        SubjectList actual = jsonbContext
                .unmarshall(get(String.format("%s/%s", promatServiceBaseUrl, "v1/api/subjects")), SubjectList.class);
        assertThat("Service returns all persisted subjects", actual.toString(), is(expected.toString()));
    }

    public String get(String uri) {
        final Response response = new HttpGet(httpClient)
                .withBaseUrl(uri)
                .execute();
        return response.readEntity(String.class);
    }
}

