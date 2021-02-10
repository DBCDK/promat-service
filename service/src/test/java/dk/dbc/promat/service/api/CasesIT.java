/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.ListCasesParams;
import dk.dbc.promat.service.connector.PromatServiceConnectorException;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.dto.CaseSummaryList;
import dk.dbc.promat.service.dto.CriteriaOperator;
import dk.dbc.promat.service.dto.TaskDto;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PayCategory;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.Subject;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.persistence.TaskType;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

public class CasesIT extends ContainerTest {

    @Test
    public void testCreateCase() throws JsonProcessingException {

        CaseRequest dto = new CaseRequest();

        // Empty dto
        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(400));

        // title set
        dto.setTitle("Title for 1001111");
        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(400));

        // primaryFaust set
        dto.setPrimaryFaust("1001111");
        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(400));

        // creator set
        dto.setCreator(13);
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
        CaseRequest dto = new CaseRequest()
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
    public void testCreateCaseWithSubjectAndReviewerAndEditor() throws JsonProcessingException {

        // Note: This test depends on subjects with id 3 and 4, and reviewer with id 1
        // being injected by the dumpfiles also used by the reviewer and subject tests

        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("3001111")
                .withTitle("Title for 3001111")
                .withMaterialType(MaterialType.BOOK)
                .withReviewer(1)
                .withEditor(10)
                .withDeadline("2021-01-15")
                .withSubjects(Arrays.asList(3, 4));

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));

        String obj = response.readEntity(String.class);
        PromatCase created = mapper.readValue(obj, PromatCase.class);
        assertThat("primary faust", created.getPrimaryFaust(), is("3001111"));
        assertThat("title", created.getTitle(), is("Title for 3001111"));
        assertThat("materialType", created.getMaterialType(), is(MaterialType.BOOK));
        assertThat("status", created.getStatus(), is(CaseStatus.ASSIGNED));
        assertThat("reviwer id", created.getReviewer().getId(), is(1));
        assertThat("reviwer firstname", created.getReviewer().getFirstName(), is("Hans"));

        assertThat("editor id", created.getEditor().getId(), is(10));
        assertThat("editor firstname", created.getEditor().getFirstName(), is("Ed"));

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

        CaseRequest dto = new CaseRequest()
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
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("6001111")
                .withTitle("Title for 6001111")
                .withDetails("Details for 6001111")
                .withMaterialType(MaterialType.BOOK)
                .withReviewer(1)
                .withEditor(11)
                .withSubjects(Arrays.asList(3, 4))
                .withAssigned("2020-04-11")
                .withDeadline("2020-04-12")
                .withRelatedFausts(Arrays.asList("6002222", "6003333"))
                .withStatus(CaseStatus.ASSIGNED)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF),
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Arrays.asList(new String[] {"6002222", "6004444"}))
                ));

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));

        // Verify that the case has some data and a list with 2 tasks
        String obj = response.readEntity(String.class);
        PromatCase created = mapper.readValue(obj, PromatCase.class);
        assertThat("has 2 tasks", created.getTasks().size(), is(2));

        // Check the first created task
        assertThat("task 1 type", created.getTasks().get(0).getTaskType(), is(TaskType.GROUP_1_LESS_THAN_100_PAGES));
        assertThat("task 1 field type", created.getTasks().get(1).getTaskFieldType(), is(TaskFieldType.BRIEF));
        assertThat("task 1 paycategory", created.getTasks().get(0).getPayCategory(), is(PayCategory.BRIEF));
        assertThat("task 1 created", created.getTasks().get(0).getCreated(), is(LocalDate.now()));
        assertThat("task 1 approved", created.getTasks().get(0).getApproved(), is(IsNull.nullValue()));
        assertThat("task 1 payed", created.getTasks().get(0).getPayed(), is(IsNull.nullValue()));
        assertThat("task 1 targetFausts", created.getTasks().get(0).getTargetFausts(), is(IsNull.nullValue()));

        // Check the second created task
        assertThat("task 2 type", created.getTasks().get(1).getTaskType(), is(TaskType.GROUP_1_LESS_THAN_100_PAGES));
        assertThat("task 2 field type", created.getTasks().get(1).getTaskFieldType(), is(TaskFieldType.BRIEF));
        assertThat("task 2 paycategory", created.getTasks().get(1).getPayCategory(), is(PayCategory.BRIEF));
        assertThat("task 2 created", created.getTasks().get(1).getCreated(), is(LocalDate.now()));
        assertThat("task 2 approved", created.getTasks().get(1).getApproved(), is(IsNull.nullValue()));
        assertThat("task 2 payed", created.getTasks().get(1).getPayed(), is(IsNull.nullValue()));
        assertThat("task 2 targetFausts", created.getTasks().get(1).getTargetFausts(), is(IsNull.notNullValue()));
        assertThat("task 2 targetFausts size", created.getTasks().get(1).getTargetFausts().size(), is(2));
        assertThat("task 2 targetFausts contains", created.getTasks().get(1).getTargetFausts()
                        .stream().sorted().collect(Collectors.toList()).toString(),
                is(Arrays.asList(new String [] {"6002222", "6004444"})
                        .stream().sorted().collect(Collectors.toList()).toString()));
        assertThat("related fausts contains", created.getRelatedFausts()
                        .stream().sorted().collect(Collectors.toList()).toString(),
                is(Arrays.asList(new String [] {"6002222", "6003333", "6004444"})
                        .stream().sorted().collect(Collectors.toList()).toString()));

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
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("7001111")
                .withTitle("Title for 7001111")
                .withDetails("Details for 7001111")
                .withMaterialType(MaterialType.BOOK)
                .withAssigned("2020-04-11")
                .withDeadline("2020-04-12")
                .withRelatedFausts(Arrays.asList("7002222", "7003333"))
                .withStatus(CaseStatus.ASSIGNED);

        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(400));

        // Change status to EXPORTED (which is even worse)
        dto.setStatus(CaseStatus.EXPORTED);
        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(400));

        // Get frustrated and remove the status
        dto.setStatus(null);
        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(201));

        // Tests for valid state CREATED and ASSIGNED with an reviewer set is
        // covered in previous tests
    }

    @Test
    public void testCheckCaseWithFaustExists() {

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
    public void testGetCasesWithStatus() throws PromatServiceConnectorException {

        // There are 8 cases preloaded into the database, others may have been created
        // by previously run tests
        // Cases with status CLOSED
        CaseSummaryList fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withStatus(CaseStatus.CLOSED));
        assertThat("Number of cases with status CLOSED", fetched.getNumFound(), is(greaterThanOrEqualTo(1)));

        // Cases with status CREATED
        fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withStatus(CaseStatus.CREATED));
        assertThat("Number of cases with status CREATED", fetched.getNumFound(), is(greaterThanOrEqualTo(1)));

        // Cases with status REJECTED
        fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withStatus(CaseStatus.REJECTED));
        assertThat("Number of cases with status CREATED", fetched.getNumFound(), is(0));

        // Cases with status CLOSED or EXPORTED
        fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withStatus(CaseStatus.CLOSED)
                .withStatus(CaseStatus.EXPORTED));
        assertThat("Number of cases with status CLOSED or EXPORTED", fetched.getNumFound(), is(greaterThanOrEqualTo(2)));
    }

    @Test
    public void testGetCasesWithLimit() throws PromatServiceConnectorException {

        // Get 4 cases with status CREATED - there is 8 or more in the database
        final CaseSummaryList fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withStatus(CaseStatus.CREATED)
                .withLimit(4));
        assertThat("Number of cases with status CREATED", fetched.getNumFound(), is(4));

        // Check id ordering
        assertThat(fetched.getCases().get(0).getId(), is(1));
        assertThat(fetched.getCases().get(1).getId(), is(2));
        assertThat(fetched.getCases().get(2).getId(), is(3));
        assertThat(fetched.getCases().get(3).getId(), is(5));
    }

    @Test
    public void testGetCasesWithNoOrInvalidFilters() throws JsonProcessingException {

        Response response = getResponse("v1/api/cases");
        assertThat("status code", response.getStatus(), is(200));
        String obj = response.readEntity(String.class);
        CaseSummaryList fetched = mapper.readValue(obj, CaseSummaryList.class);
        assertThat("Number of cases with no filters", fetched.getNumFound(), is(greaterThanOrEqualTo(11)));

        response = getResponse("v1/api/cases", Map.of("faust", "  "));
        assertThat("status code", response.getStatus(), is(200));
        obj = response.readEntity(String.class);
        fetched = mapper.readValue(obj, CaseSummaryList.class);
        assertThat("Number of cases with status as blankspaces", fetched.getNumFound(), is(greaterThanOrEqualTo(11)));

        response = getResponse("v1/api/cases", Map.of("status", "  "));
        assertThat("status code", response.getStatus(), is(200));
        obj = response.readEntity(String.class);
        fetched = mapper.readValue(obj, CaseSummaryList.class);
        assertThat("Number of cases with status as blankspaces", fetched.getNumFound(), is(greaterThanOrEqualTo(11)));

        response = getResponse("v1/api/cases", Map.of("status", "NO_SUCH_STATUS"));
        assertThat("status code", response.getStatus(), is(400));
    }

    @Test
    public void testGetCasesWithLimitAndFrom() throws PromatServiceConnectorException {

        // Get 4 cases with status CREATED from id 1
        CaseSummaryList fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withStatus(CaseStatus.CREATED)
                .withLimit(4)
                .withFrom(0));
        assertThat("Number of cases with status CREATED", fetched.getNumFound(), is(4));

        // Check id ordering
        assertThat(fetched.getCases().get(0).getId(), is(1));
        assertThat(fetched.getCases().get(1).getId(), is(2));
        assertThat(fetched.getCases().get(2).getId(), is(3));
        assertThat(fetched.getCases().get(3).getId(), is(5));

        // Get 4 cases with status CREATED from id 7
        fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withStatus(CaseStatus.CREATED)
                .withLimit(4)
                .withFrom(5));
        assertThat("Number of cases with status CREATED", fetched.getNumFound(), is(4));

        // Check id ordering
        assertThat(fetched.getCases().get(0).getId(), is(7));
        assertThat(fetched.getCases().get(1).getId(), is(8));
        assertThat(fetched.getCases().get(2).getId(), is(10));
        assertThat(fetched.getCases().get(3).getId(), is(11));

        // Get All cases with status created, then get the last few of them
        fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withStatus(CaseStatus.CREATED));
        assertThat("Number of cases with status CREATED", fetched.getNumFound(), is(greaterThanOrEqualTo(8)));
        final int lastId = fetched.getCases().get(fetched.getCases().size() - 1).getId();

        fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withStatus(CaseStatus.CREATED)
                .withLimit(4)
                .withFrom(lastId - 1));
        assertThat("Number of cases with status CREATED", fetched.getNumFound(), is(1));
        assertThat(fetched.getCases().get(0).getId(), is(lastId));
    }

    @Test
    public void testGetCasesWithReviewer() throws PromatServiceConnectorException {

        // Cases with reviewer '9999' (no such user)
        Response response = getResponse("v1/api/cases", Map.of("reviewer", 9999));
        assertThat("status code", response.getStatus(), is(404));

        // Cases with reviewer '10' (not an reviewer)
        response = getResponse("v1/api/cases", Map.of("reviewer", 10));
        assertThat("status code", response.getStatus(), is(404));

        // Cases with reviewer '1'
        CaseSummaryList fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withReviewer(1));
        assertThat("Number of cases with reviewer 1", fetched.getNumFound(), is(greaterThanOrEqualTo(1)));

        // Cases with reviewer '1' and status 'REJECTED' (no cases with status REJECTED)
        fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withReviewer(1)
                .withStatus(CaseStatus.REJECTED));
        assertThat("Number of cases with reviewer 1 and status REJECTED", fetched.getNumFound(), is(0));

        // Cases with editor '2' and status 'CREATED'
        fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withReviewer(2)
                .withStatus(CaseStatus.CREATED));
        assertThat("Number of cases with reviewer 2 and status CREATED", fetched.getNumFound(), is(1));
        assertThat("case id", fetched.getCases().get(0).getId(), is(11));
    }

    @Test
    public void testGetCasesWithEditor() throws PromatServiceConnectorException {

        // Cases with editor '9999' (no such user)
        Response response = getResponse("v1/api/cases", Map.of("editor", 9999));
        assertThat("status code", response.getStatus(), is(404));

        // Cases with editor '5' (not an editor)
        response = getResponse("v1/api/cases", Map.of("editor", 5));
        assertThat("status code", response.getStatus(), is(404));

        // Cases with editor '10'
        CaseSummaryList fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withEditor(10));
        assertThat("Number of cases with editor 10", fetched.getNumFound(), is(greaterThanOrEqualTo(1)));

        // Cases with editor '11' and status 'REJECTED' (no cases with status REJECTED)
        fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withEditor(11)
                .withStatus(CaseStatus.REJECTED));
        assertThat("Number of cases with editor 11 and status REJECTED", fetched.getNumFound(), is(0));

        // Cases with editor '11' and status 'CREATED'
        fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withEditor(11)
                .withStatus(CaseStatus.CREATED));
        assertThat("Number of cases with editor 11 and status CREATED", fetched.getNumFound(), is(3));
        assertThat("case id", fetched.getCases().get(0).getId(), is(10));
    }

    @Test
    public void testGetCasesWithTitle() throws PromatServiceConnectorException {

        // Cases with part of title 'no_such_title'
        Response response = getResponse("v1/api/cases", Map.of("title", "no_such_title"));
        assertThat("status code", response.getStatus(), is(404));

        // Cases with part of title 'Title'
        CaseSummaryList fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withTitle("Title"));
        assertThat("Number of cases with 'Title' as part of title", fetched.getNumFound(), is(greaterThanOrEqualTo(11)));

        // Cases with part of title 'title' (lowercase starting 't')
        fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withTitle("title"));
        assertThat("Number of cases with 'title' as part of title", fetched.getNumFound(), is(greaterThanOrEqualTo(11)));

        // Cases with full title 'Title for 001111'
        fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withTitle("Title for 001111"));
        assertThat("Number of cases with title 'Title for 001111'", fetched.getNumFound(), is(1));
    }

    @Test
    public void getCasesWithTrimmedWeekcode() throws PromatServiceConnectorException {
        CaseSummaryList fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withTrimmedWeekcode("202102"));
        assertThat("Number of cases with trimmed weekcode '202102'", fetched.getNumFound(), is(0));

        fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withTrimmedWeekcode("202101"));
        assertThat("Number of cases with trimmed weekcode '202101'", fetched.getNumFound(), is(1));
        assertThat("Case with trimmed weekcode '202101'", fetched.getCases().get(0).getId(), is(1));

        fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withTrimmedWeekcodeOperator(CriteriaOperator.LESS_THAN_OR_EQUAL_TO)
                .withTrimmedWeekcode("202101"));
        assertThat("Number of cases with weekcode less than '202101'", fetched.getNumFound(), is(2));
        assertThat("1st case with weekcode less than '202101'", fetched.getCases().get(0).getId(), is(1));
        assertThat("2nd case with weekcode less than '202101'", fetched.getCases().get(1).getId(), is(2));
    }

    @Test
    public void testEditCase() throws JsonProcessingException, PromatServiceConnectorException {

        // Update of unknown case - should return 404 NOT FOUND
        CaseRequest dto = new CaseRequest();
        Response response = postResponse("v1/api/cases/9876", dto);
        assertThat("status code", response.getStatus(), is(404));

        // Attemtp to set field 'assigned' - should return 400 BAD REQUEST
        dto = new CaseRequest().withAssigned("2020-11-18");
        response = postResponse("v1/api/cases/1", dto);
        assertThat("status code", response.getStatus(), is(400));

        // Attemtp to set field 'status' - should return 400 BAD REQUEST
        dto = new CaseRequest().withStatus(CaseStatus.ASSIGNED);
        response = postResponse("v1/api/cases/1", dto);
        assertThat("status code", response.getStatus(), is(400));

        // Attemtp to set field 'reviewer' with non existing reviewer - should return 400 BAD REQUEST
        dto = new CaseRequest().withReviewer(9999);
        response = postResponse("v1/api/cases/1", dto);
        assertThat("status code", response.getStatus(), is(400));

        // Attemtp to set field 'editor' with non existing editor - should return 400 BAD REQUEST
        dto = new CaseRequest().withEditor(9999);
        response = postResponse("v1/api/cases/1", dto);
        assertThat("status code", response.getStatus(), is(400));

        // Attemtp to set field 'subject' with non existing subject - should return 400 BAD REQUEST
        dto = new CaseRequest().withSubjects(Arrays.asList(9999));
        response = postResponse("v1/api/cases/1", dto);
        assertThat("status code", response.getStatus(), is(400));

        // Create a new case
        dto = new CaseRequest()
                .withTitle("Title for 8001111")
                .withDetails("Details for 8001111")
                .withPrimaryFaust("8001111")
                .withRelatedFausts(Arrays.asList("8002222", "8003333"))
                .withEditor(10)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2020-12-18")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Arrays.asList(new String[] {"8002222", "8004444"})),
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BKM)
                ));
        response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Update case - should return 200 OK
        dto = new CaseRequest()
                .withTitle("New title for 8001111")
                .withDetails("New details for 8001111")
                .withPrimaryFaust("8002222")
                .withRelatedFausts(Arrays.asList("8001111", "8003333", "8004444"))
                .withReviewer(1)
                .withEditor(11)
                .withSubjects(Arrays.asList(5))
                .withDeadline("2021-01-18")
                .withMaterialType(MaterialType.MULTIMEDIA)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Arrays.asList(new String[] {"8001111", "8004444"})),
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BKM),
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.DESCRIPTION)
                ));
        PromatCase updated = promatServiceConnector.updateCase(created.getId(), dto);

        assertThat("updated case did not change id", updated.getId(), is(created.getId()));
        assertThat("updated title", updated.getTitle(), is("New title for 8001111"));
        assertThat("updated details", updated.getDetails(), is("New details for 8001111"));
        assertThat("updated primary faust", updated.getPrimaryFaust(), is("8002222"));
        assertThat("updated relatedFausts", updated.getRelatedFausts().
                stream()
                .sorted()
                .collect(Collectors.toList())
                .equals(Arrays.stream(new String[]{"8001111", "8003333", "8004444"})
                        .collect(Collectors.toList())), is(true));
        assertThat("updated reviewer", updated.getReviewer().getId(), is(1));
        assertThat("status changed", updated.getStatus(), is(CaseStatus.ASSIGNED));
        assertThat("updated editor", updated.getEditor().getId(), is(11));
        assertThat("updated subjects size", updated.getSubjects().size(), is(1));
        List<Integer> actual = updated.getSubjects()
                .stream()
                .sorted(Comparator.comparingInt(Subject::getId))
                .map(s -> s.getId()).collect(Collectors.toList());
        List<Integer> expected = new ArrayList<Integer>();
        expected.add(5);
        assertThat("updated subject ids", actual.equals(expected), is(true) );
        assertThat("updated deadline", updated.getDeadline(),  is(LocalDate.parse("2021-01-18")));
        assertThat("updated materialtype", updated.getMaterialType(), is(MaterialType.MULTIMEDIA));
        assertThat("updated tasks (should remain unchanged)", updated.getTasks().equals(created.getTasks()));

        // Fetch the case, check that it matches the updated case
        response = getResponse("v1/api/cases/" + created.getId().toString());
        assertThat("status code", response.getStatus(), is(200));
        PromatCase fetched = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("fetched case is same as updated", updated.equals(fetched), is(true));

        // Check that the BKM task is not approved
        PromatTask bkmTask = fetched.getTasks().stream()
                .filter(task -> task.getTaskFieldType().equals(TaskFieldType.BKM))
                .findFirst()
                .get();
        assertThat("not approved", bkmTask.getApproved(), is(nullValue()));

        // Change status to PENDING_CLOSE
        dto.setStatus(CaseStatus.PENDING_CLOSE);
        response = postResponse("v1/api/cases/" + created.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status", updated.getStatus(), is(CaseStatus.PENDING_CLOSE));

        // Check that the BKM task is now approved
        bkmTask = updated.getTasks().stream()
                .filter(task -> task.getTaskFieldType().equals(TaskFieldType.BKM))
                .findFirst()
                .get();
        assertThat("approved", bkmTask.getApproved(), is(notNullValue()));

        // And make sure that no other task has been approved
        long approvedTasks = fetched.getTasks().stream()
                .filter(task -> !task.getTaskFieldType().equals(TaskFieldType.BKM))
                .filter(task -> task.getApproved() != null)
                .count();
        assertThat("no other tasks is approved", approvedTasks, is(0L));

        // Close the case
        dto.setStatus(CaseStatus.CLOSED);
        response = postResponse("v1/api/cases/" + created.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status", updated.getStatus(), is(CaseStatus.CLOSED));

        // Try to reopen the case by changing status to ASSIGNED
        dto.setStatus(CaseStatus.ASSIGNED);
        response = postResponse("v1/api/cases/" + created.getId(), dto);
        assertThat("status code", response.getStatus(), is(400));

        // Reopen the case. The case has been assigned to, so status should move to ASSIGNED
        dto.setStatus(CaseStatus.CREATED);
        response = postResponse("v1/api/cases/" + created.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status", updated.getStatus(), is(CaseStatus.ASSIGNED));

        // Update the case with the dto as-is
        response = postResponse("v1/api/cases/" + created.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));

        // Change the primary faustnumber to something completely new
        dto.setPrimaryFaust("8005555");
        response = postResponse("v1/api/cases/" + created.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));

        // Try to change the primary faustnumber to something that exists
        dto.setPrimaryFaust("001111");
        response = postResponse("v1/api/cases/" + created.getId(), dto);
        assertThat("status code", response.getStatus(), is(409));

        // Try to add a faustnumber to related faustnumbers, that exists on an active case
        dto.setRelatedFausts(Arrays.asList("8001111", "8003333", "8004444", "001111"));
        response = postResponse("v1/api/cases/" + created.getId(), dto);
        assertThat("status code", response.getStatus(), is(409));
    }

    @Test
    public void testCreateWithFieldsCreatorAuthorPublisherAndWeekcode() throws JsonProcessingException {
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("9001211")
                .withTitle("Title for 9001211")
                .withDetails("Details for 9001211")
                .withTasks(Collections.singletonList(new TaskDto().withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES).withTaskFieldType(TaskFieldType.BKM)))
                .withPublisher("Publisher for 9001211")
                .withAuthor("Author for 9001211")
                .withWeekCode("Weekcode for 9001211")
                .withMaterialType(MaterialType.BOOK);

        // Case 1: Create case with creator.
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Case 2:Check that creator can be set.
        dto.setCreator(11);
        response = postResponse(String.format("v1/api/cases/%s", created.getId()), dto);
        assertThat("status code", response.getStatus(), is(200));

        // Case 3: Check that the creator no longer can be changed.
        dto.setCreator(13);
        response = postResponse(String.format("v1/api/cases/%s", created.getId()), dto);
        assertThat("status code", response.getStatus(), is(401));

        // Case 4: Check that what we created, is also what is returned.
        response = getResponse(String.format("v1/api/cases/%s", created.getId()));
        assertThat("status code", response.getStatus(), is(200));
        PromatCase fetched = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("publisher", fetched.getPublisher(), is(dto.getPublisher()));
        assertThat("author", fetched.getAuthor(), is(dto.getAuthor()));
        assertThat("weekcode", fetched.getWeekCode(), is(dto.getWeekCode()));
        assertThat("creator", fetched.getCreator().getId(), is(11));
    }

    @Test
    public void testCreateCaseWithTasksWithMissingFields() throws JsonProcessingException {

        // Create case without tasktype and taskfieldtype
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("9001111")
                .withTitle("Title for 9001111")
                .withDetails("Details for 9001111")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.asList(
                        new TaskDto()
                ));

        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(400));

        // Create case without taskfieldtype
        dto = new CaseRequest()
                .withPrimaryFaust("9001111")
                .withTitle("Title for 9001111")
                .withDetails("Details for 9001111")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                ));

        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(400));

        // Create case with tasktype and taskfieldtype - should succeed now
        dto = new CaseRequest()
                .withPrimaryFaust("9001111")
                .withTitle("Title for 9001111")
                .withDetails("Details for 9001111")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                ));

        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(201));
    }

    @Test
    public void testAddFirstTask() throws JsonProcessingException {

        // Create new case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("12001111")
                .withTitle("Title for 12001111")
                .withDetails("Details for 12001111")
                .withMaterialType(MaterialType.BOOK);

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        TaskDto taskDto = new TaskDto()
                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                .withTaskFieldType(TaskFieldType.BRIEF)
                .withData("anoroc kcuf")
                .withTargetFausts(Arrays.asList("12002222", "12003333"));
        response = postResponse("v1/api/cases/" + created.getId() + "/tasks", taskDto);
        assertThat("status code", response.getStatus(), is(201));
        PromatTask createdTask = mapper.readValue(response.readEntity(String.class), PromatTask.class);
        assertThat("created task targetfausts is not null", createdTask.getTargetFausts(), is(notNullValue()));
        assertThat("created task has 2 targetfausts", createdTask.getTargetFausts().size(), is(2));

        // Get the case we created
        response = getResponse("v1/api/cases/" + created.getId().toString());
        assertThat("status code", response.getStatus(), is(200));
        PromatCase fetched = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Check that the two targetfausts has been added as related fausts
        assertThat("case has related fausts", fetched.getRelatedFausts(), is(notNullValue()));
        assertThat("case has 2 related fausts", fetched.getRelatedFausts().size(), is(2));
        assertThat("case has the expected 2 related fausts", fetched.getRelatedFausts().stream().sorted().collect(Collectors.toList()).equals(Arrays.asList("12002222", "12003333")));

        // Check that the case now has 1 task which matches the created task
        assertThat("case tasks is not null", fetched.getTasks(), is(notNullValue()));
        assertThat("case has 1 task", fetched.getTasks().size(), is(1));
        assertThat("task is the created task", fetched.getTasks().get(0).equals(createdTask));

        // Check that the case now has 1 task with the expected types and data
        assertThat("task is expected TaskType", fetched.getTasks().get(0).getTaskType(), is(TaskType.GROUP_1_LESS_THAN_100_PAGES));
        assertThat("task is expected TaskFieldType", fetched.getTasks().get(0).getTaskFieldType(), is(TaskFieldType.BRIEF));
        assertThat("task has expected data", fetched.getTasks().get(0).getData().equals("anoroc kcuf"));
    }

    @Test
    public void testAddNextTask() throws JsonProcessingException {

        // Create new case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("13001111")
                .withTitle("Title for 13001111")
                .withDetails("Details for 13001111")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                ));
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("case has 1 task", created.getTasks().size(), is(1));

        TaskDto taskDto = new TaskDto()
                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                .withTaskFieldType(TaskFieldType.BRIEF);
        response = postResponse("v1/api/cases/" + created.getId() + "/tasks", taskDto);
        assertThat("status code", response.getStatus(), is(201));

        // Get the case we created
        response = getResponse("v1/api/cases/" + created.getId().toString());
        assertThat("status code", response.getStatus(), is(200));
        PromatCase fetched = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("case has 2 tasks", fetched.getTasks().size(), is(2));
    }

    @Test
    public void testAddTaskWithFaustInUse() throws JsonProcessingException {

        // Create case 1
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("14001111")
                .withRelatedFausts(Arrays.asList("14002222"))
                .withTitle("Title for 14001111")
                .withDetails("Details for 14001111")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Arrays.asList("14003333"))
                ));
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));

        // Create case 2
        dto = new CaseRequest()
                .withPrimaryFaust("15001111")
                .withRelatedFausts(Arrays.asList("15002222"))
                .withTitle("Title for 15001111")
                .withDetails("Details for 15001111")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Arrays.asList("15003333"))
                ));
        response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase secondTask = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        TaskDto taskDto = new TaskDto()
                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                .withTaskFieldType(TaskFieldType.BRIEF)
                .withTargetFausts(Arrays.asList("15002222"));

        // Adding a task with a targetfaust used by the case is allowed
        response = postResponse("v1/api/cases/" + secondTask.getId() + "/tasks", taskDto);
        assertThat("status code", response.getStatus(), is(201));

        // Adding a task with a targetfaust used by another task on the case is allowed
        taskDto.setTargetFausts(Arrays.asList("15003333"));
        response = postResponse("v1/api/cases/" + secondTask.getId() + "/tasks", taskDto);
        assertThat("status code", response.getStatus(), is(201));

        // Adding a task with a targetfaust used by the another case is not allowed
        taskDto.setTargetFausts(Arrays.asList("14002222"));
        response = postResponse("v1/api/cases/" + secondTask.getId() + "/tasks", taskDto);
        assertThat("status code", response.getStatus(), is(409));
    }

    @Test
    public void testAddTaskWithMissingFields() throws JsonProcessingException {

        // Create new case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("16001111")
                .withTitle("Title for 16001111")
                .withDetails("Details for 16001111")
                .withMaterialType(MaterialType.BOOK)
                .withCreator(13);
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        TaskDto taskDto = new TaskDto();

        // Empty dto
        assertThat("status code", postResponse("v1/api/cases/" + created.getId() + "/tasks", taskDto).getStatus(), is(400));

        // Add TaskType
        taskDto.setTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES);
        assertThat("status code", postResponse("v1/api/cases/" + created.getId() + "/tasks", taskDto).getStatus(), is(400));

        // Add TaskFieldType
        taskDto.setTaskFieldType(TaskFieldType.BRIEF);
        assertThat("status code", postResponse("v1/api/cases/" + created.getId() + "/tasks", taskDto).getStatus(), is(201));
    }

    @Test
    public void testCreateCaseWithTaskWithUsedFaust() {

        // Create case 1
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("17001111")
                .withRelatedFausts(Arrays.asList("17002222"))
                .withTitle("Title for 17001111")
                .withDetails("Details for 17001111")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Arrays.asList("17003333"))
                ));

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));

        // Create case 2
        dto = new CaseRequest()
                .withPrimaryFaust("18001111")
                .withRelatedFausts(Arrays.asList("18002222"))
                .withTitle("Title for 18001111")
                .withDetails("Details for 18001111")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Arrays.asList("17003333"))
                ));
        response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(409));
    }

    @Test
    public void testDeleteCase() throws JsonProcessingException {
        int CASEID_TO_BE_DELETED = 12;

        // Check that the case at first IS in the response
        Response response = getResponse("v1/api/cases");
        String obj = response.readEntity(String.class);
        CaseSummaryList fetched = mapper.readValue(obj, CaseSummaryList.class);
        assertThat("Case with this id exists",
                fetched.getCases().stream().filter(procase -> procase.getId() == CASEID_TO_BE_DELETED).count(),
                is(1L));

        // Delete it
        response = deleteResponse(String.format("v1/api/cases/%s", CASEID_TO_BE_DELETED));
        assertThat("status code", response.getStatus(), is(200));

        // Check that the case is no longer to be found in the returned cases
        response = getResponse("v1/api/cases");
        obj = response.readEntity(String.class);
        fetched = mapper.readValue(obj, CaseSummaryList.class);
        assertThat("No case with this id",
                fetched.getCases().stream().filter(procase -> procase.getId() == CASEID_TO_BE_DELETED).count(),
                is(0L));

        // Check that the case still can be fetched directly
        response = getResponse(String.format("v1/api/cases/%s", CASEID_TO_BE_DELETED));
        assertThat("status code", response.getStatus(), is(200));

        // Give me the list of cases where status is deleted
        response = getResponse("v1/api/cases", Map.of("status","DELETED"));
        obj = response.readEntity(String.class);
        fetched = mapper.readValue(obj, CaseSummaryList.class);
        assertThat("The deleted case is amongst the cases with flagged 'deleted'",
                fetched.getCases().stream().filter(procase -> procase.getId() == CASEID_TO_BE_DELETED).count(),
                is(1L));
    }

    @Test
    public void testApproveCaseWithUnfinishedMetakompasTask() throws JsonProcessingException {

        // Reviewer has done his job and sets the case in state PENDING_APPROVAL
        CaseRequest requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        Response response = postResponse("v1/api/cases/15", requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Editor finds an error and returns the case to further editing
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_ISSUES);
        response = postResponse("v1/api/cases/15", requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Reviewer has fixed the errors and sets the case in state PENDING_APPROVAL again
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        response = postResponse("v1/api/cases/15", requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Check that we are not able to set state PENDING_EXTERNAL directly
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_EXTERNAL);
        response = postResponse("v1/api/cases/15", requestDto);
        assertThat("status code", response.getStatus(), is(400));

        // Editor approves the case and puts the case in state APPROVED
        requestDto.setStatus(CaseStatus.APPROVED);
        response = postResponse("v1/api/cases/15", requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Check that status is now PENDING_EXTERNAL due to the not approved metakompas task
        // and that all but the metakompas task is not approved
        PromatCase updated = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status", updated.getStatus(), is(CaseStatus.PENDING_EXTERNAL));
        assertThat("approved", updated.getTasks().stream().filter(task -> task.getApproved() != null).count(), is(3L));
        assertThat("approved", updated.getTasks().stream().filter(task -> task.getTaskFieldType() == TaskFieldType.METAKOMPAS)
                .findFirst().get().getApproved(), is(nullValue()));

        // Delete the case so that we dont mess up payments tests
        response = deleteResponse("v1/api/cases/15");
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testApproveCaseWithApprovedMetakompasTask() throws JsonProcessingException {

        // Reviewer has done his job and sets the case in state PENDING_APPROVAL
        CaseRequest requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        Response response = postResponse("v1/api/cases/16", requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Editor approves the case and puts the case in state APPROVED
        requestDto.setStatus(CaseStatus.APPROVED);
        response = postResponse("v1/api/cases/16", requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Check that status is now APPROVED
        PromatCase updated = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status", updated.getStatus(), is(CaseStatus.APPROVED));
        assertThat("approved", updated.getTasks().stream().filter(task -> task.getApproved() != null).count(), is(3L));

        // Delete the case so that we dont mess up payments tests
        response = deleteResponse("v1/api/cases/16");
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testApproveCase() throws JsonProcessingException {

        // Reviewer has done his job and sets the case in state PENDING_APPROVAL
        CaseRequest requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        Response response = postResponse("v1/api/cases/17", requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Editor approves the case and puts the case in state APPROVED
        requestDto.setStatus(CaseStatus.APPROVED);
        response = postResponse("v1/api/cases/17", requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Check that status is now APPROVED
        PromatCase updated = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status", updated.getStatus(), is(CaseStatus.APPROVED));
        assertThat("approved", updated.getTasks().stream().filter(task -> task.getApproved() != null).count(), is(2L));

        // Delete the case so that we dont mess up payments tests
        response = deleteResponse("v1/api/cases/17");
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testLookUpOnAuthorField() throws PromatServiceConnectorException {
        Set<Integer> expected = createCasesWithAuthorsAndWeekcodes(
                Map.of(1, List.of("Klavs Hansen"), 2, List.of("Niels (hansen)-Nielsen"), 3, List.of("HANSE HANSEN"))
        );

        Set<Integer> others = createCasesWithAuthorsAndWeekcodes(
                Map.of(4, List.of("Ole Olesen"), 5, List.of("Søren Sørensen"))
        );

        CaseSummaryList fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withAuthor("Hans"));
        assertThat("Number of cases with author 'Hans'", fetched.getNumFound(), is(3));

        Set<Integer> actual = fetched.getCases().stream().map( c -> c.getId()).collect(Collectors.toSet());
        assertThat("caseIds match", actual, is(expected));

        Response response;
        // Delete cases so that we dont mess up payments tests
        for(Integer cid : expected) {
            response = deleteResponse("v1/api/cases/"+cid);
            assertThat("status code", response.getStatus(), is(200));
        }

        for(Integer cid : others) {
            response = deleteResponse("v1/api/cases/"+cid);
            assertThat("status code", response.getStatus(), is(200));
        }

    }

    @Test
    public void testLookupOfWeekcodes() throws PromatServiceConnectorException {
        Set<Integer> expected = createCasesWithAuthorsAndWeekcodes(
                Map.of(6, List.of("NONE", "BKM202102"), 7, List.of("NONE", "BKM202102"), 8, List.of("NONE", "BKM202102"))
        );

        Integer someOther = createCaseWithAuthorAndWeekCode(9, "NONE", "BKM202052").getId();


        CaseSummaryList fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withWeekCode("BKM202102"));
        assertThat("Cases found", fetched.getNumFound(), greaterThanOrEqualTo(3));
        Set<Integer> actual = fetched.getCases().stream().map( c -> c.getId()).collect(Collectors.toSet());
        assertThat("cases are all there", actual.containsAll(expected));
        assertThat("Case prior to weekcode is not there", !actual.contains(someOther));


        // Add the 'other' to the ones that needs to be deleted
        expected.add(someOther);

        // Delete cases so that we dont mess up payments tests
        Response response;
        for(Integer cid : expected) {
            response = deleteResponse("v1/api/cases/"+cid);
            assertThat("status code", response.getStatus(), is(200));
        }
    }

    @Test
    void getFulltext(@TempDir Path tempDir) throws PromatServiceConnectorException, IOException, InterruptedException {
        final String fulltextLink = getWiremockUrl("/testsite/downloads/downloadfile.php?file=Data16Bytes.dat&cd=attachment+filename");
        promatServiceConnector.updateCase(1, new CaseRequest().withFulltextLink(fulltextLink));

        var apiLink = promatServiceBaseUrl + "/v1/api/cases/1/fulltext";

        var client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        var request = HttpRequest.newBuilder()
                .uri(URI.create(apiLink))
                .build();

        final HttpResponse<Path> httpResponse = client.send(request,
                HttpResponse.BodyHandlers.ofFileDownload(tempDir, WRITE, CREATE_NEW));

        assertThat("file with name Data16Bytes.dat was downloaded",
                Files.exists(tempDir.resolve("Data16Bytes.dat")), is(true));
        assertThat("file content", Files.readAllBytes(httpResponse.body()),
                is(Files.readAllBytes(Path.of("src/test/resources/__files/body-testsite-downloads-downloadfile.php-0KwVu.php"))));
    }

    @Test
    void addDraftOrUpdateExistingCase() throws JsonProcessingException, PromatServiceConnectorException {
        final CaseRequest caseRequest = new CaseRequest()
                .withPrimaryFaust("11111111")
                .withTitle("Title for draft - print")
                .withMaterialType(MaterialType.BOOK)
                .withFulltextLink("link");

        final Response caseCreatedResponse = postResponse("v1/api/drafts", caseRequest);
        assertThat("case created status code", caseCreatedResponse.getStatus(), is(201));

        final PromatCase caseCreated = mapper.readValue(caseCreatedResponse.readEntity(String.class), PromatCase.class);
        assertThat("case created", promatServiceConnector.getCase(caseCreated.getId()).getTitle(),
                is(caseRequest.getTitle()));

        final CaseRequest matchingCaseRequest = new CaseRequest()
                .withPrimaryFaust("22222222")
                .withTitle("Title for draft - ebook")
                .withMaterialType(MaterialType.BOOK)
                .withFulltextLink("updated link");

        final Response caseUpdatedResponse = postResponse("v1/api/drafts", matchingCaseRequest);
        assertThat("case updated status code", caseUpdatedResponse.getStatus(), is(200));
        assertThat("case updated",
                mapper.readValue(caseUpdatedResponse.readEntity(String.class), PromatCase.class).getId(),
                is(caseCreated.getId()));

        final PromatCase caseUpdated = promatServiceConnector.getCase(caseCreated.getId());
        assertThat("manifestation added as related faust",
                caseUpdated.getRelatedFausts().contains(matchingCaseRequest.getPrimaryFaust()),
                is(true));
        assertThat("fulltext link updated", caseUpdated.getFulltextLink(),
                is(matchingCaseRequest.getFulltextLink()));
        assertThat("title not updated", caseUpdated.getTitle(),
                is(caseCreated.getTitle()));
    }

    private PromatCase createCaseWithAuthorAndWeekCode(Integer someUniqueIdNumber, String author, String weekCode) {
        CaseRequest dto = new CaseRequest();

        dto.withTitle(String.format("Title for %s", someUniqueIdNumber))
                .withCreator(13)
                .withEditor(10)
                .withPrimaryFaust(String.format("400000000%s", someUniqueIdNumber))
                .withMaterialType(MaterialType.BOOK)
                .withAuthor(author)
                .withWeekCode(weekCode);

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        try {
            PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);
            return created;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Set<Integer> createCasesWithAuthorsAndWeekcodes(Map<Integer, List<String>> caseData) {
        Set<Integer> expected = new HashSet<>();
        caseData.forEach((id, fieldValues) ->
        {
            String author = null;
            String weekCode = null;
            switch (fieldValues.size()) {
                case 0:
                    break;
                case 1:
                    author = fieldValues.get(0);
                    break;
                default:
                    author = fieldValues.get(0);
                    weekCode = fieldValues.get(1);
            }

            expected.add(createCaseWithAuthorAndWeekCode(id, author, weekCode).getId());
        });
        return expected;
    }
}
