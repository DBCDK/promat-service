package dk.dbc.promat.service.api;

import dk.dbc.httpclient.HttpGet;
import dk.dbc.promat.service.ContainerTest;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class PromatApplicationIT extends ContainerTest {
    @Test
    void openapi() {
        final HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("health");

        try (final Response response = httpClient.execute(httpGet)) {
            assertThat("status code", response.getStatus(), is(200));
        }
    }
}
