package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.EditorList;
import dk.dbc.promat.service.dto.EditorRequest;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.templating.Formatting;

import java.sql.Date;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

class EditorsIT extends ContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditorsIT.class);

    @Test
    void getEditor() throws JsonProcessingException {
        final Editor expectedEditor = new Editor();
        expectedEditor.setId(10);
        expectedEditor.setCulrId("51");
        expectedEditor.setActive(true);
        expectedEditor.setFirstName("Ed");
        expectedEditor.setLastName("Itor");
        expectedEditor.setEmail("ed.itor@dbc.dk");
        expectedEditor.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
        expectedEditor.setAgency("790900");
        expectedEditor.setUserId("jcba");

        Response response = getResponse("v1/api/editors/10", "1-2-3-4-5");
        final Editor actual = mapper.readValue(response.readEntity(String.class), Editor.class);
        actual.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
        actual.setDeactivated(null);
        assertThat(actual, is(expectedEditor));
    }

    @Test
    void getEditorByProfessionalLogin() throws JsonProcessingException {
        final Editor expectedEditor = new Editor();
        expectedEditor.setId(13);
        expectedEditor.setCulrId("klnp");
        expectedEditor.setActive(true);
        expectedEditor.setFirstName("Editte");
        expectedEditor.setLastName("Ore");
        expectedEditor.setEmail("editte.ore@dbc.dk");
        expectedEditor.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
        expectedEditor.setAgency("790900");
        expectedEditor.setUserId("klnp");

        Response response = getResponse("v1/api/editors/13", "2-3-4-5-6");
        final Editor actual = mapper.readValue(response.readEntity(String.class), Editor.class);
        actual.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
        actual.setDeactivated(null);
        assertThat(actual, is(expectedEditor));
    }

    @Test
    void getAllEditors() throws JsonProcessingException {
        Response response = getResponse("v1/api/editors");

        EditorList<Editor> editors = mapper.readValue(response.readEntity(String.class), EditorList.class);

        long active = 0;
        long inActive = 0;
        for (Editor editor : editors.getEditors()) {
            if (editor.isActive()) {
                active += 1;
            } else {
                inActive += 1;
            }
        }
        // More than five active editors.
        // Less than two inactive.
        assertThat("Active editors", active >= 4);
        assertThat("Inactive editors", inActive <= 2);
    }

    @Test
    void editorNotFound() {
        final Response response = getResponse("v1/api/editors/4242", "1-2-3-4-5");

        assertThat(response.getStatus(), is(404));
    }

    @Test
    void updateEditor() throws JsonProcessingException {

        // Check that we have the expected start-state
        final Editor expectedEditor = new Editor();
        expectedEditor.setId(12);
        expectedEditor.setCulrId("53");
        expectedEditor.setActive(true);
        expectedEditor.setFirstName("Edi");
        expectedEditor.setLastName("tor");
        expectedEditor.setEmail("edi.tor@dbc.dk");
        expectedEditor.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
        expectedEditor.setAgency("097900");
        expectedEditor.setUserId("toredi");

        Response response = getResponse("v1/api/editors/12", "1-2-3-4-5");
        final Editor actual = mapper.readValue(response.readEntity(String.class), Editor.class);
        actual.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
        actual.setDeactivated(null);
        assertThat(actual, is(expectedEditor));

        // Change editor values
        final EditorRequest editorRequest = new EditorRequest()
                .withActive(false)
                .withEmail("edito.r@dbc.dk")
                .withFirstName("Edito")
                .withLastName("r")
                .withAgency("790900")
                .withUserId("editor");

        response = putResponse("v1/api/editors/12", editorRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        final Editor updated = mapper.readValue(response.readEntity(String.class), Editor.class);

        // Change expected updated-state
        expectedEditor.setActive(false);
        expectedEditor.setFirstName("Edito");
        expectedEditor.setLastName("r");
        expectedEditor.setEmail("edito.r@dbc.dk");
        expectedEditor.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
        expectedEditor.setAgency("790900");
        expectedEditor.setUserId("editor");

        // Verify correct update
        updated.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
        updated.setDeactivated(null);
        assertThat(updated, is(expectedEditor));

        // Check that we got an auditlog entry
        assertThat("auditlog update", promatServiceContainer.getLogs().contains("{\"Update and view full editor profile\":\"editors/12\",\"Response\":\"200\"}"));
    }

    @Test
    void updateEditorByProfessionalLogin() throws JsonProcessingException {

        // Request is a no-change, we just need to check that we pass authentication
        final EditorRequest editorRequest = new EditorRequest()
                .withFirstName("Editte")
                .withLastName("Ore");

        Response response = putResponse("v1/api/editors/13", editorRequest, "2-3-4-5-6");
        assertThat("response status", response.getStatus(), is(200));

        // Check that we got an auditlog entry
        assertThat("auditlog update", promatServiceContainer.getLogs().contains("{\"Update and view full editor profile\":\"editors/13\",\"Response\":\"200\"}"));
    }

    @Test
    void createEditor() throws JsonProcessingException {

        final EditorRequest editorRequest = new EditorRequest()
                .withFirstName("Edi")
                .withLastName("Tore")
                .withEmail("edi.tore@dbc.dk")
                .withPaycode(9999);

        // First attempt without required agency and userId fields
        Response response = postResponse("v1/api/editors", editorRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(400));

        // Add cpr, agency and userId fields
        editorRequest.setCprNumber("2201211154");
        editorRequest.setAgency("790900");
        editorRequest.setUserId("etre");

        response = postResponse("v1/api/editors", editorRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(201));
        final Editor created = mapper.readValue(response.readEntity(String.class), Editor.class);

        // Check that the editor was created with all values
        final Editor expected = new Editor();
        expected.setId(created.getId());
        expected.setActive(true);
        expected.setCulrId("9eb694d3-e734-43f5-b30c-b03d1db1b523");
        expected.setFirstName("Edi");
        expected.setLastName("Tore");
        expected.setEmail("edi.tore@dbc.dk");
        expected.setAgency("790900");
        expected.setUserId("etre");
        assertThat(created, is(expected));

        response = getResponse("v1/api/editors/" + created.getId(), "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        final Editor existing = mapper.readValue(response.readEntity(String.class), Editor.class);

        // Check that the editor has been persisted and has all values
        assertThat(created, is(existing));
        assertThat(existing, is(expected));
    }

    @Test
    public void testEditorFormat() {
        Editor editor = new Editor();
        String actual = Formatting.format(editor);
        assertThat("name is correct formatted", actual.equals(""));

        editor = new Editor()
                .withFirstName("Hans Ole Erik");
        actual = Formatting.format(editor);
        assertThat("name is correct formatted", actual.equals("Hans Ole Erik"));

        editor = new Editor()
                .withLastName("Petersen Sørensen");
        actual = Formatting.format(editor);
        assertThat("name is correct formatted", actual.equals("Petersen Sørensen"));

        editor = new Editor()
                .withFirstName("Hans Ole Erik")
                .withLastName("Petersen Sørensen");
        actual = Formatting.format(editor);
        assertThat("name is correct formatted", actual.equals("Hans Ole Erik Petersen Sørensen"));

        editor = new Editor()
                .withFirstName("Hans")
                .withLastName("Hansen");
        actual = Formatting.format(editor);
        assertThat("name is correct formatted", actual.equals("Hans Hansen"));
    }

    @Test
    void getEditorWithInactiveAuthToken() {
        final Response response = getResponse("v1/api/reviewers/13", "6-7-8-9-0");
        assertThat("response status", response.getStatus(), is(401));

        // Example log entry:
        // [docker-java-stream--208115991] INFO dk.dbc.promat.service.ContainerTest - STDOUT: 14:14:47.700 [INFO] [http-thread-pool::http-listener(18)] dk.dbc.commons.rs.auth.DBCAuthenticationMechanism - Token is invalid: 6-7-8-9-0
        assertThat("rs-auth log entry", promatServiceContainer.getLogs().contains("Session 6-7-8-9-0 is not active"));
    }
}
