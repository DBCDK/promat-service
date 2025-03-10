package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.dto.MarkAsReadRequest;
import dk.dbc.promat.service.dto.MessageRequestDto;
import dk.dbc.promat.service.dto.PromatMessagesList;
import dk.dbc.promat.service.dto.TaskDto;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatMessage;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.persistence.TaskType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MessagesIT extends ContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagesIT.class);

    @Test
    @Order(1)
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

        response = getResponse(String.format("v1/api/cases/%s", CASE_ID));
        PromatCase promatCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("new messages to reviewer", promatCase.getNewMessagesToReviewer(), is(true));
        assertThat("no new messages to editor", promatCase.getNewMessagesToEditor(), is(false));

        // Mark message as read
        response = putResponse(String.format("v1/api/cases/%s/messages/markasread", promatCase.getId()),
                new MarkAsReadRequest()
                        .withDirection(PromatMessage.Direction.EDITOR_TO_REVIEWER));
        assertThat("201 updated", response.getStatus(), is(201));

        // Fetch case again to make sure newMessages pin for reviewer is false.
        response = getResponse(String.format("v1/api/cases/%s", CASE_ID));
        promatCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("new messages to reviewer", promatCase.getNewMessagesToReviewer(), is(false));


    }

    @Test
    @Order(2)
    public void createACaseAndSendSomeMessages() throws IOException {
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

        // Editor comments on that (reviewer will also receivce a mail)
        response = postMessage(aCase.getId(), "Hi Kirsten\n Good to hear!",
                EDITOR_ID, PromatMessage.Direction.EDITOR_TO_REVIEWER);
        assertThat("status code", response.getStatus(), is(201));

        // Another editor interferes (reviewer will also receivce a mail).
        response = postMessage(aCase.getId(),
                "Hi Kirsten\n Since E is on vacation, for the next week, I will probably" +
                        " be handling the case from now on.\n I myself will be on vacation for the next " +
                        "14 years though.\n Trying to beat the record!\n Yours Sincerely\nEdit.",
                ANOTHER_EDITOR_ID, PromatMessage.Direction.EDITOR_TO_REVIEWER);
        assertThat("status code", response.getStatus(), is(201));

        // Make sure that reviewer also receives mails on the two messages posted to her.
        List<Notification> notifications =
                getNotifications(String.format("Ny besked fra redaktøren på ProMat anmeldelse: %s",
                        "Title for 3002111"),
                        null);
        assertThat("There are two messages from editor", notifications.size(), is(2));

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
                is(2L));

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

        // Make sure that three mails are sent to reviewer:
        // 1) The assignment mail
        // 2) The follow up message/mail from main editor
        // 3) The vacation stand-in editor informs
        assertThat( "Three mails are sent to the reviewer",
                size(getNotifications(), "kirsten@kirstensen.dk"),
                is(3L)
        );

        assertThat("Two out of the three informs of messages from editor (not assignment)",
                size(getNotifications("Ny besked fra redaktøren på ProMat anmeldelse", null), "kirsten@kirstensen.dk"),
                is(2L));

        // Make sure that no mails were sent to editor
        assertThat( "No mails are sent to editor",
                size(getNotifications(), "e.ditor@dbc.dk"),
                is(0L)
        );

        String expectedBodyText =
                Files.readString(Path.of("src/test/resources/mailBodys/messageFromTheEditor.html"));
        assertThat("Mails sent about new messages from the editor are formatted ok",
                getNotifications("Ny besked fra redaktøren på ProMat anmeldelse",
                        "Since E is on vacation, for the next week").get(0).getBodyText(),
                is(expectedBodyText));

    }

    @Test
    @Order(3)
    public void sendAndDeleteMessage() throws IOException {
        final int CREATOR_ID = 11;
        final int REVIEWER_ID = 4;
        final int EDITOR_ID = 10;

        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("40001234")
                .withTitle("Title for 40001234")
                .withMaterialType(MaterialType.BOOK)
                .withCreator(CREATOR_ID)
                .withEditor(EDITOR_ID)
                .withReviewer(REVIEWER_ID)
                .withDeadline("2021-08-04")
                .withSubjects(Arrays.asList(3, 4));

        // Create case.
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));

        String obj = response.readEntity(String.class);
        PromatCase aCase = mapper.readValue(obj, PromatCase.class);

        // Now follows a conversation

        response = postMessage(aCase.getId(), "Hi E\n I will look into it soon.",
                REVIEWER_ID, PromatMessage.Direction.REVIEWER_TO_EDITOR);
        assertThat("status code", response.getStatus(), is(201));

        response = postMessage(aCase.getId(), "Hi Kirsten\n Good to hear!",
                EDITOR_ID, PromatMessage.Direction.EDITOR_TO_REVIEWER);
        assertThat("status code", response.getStatus(), is(201));

        response = postMessage(aCase.getId(), "What a piece of total CRAP!!!.. must I really read this piece of shit!",
                REVIEWER_ID, PromatMessage.Direction.REVIEWER_TO_EDITOR);
        assertThat("status code", response.getStatus(), is(201));
        PromatMessage aMessage = mapper.readValue(response.readEntity(String.class), PromatMessage.class);
        int firstBadMessage = aMessage.getId();

        response = postMessage(aCase.getId(), "Oh sorry.. got the wrong book. Strange with two books having almost the same title..  e-hehe..  hee.",
                REVIEWER_ID, PromatMessage.Direction.REVIEWER_TO_EDITOR);
        assertThat("status code", response.getStatus(), is(201));
        aMessage = mapper.readValue(response.readEntity(String.class), PromatMessage.class);
        int secondBadMessage = aMessage.getId();

        response = postMessage(aCase.getId(), "Nice book.. I'll be done in a jiffy",
                REVIEWER_ID, PromatMessage.Direction.REVIEWER_TO_EDITOR);
        assertThat("status code", response.getStatus(), is(201));

        response = postMessage(aCase.getId(), "Good You liked it afterall.. Happy reading",
                EDITOR_ID, PromatMessage.Direction.EDITOR_TO_REVIEWER);
        assertThat("status code", response.getStatus(), is(201));

        // After that, we now have 2 messages from the editor to the reviewer
        assertThat("Editor to reviewer messages",
                size(getMessageList(aCase), PromatMessage.Direction.EDITOR_TO_REVIEWER, true),
                is(2L));

        // and 4 messages from the reviewer to the editor, some not so very nice
        assertThat("Reviewer to editor messages",
                size(getMessageList(aCase), PromatMessage.Direction.REVIEWER_TO_EDITOR, true),
                is(4L));

        // Remove those nasty messages in the middle
        response = deleteResponse("/v1/api/messages/" + firstBadMessage);
        assertThat("status code", response.getStatus(), is(200));
        response = deleteResponse("/v1/api/messages/" + secondBadMessage);
        assertThat("status code", response.getStatus(), is(200));

        // We now have 2 messages from the editor to the reviewer
        assertThat("Editor to reviewer messages",
                size(getMessageList(aCase), PromatMessage.Direction.EDITOR_TO_REVIEWER, true),
                is(2L));

        // ..and 4 messages from reviewer to editor
        assertThat("Editor to reviewer messages",
                size(getMessageList(aCase), PromatMessage.Direction.EDITOR_TO_REVIEWER, true),
                is(2L));

        // ...and 6 messages all in all. Including deleted ones.
        assertThat("Messages all in all",
                getAllMessageList(aCase).getPromatMessages().size(),
                is(6));


        // and 2 nice messages from the reviewer to the editor
        PromatMessagesList messages = getMessageList(aCase);
        assertThat("Reviewer to editor messages",
                size(messages, PromatMessage.Direction.REVIEWER_TO_EDITOR, true),
                is(2L));
        assertThat("First bad message has been deleted", messages.getPromatMessages().stream()
                .filter(m -> m.getId() == firstBadMessage)
                .count(), is(0L));
        assertThat("Second bad message has been deleted", messages.getPromatMessages().stream()
                .filter(m -> m.getId() == secondBadMessage)
                .count(), is(0L));

        // Try deleting a non-existing message
        response = deleteResponse("/v1/api/messages/987654321");
        assertThat("status code", response.getStatus(), is(404));

        // Cleanup
        response = deleteResponse("v1/api/cases/" + aCase.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    @Order(4)
    public void createACaseWithReviewerAndCheckInitialMessage() throws IOException {
        final int REVIEWER_ID = 4;
        final int EDITOR_ID = 10;
        final String caseNote = "Du bedes tildele metadata via <a href=\"https://metakompas.dk\">https://metakompas.dk</a>";

        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("5004321")
                .withTitle("Title for 5004321")
                .withMaterialType(MaterialType.BOOK)
                .withReviewer(REVIEWER_ID)
                .withEditor(EDITOR_ID)
                .withDeadline("2021-08-28")
                .withSubjects(Arrays.asList(3, 4))
                .withNote(caseNote);

        // Create case.
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase aCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Make sure that we have 1 message from the editor to the reviewer and
        // no messages from the reviewer to the editor
        assertThat("1 editor to reviewer message",
                size(getMessageList(aCase), PromatMessage.Direction.EDITOR_TO_REVIEWER, true),
                is(1L)
        );
        assertThat("No reviewer to editor messages",
                size(getMessageList(aCase), PromatMessage.Direction.REVIEWER_TO_EDITOR, true),
                is(0L)
        );

        // Check that we have the correct text in the message and the direction is correct
        assertThat("Message contains note", getMessageList(aCase)
                .getPromatMessages().stream()
                .findFirst().orElseThrow()
                .getMessageText().equals(caseNote));

        // Cleanup
        response = deleteResponse("v1/api/cases/" + aCase.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    @Order(5)
    public void createACaseThenAssignReviewerAndCheckInitialMessage() throws IOException {
        final int REVIEWER_ID = 4;
        final int EDITOR_ID = 10;
        final String caseNote = "Du bedes tildele metadata via <a href=\"https://metakompas.dk\">https://metakompas.dk</a>";

        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("5004321")
                .withTitle("Title for 5004321")
                .withMaterialType(MaterialType.BOOK)
                .withEditor(EDITOR_ID)
                .withDeadline("2021-08-28")
                .withSubjects(Arrays.asList(3, 4))
                .withNote(caseNote);

        // Create case.
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase aCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("not assigned", aCase.getStatus(), is(CaseStatus.CREATED));

        // We should have no messages
        assertThat("No editor to reviewer messages",
                size(getMessageList(aCase), PromatMessage.Direction.EDITOR_TO_REVIEWER, true),
                is(0L)
        );
        assertThat("No reviewer to editor messages",
                size(getMessageList(aCase), PromatMessage.Direction.REVIEWER_TO_EDITOR, true),
                is(0L)
        );

        // Assign the case
        dto.setReviewer(REVIEWER_ID);
        response = postResponse("v1/api/cases/" + aCase.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        aCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("assigned", aCase.getStatus(), is(CaseStatus.ASSIGNED));

        // Make sure that we have 1 message from the editor to the reviewer and
        // no messages from reviewer to the editor
        assertThat("1 editor to reviewer message",
                size(getMessageList(aCase), PromatMessage.Direction.EDITOR_TO_REVIEWER, true),
                is(1L)
        );
        assertThat("No reviewer to editor messages",
                size(getMessageList(aCase), PromatMessage.Direction.REVIEWER_TO_EDITOR, true),
                is(0L)
        );

        // Check that we have the correct text in the message
        assertThat("Message contains note", getMessageList(aCase)
                .getPromatMessages().stream()
                .findFirst().orElseThrow()
                .getMessageText().equals(caseNote));

        // Cleanup
        response = deleteResponse("v1/api/cases/" + aCase.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    @Order(6)
    public void createACaseWithReviewerAndCheckInitialMessageThenReassign() throws IOException {
        final int REVIEWER_ID = 4;
        final int ANOTHER_REVIEWER_ID = 5;  // Reviewer details may change in other tests, but we only need the id here
        final int EDITOR_ID = 10;
        final String caseNote = "Du bedes tildele metadata via <a href=\"https://metakompas.dk\">https://metakompas.dk</a>";

        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("5004321")
                .withTitle("Title for 5004321")
                .withMaterialType(MaterialType.BOOK)
                .withReviewer(REVIEWER_ID)
                .withEditor(EDITOR_ID)
                .withDeadline("2021-08-28")
                .withSubjects(Arrays.asList(3, 4))
                .withNote(caseNote);

        // Create case.
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase aCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Make sure that we have 1 message from the editor to the reviewer and
        // no messages from the reviewer to the editor
        assertThat("1 editor to reviewer message",
                size(getMessageList(aCase), PromatMessage.Direction.EDITOR_TO_REVIEWER, true),
                is(1L)
        );
        assertThat("No reviewer to editor messages",
                size(getMessageList(aCase), PromatMessage.Direction.REVIEWER_TO_EDITOR, true),
                is(0L)
        );

        // Check that we have the correct text in the message and the direction is correct
        LOGGER.info(getMessageList(aCase)
                .getPromatMessages().stream()
                .findFirst().orElseThrow()
                .getMessageText());
        assertThat("Message contains note", getMessageList(aCase)
                .getPromatMessages().stream()
                .findFirst().orElseThrow()
                .getMessageText().equals(caseNote));

        Integer onlyMessageId = getMessageList(aCase).getPromatMessages().stream()
                .findFirst().orElseThrow().getId();

        // Reassign the case
        dto.setReviewer(ANOTHER_REVIEWER_ID);
        response = postResponse("v1/api/cases/" + aCase.getId(), dto, "1-2-3-4-5");
        assertThat("status code", response.getStatus(), is(200));

        // Make sure that we have 2 messages now:
        // * The very first assignment from the editor to the originalæ reviewer and
        // * The second when reassigned to new reviewer.
        // no messages from reviewer to the editor
        assertThat("2 editor to reviewer messages",
                size(getMessageList(aCase), PromatMessage.Direction.EDITOR_TO_REVIEWER, true),
                is(2L)
        );
        assertThat("No reviewer to editor messages",
                size(getMessageList(aCase), PromatMessage.Direction.REVIEWER_TO_EDITOR, true),
                is(0L)
        );

        // And that it is the same message as previous
        assertThat("same message", getMessageList(aCase)
                .getPromatMessages().stream()
                .reduce((first, second) -> second)
                .orElseThrow()
                .getId(), is(onlyMessageId));

        // Cleanup
        response = deleteResponse("v1/api/cases/" + aCase.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    @Order(7)
    public void createACaseAssignAndCheckInitialMessageFromTasks() throws IOException {
        final int REVIEWER_ID = 4;
        final int EDITOR_ID = 10;

        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("5004321")
                .withTitle("Title for 5004321")
                .withMaterialType(MaterialType.BOOK)
                .withEditor(EDITOR_ID)
                .withDeadline("2021-08-28")
                .withSubjects(Arrays.asList(3, 4))
                .withNote("hej")
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Arrays.asList("5004321")),
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BKM)
                                .withTargetFausts(Arrays.asList("5004321"))
                ));

        // Create case.
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase aCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("not assigned", aCase.getStatus(), is(CaseStatus.CREATED));

        // We should have no messages
        assertThat("No editor to reviewer messages",
                size(getMessageList(aCase), PromatMessage.Direction.EDITOR_TO_REVIEWER, true),
                is(0L)
        );
        assertThat("No reviewer to editor messages",
                size(getMessageList(aCase), PromatMessage.Direction.REVIEWER_TO_EDITOR, true),
                is(0L)
        );

        // Assign the case
        dto.setReviewer(REVIEWER_ID);
        response = postResponse("v1/api/cases/" + aCase.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        aCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("assigned", aCase.getStatus(), is(CaseStatus.ASSIGNED));

        // Make sure that we have 1 message from the editor to the reviewer and
        // no messages from reviewer to the editor
        assertThat("1 editor to reviewer message",
                size(getMessageList(aCase), PromatMessage.Direction.EDITOR_TO_REVIEWER, true),
                is(1L)
        );
        assertThat("No reviewer to editor messages",
                size(getMessageList(aCase), PromatMessage.Direction.REVIEWER_TO_EDITOR, true),
                is(0L)
        );

        // Check that the message body is not blank
        assertThat("Message contains note", getMessageList(aCase)
                .getPromatMessages().stream()
                .findFirst().orElseThrow()
                .getMessageText().isEmpty(), is(false));

        // Cleanup
        response = deleteResponse("v1/api/cases/" + aCase.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    @Order(8)
    public void createACaseWithReviewerAndCheckNoInitialMessage() throws IOException {
        final int REVIEWER_ID = 4;
        final int EDITOR_ID = 10;

        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("5004321")
                .withTitle("Title for 5004321")
                .withMaterialType(MaterialType.BOOK)
                .withReviewer(REVIEWER_ID)
                .withEditor(EDITOR_ID)
                .withDeadline("2021-08-28")
                .withSubjects(Arrays.asList(3, 4));

        // Create case.
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase aCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Make sure that we have 0 messages from the editor to the reviewer and
        // no messages from the reviewer to the editor
        assertThat("No editor to reviewer messages",
                size(getMessageList(aCase), PromatMessage.Direction.EDITOR_TO_REVIEWER, true),
                is(0L)
        );
        assertThat("No reviewer to editor messages",
                size(getMessageList(aCase), PromatMessage.Direction.REVIEWER_TO_EDITOR, true),
                is(0L)
        );

        // Cleanup
        response = deleteResponse("v1/api/cases/" + aCase.getId());
        assertThat("status code", response.getStatus(), is(200));
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
        return size(promatMessagesList, direction, false);
    }

    private long size(PromatMessagesList promatMessagesList, PromatMessage.Direction direction, boolean all) {
        List<PromatMessage> messageList = promatMessagesList.getPromatMessages();
        return messageList.stream()
                .filter(message -> (message.getRead() || all) &&
                        message.getDirection() == direction).count();
    }

    private PromatMessagesList getMessageList(PromatCase aCase) throws JsonProcessingException {
        Response response = getResponse(String.format("v1/api/cases/%s/messages", aCase.getId()));
        assertThat("status code", response.getStatus(), is(200));

        return mapper.readValue(response.readEntity(String.class), PromatMessagesList.class);
    }

    private PromatMessagesList getAllMessageList(PromatCase aCase) throws JsonProcessingException {
        Response response = getResponse(String.format("v1/api/cases/%s/audit/messages", aCase.getId()), "1-2-3-4-5" );
        assertThat("status code", response.getStatus(), is(200));

        return mapper.readValue(response.readEntity(String.class), PromatMessagesList.class);
    }

    private long size(List<Notification> notifications, String mailAddress) {
        return notifications.stream().filter(notification -> notification.getToAddress().contains(mailAddress)).count();
    }
}
