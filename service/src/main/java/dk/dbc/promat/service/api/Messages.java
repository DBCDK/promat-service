package dk.dbc.promat.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.promat.service.Repository;
import dk.dbc.promat.service.dto.MessageRequest;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.PromatMessage;
import dk.dbc.promat.service.rest.JsonMapperProvider;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
@Path("")
public class Messages {
    private static final Logger LOGGER = LoggerFactory.getLogger(Messages.class);
    final ObjectMapper objectMapper = new JsonMapperProvider().getObjectMapper();

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @EJB
    Repository repository;

    @POST
    @Path("cases/{id}/messages")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postMessage(@PathParam("id") Integer caseId, MessageRequest messageRequest) {
        LOGGER.info("messages/ (POST) body:{}", messageRequest);
        try {
            // Fetch the case that this message concerns
            PromatCase promatCase = entityManager.find(PromatCase.class, caseId);
            if (promatCase == null) {
                LOGGER.info("No such case {}", caseId);
                return ServiceErrorDto.NotFound("No such case",
                        String.format("Case with id {} does not exist", caseId));
            }

            repository.getExclusiveAccessToTable(PromatMessage.TABLE_NAME);
            PromatMessage promatMessage = new PromatMessage()
                    .withMessageText(messageRequest.getMessageText())
                    .withPromatCase(promatCase)
                    .withEditor(promatCase.getEditor())
                    .withReviewer(promatCase.getReviewer())
                    .withDirection(messageRequest.getDirection())
                    .withIsRead(Boolean.FALSE);

            entityManager.persist(promatMessage);
            entityManager.flush();

            // 201 CREATED
            return Response.status(201)
                    .entity(objectMapper
                            .writerWithView(CaseFormat.IDENTITY.getViewClass())
                            .writeValueAsString(promatMessage))
                    .build();
        } catch (Exception e) {
            LOGGER.error("Caught exception: {}", e.getMessage());
            return ServiceErrorDto.Failed(e.getMessage());
        }
    }

    @GET
    @Path("cases/messages/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMessage(@PathParam("id") final Integer id) {
        try {
            PromatMessage message = entityManager.find(PromatMessage.class, id);
            return Response.ok().entity(message).build();
        } catch (Exception e) {
            LOGGER.error("Caught exception: {}", e.getMessage());
            return ServiceErrorDto.Failed(e.getMessage());
        }
    }

    // ToDo:
    //  * Get endpoint: Fetch a list of messages associated with caseid.
    //  * Post endpont: Set all messages with caseid and direction to "read".
}
