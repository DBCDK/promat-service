package dk.dbc.promat.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.promat.service.Repository;
import dk.dbc.promat.service.dto.MessageRequestDto;
import dk.dbc.promat.service.dto.PromatMessagesList;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.PromatMessage;
import dk.dbc.promat.service.persistence.PromatUser;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.rest.JsonMapperProvider;
import java.time.LocalDate;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
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
    @Path("cases/{caseId}/messages/{userId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postMessage(@PathParam("caseId") Integer caseId, @PathParam("userId") Integer userId,
                                MessageRequestDto messageRequestDto) {
        LOGGER.info("messages/ (POST) body:{}", messageRequestDto);
        try {
            // Fetch the case that this message concerns
            PromatCase promatCase = entityManager.find(PromatCase.class, caseId);
            if (promatCase == null) {
                LOGGER.info("No such case {}", caseId);
                return ServiceErrorDto.NotFound("No such case",
                        String.format("Case with id {} does not exist", caseId));
            }

            // Fetch the user that wants to send this
            PromatUser promatUser = null;
            if (messageRequestDto.getDirection() == PromatMessage.Direction.EDITOR_TO_REVIEWER) {
                promatUser = entityManager.find(Editor.class, userId);
            } else {
                promatUser = entityManager.find(Reviewer.class, userId);
            }

            repository.getExclusiveAccessToTable(PromatMessage.TABLE_NAME);
            PromatMessage promatMessage = new PromatMessage()
                    .withMessageText(messageRequestDto.getMessageText())
                    .withCaseId(caseId)
                    .withAuthor(PromatMessage.Author.fromPromatUser(promatUser))
                    .withDirection(messageRequestDto.getDirection())
                    .withCreated(LocalDate.now())
                    .withIsRead(Boolean.FALSE);

            // Todo: Should we also send a mail when EDITOR_TO_REVIEWER?

            entityManager.persist(promatMessage);
            entityManager.flush();

            // 201 CREATED
            return Response.status(201)
                    .entity(promatMessage)
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

    @GET
    @Path("cases/{id}/messages")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMessagesForCase(@PathParam("id") Integer id) {
        try {
            TypedQuery<PromatMessage> query = entityManager
                    .createNamedQuery(PromatMessage.GET_MESSAGES_FOR_CASE, PromatMessage.class);
            query.setParameter("caseId", id);
            final List<PromatMessage> messages = query.getResultList();

            return Response.ok().entity(new PromatMessagesList().withPromatMessages(messages)).build();

        } catch (Exception e) {
            LOGGER.error("Caught exception: {}", e.getMessage());
            return ServiceErrorDto.Failed(e.getMessage());
        }
    }

    @GET
    @Path("cases/{id}/messages/markasread/{direction}")
    public Response markAsRead(@PathParam("id") Integer id,
                               @PathParam("direction") PromatMessage.Direction direction) {
        try {
            TypedQuery<PromatMessage> query =
                    entityManager.createNamedQuery(PromatMessage.UPDATE_READ_STATE, PromatMessage.class);
            query.setParameter("caseId", id);
            query.setParameter("direction", direction);
            query.setParameter("isRead", Boolean.TRUE);
            query.executeUpdate();
            entityManager.flush();
            return Response.status(201).build();
        } catch (Exception e) {
            LOGGER.error("Caught exception {}", e.getMessage());
            return ServiceErrorDto.Failed(e.getMessage());
        }
    }
}
