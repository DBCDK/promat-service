package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.RecordsListDto;
import dk.dbc.promat.service.persistence.MaterialType;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.core.Is.is;

public class RecordsIT extends ContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordsIT.class);

    @Test
    public void testResolveFaust() throws JsonProcessingException {

        Response response = getResponse("v1/api/records/24699773");
        assertThat("status code", response.getStatus(), is(200));
        RecordsListDto resolved = mapper.readValue(response.readEntity(String.class), RecordsListDto.class);

        assertThat("results", resolved.getNumFound(), greaterThanOrEqualTo(1));
        assertThat("expected faust", resolved.getRecords().getFirst().getFaust(), is("24699773"));
        assertThat("number of types", resolved.getRecords().getFirst().getTypes().size(), is(1));
        assertThat("expected type", resolved.getRecords().getFirst().getTypes().getFirst().getMaterialType(), is(MaterialType.BOOK));
    }

    @Test
    public void testResolveIsbn() throws JsonProcessingException {

        Response response = getResponse("v1/api/records/9788764432589");
        assertThat("status code", response.getStatus(), is(200));
        RecordsListDto resolved = mapper.readValue(response.readEntity(String.class), RecordsListDto.class);

        assertThat("results", resolved.getNumFound(), greaterThanOrEqualTo(1));
        assertThat("expected faust", resolved.getRecords().getFirst().getFaust(), is("24699773"));
        assertThat("number of types", resolved.getRecords().getFirst().getTypes().size(), is(1));
        assertThat("expected type", resolved.getRecords().getFirst().getTypes().getFirst().getMaterialType(), is(MaterialType.BOOK));
    }

}
