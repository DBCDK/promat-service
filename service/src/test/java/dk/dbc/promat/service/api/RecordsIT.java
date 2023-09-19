package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.RecordsListDto;
import dk.dbc.promat.service.persistence.MaterialType;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

public class RecordsIT extends ContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordsIT.class);

    @Test
    public void testResolveFaust() throws JsonProcessingException {

        Response response = getResponse("v1/api/records/24699773");
        assertThat("status code", response.getStatus(), is(200));
        RecordsListDto resolved = mapper.readValue(response.readEntity(String.class), RecordsListDto.class);

        assertThat("results", resolved.getNumFound(), greaterThanOrEqualTo(1));
        assertThat("expected faust", resolved.getRecords().get(0).getFaust(), is("24699773"));
        assertThat("number of types", resolved.getRecords().get(0).getTypes().size(), is(1));
        assertThat("expected type", resolved.getRecords().get(0).getTypes().get(0).getMaterialType(), is(MaterialType.BOOK));
    }

    @Test
    public void testResolveIsbn() throws JsonProcessingException {

        Response response = getResponse("v1/api/records/9788764432589");
        assertThat("status code", response.getStatus(), is(200));
        RecordsListDto resolved = mapper.readValue(response.readEntity(String.class), RecordsListDto.class);

        assertThat("results", resolved.getNumFound(), greaterThanOrEqualTo(1));
        assertThat("expected faust", resolved.getRecords().get(0).getFaust(), is("24699773"));
        assertThat("number of types", resolved.getRecords().get(0).getTypes().size(), is(1));
        assertThat("expected type", resolved.getRecords().get(0).getTypes().get(0).getMaterialType(), is(MaterialType.BOOK));
    }

    @Test
    public void testResolveBarcode() throws JsonProcessingException {

        Response response = getResponse("v1/api/records/5053083221386");
        assertThat("status code", response.getStatus(), is(200));
        RecordsListDto resolved = mapper.readValue(response.readEntity(String.class), RecordsListDto.class);

        assertThat("results", resolved.getNumFound(), is(1));
        assertThat("expected faust", resolved.getRecords().get(0).getFaust(), is("38352296"));
        assertThat("number of types", resolved.getRecords().get(0).getTypes().size(), is(1));
        assertThat("expected type", resolved.getRecords().get(0).getTypes().get(0).getMaterialType(), is(MaterialType.MOVIE));
    }

    @Test
    public void testResolveBarcodeWithSomeWorksNotFound() throws JsonProcessingException {

        Response response = getResponse("v1/api/records/5712976001848");
        assertThat("status code", response.getStatus(), is(200));
        RecordsListDto resolved = mapper.readValue(response.readEntity(String.class), RecordsListDto.class);

        assertThat("results", resolved.getNumFound(), is(2));
        assertThat("expected fausts", resolved.getRecords().stream()
                .anyMatch(r -> List.of("38604929", "38478508").contains(r.getFaust())));
    }

    @Test
    public void testConsistentReturnForNonexistingFausts() throws JsonProcessingException {

        // There is situations where opensearch has results, but work-presentation
        // has none, and so the final result is 'no records'. But the records property
        // is then set to either a null OR an empty array.. This is very confusing for
        // the frontend. Check that this situation has been fixed
        //
        // 15/05-2023: WorkPresentation has been dropped, we should get all results found by opensearch

        Response response = getResponse("v1/api/records/123");
        assertThat("status code", response.getStatus(), is(200));
        RecordsListDto resolved_123 = mapper.readValue(response.readEntity(String.class), RecordsListDto.class);
        assertThat("results", resolved_123.getNumFound(), is(3));
        assertThat("records", resolved_123.getRecords(), is(not(nullValue())));
        assertThat("records length", resolved_123.getRecords().size(), is(3));

        response = getResponse("v1/api/records/226777809");
        assertThat("status code", response.getStatus(), is(200));
        RecordsListDto resolved_226777809 = mapper.readValue(response.readEntity(String.class), RecordsListDto.class);
        assertThat("results", resolved_226777809.getNumFound(), is(0));
        assertThat("records", resolved_226777809.getRecords(), is(not(nullValue())));
        assertThat("records length", resolved_226777809.getRecords().size(), is(0));
    }
}
