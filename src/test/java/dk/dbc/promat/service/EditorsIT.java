/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service;

import dk.dbc.promat.service.persistence.Editor;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class EditorsIT extends ContainerTest {

    @Test
    void getEditor() {
        final Editor expectedEditor = new Editor();
        expectedEditor.setId(10);
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
}
