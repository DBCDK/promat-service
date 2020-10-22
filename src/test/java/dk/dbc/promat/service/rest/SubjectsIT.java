package dk.dbc.promat.service.rest;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.SubjectList;
import dk.dbc.promat.service.entity.Subject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@TestMethodOrder(MethodOrderer.Alphanumeric.class)
public class SubjectsIT extends ContainerTest {
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
    public void A_test_referential_hierarchy() throws SQLException, JSONBException {
        SubjectList expected = new SubjectList().withSubjects(persistTestData());

        SubjectList actual = jsonbContext
                .unmarshall(get(String.format("%s/%s", promatServiceBaseUrl, "v1/api/subjects")), SubjectList.class);
        assertThat("Service returns all persisted subjects", actual.toString(), is(expected.toString()));
    }

    @Test
    public void B_test_exception_when_parentid_does_not_exist() {
        try {
            persistSubject(new Subject().withId(5).withName("Harry Potter like").withParentId(99));
        } catch (SQLException e) {
            assertThat("Message is: 'non-existing parent'", e.getMessage().contains("Detail: Key (parentid)=(99) is not present in table \"subject\"."));
            return;
        }
        assert(false);
    }

    @Test
    public void C_test_that_delete_of_parent_subject_is_not_allowed() throws SQLException {
        try {
            deleteSubject(new Subject().withId(0));
        } catch (SQLException e) {
            assertThat("Message is: 'cannot be deleted'",
                    e.getMessage().contains("Detail: Key (id)=(0) is still referenced from table \"subject\"."));
            return;
        }
        assert(false);

    }

    private List<Subject> persistTestData() throws SQLException {
        return List.of(
                persistSubject(new Subject().withId(1).withName("Voksen").withParentId(0)),
                persistSubject(new Subject().withId(2).withName("Roman").withParentId(1)),
                persistSubject(new Subject().withId(3).withName("Eventyr, fantasy").withParentId(2)),
                persistSubject(new Subject().withId(4).withName("Digte").withParentId(1)));
    }

    private Subject persistSubject(Subject subject) throws SQLException {
        preparedInsertStatement.setLong(1, subject.getId());
        preparedInsertStatement.setString(2, subject.getName());
        preparedInsertStatement.setLong(3, subject.getParentId());
        preparedInsertStatement.executeUpdate();
        return subject;
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

