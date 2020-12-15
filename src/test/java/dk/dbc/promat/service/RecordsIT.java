/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.dto.RecordsListDto;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import java.io.Console;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class RecordsIT extends ContainerTest {

    @Test
    public void testResolveFaust() throws JsonProcessingException {

        Response response = getResponse("v1/api/records/24699773");
        assertThat("status code", response.getStatus(), is(200));
        RecordsListDto resolved = mapper.readValue(response.readEntity(String.class), RecordsListDto.class);

        assertThat("results", resolved.getNumFound(), is(19));
    }

    @Test
    public void testResolveIsbn() throws JsonProcessingException {

        Response response = getResponse("v1/api/records/9788764432589");
        assertThat("status code", response.getStatus(), is(200));
        RecordsListDto resolved = mapper.readValue(response.readEntity(String.class), RecordsListDto.class);

        assertThat("results", resolved.getNumFound(), is(19));
    }

    @Test
    public void testResolveBarcode() throws JsonProcessingException {

        Response response = getResponse("v1/api/records/5053083221386");
        assertThat("status code", response.getStatus(), is(200));
        RecordsListDto resolved = mapper.readValue(response.readEntity(String.class), RecordsListDto.class);

        assertThat("results", resolved.getNumFound(), is(2));
    }
}
