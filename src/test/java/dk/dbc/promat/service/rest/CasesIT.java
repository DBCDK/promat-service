package dk.dbc.promat.service.rest;

import com.fasterxml.jackson.core.JsonProcessingException;

import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.CaseRequestDto;
import dk.dbc.promat.service.dto.TaskDto;
import dk.dbc.promat.service.persistence.Case;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.Paycode;
import dk.dbc.promat.service.persistence.Subject;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import dk.dbc.promat.service.persistence.TaskType;
import org.hamcrest.core.IsNull;
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
    public void testCreateCaseForExistingPrimaryFaust() {

        // First case
        CaseRequestDto dto = new CaseRequestDto()
                .withPrimaryFaust("08345678")
                .withRelatedFausts(Arrays.asList(new String[] {"09345678", "00145678"}))
                .withTitle("Title for 06345678")
                .withMaterialType(MaterialType.BOOK);

        HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases")
                .withData(dto, "application/json");

        Response response = httpClient.execute(httpPost);
        assertThat("status code", response.getStatus(), is(201));

        // New case with primary faust 08345678 (exists as primary faust)
        dto.setTitle("New title for 08345678");
        dto.setMaterialType(MaterialType.MOVIE);

        httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases")
                .withData(dto, "application/json");

        response = httpClient.execute(httpPost);
        assertThat("status code", response.getStatus(), is(409));

        // New case with primary faust 07345678 (exists as related faust)
        dto.setPrimaryFaust("09345678");
        dto.setTitle("New title for 09345678");
        dto.setMaterialType(MaterialType.MOVIE);

        httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases")
                .withData(dto, "application/json");

        response = httpClient.execute(httpPost);
        assertThat("status code", response.getStatus(), is(409));

        // New case with primary faust 00245678 and related faust 00145678 (related faust exists)
        dto.setPrimaryFaust("00245678");
        dto.setTitle("New title for 00245678");
        dto.setRelatedFausts(Arrays.asList(new String[] {"00145678"}));
        dto.setMaterialType(MaterialType.MOVIE);

        httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases")
                .withData(dto, "application/json");

        response = httpClient.execute(httpPost);
        assertThat("status code", response.getStatus(), is(409));

        // New case with primary faust 00245678 and related faust 00345678 (all is good)
        dto.setPrimaryFaust("00245678");
        dto.setTitle("New title for 00245678");
        dto.setRelatedFausts(Arrays.asList(new String[] {"00345678"}));
        dto.setMaterialType(MaterialType.MOVIE);

        httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases")
                .withData(dto, "application/json");

        response = httpClient.execute(httpPost);
        assertThat("status code", response.getStatus(), is(201));
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
                .withSubjects(Arrays.asList(3, 4));

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
                .withRelatedFausts(Arrays.asList("42345678", "52345678"))
                .withStatus(CaseStatus.CREATED);

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
        assertThat(created.getStatus(), is(CaseStatus.CREATED));
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
                .withSubjects(Arrays.asList(3, 4))
                .withAssigned("2020-04-11")
                .withDeadline("2020-04-12")
                .withRelatedFausts(Arrays.asList("72345678", "82345678"))
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

    @Test
    public void testCreateCaseWithTasks() throws JsonProcessingException {

        // Create case
        CaseRequestDto dto = new CaseRequestDto()
                .withPrimaryFaust("92345678")
                .withTitle("Title for 92345678")
                .withDetails("Details for 92345678")
                .withMaterialType(MaterialType.BOOK)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withAssigned("2020-04-11")
                .withDeadline("2020-04-12")
                .withRelatedFausts(Arrays.asList("03345678", "04345678"))
                .withStatus(CaseStatus.ASSIGNED)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withPaycode(Paycode.NONE)
                                .withTypeOfTask(TaskType.ABOUT)
                                .withTargetFausts(Arrays.asList(new String[] {"04345678", "14345678"})),
                        new TaskDto()
                                .withPaycode(Paycode.NONE)
                                .withTypeOfTask(TaskType.BKM)
                ));

        HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases")
                .withData(dto, "application/json");

        Response response = httpClient.execute(httpPost);
        assertThat("status code", response.getStatus(), is(201));

        // Verify that the case has some data and a list with 2 tasks
        String obj = response.readEntity(String.class);
        Case created = mapper.readValue(obj, Case.class);
        assertThat("has 2 tasks", created.getTasks().size(), is(2));

        // Check the first created task
        assertThat("task 1 type", created.getTasks().get(0).getTypeOfTask(), is(TaskType.ABOUT));
        assertThat("task 1 paycode", created.getTasks().get(0).getPaycode(), is(Paycode.NONE));
        assertThat("task 1 created", created.getTasks().get(0).getCreated(), is(LocalDate.now()));
        assertThat("task 1 approved", created.getTasks().get(0).getApproved(), is(IsNull.nullValue()));
        assertThat("task 1 payed", created.getTasks().get(0).getPayed(), is(IsNull.nullValue()));
        assertThat("task 1 targetFausts", created.getTasks().get(0).getTargetFausts(), is(IsNull.notNullValue()));
        assertThat("task 1 targetFausts size", created.getTasks().get(0).getTargetFausts().size(), is(2));
        assertThat("task 1 targetFausts contains", created.getTasks().get(0).getTargetFausts()
                        .stream().sorted().collect(Collectors.toList()).toString(),
                is(Arrays.asList(new String [] {"04345678", "14345678"})
                        .stream().sorted().collect(Collectors.toList()).toString()));
        assertThat("related fausts contains", created.getRelatedFausts()
                        .stream().sorted().collect(Collectors.toList()).toString(),
                is(Arrays.asList(new String [] {"03345678", "04345678", "14345678"})
                        .stream().sorted().collect(Collectors.toList()).toString()));

        // Check the second created task
        assertThat("task 2 type", created.getTasks().get(1).getTypeOfTask(), is(TaskType.BKM));
        assertThat("task 2 paycode", created.getTasks().get(1).getPaycode(), is(Paycode.NONE));
        assertThat("task 2 created", created.getTasks().get(1).getCreated(), is(LocalDate.now()));
        assertThat("task 2 approved", created.getTasks().get(1).getApproved(), is(IsNull.nullValue()));
        assertThat("task 2 payed", created.getTasks().get(1).getPayed(), is(IsNull.nullValue()));
        assertThat("task 2 targetFausts", created.getTasks().get(1).getTargetFausts(), is(IsNull.nullValue()));

        // Get the case we created
        HttpGet httpGet= new HttpGet(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases", created.getId().toString());

        response = httpClient.execute(httpGet);
        assertThat("status code", response.getStatus(), is(200));

        // Verify that the case matches the created case
        obj = response.readEntity(String.class);
        Case fetched = mapper.readValue(obj, Case.class);
        assertThat("fetched case is same as created", created.equals(fetched), is(true));
    }

    @Test
    public void testCreateCaseWithInvalidStatus() {

        // Create case with status ASSIGNED but no reviewer given
        CaseRequestDto dto = new CaseRequestDto()
                .withPrimaryFaust("05345678")
                .withTitle("Title for 05345678")
                .withDetails("Details for 05345678")
                .withMaterialType(MaterialType.BOOK)
                .withAssigned("2020-04-11")
                .withDeadline("2020-04-12")
                .withRelatedFausts(Arrays.asList("06345678", "07345678"))
                .withStatus(CaseStatus.ASSIGNED);

        HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases")
                .withData(dto, "application/json");

        Response response = httpClient.execute(httpPost);
        assertThat("status code", response.getStatus(), is(400));

        // Change status to DONE (which is even worse)
        dto.setStatus(CaseStatus.DONE);

        httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases")
                .withData(dto, "application/json");

        response = httpClient.execute(httpPost);
        assertThat("status code", response.getStatus(), is(400));

        // Get frustrated and remove the status
        dto.setStatus(null);

        httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements("v1", "api", "cases")
                .withData(dto, "application/json");

        response = httpClient.execute(httpPost);
        assertThat("status code", response.getStatus(), is(201));

        // Tests for valid state CREATED and ASSIGNED with an reviewer set is
        // covered in previous tests
    }
}
