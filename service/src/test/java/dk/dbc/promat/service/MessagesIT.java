package dk.dbc.promat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.dto.PromatMessageDto;
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
        PromatMessageDto dto = new PromatMessageDto()
                .withMessageText("First Text to reviewer")
                .withCaseId(14)
                .withDirection(PromatMessage.Direction.EDITOR_TO_REVIEWER);
        Response response = postResponse("v1/api/messages", dto);
        String resp =response.readEntity(String.class);
        PromatMessage message = mapper.readValue(resp, PromatMessage.class);
        LOGGER.info("message returned as json is: {}", resp);
        assertThat("message", message.getMessageText(), is(dto.getMessageText()));
        assertThat("case", message.getPromatCase().getId(), is(dto.getCaseId()));
        assertThat("direction", message.getDirection(), is(dto.getDirection()));

        // ToDo:
        //   * Check that be fetched from endpoint.
    }
    // ToDo tests:
    //  * Post endpoint with template handling.
    //  * Get endpoint: Fetch a list of messages associated with caseid.
    //  * Post endpont: Set all messages with caseid and direction to "read".

}
