/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.EditorRequest;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@Path("")
public class Editors {
    private static final Logger LOGGER = LoggerFactory.getLogger(Editors.class);

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @GET
    @Path("editors/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getEditor(@PathParam("id") Integer id) {
        final Editor editor = entityManager.find(Editor.class, id);
        if (editor == null) {
            return Response.status(404).build();
        }
        return Response.ok(editor).build();
    }

    @PUT
    @Path("editors/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateEditor(@PathParam("id") final Integer id, EditorRequest editorRequest) {
        LOGGER.info("editors/{} (PUT)", id);

        try {

            // Find the existing user
            final Editor editor = entityManager.find(Editor.class, id);
            if (editor == null) {
                LOGGER.info("Editor with id {} does not exists", id);
                return Response.status(404).build();
            }

            // Update by patching
            if(editorRequest.isActive() != null) {
                editor.setActive(editorRequest.isActive());
            }
            if(editorRequest.getEmail() != null) {
                editor.setEmail(editorRequest.getEmail());
            }
            if(editorRequest.getFirstName() != null) {
                editor.setFirstName(editorRequest.getFirstName());
            }
            if(editorRequest.getLastName() != null) {
                editor.setLastName(editorRequest.getLastName());
            }

            return Response.status(200)
                    .entity(editor)
                    .build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

}
