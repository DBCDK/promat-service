package dk.dbc.promat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.dto.MessageRequest;
import dk.dbc.promat.service.persistence.PromatMessage;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class MessagesIT extends ContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagesIT.class);

    @Test
    public void testSendMessage() throws JsonProcessingException {
        final int EDITOR_ID = 10;
        final int REVIEWER_ID =1;
        final int CASE_ID = 14;

        final MessageRequest dto = new MessageRequest()
                .withMessageText("First Text to reviewer")
                .withDirection(PromatMessage.Direction.EDITOR_TO_REVIEWER);
        Response response = postResponse(String.format("v1/api/cases/%s/messages", CASE_ID), dto);
        assertThat("201 httpcode", response.getStatus(), is(201));

        PromatMessage message = mapper.readValue(response.readEntity(String.class), PromatMessage.class);

        response = getResponse(String.format("v1/api/cases/messages/%s", message.getId()));
        String messageAsjson =response.readEntity(String.class);
        mapper.readValue(messageAsjson, PromatMessage.class);

        LOGGER.info("message returned as json is: {}", messageAsjson);
        assertThat("message", message.getMessageText(), is(dto.getMessageText()));
        assertThat("case", message.getPromatCase().getId(), is(CASE_ID));
        assertThat("direction", message.getDirection(), is(dto.getDirection()));
        assertThat("reviewer", message.getReviewer().getId(), is(REVIEWER_ID));
        assertThat("editor", message.getEditor().getId(), is(EDITOR_ID));
    }
    // ToDo tests:
    //  * Get endpoint: Fetch a list of messages associated with caseid.
    //  * Post endpont: Set all messages with caseid and direction to "read".
}
