package dk.dbc.promat.service.rest;

import com.fasterxml.jackson.core.JsonProcessingException;

import dk.dbc.httpclient.HttpPost;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.CaseRequestDto;
import dk.dbc.promat.service.persistence.Case;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.Subject;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import org.junit.jupiter.api.Test;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CasesIT extends ContainerTest {

    @Test
    public void testCreateCase() throws JsonProcessingException {

        CaseRequestDto dto = new CaseRequestDto();

        // Empty dto
        HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases")
                .withData(dto, "application/json");
        Response response = httpClient.execute(httpPost);
        assertThat("status code", response.getStatus(), is(400));

        // title set
        dto.setTitle("Title for 02345678");
        httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases")
                .withData(dto, "application/json");
        response = httpClient.execute(httpPost);
        assertThat("status code", response.getStatus(), is(400));

        // primaryFaust set
        dto.setPrimaryFaust("02345678");
        httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases")
                .withData(dto, "application/json");
        response = httpClient.execute(httpPost);
        assertThat("status code", response.getStatus(), is(400));

        // materialType set
        dto.setMaterialType(MaterialType.BOOK);
        httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases")
                .withData(dto, "application/json");
        response = httpClient.execute(httpPost);
        assertThat("status code", response.getStatus(), is(201));

        String obj = response.readEntity(String.class);
        Case created = mapper.readValue(obj, Case.class);
        assertThat("primary faust", created.getPrimaryFaust(), is("02345678"));
        assertThat("title", created.getTitle(), is("Title for 02345678"));
        assertThat("materialType", created.getMaterialType(), is(MaterialType.BOOK));
    }

    @Test
    public void testCreateCaseForExistingFaust() {

        // First case with faust 12345678
        CaseRequestDto dto = new CaseRequestDto()
                .withPrimaryFaust("12345678")
                .withTitle("Title for 12345678")
                .withMaterialType(MaterialType.BOOK);

        HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases")
                .withData(dto, "application/json");

        Response response = httpClient.execute(httpPost);
        assertThat("status code", response.getStatus(), is(201));

        // New case with faust 12345678 (exists)
        dto.setTitle("New title for 12345678");
        dto.setMaterialType(MaterialType.MOVIE);

        httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases")
                .withData(dto, "application/json");

        response = httpClient.execute(httpPost);
        assertThat("status code", response.getStatus(), is(409));
    }

    @Test
    public void testCreateCaseWithSubjectAndReviewer() throws JsonProcessingException {

        // Note: This test depends on subjects with id 3 and 4, and reviewer with id 1
        // being injected by the dumpfiles also used by the reviewer and subject tests

        CaseRequestDto dto = new CaseRequestDto()
                .withPrimaryFaust("22345678")
                .withTitle("Title for 22345678")
                .withMaterialType(MaterialType.BOOK)
                .withReviewer(1)
                .withSubjects(new int[] {3, 4});

        HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases")
                .withData(dto, "application/json");

        Response response = httpClient.execute(httpPost);
        assertThat("status code", response.getStatus(), is(201));

        String obj = response.readEntity(String.class);
        Case created = mapper.readValue(obj, Case.class);
        assertThat("primary faust", created.getPrimaryFaust(), is("22345678"));
        assertThat("title", created.getTitle(), is("Title for 22345678"));
        assertThat("materialType", created.getMaterialType(), is(MaterialType.BOOK));

        assertThat("reviwer id", created.getReviewer().getId(), is(1));
        assertThat("reviwer firstname", created.getReviewer().getFirstName(), is("Hans"));

        assertThat("subjects size", created.getSubjects().size(), is(2));
        List<Integer> actual = created.getSubjects()
                .stream()
                .sorted(Comparator.comparingInt(Subject::getId))
                .map(s -> s.getId()).collect(Collectors.toList());
        List<Integer> expected = new ArrayList<Integer>();
        expected.add(3);
        expected.add(4);
        assertThat("subject ids", actual.equals(expected), is(true) );
    }
}
