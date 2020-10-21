package dk.dbc.promat.service.rest;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.entity.Subject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class SubjectsIT extends ContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubjectsIT.class);
    private static PreparedStatement preparedInsertStatement;
    private static PreparedStatement preparedDeleteStatement;
    private final JSONBContext jsonbContext = new JSONBContext();

    @BeforeAll
    private static void setUp() throws SQLException {
        Connection connection = connectToPromatDB();
        preparedInsertStatement = connection
                .prepareStatement("INSERT INTO subject(id, name, parentid) VALUES (?,?,?)");
        preparedDeleteStatement = connection
                .prepareStatement("DELETE FROM subject WHERE id=?");
    }

    @Test
    @Order(1)
    public void test_referential_hierarchy() throws SQLException, JSONBException {
        Set<String> expected = persistTestData();

        Set<String> actual =
                Arrays.stream(jsonbContext
                .unmarshall(get(String.format("%s/%s", promatServiceBaseUrl, "v1/api/subjects")), Subject[].class))
                .map(Object::toString).collect(Collectors.toSet());
        assertThat("Service returns all persisted subjects", actual, is(expected));
    }

    @Test
    @Order(2)
    public void test_exception_when_parentid_does_not_exist() {
        try {
            persistSubject(new Subject().withId(4).withName("Harry Potter like").withParentId(99));
        } catch (SQLException e) {
            assertThat("Message is: 'non-existing parent'", e.getMessage().contains("Unknown parentId:99"));
            return;
        }
        assert(false);
    }

    @Test
    @Order(3)
    public void test_that_delete_of_parent_subject_is_not_allowed() throws SQLException {
        persistTestData();
        try {
            deleteSubject(new Subject().withId(0));
        } catch (SQLException e) {
            assertThat("Message is: 'cannot be deleted'",
                    e.getMessage().contains("Remove child subjects, before deleting subject with id: 0"));
            return;
        }
        assert(false);

    }

    private Set<String> persistTestData() throws SQLException {
        return Set.of(
                persistSubject(new Subject().withId(0).withName("Voksen")),
                persistSubject(new Subject().withId(1).withName("Roman").withParentId(0)),
                persistSubject(new Subject().withId(2).withName("Eventyr, fantasy").withParentId(1)),
                persistSubject(new Subject().withId(3).withName("Digte").withParentId(0)));

    }

    private String persistSubject(Subject subject) throws SQLException {
        preparedInsertStatement.setLong(1, subject.getId());
        preparedInsertStatement.setString(2, subject.getName());
        preparedInsertStatement.setLong(3, subject.getParentId());
        preparedInsertStatement.executeUpdate();
        return subject.toString();
    }

    private void deleteSubject(Subject subject) throws SQLException {
        preparedDeleteStatement.setLong(1, subject.getId());
        preparedDeleteStatement.executeUpdate();
    }

    public String get(String uri) {
        final Response response = new HttpGet(httpClient)
                .withBaseUrl(uri)
                .execute();
        return response.readEntity(String.class);
    }
}

