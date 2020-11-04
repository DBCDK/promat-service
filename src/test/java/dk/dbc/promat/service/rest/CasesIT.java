package dk.dbc.promat.service.rest;

import com.fasterxml.jackson.core.JsonProcessingException;

import dk.dbc.httpclient.HttpPost;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.CaseRequestDto;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;

public class CasesIT extends ContainerTest {

    @Test
    public void crude_test() throws JsonProcessingException {

        CaseRequestDto dto = new CaseRequestDto();

        final HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases")
                .withData(dto, "application/json");

        final Response response = httpClient.execute(httpPost);
        assertThat("status code", response.getStatus(), CoreMatchers.is(201));

        // Todo: further testing on the returned object
    }
}
