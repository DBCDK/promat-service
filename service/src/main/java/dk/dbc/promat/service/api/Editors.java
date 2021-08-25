/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import dk.dbc.connector.culr.CulrConnectorException;
import dk.dbc.promat.service.dto.EditorList;
import dk.dbc.promat.service.dto.EditorRequest;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.PromatEntityManager;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.persistence.TypedQuery;
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

    @Inject
    CulrHandler culrHandler;

    @POST
    @Path("editors")
    @Produces({MediaType.APPLICATION_JSON})
    public Response createEditor(EditorRequest editorRequest) throws CulrConnectorException {
        LOGGER.info("editors (POST)");

        final String firstName = editorRequest.getFirstName();
        if ( firstName == null || firstName.isBlank()) {
            return ServiceErrorDto.InvalidRequest("Missing required field in the request data",
                    "Field 'firstName' must be supplied and not be blank when creating a new reviewer");
        }

        final String lastName = editorRequest.getLastName();
        if ( lastName == null || lastName.isBlank()) {
            return ServiceErrorDto.InvalidRequest("Missing required field in the request data",
                    "Field 'lastName' must be supplied and not be blank when creating a new reviewer");
        }

        final String email = editorRequest.getEmail();
        if ( email == null || email.isBlank()) {
            return ServiceErrorDto.InvalidRequest("Missing required field in the request data",
                    "Field 'email' must be supplied and not be blank when creating a new reviewer");
        }

        final String cprNumber = editorRequest.getCprNumber();
        if (cprNumber == null || cprNumber.isBlank()) {
            return ServiceErrorDto.InvalidRequest("Missing required field in the request data",
                    "Field 'cprNumber' must be supplied when creating a new reviewer");
        }

        final Integer paycode = editorRequest.getPaycode();
        if (paycode == null) {
            return ServiceErrorDto.InvalidRequest("Missing required field in the request data",
                    "Field 'paycode' must be supplied when creating a new reviewer");
        }

        final String culrId;
        try {
            culrId = culrHandler.createCulrAccount(cprNumber, String.valueOf(paycode));
            LOGGER.info("Obtained CulrId {} for new editor", culrId);
        } catch (ServiceErrorException e) {
            return Response.status(500).entity(e.getServiceErrorDto()).build();
        }

        try {
            final Editor entity = new Editor()
                    .withActive(editorRequest.isActive() != null ? editorRequest.isActive() : true)  // New users defaults to active
                    .withFirstName(firstName)
                    .withLastName(lastName)
                    .withEmail(email);

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

    @GET
    @Path("editors")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAllEditors() {
        try {
            TypedQuery<Editor> q = entityManager.createNamedQuery(Editor.GET_ALL_EDITORS, Editor.class);
            final List<Editor> editors = q.getResultList();
            return Response.ok().entity(new EditorList<>().withEditors(editors)).build();
        } catch (Exception e) {
            return ServiceErrorDto.Failed(e.getMessage());
        }
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
                editor.setActiveChanged(Date.from(Instant.now()));
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
