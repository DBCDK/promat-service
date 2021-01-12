/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.dto.EditorRequest;
import dk.dbc.promat.service.dto.ReviewerRequest;
import dk.dbc.promat.service.persistence.Address;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.Subject;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class EditorsIT extends ContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditorsIT.class);

    @Test
    void getEditor() {
        final Editor expectedEditor = new Editor();
        expectedEditor.setId(10);
        expectedEditor.setActive(true);
        expectedEditor.setFirstName("Ed");
        expectedEditor.setLastName("Itor");
        expectedEditor.setEmail("ed.itor@dbc.dk");

        assertThat(get("v1/api/editors/10", Editor.class), is(expectedEditor));
    }

    @Test
    void editorNotFound() {
        final Response response = getResponse("v1/api/editors/4242");

        assertThat(response.getStatus(), is(404));
    }

    @Test
    void updateEditor() throws JsonProcessingException {

        final EditorRequest editorRequest = new EditorRequest()
                .withActive(false)
                .withEmail("edito.r@dbc.dk")
                .withFirstName("Edito")
                .withLastName("r");

        final Response response = putResponse("v1/api/editors/12", editorRequest);
        assertThat("response status", response.getStatus(), is(200));
        final Editor updated = mapper.readValue(response.readEntity(String.class), Editor.class);

        final Editor expected = new Editor();
        loadUpdatedEditor(expected);

        assertThat("Editor has been updated", updated.equals(expected));
    }

    @Test
    void addEditor() throws JsonProcessingException {

        final EditorRequest editorRequest = new EditorRequest()
                .withActive(true)
                .withEmail("edi.tore@dbc.dk")
                .withFirstName("Edi")
                .withLastName("Tore");

        Response response = postResponse("v1/api/editors", editorRequest);
        assertThat("response status", response.getStatus(), is(201));
        final Editor created = mapper.readValue(response.readEntity(String.class), Editor.class);

        final Editor expected = new Editor();
        expected.setId(created.getId());
        loadCreatedEditor(expected);

        assertThat("Editor has been created", created.equals(expected));

        response = getResponse("v1/api/editors/" + created.getId());
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
    }

    private void loadCreatedEditor(Editor editor) {
        editor.setActive(true);
        editor.setCulrId("");
        editor.setFirstName("Edi");
        editor.setLastName("Tore");
        editor.setEmail("edi.tore@dbc.dk");
    }
}
