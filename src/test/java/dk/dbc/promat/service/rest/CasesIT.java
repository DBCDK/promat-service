package dk.dbc.promat.service.rest;

import com.fasterxml.jackson.core.JsonProcessingException;

import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.CaseRequestDto;
import dk.dbc.promat.service.dto.CaseSummaryList;
import dk.dbc.promat.service.dto.TaskDto;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.Paycode;
import dk.dbc.promat.service.persistence.Subject;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.Matchers.greaterThan;

import dk.dbc.promat.service.persistence.TaskType;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Test;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CasesIT extends ContainerTest {

    @Test
    public void testCreateCase() throws JsonProcessingException {

        CaseRequestDto dto = new CaseRequestDto();

        // Empty dto
        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(400));

        // title set
        dto.setTitle("Title for 1001111");
        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(400));

        // primaryFaust set
        dto.setPrimaryFaust("1001111");
        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(400));

        // materialType set
        dto.setMaterialType(MaterialType.BOOK);
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));

        // Check that the returned object has title, primary faust and materialtype set
        String obj = response.readEntity(String.class);
        PromatCase created = mapper.readValue(obj, PromatCase.class);
        assertThat("primary faust", created.getPrimaryFaust(), is("1001111"));
        assertThat("title", created.getTitle(), is("Title for 1001111"));
        assertThat("materialType", created.getMaterialType(), is(MaterialType.BOOK));
    }

    @Test
    public void testCreateCaseForExistingPrimaryFaust() {

        // New case with primary faust 001111 which already exists
        CaseRequestDto dto = new CaseRequestDto()
                .withPrimaryFaust("001111")
                .withRelatedFausts(Arrays.asList(new String[] {"002222", "003333"}))
                .withTitle("Title for 001111")
                .withMaterialType(MaterialType.MOVIE);

        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(409));

        // New case with primary faust 002222 which exists as related faust
        dto.setPrimaryFaust("002222");
        dto.setTitle("New title for 002222");
        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(409));

        // New case with primary faust 2004444 and related faust 002222 which exists as related faust
        dto.setPrimaryFaust("204444");
        dto.setTitle("New title for 2004444");
        dto.setRelatedFausts(Arrays.asList(new String[] {"002222"}));
        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(409));

        // New case with primary faust 2004444 and related faust 2005555 (all is good)
        dto.setRelatedFausts(Arrays.asList(new String[] {"2005555"}));
        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(201));
    }

    @Test
    public void testCreateCaseWithSubjectAndReviewer() throws JsonProcessingException {

        // Note: This test depends on subjects with id 3 and 4, and reviewer with id 1
        // being injected by the dumpfiles also used by the reviewer and subject tests

        CaseRequestDto dto = new CaseRequestDto()
                .withPrimaryFaust("3001111")
                .withTitle("Title for 3001111")
                .withMaterialType(MaterialType.BOOK)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4));

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));

        String obj = response.readEntity(String.class);
        PromatCase created = mapper.readValue(obj, PromatCase.class);
        assertThat("primary faust", created.getPrimaryFaust(), is("3001111"));
        assertThat("title", created.getTitle(), is("Title for 3001111"));
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
                .withPrimaryFaust("4001111")
                .withTitle("Title for 4001111")
                .withDetails("Details for 4001111")
                .withMaterialType(MaterialType.BOOK)
                .withAssigned("2020-04-11")
                .withDeadline("2020-04-12")
                .withRelatedFausts(Arrays.asList("4002222", "4003333"))
                .withStatus(CaseStatus.CREATED);

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));

        String obj = response.readEntity(String.class);
        PromatCase created = mapper.readValue(obj, PromatCase.class);

        assertThat("primaryFaust", created.getPrimaryFaust(), is("4001111"));
        assertThat("title", created.getTitle(), is("Title for 4001111"));
        assertThat("details", created.getDetails(), is("Details for 4001111"));
        assertThat("materialType", created.getMaterialType(), is(MaterialType.BOOK));
        assertThat("created", created.getCreated(), is(LocalDate.now()));
        assertThat("assigned", created.getAssigned(), is(LocalDate.parse("2020-04-11")));
        assertThat("deadline", created.getDeadline(), is(LocalDate.parse("2020-04-12")));
        assertThat("relatedFausts", created.getRelatedFausts().
                stream()
                .sorted()
                .collect(Collectors.toList())
                .equals(Arrays.stream(new String[]{"4002222", "4003333"})
                        .collect(Collectors.toList())), is(true));
        assertThat(created.getStatus(), is(CaseStatus.CREATED));
    }

    @Test
    public void testGetCase() throws JsonProcessingException {

        // Get case with nonexisting id, expect 404 (NOT FOUND)
        Response response = getResponse("v1/api/cases/1234");
        assertThat("status code", response.getStatus(), is(404));

        // Get an existing case
        response = getResponse("v1/api/cases/1");
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testCreateCaseWithTasks() throws JsonProcessingException {

        // Create case
        CaseRequestDto dto = new CaseRequestDto()
                .withPrimaryFaust("6001111")
                .withTitle("Title for 6001111")
                .withDetails("Details for 6001111")
                .withMaterialType(MaterialType.BOOK)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withAssigned("2020-04-11")
                .withDeadline("2020-04-12")
                .withRelatedFausts(Arrays.asList("6002222", "6003333"))
                .withStatus(CaseStatus.ASSIGNED)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withPaycode(Paycode.NONE)
                                .withTypeOfTask(TaskType.ABOUT)
                                .withTargetFausts(Arrays.asList(new String[] {"6002222", "6004444"})),
                        new TaskDto()
                                .withPaycode(Paycode.NONE)
                                .withTypeOfTask(TaskType.BKM)
                ));

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));

        // Verify that the case has some data and a list with 2 tasks
        String obj = response.readEntity(String.class);
        PromatCase created = mapper.readValue(obj, PromatCase.class);
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
                is(Arrays.asList(new String [] {"6002222", "6004444"})
                        .stream().sorted().collect(Collectors.toList()).toString()));
        assertThat("related fausts contains", created.getRelatedFausts()
                        .stream().sorted().collect(Collectors.toList()).toString(),
                is(Arrays.asList(new String [] {"6002222", "6003333", "6004444"})
                        .stream().sorted().collect(Collectors.toList()).toString()));

        // Check the second created task
        assertThat("task 2 type", created.getTasks().get(1).getTypeOfTask(), is(TaskType.BKM));
        assertThat("task 2 paycode", created.getTasks().get(1).getPaycode(), is(Paycode.NONE));
        assertThat("task 2 created", created.getTasks().get(1).getCreated(), is(LocalDate.now()));
        assertThat("task 2 approved", created.getTasks().get(1).getApproved(), is(IsNull.nullValue()));
        assertThat("task 2 payed", created.getTasks().get(1).getPayed(), is(IsNull.nullValue()));
        assertThat("task 2 targetFausts", created.getTasks().get(1).getTargetFausts(), is(IsNull.nullValue()));

        // Get the case we created
        response = getResponse("v1/api/cases/" + created.getId().toString());
        assertThat("status code", response.getStatus(), is(200));

        // Verify that the case matches the created case
        obj = response.readEntity(String.class);
        PromatCase fetched = mapper.readValue(obj, PromatCase.class);
        assertThat("fetched case is same as created", created.equals(fetched), is(true));
    }

    @Test
    public void testCreateCaseWithInvalidStatus() {

        // Create case with status ASSIGNED but no reviewer given
        CaseRequestDto dto = new CaseRequestDto()
                .withPrimaryFaust("7001111")
                .withTitle("Title for 7001111")
                .withDetails("Details for 7001111")
                .withMaterialType(MaterialType.BOOK)
                .withAssigned("2020-04-11")
                .withDeadline("2020-04-12")
                .withRelatedFausts(Arrays.asList("7002222", "7003333"))
                .withStatus(CaseStatus.ASSIGNED);

        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(400));

        // Change status to DONE (which is even worse)
        dto.setStatus(CaseStatus.DONE);
        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(400));

        // Get frustrated and remove the status
        dto.setStatus(null);
        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(201));

        // Tests for valid state CREATED and ASSIGNED with an reviewer set is
        // covered in previous tests
    }

    @Test
    public void testCheckCaseWithFaustExists() throws JsonProcessingException {

        // Check if various fausts exists.
        // (DBC's HttpClient currently do not support HEAD operations, so we use GET and throw away the response body)

        // Case with id 1, primary faust
        assertThat("status code", getResponse("v1/api/cases", Map.of("faust","001111")).getStatus(), is(200));

        // Case with id 1, related faust
        assertThat("status code", getResponse("v1/api/cases", Map.of("faust","002222")).getStatus(), is(200));

        // Case with id 2, related faust
        assertThat("status code", getResponse("v1/api/cases", Map.of("faust","006666")).getStatus(), is(200));

        // Check a faustnumber that does not exist
        assertThat("status code", getResponse("v1/api/cases", Map.of("faust","007777")).getStatus(), is(404));
    }

    @Test
    public void testGetCasesWithStatus() throws JsonProcessingException {

        // Cases with status CREATED
        // There are 8 cases preloaded into the database, others may have been created
        // by previously run tests
        Response response = getResponse("v1/api/cases", Map.of("status","CREATED"));
        assertThat("status code", response.getStatus(), is(200));
        String obj = response.readEntity(String.class);
        CaseSummaryList fetched = mapper.readValue(obj, CaseSummaryList.class);
        assertThat("Number of cases with status CREATED", fetched.getNumFound(), is(greaterThan(8)));

        // Cases with status CREATED
        response = getResponse("v1/api/cases", Map.of("status","CLOSED"));
        assertThat("status code", response.getStatus(), is(200));
        obj = response.readEntity(String.class);
        fetched = mapper.readValue(obj, CaseSummaryList.class);
        assertThat("Number of cases with status CREATED", fetched.getNumFound(), is(1));

        // Cases with status REJECTED
        response = getResponse("v1/api/cases", Map.of("status","REJECTED"));
        assertThat("status code", response.getStatus(), is(404));
        obj = response.readEntity(String.class);
        fetched = mapper.readValue(obj, CaseSummaryList.class);
        assertThat("Number of cases with status CREATED", fetched.getNumFound(), is(0));

        // Cases with status CLOSED or DONE
        response = getResponse("v1/api/cases", Map.of("status","CLOSED,DONE"));
        assertThat("status code", response.getStatus(), is(200));
        obj = response.readEntity(String.class);
        fetched = mapper.readValue(obj, CaseSummaryList.class);
        assertThat("Number of cases with status CLOSED or DONE", fetched.getNumFound(), is(2));
    }
}
