package dk.dbc.promat.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.promat.service.dto.EditorList;
import dk.dbc.promat.service.dto.EditorRequest;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.EditorView;
import dk.dbc.promat.service.persistence.JsonMapperProvider;
import dk.dbc.promat.service.persistence.PromatEntityManager;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Stateless
@Path("")
public class Editors {
    private static final Logger LOGGER = LoggerFactory.getLogger(Editors.class);

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @Inject
    AuditLogHandler auditLogHandler;

    @POST
    @Path("editors")
    @Produces({MediaType.APPLICATION_JSON})
    @RolesAllowed({"authenticated-user"})
    public Response createEditor(EditorRequest editorRequest, @Context UriInfo uriInfo) {
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

        final Integer paycode = editorRequest.getPaycode();
        if (paycode == null) {
            return ServiceErrorDto.InvalidRequest("Missing required field in the request data",
                    "Field 'paycode' must be supplied when creating a new reviewer");
        }

        final String agency = editorRequest.getAgency();
        if ( agency == null || agency.isBlank()) {
            return ServiceErrorDto.InvalidRequest("Missing required field in the request data",
                    "Field 'agency' must be supplied and not be blank when creating a new reviewer");
        }

        final String userId = editorRequest.getUserId();
        if ( userId == null || userId.isBlank()) {
            return ServiceErrorDto.InvalidRequest("Missing required field in the request data",
                    "Field 'userId' must be supplied and not be blank when creating a new reviewer");
        }

        try {
            final Editor entity = new Editor()
                    .withActive(editorRequest.isActive() == null || editorRequest.isActive())  // New users defaults to active
                    .withFirstName(firstName)
                    .withLastName(lastName)
                    .withEmail(email)
                    .withAgency(agency)
                    .withUserId(userId);

            entityManager.persist(entity);
            entityManager.flush();

            auditLogHandler.logTraceCreateForToken("Created new editor", uriInfo, entity.getId(), 201);
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
    @RolesAllowed({"authenticated-user"})
    public Response getEditor(@PathParam("id") Integer id, @Context UriInfo uriInfo) {
        final Editor editor = entityManager.find(Editor.class, id);
        if (editor == null) {
            auditLogHandler.logTraceReadForToken("Request for full profile", uriInfo, 0, 404);
            return Response.status(404).build();
        }
        auditLogHandler.logTraceReadForToken("View full editor profile", uriInfo, editor.getId(), 200);
        return Response.ok(editor).build();
    }

    @GET
    @Path("editors")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAllEditors() {
        final ObjectMapper objectMapper = new JsonMapperProvider().getObjectMapper();

        try {
            TypedQuery<Editor> q = entityManager.createNamedQuery(Editor.GET_ALL_EDITORS, Editor.class);
            final List<Editor> editors = q.getResultList();
            return Response.ok(objectMapper.writerWithView(EditorView.Summary.class).writeValueAsString(new EditorList<>().withEditors(editors))).build();
        } catch (Exception e) {
            return ServiceErrorDto.Failed(e.getMessage());
        }
    }

    @PUT
    @Path("editors/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    @RolesAllowed({"authenticated-user"})
    public Response updateEditor(@PathParam("id") final Integer id, EditorRequest editorRequest, @Context UriInfo uriInfo) {
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
                if( editorRequest.isActive() ) {
                    editor.setDeactivated(null);
                }
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
            if(editorRequest.getAgency() != null) {
                editor.setAgency(editorRequest.getAgency());
            }
            if(editorRequest.getUserId() != null) {
                editor.setUserId(editorRequest.getUserId());
            }

            auditLogHandler.logTraceUpdateForToken("Update and view full editor profile", uriInfo, editor.getId(), 200);
            return Response.status(200)
                    .entity(editor)
                    .build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

}
