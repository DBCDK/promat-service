package dk.dbc.promat.service.api;

import dk.dbc.promat.service.Repository;
import dk.dbc.promat.service.dto.MarkAsReadRequest;
import dk.dbc.promat.service.dto.MessageRequestDto;
import dk.dbc.promat.service.dto.PromatMessagesList;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.PromatMessage;
import dk.dbc.promat.service.persistence.PromatUser;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.templating.NotificationFactory;
import dk.dbc.promat.service.templating.model.MailToReviewerOnNewMessage;
import java.time.LocalDateTime;

import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Stateless
@Path("")
public class Messages {
    private static final Logger LOGGER = LoggerFactory.getLogger(Messages.class);

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @EJB
    Repository repository;

    @EJB
    NotificationFactory notificationFactory;

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
                    .withCreated(LocalDateTime.now())
                    .withIsRead(Boolean.FALSE);

            // Send a mail when EDITOR_TO_REVIEWER
            if (promatMessage.getDirection() == PromatMessage.Direction.EDITOR_TO_REVIEWER) {
                Notification notification = notificationFactory.notificationOf(
                        new MailToReviewerOnNewMessage().withMessage(promatMessage).withPromatCase(promatCase)
                );
                entityManager.persist(notification);
            }

            entityManager.persist(promatMessage);

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
    @Path("messages/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMessage(@PathParam("id") final Integer id) {
        try {
            PromatMessage message = entityManager.find(PromatMessage.class, id);
            if( message == null || message.getDeleted() ) {
                LOGGER.info("Requested message {} does not exist", id);
                return ServiceErrorDto.NotFound("Message not found",
                        String.format("Requested message %s does not exist", id));
            }

            return Response.ok().entity(message).build();
        } catch (Exception e) {
            LOGGER.error("Caught exception: {}", e.getMessage());
            return ServiceErrorDto.Failed(e.getMessage());
        }
    }

    @GET
    @Path("cases/{id}/audit/messages")
    @Produces({MediaType.APPLICATION_JSON})
    @RolesAllowed({"authenticated-user"})
    public Response getAllMessagesForcase(@PathParam("id") Integer id) {
        try {
            TypedQuery<PromatMessage> query = entityManager
                    .createNamedQuery(PromatMessage.GET_ALL_MESSAGES_FOR_CASE, PromatMessage.class);
            query.setParameter("caseId", id);
            final List<PromatMessage> messages = query.getResultList();

            return Response.ok().entity(new PromatMessagesList().withPromatMessages(messages)).build();

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

    @PUT
    @Path("cases/{id}/messages/markasread")
    public Response markAsRead(@PathParam("id") Integer id,
                               MarkAsReadRequest markAsReadRequest) {
        try {
            TypedQuery<PromatMessage> query =
                    entityManager.createNamedQuery(PromatMessage.UPDATE_READ_STATE, PromatMessage.class);
            query.setParameter("caseId", id);
            query.setParameter("direction", markAsReadRequest.getDirection());
            query.setParameter("isRead", Boolean.TRUE);
            query.executeUpdate();

            return Response.status(201).build();
        } catch (Exception e) {
            LOGGER.error("Caught exception {}", e.getMessage());
            return ServiceErrorDto.Failed(e.getMessage());
        }
    }

    @DELETE
    @Path("messages/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMessage(@PathParam("id") final Integer id) {
        try {
            PromatMessage message = entityManager.find(PromatMessage.class, id);
            if( message == null ) {
                LOGGER.info("Requested message {} does not exist", id);
                return ServiceErrorDto.NotFound("Message not found",
                        String.format("Requested message %s does not exist", id));
            }

            // Delete the message
            message.setDeleted(true);
            message.setRead(true);

            return Response.ok().build();
        } catch (Exception e) {
            LOGGER.error("Caught exception: {}", e.getMessage());
            return ServiceErrorDto.Failed(e.getMessage());
        }
    }
}
