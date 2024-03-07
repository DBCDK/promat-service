package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.commons.testcontainers.postgres.AbstractJpaAndRestTestBase;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.EditorList;
import dk.dbc.promat.service.dto.EditorRequest;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.templating.Formatting;

import java.sql.Date;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class EditorsIT extends ContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditorsIT.class);

    @Test
    void getEditor() throws JsonProcessingException {
        final Editor expectedEditor = new Editor();
        expectedEditor.setId(10);
        expectedEditor.setActive(true);
        expectedEditor.setFirstName("Ed");
        expectedEditor.setLastName("Itor");
        expectedEditor.setEmail("ed.itor@dbc.dk");
        expectedEditor.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));

        Response response = getResponse("v1/api/editors/10", "1-2-3-4-5");
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

        final EditorRequest editorRequest = new EditorRequest()
                .withActive(false)
                .withEmail("edito.r@dbc.dk")
                .withFirstName("Edito")
                .withLastName("r");

        final Response response = putResponse("v1/api/editors/12", editorRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        final Editor updated = mapper.readValue(response.readEntity(String.class), Editor.class);

        final Editor expected = new Editor();
        loadUpdatedEditor(expected);

        updated.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
        updated.setDeactivated(null);
        assertThat("Editor has been updated", updated.equals(expected));

        assertThat("auditlog update", promatServiceContainer.getLogs().contains("{\"Update and view full editor profile\":\"editors/12\",\"Response\":\"200\"}"));
    }

    @Test
    void createEditor() throws JsonProcessingException {

        final EditorRequest editorRequest = new EditorRequest()
                .withCprNumber("2201211154")
                .withFirstName("Edi")
                .withLastName("Tore")
                .withEmail("edi.tore@dbc.dk")
                .withPaycode(9999);

        Response response = postResponse("v1/api/editors", editorRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(201));
        final Editor created = mapper.readValue(response.readEntity(String.class), Editor.class);

        final Editor expected = new Editor();
        expected.setId(created.getId());
        loadCreatedEditor(expected);

        assertThat("Editor has been created", created.equals(expected));

        response = getResponse("v1/api/editors/" + created.getId(), "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        final Editor existing = mapper.readValue(response.readEntity(String.class), Editor.class);

        assertThat("Editor has been persisted", created.equals(existing));
    }

    private void loadUpdatedEditor(Editor editor) {
        editor.setId(12);
        editor.setActive(false);
        editor.setCulrId("53");
        editor.setFirstName("Edito");
        editor.setLastName("r");
        editor.setEmail("edito.r@dbc.dk");
        editor.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
        editor.setDeactivated(null);
    }

    private void loadCreatedEditor(Editor editor) {
        editor.setActive(true);
        editor.setCulrId("9eb694d3-e734-43f5-b30c-b03d1db1b523");
        editor.setFirstName("Edi");
        editor.setLastName("Tore");
        editor.setEmail("edi.tore@dbc.dk");
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
}
