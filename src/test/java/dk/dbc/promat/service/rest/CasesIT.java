package dk.dbc.promat.service.rest;

import com.fasterxml.jackson.core.JsonProcessingException;

import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.CaseRequestDto;
import dk.dbc.promat.service.persistence.Case;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.Subject;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import org.junit.jupiter.api.Test;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
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

    @Test
    public void testCreateCaseWithTrivialFields() throws JsonProcessingException {

        CaseRequestDto dto = new CaseRequestDto()
                .withPrimaryFaust("32345678")
                .withTitle("Title for 32345678")
                .withDetails("Details for 32345678")
                .withMaterialType(MaterialType.BOOK)
                .withAssigned("2020-04-11")
                .withDeadline("2020-04-12")
                .withRelatedFausts(new String[] {"42345678", "52345678"})
                .withStatus(CaseStatus.ASSIGNED);

        HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases")
                .withData(dto, "application/json");

        Response response = httpClient.execute(httpPost);
        assertThat("status code", response.getStatus(), is(201));

        String obj = response.readEntity(String.class);
        Case created = mapper.readValue(obj, Case.class);

        assertThat("primaryFaust", created.getPrimaryFaust(), is("32345678"));
        assertThat("title", created.getTitle(), is("Title for 32345678"));
        assertThat("details", created.getDetails(), is("Details for 32345678"));
        assertThat("materialType", created.getMaterialType(), is(MaterialType.BOOK));
        assertThat("created", created.getCreated(), is(LocalDate.now()));
        assertThat("assigned", created.getAssigned(), is(LocalDate.parse("2020-04-11")));
        assertThat("deadline", created.getDeadline(), is(LocalDate.parse("2020-04-12")));
        assertThat("relatedFausts", created.getRelatedFausts().
                stream()
                .sorted()
                .collect(Collectors.toList())
                .equals(Arrays.stream(new String[]{"42345678", "52345678"})
                        .collect(Collectors.toList())), is(true));
        assertThat(created.getStatus(), is(CaseStatus.ASSIGNED));
    }

    @Test
    public void testGetCase() throws JsonProcessingException {

        // Create case
        CaseRequestDto dto = new CaseRequestDto()
                .withPrimaryFaust("62345678")
                .withTitle("Title for 62345678")
                .withDetails("Details for 62345678")
                .withMaterialType(MaterialType.BOOK)
                .withReviewer(1)
                .withSubjects(new int[] {3, 4})
                .withAssigned("2020-04-11")
                .withDeadline("2020-04-12")
                .withRelatedFausts(new String[] {"72345678", "82345678"})
                .withStatus(CaseStatus.ASSIGNED);

        HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases")
                .withData(dto, "application/json");

        Response response = httpClient.execute(httpPost);
        assertThat("status code", response.getStatus(), is(201));

        // Verify that the case has some data
        String obj = response.readEntity(String.class);
        Case created = mapper.readValue(obj, Case.class);

        assertThat("primary faust", created.getPrimaryFaust(), is("62345678"));
        assertThat("title", created.getTitle(), is("Title for 62345678"));
        assertThat("details", created.getDetails(), is("Details for 62345678"));

        // Get case with nonexisting id, expect 204 (NOT FOUND)
        HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases", "1234");

        response = httpClient.execute(httpGet);
        assertThat("status code", response.getStatus(), is(204));

        // Get the case we created
        httpGet = new HttpGet(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases", created.getId().toString());

        response = httpClient.execute(httpGet);
        assertThat("status code", response.getStatus(), is(200));

        // Verify that the case matches the created case
        obj = response.readEntity(String.class);
        Case fetched = mapper.readValue(obj, Case.class);
        assertThat("fetched case is same as created", created.equals(fetched), is(true));
    }
}
