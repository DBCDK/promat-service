/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.EditorRequest;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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

    @POST
    @Path("editors")
    @Produces({MediaType.APPLICATION_JSON})
    public Response createEditor(EditorRequest editorRequest) {
        LOGGER.info("editors (POST)");

        if (editorRequest.getFirstName() == null || editorRequest.getFirstName().isBlank()) {
            return ServiceErrorDto.InvalidRequest("Missing required field in the request data",
                    "Field 'firstName' must be supplied and not be blank when creating a new reviewer");
        }
        if (editorRequest.getLastName() == null || editorRequest.getLastName().isBlank()) {
            return ServiceErrorDto.InvalidRequest("Missing required field in the request data",
                    "Field 'lastName' must be supplied and not be blank when creating a new reviewer");
        }
        if (editorRequest.getEmail() == null || editorRequest.getEmail().isBlank()) {
            return ServiceErrorDto.InvalidRequest("Missing required field in the request data",
                    "Field 'email' must be supplied and not be blank when creating a new reviewer");
        }

        // Todo: Determine how to obtain a culr-id
        final String culrId = "";

        try {
            final Editor entity = new Editor()
                    .withActive(editorRequest.isActive() != null ? editorRequest.isActive() : true)  // New users defaults to active
                    .withFirstName(editorRequest.getFirstName())
                    .withLastName(editorRequest.getLastName())
                    .withEmail(editorRequest.getEmail());

            entity.setCulrId(culrId);

            entityManager.persist(entity);
            entityManager.flush();

            LOGGER.info("Created new editor with ID {} for request {}", entity.getId(), editorRequest);
            return Response.status(201)
                    .entity(entity)
                    .build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

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
