package dk.dbc.promat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.dto.MarkAsReadRequest;
import dk.dbc.promat.service.dto.MessageRequestDto;
import dk.dbc.promat.service.dto.PromatMessagesList;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.NotificationStatus;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatMessage;
import java.util.Arrays;
import java.util.List;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;



public class MessagesIT extends ContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagesIT.class);

    @Test
    public void testSendMessage() throws JsonProcessingException {
        final int EDITOR_ID = 10;
        final int REVIEWER_ID = 1;
        final int CASE_ID = 14;

        final MessageRequestDto dto = new MessageRequestDto()
                .withMessageText("First from editor to reviewer")
                .withDirection(PromatMessage.Direction.EDITOR_TO_REVIEWER);
        Response response = postResponse(String.format("v1/api/cases/%s/messages/%s", CASE_ID, EDITOR_ID), dto);
        assertThat("201 httpcode", response.getStatus(), is(201));

        PromatMessage message = mapper.readValue(response.readEntity(String.class), PromatMessage.class);

        response = getResponse(String.format("v1/api/messages/%s", message.getId()));
        String messageAsjson = response.readEntity(String.class);
        mapper.readValue(messageAsjson, PromatMessage.class);

        assertThat("message", message.getMessageText(), is(dto.getMessageText()));
        assertThat("direction", message.getDirection(), is(PromatMessage.Direction.EDITOR_TO_REVIEWER));
        assertThat("author", message.getAuthor().getId(), is(EDITOR_ID));
        assertThat("createDate", message.getCreated(), notNullValue());
        assertThat("not read", message.getRead(), is(Boolean.FALSE));
    }

    @Test
    public void createACaseAndSendSomeMessages() throws JsonProcessingException {
        final int REVIEWER_ID = 4;
        final int EDITOR_ID = 10;
        final int ANOTHER_EDITOR_ID = 11;

        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("3002111")
                .withTitle("Title for 3002111")
                .withMaterialType(MaterialType.BOOK)
                .withReviewer(REVIEWER_ID)
                .withEditor(EDITOR_ID)
                .withDeadline("2021-01-18")
                .withSubjects(Arrays.asList(3, 4));

        // Create case.
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));

        String obj = response.readEntity(String.class);
        PromatCase aCase = mapper.readValue(obj, PromatCase.class);

        // Reviewer accepts
        response = postMessage(aCase.getId(), "Hi E\n I will look into it soon.",
                REVIEWER_ID, PromatMessage.Direction.REVIEWER_TO_EDITOR);

        assertThat("status code", response.getStatus(), is(201));

        // Editor comments on that
        response = postMessage(aCase.getId(), "Hi Kirsten\n Good to hear!",
                EDITOR_ID, PromatMessage.Direction.EDITOR_TO_REVIEWER);
        assertThat("status code", response.getStatus(), is(201));

        // Another editor interferes.
        response = postMessage(aCase.getId(),
                "Hi Kirsten\n Since E is on vacation, for the next week, I will (...)",
                ANOTHER_EDITOR_ID, PromatMessage.Direction.EDITOR_TO_REVIEWER);
        assertThat("status code", response.getStatus(), is(201));


        // Make sure none is read yet
        assertThat("Editor to reviewer messages are not read",
                size(getMessageList(aCase), PromatMessage.Direction.EDITOR_TO_REVIEWER),
                is(0L)
                );

        // And the same in opposite direction
        assertThat("Reviewer to Editor messages are not read",
                size(getMessageList(aCase), PromatMessage.Direction.REVIEWER_TO_EDITOR),
                is(0L)
        );


        // Set all messages to reviewer on this case to "read"
        response = putResponse(
                String.format("v1/api/cases/%s/messages/markasread",
                        aCase.getId()), new MarkAsReadRequest()
                        .withDirection(PromatMessage.Direction.EDITOR_TO_REVIEWER));
        assertThat("status code", response.getStatus(), is(201));

        // Make sure all messages to reviewer are now status "read"
        assertThat("Editor to reviewer messages are read",
                size(getMessageList(aCase), PromatMessage.Direction.EDITOR_TO_REVIEWER),
                is(3L));

        // And in the opposite direction: No changes
        assertThat("Reviewer to editor messages are not read",
                size(getMessageList(aCase), PromatMessage.Direction.REVIEWER_TO_EDITOR),
                is(0L));

        // Set all messages to editor on this case to "read"
        response = putResponse(
                String.format("v1/api/cases/%s/messages/markasread",
                        aCase.getId()), new MarkAsReadRequest().
                        withDirection(PromatMessage.Direction.REVIEWER_TO_EDITOR));
        assertThat("status code", response.getStatus(), is(201));

        // Make sure all messages to editor are now status "read"
        assertThat("Reviewer to editor messages are read",
                size(getMessageList(aCase), PromatMessage.Direction.REVIEWER_TO_EDITOR),
                is(1L));

        // Make sure that ONLY the assignment-mail is sent to reviewer
        assertThat( "Only one mail is sent to reviewer",
                size(getNotifications(), "kirsten@kirstensen.dk"),
                is(1L)
        );

        // Make sure that no mails were sent to editor
        assertThat( "No mails are sent to editor",
                size(getNotifications(), "e.ditor@dbc.dk"),
                is(0L)
        );

        // Todo: Rework this when all messages sent to reviewer also results in mails.

    }

    private Response postMessage(Integer caseId, String messageText,
                                 Integer userId, PromatMessage.Direction direction) {
        MessageRequestDto messageRequestDto = new MessageRequestDto()
                .withMessageText(messageText)
                .withDirection(direction);
        return postResponse(
                String.format("v1/api/cases/%s/messages/%s", caseId, userId),
                messageRequestDto);
    }

    private long size(PromatMessagesList promatMessagesList, PromatMessage.Direction direction) {
        List<PromatMessage> messageList = promatMessagesList.getPromatMessages();
        return messageList.stream()
                .filter(message -> message.getRead() &&
                        message.getDirection() == direction).count();
    }

    private PromatMessagesList getMessageList(PromatCase aCase) throws JsonProcessingException {
        Response response = getResponse(String.format("v1/api/cases/%s/messages", aCase.getId()));
        assertThat("status code", response.getStatus(), is(200));

        return mapper.readValue(response.readEntity(String.class), PromatMessagesList.class);
    }

    private List<Notification> getNotifications() {
        TypedQuery<Notification> query = entityManager
                .createQuery("SELECT notification " +
                        "FROM Notification notification ORDER BY notification.id", Notification.class);
        List<Notification> notifications = query.getResultList();
        return notifications;
    }

    private long size(List<Notification> notifications, String mailAddress) {
        return notifications.stream().filter(notification -> notification.getToAddress().equals(mailAddress)).count();
    }
}
