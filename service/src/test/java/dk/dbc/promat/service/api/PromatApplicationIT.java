/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import dk.dbc.httpclient.HttpGet;
import dk.dbc.promat.service.ContainerTest;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class PromatApplicationIT extends ContainerTest {
    @Test
    void openapi() {
        final HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("openapi");

        final Response response = httpClient.execute(httpGet);
        assertThat("status code", response.getStatus(), is(200));
        final String openapi = response.readEntity(String.class);
        assertThat("openapi", openapi, containsString("Provides backend services for the Promat system"));
    }
}