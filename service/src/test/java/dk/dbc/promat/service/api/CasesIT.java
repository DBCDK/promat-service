/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.connector.PromatServiceConnectorException;
import dk.dbc.promat.service.connector.PromatServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.dto.CaseSummaryList;
import dk.dbc.promat.service.dto.CreateStatus;
import dk.dbc.promat.service.dto.CreateStatusDto;
import dk.dbc.promat.service.dto.CriteriaOperator;
import dk.dbc.promat.service.dto.ListCasesParams;
import dk.dbc.promat.service.dto.TagList;
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
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;

@SuppressWarnings("resource")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CasesIT extends ContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CasesIT.class);

    @Test
    public void testCreateCase() throws JsonProcessingException, PromatServiceConnectorException {

        CaseRequest dto = new CaseRequest();

        // Empty dto
        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(400));

        // title set
        dto.setTitle("Title for 11001111");
        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(400));

        // primaryFaust set
        dto.setPrimaryFaust("11001111");
        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(400));

        // materialType set
        dto.setMaterialType(MaterialType.BOOK);
        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, CREATED);

        // Check that the returned object has title, primary faust and materialtype set
        assertThat("primary faust", created.getPrimaryFaust(), is("11001111"));
        assertThat("title", created.getTitle(), is("Title for 11001111"));
        assertThat("materialType", created.getMaterialType(), is(MaterialType.BOOK));
    }

    @Test
    public void testCreateCaseForExistingPrimaryFaust() {

        // New case with primary faust 001111 which already exists
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("001111")
                .withTitle("Title for 001111")
                .withMaterialType(MaterialType.MOVIE);

        assertThat("status code", postResponse("v1/api/cases", dto).getStatus(), is(409));

        // New case with primary faust 002222 which exists as related faust
        dto.setPrimaryFaust("002222");
        dto.setTitle("New title for 002222");
        postAndAssert("v1/api/cases", dto, CONFLICT);

        // New case with primary faust 2004444 and related faust 2005555 (all is good)
        dto.setPrimaryFaust("2004444");
        postAndAssert("v1/api/cases", dto, CREATED);
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

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, CREATED);

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
                .map(Subject::getId).collect(Collectors.toList());
        List<Integer> expected = new ArrayList<>();
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
                .withDeadline("2020-04-12")
                .withStatus(CaseStatus.CREATED);

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, CREATED);

        assertThat("primaryFaust", created.getPrimaryFaust(), is("4001111"));
        assertThat("title", created.getTitle(), is("Title for 4001111"));
        assertThat("details", created.getDetails(), is("Details for 4001111"));
        assertThat("materialType", created.getMaterialType(), is(MaterialType.BOOK));
        assertThat("created", created.getCreated(), is(LocalDate.now()));
        assertThat("assigned", created.getAssigned(), is(nullValue()));
        assertThat("deadline", created.getDeadline(), is(LocalDate.parse("2020-04-12")));
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
                .withDeadline("2020-04-12")
                .withStatus(CaseStatus.ASSIGNED)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(List.of("6001111")),
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(List.of("6002222", "6004444"))
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
        assertThat("task 1 targetFausts", created.getTasks().get(0).getTargetFausts(), is(List.of("6001111")));

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
                        .stream().sorted().collect(Collectors.toList()),
                is(List.of("6002222", "6004444")));

        // Get the case we created
        response = getResponse("v1/api/cases/" + created.getId().toString());
        assertThat("status code", response.getStatus(), is(200));

        // Verify that the case matches the created case
        obj = response.readEntity(String.class);
        PromatCase fetched = mapper.readValue(obj, PromatCase.class);
        assertThat("fetched case is same as created", fetched, is(created));
    }

    @Test
    public void testCreateCaseWithInvalidStatus() {

        // Create case with status ASSIGNED but no reviewer given
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("7001111")
                .withTitle("Title for 7001111")
                .withDetails("Details for 7001111")
                .withMaterialType(MaterialType.BOOK)
                .withDeadline("2020-04-12")
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

        // Get 4 cases with status CREATED from id 1 (first id after 'from' should be 1)
        CaseSummaryList fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withStatus(CaseStatus.CREATED)
                .withLimit(4)
                .withFrom(0));

        assertThat("Number of cases with status CREATED", fetched.getNumFound(), is(4));


        // Check id ordering
        assertThat(fetched.getCases().get(0).getId(), is(1));
        assertThat(fetched.getCases().get(1).getId(), is(2));
        assertThat(fetched.getCases().get(2).getId(), is(3));

        // Get 4 cases with status CREATED from id 7 and backwards
        fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withStatus(CaseStatus.CREATED)
                .withLimit(4)
                .withTo(8)
                .withOrder(ListCasesParams.Order.DESCENDING));
        assertThat("Number of cases with status CREATED", fetched.getNumFound(), is(4));

        // Check id ordering
        assertThat(fetched.getCases().get(0).getId(), is(7));
        assertThat(fetched.getCases().get(1).getId(), is(5));
        assertThat(fetched.getCases().get(2).getId(), is(3));
        assertThat(fetched.getCases().get(3).getId(), is(2));

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

        // Attempt to set historic, but now obsoleted/removed, field 'assigned'
        // should be ignored and return 200 OK
        String json = "{\"assigned\":\"2020-11-18\"}";
        response = postResponse("v1/api/cases/1", json);
        assertThat("status code", response.getStatus(), is(200));

        // Attempt to set field 'status' - should return 400 BAD REQUEST
        dto = new CaseRequest().withStatus(CaseStatus.ASSIGNED);
        response = postResponse("v1/api/cases/1", dto);
        assertThat("status code", response.getStatus(), is(400));

        // Attempt to set field 'reviewer' with non existing reviewer - should return 400 BAD REQUEST
        dto = new CaseRequest().withReviewer(9999);
        response = postResponse("v1/api/cases/1", dto);
        assertThat("status code", response.getStatus(), is(400));

        // Attempt to set field 'editor' with non existing editor - should return 400 BAD REQUEST
        dto = new CaseRequest().withEditor(9999);
        response = postResponse("v1/api/cases/1", dto);
        assertThat("status code", response.getStatus(), is(400));

        // Attempt to set field 'subject' with non existing subject - should return 400 BAD REQUEST
        dto = new CaseRequest().withSubjects(Arrays.asList(9999));
        response = postResponse("v1/api/cases/1", dto);
        assertThat("status code", response.getStatus(), is(400));

        // Create a new case
        dto = new CaseRequest()
                .withTitle("Title for 8001111")
                .withDetails("Details for 8001111")
                .withPrimaryFaust("8001111")
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
                                .withTargetFausts(Arrays.asList("8001111"))
                ));
        response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Update case - should return 200 OK
        dto = new CaseRequest()
                .withTitle("New title for 8001111")
                .withDetails("New details for 8001111")
                .withPrimaryFaust("8002222")
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
                                .withTaskFieldType(TaskFieldType.BKM)
                                .withTargetFausts(Arrays.asList("8002222")),
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.DESCRIPTION)
                                .withTargetFausts(Arrays.asList("8003333"))
                ));
        PromatCase updated = promatServiceConnector.updateCase(created.getId(), dto);

        assertThat("updated case did not change id", updated.getId(), is(created.getId()));
        assertThat("updated title", updated.getTitle(), is("New title for 8001111"));
        assertThat("updated details", updated.getDetails(), is("New details for 8001111"));
        assertThat("updated primary faust", updated.getPrimaryFaust(), is("8002222"));
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
        assertThat("fetched case is same as updated", fetched, is(updated));

        // Check that the BKM task is not approved
        PromatTask bkmTask = fetched.getTasks().stream()
                .filter(task -> task.getTaskFieldType().equals(TaskFieldType.BKM))
                .findFirst()
                .orElseThrow();
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
                .orElseThrow();
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
    }

    @Test
    public void testCreateWithFieldsCreatorAuthorPublisherAndWeekcode() throws JsonProcessingException {
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("9001211")
                .withTitle("Title for 9001211")
                .withDetails("Details for 9001211")
                .withTasks(Collections.singletonList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BKM)
                        .withTargetFausts(Arrays.asList("9001211"))))
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
                                .withTargetFausts(Arrays.asList("9001111"))
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

        // Check that the case now has 1 task which matches the created task
        assertThat("case tasks is not null", fetched.getTasks(), is(notNullValue()));
        assertThat("case has 1 task", fetched.getTasks().size(), is(1));
        assertThat("task is the created task", fetched.getTasks().get(0).equals(createdTask));
        assertThat("has two targetfaust", fetched.getTasks().get(0).getTargetFausts().size(), is(2));
        assertThat("First targetfaust", fetched.getTasks().get(0).getTargetFausts().contains("12002222"), is(true));
        assertThat("Second targetfausts", fetched.getTasks().get(0).getTargetFausts().contains("12003333"), is(true));

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
                                .withTargetFausts(Arrays.asList("13001111"))
                ));
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("case has 1 task", created.getTasks().size(), is(1));

        TaskDto taskDto = new TaskDto()
                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                .withTaskFieldType(TaskFieldType.DESCRIPTION)
                .withTargetFausts(Arrays.asList("13001111"));
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
                .withTitle("Title for 14001111")
                .withDetails("Details for 14001111")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Arrays.asList("14002222"))
                ));
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));

        // Create case 2
        dto = new CaseRequest()
                .withPrimaryFaust("15001111")
                .withTitle("Title for 15001111")
                .withDetails("Details for 15001111")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Arrays.asList("15002222"))
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
        assertThat("status code", postResponse("v1/api/cases/" + created.getId() + "/tasks", taskDto).getStatus(), is(400));

        // Add Targetfaust
        taskDto.setTargetFausts(Arrays.asList("16001111"));
        assertThat("status code", postResponse("v1/api/cases/" + created.getId() + "/tasks", taskDto).getStatus(), is(201));
    }

    @Test
    public void testCreateCaseWithTaskWithUsedFaust() {

        // Create case 1
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("17001111")
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
                .findFirst().orElseThrow().getApproved(), is(nullValue()));

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
    public void testLookupOfWeekcodesInCodes() throws PromatServiceConnectorException {
        CaseSummaryList fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withWeekCode("BKM197901"));
        assertThat("Cases found", fetched.getNumFound(), is(0));

        fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withWeekCode("BKM202104"));
        assertThat("Cases found", fetched.getNumFound(), is(2));

        fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withWeekCode("BKM202110"));
        assertThat("Cases found", fetched.getNumFound(), is(1));

        fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withWeekCode("BKX202107"));
        assertThat("Cases found", fetched.getNumFound(), is(2));

        fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withWeekCode("bkx202107"));
        assertThat("Cases found", fetched.getNumFound(), is(2));
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
    void getFulltextForArbitraryFaust(@TempDir Path tempDir) throws PromatServiceConnectorException, IOException, InterruptedException {
        final String faust = "39482533";


        var apiLink = promatServiceBaseUrl + String.format("/v1/api/cases/faust/%s/fulltext", faust);

        var client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        var request = HttpRequest.newBuilder()
                .uri(URI.create(apiLink))
                .build();

        final HttpResponse<Path> httpResponse = client.send(request,
                HttpResponse.BodyHandlers.ofFileDownload(tempDir, WRITE, CREATE_NEW));

        assertThat("file content", new String(Files.readAllBytes(httpResponse.body())),
                is("epub data her\n"));
    }

    @Test
    void addDraftOrUpdateExistingCase() throws JsonProcessingException, PromatServiceConnectorException {
        final CaseRequest caseRequest = new CaseRequest()
                .withPrimaryFaust("11111111")
                .withTitle("Title for draft - print")
                .withMaterialType(MaterialType.BOOK)
                .withFulltextLink("link");


        final PromatCase caseCreated = promatServiceConnector.createDraft(caseRequest);

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

    @Test
    public void testChangeStatusPendingExportToExported() throws JsonProcessingException {

        // Case is ready for export, the dataio-harvester will change status to EXPORTED
        CaseRequest requestDto = new CaseRequest().withStatus(CaseStatus.EXPORTED);
        Response response = postResponse("v1/api/cases/18", requestDto);
        assertThat("status code", response.getStatus(), is(200));

        PromatCase updated = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status", updated.getStatus(), is(CaseStatus.EXPORTED));
    }

    @Test
    public void testChangeStatusExportedToPendingExport() throws JsonProcessingException {

        // Case has been exported but we need to export it again since some error occurred
        // downstream - we dont want to edit the database directly, nor do we want to
        // let the case go through an entire CREATED->ASSIGNED->PENDING_APPROVAL->....
        // chain of status changes.
        CaseRequest requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_EXPORT);
        Response response = postResponse("v1/api/cases/19", requestDto);
        assertThat("status code", response.getStatus(), is(200));

        PromatCase updated = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status", updated.getStatus(), is(CaseStatus.PENDING_EXPORT));
    }

    @Test
    public void testDbcKatHtmlViewFaustNotFoundReturnsNotFound() throws PromatServiceConnectorException {
        try {
            promatServiceConnector.getCaseview("98765432123456789", "HTML");
        } catch(PromatServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("must return 404 NOT FOUND", e.getStatusCode(), is(404));
        }
    }

    @Test
    public void testDbckatHtmlViewOfPrimaryFaust() throws IOException, PromatServiceConnectorException {

        // Fetch the html view for the primary faustnumber
        String expected = new String(Files.readAllBytes(Path.of("src/test/resources/__files/case-view-for-id-20-100000.html")));
        String actual = promatServiceConnector.getCaseview("100000", "HTML");

        // Not checking that the documents are strictly equal comparing html output, but it does
        // check that all textual content in the body is equal.
        String expectedText = Jsoup.parse(expected).normalise().text();
        String actualText = Jsoup.parse(actual).normalise().text();
        assertThat("case view is correct", expectedText.equals(actualText));
    }

    @Test
    public void testDbckatHtmlViewOfRelatedFausts() throws IOException, PromatServiceConnectorException {

        // Fetch the html view for the first related faustnumber
        String expected = new String(Files.readAllBytes(Path.of("src/test/resources/__files/case-view-for-id-20-100001.html")));
        String actual = promatServiceConnector.getCaseview("100001", "HTML");

        // Not checking that the documents are strictly equal comparing html output, but it does
        // check that all textual content in the body is equal.
        String expectedText = Jsoup.parse(expected).normalise().text();
        String actualText = Jsoup.parse(actual).normalise().text();
        assertThat("case view is correct", expectedText.equals(actualText));

        // Fetch the html view for the second related faustnumber
        expected = new String(Files.readAllBytes(Path.of("src/test/resources/__files/case-view-for-id-20-100002.html")));
        actual = promatServiceConnector.getCaseview("100002", "HTML");

        // Not checking that the documents are strictly equal comparing html output, but it does
        // check that all textual content in the body is equal.
        expectedText = Jsoup.parse(expected).normalise().text();
        actualText = Jsoup.parse(actual).normalise().text();
        assertThat("case view is correct", expectedText.equals(actualText));
    }

    @Test
    public void testDbckatHtmlViewOfPrimaryFaustWithNullData() throws IOException, PromatServiceConnectorException {

        // Fetch the html view for the primary faustnumber
        // Note: it is not normal that field data, or case metadata, is NULL when the case is approved (or beyond)
        String expected = new String(Files.readAllBytes(Path.of("src/test/resources/__files/case-view-for-id-21-100003.html")));
        String actual = promatServiceConnector.getCaseview("100003", "HTML");

        // Not checking that the documents are strictly equal comparing html output, but it does
        // check that all textual content in the body is equal.
        String expectedText = Jsoup.parse(expected).normalise().text();
        String actualText = Jsoup.parse(actual).normalise().text();
        assertThat("case view is correct", expectedText.equals(actualText));

        // Move case back to status CREATED(==>ASSIGNED) since it is clearly not done..  Expect no html view available
        promatServiceConnector.updateCase(21, new CaseRequest().withStatus(CaseStatus.CREATED));
        assertThrows(PromatServiceConnectorUnexpectedStatusCodeException.class, () -> {
            try {
                promatServiceConnector.getCaseview("100003", "HTML");
            } catch (PromatServiceConnectorUnexpectedStatusCodeException e) {
                assertThat("exception is 404 NOT FOUND", e.getStatusCode(), is(404));
                throw e;
            } catch (Exception e) {
                throw e;
            }
        });
    }

    @Test
    public void testDbcKatXmlViewFaustNotFoundReturnsNotFound() throws PromatServiceConnectorException {
        try {
            promatServiceConnector.getCaseview("98765432123456789", "XML");
        } catch(PromatServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("must return 404 NOT FOUND", e.getStatusCode(), is(404));
        }
    }

    @Test
    public void testDbckatXmlViewOfPrimaryFaust() throws IOException, PromatServiceConnectorException {

        // Fetch the xml view for the primary faustnumber
        String expected = new String(Files.readAllBytes(Path.of("src/test/resources/__files/case-view-for-id-20-100000.xml")), StandardCharsets.ISO_8859_1);
        String actual = promatServiceConnector.getCaseview("100000", "XML", StandardCharsets.ISO_8859_1);

        // Not checking that the documents are strictly equal comparing xml output, but it does
        // check that all text elements contains the correct information. Attributes is not
        // checked, but they are not important (at least not for dbckat)
        String expectedText = Jsoup.parse(expected, StandardCharsets.ISO_8859_1.name()).normalise().text();
        String actualText = Jsoup.parse(actual).normalise().text();
        assertThat("case view is correct", expectedText.equals(actualText));
    }

    @Test
    public void testDbckatXmlViewOfRelatedFaust() throws IOException, PromatServiceConnectorException {

        // Fetch the xml view for the first related faustnumber
        String expected = new String(Files.readAllBytes(Path.of("src/test/resources/__files/case-view-for-id-20-100001.xml")), StandardCharsets.ISO_8859_1);
        String actual = promatServiceConnector.getCaseview("100001", "XML", StandardCharsets.ISO_8859_1);

        // Not checking that the documents are strictly equal comparing xml output, but it does
        // check that all text elements contains the correct information. Attributes is not
        // checked, but they are not important (at least not for dbckat)
        String expectedText = Jsoup.parse(expected, StandardCharsets.ISO_8859_1.name()).normalise().text();
        String actualText = Jsoup.parse(actual).normalise().text();
        assertThat("case view is correct", expectedText.equals(actualText));

        // Fetch the xml view for the second related faustnumber
        expected = new String(Files.readAllBytes(Path.of("src/test/resources/__files/case-view-for-id-20-100002.xml")), StandardCharsets.ISO_8859_1);
        actual = promatServiceConnector.getCaseview("100002", "XML", StandardCharsets.ISO_8859_1);

        // Not checking that the documents are strictly equal comparing xml output, but it does
        // check that all text elements contains the correct information. Attributes is not
        // checked, but they are not important (at least not for dbckat)
        expectedText = Jsoup.parse(expected, StandardCharsets.ISO_8859_1.name()).normalise().text();
        actualText = Jsoup.parse(actual).normalise().text();
        assertThat("case view is correct", expectedText.equals(actualText));
    }

    @Test
    public void testDbckatXmlViewOfPrimaryFaustWithNullData() throws IOException, PromatServiceConnectorException, ParserConfigurationException, SAXException {

        // Fetch the xml view for the primary faustnumber
        // Note: it is not normal that field data, or case metadata, is NULL when the case is approved (or beyond)
        String expected = new String(Files.readAllBytes(Path.of("src/test/resources/__files/case-view-for-id-22-100004.xml")), StandardCharsets.ISO_8859_1);
        String actual = promatServiceConnector.getCaseview("100004", "XML", StandardCharsets.ISO_8859_1);

        // Not checking that the documents are strictly equal comparing xml output, but it does
        // check that all text elements contains the correct information. Attributes is not
        // checked, but they are not important (at least not for dbckat)
        String expectedText = Jsoup.parse(expected, StandardCharsets.ISO_8859_1.name()).normalise().text();
        String actualText = Jsoup.parse(actual).normalise().text();
        assertThat("case view is correct", expectedText.equals(actualText));

        // Move case back to status CREATED(==>ASSIGNED) since it is clearly not done..  Expect no html view available
        promatServiceConnector.updateCase(22, new CaseRequest().withStatus(CaseStatus.CREATED));
        assertThrows(PromatServiceConnectorUnexpectedStatusCodeException.class, () -> {
            try {
                promatServiceConnector.getCaseview("100004", "XML");
            } catch (PromatServiceConnectorUnexpectedStatusCodeException e) {
                assertThat("exception is 404 NOT FOUND", e.getStatusCode(), is(404));
                throw e;
            } catch (Exception e) {
                throw e;
            }
        });
    }

    @Test
    public void testDbckatHtmlViewOfPrimaryFaustNotApproved() throws IOException, PromatServiceConnectorException {

        // Casewiew not available for dbckat users and reviewers
        assertThrows(PromatServiceConnectorUnexpectedStatusCodeException.class, () -> {
            try {
                promatServiceConnector.getCaseview("100006", "HTML");
            } catch (PromatServiceConnectorUnexpectedStatusCodeException e) {
                assertThat("exception is 404 NOT FOUND", e.getStatusCode(), is(404));
                throw e;
            } catch (Exception e) {
                throw e;
            }
        });

        // Casewiew available for editors with override
        promatServiceConnector.getCaseviewWithOverride("100006", "HTML"); // Uses query parameter ?override=true
        assertThat("status", getResponse("v1/api/cases/HTML/override/100006").getStatus(), is(200)); // uses path ../override/..
    }

    @Test
    public void testDbckatHtmlViewOfPrimaryFaustClosedBKM() throws IOException, PromatServiceConnectorException {

        CaseRequest dto = new CaseRequest()
                .withTitle("Title for 90001234")
                .withDetails("Details for 90001234")
                .withPrimaryFaust("90001234")
                .withEditor(10)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-04-01")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(List.of("90001234")),
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BKM)
                                .withTargetFausts(List.of("90001234"))
                ));
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase aCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Casewiew available since the case has status 'CREATED', expect no exception
        promatServiceConnector.getCaseviewWithOverride("90001234", "HTML"); // Uses query parameter ?override=true

        // Close the case
        dto = new CaseRequest().withStatus(CaseStatus.CLOSED);
        response = postResponse("v1/api/cases/" + aCase.getId(), dto);

        // Casewiew still available when the case has status 'CLOSED', expect no exception
        promatServiceConnector.getCaseviewWithOverride("90001234", "HTML"); // Uses query parameter ?override=true
    }

    @Test
    public void testBuggiApproval() throws IOException, PromatServiceConnectorException {
        String pidPreamble = "870170-BASIS:";
        TagList tags = new TagList("hest");
        assertPromatThrows(NOT_FOUND, () -> promatServiceConnector.approveBuggiTask(pidPreamble + "12345678", tags));

        CaseRequest noBuggiReq = makeRequest("92001234", new TaskDto()
                .withTaskFieldType(TaskFieldType.BKM)
                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES));
        PromatCase noBuggiCase = postAndAssert("v1/api/cases", noBuggiReq, PromatCase.class, CREATED);
        assertPromatThrows(NOT_FOUND, () -> promatServiceConnector.approveBuggiTask(pidPreamble + noBuggiCase.getPrimaryFaust(), tags));
        deleteResponse("v1/api/cases/" + noBuggiCase.getId());

        CaseRequest cr = makeRequest("93001234", new TaskDto()
                .withTaskFieldType(TaskFieldType.BUGGI)
                .withTargetFausts(List.of())
                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES));
        PromatCase aCase = postAndAssert("v1/api/cases", cr, PromatCase.class, CREATED);

        PromatCase updatedCase = promatServiceConnector.approveBuggiTask(pidPreamble + aCase.getPrimaryFaust(), tags);
        boolean buggyApproved = updatedCase.getTasks().stream()
                .filter(t -> t.getTaskFieldType() == TaskFieldType.BUGGI)
                .anyMatch(t -> t.getApproved() != null);
        Assertions.assertTrue(buggyApproved, "Buggi task should be approved");

        deleteResponse("v1/api/cases/" + updatedCase.getId());

        postAndAssert("v1/api/cases/" + aCase.getPrimaryFaust() + "/buggi", "{'tags': ['hest', 'ko']}"
                .replace('\'', '"'), BAD_REQUEST);
    }

    private void assertPromatThrows(Response.Status status, Callable<PromatCase> callable) {
        try {
            callable.call();
        } catch (PromatServiceConnectorUnexpectedStatusCodeException pe) {
          Assertions.assertEquals(status.getStatusCode(), pe.getStatusCode(),
                  "Client was expected to throw an exception containing status code " + status);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CaseRequest makeRequest(String faust, TaskDto... tasks) {
        return new CaseRequest()
                .withTitle("Title for " + faust)
                .withDetails("Details for " + faust)
                .withPrimaryFaust(faust)
                .withEditor(10)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline(LocalDate.now().plus(10, ChronoUnit.DAYS).toString())
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.stream(tasks).map(t -> t.withTargetFausts(List.of(faust))).collect(Collectors.toList()));
    }

    private void assertStatus(Response response, Response.Status status) {
        Assertions.assertEquals(status, response.getStatusInfo().toEnum());
    }

    @Test
    public void testMaterialsFilterQuery() throws PromatServiceConnectorException, JsonProcessingException {
        // Create a BOOK case
        CaseRequest dto = new CaseRequest()
                .withTitle("Title for 80011112")
                .withDetails("Details for 80011112")
                .withPrimaryFaust("80011112")
                .withEditor(10)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-04-01")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(List.of("80022222", "80044442")),
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BKM)
                                .withTargetFausts(List.of("80011112"))
                ));
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase book = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Create a MOVIE case
        dto = new CaseRequest()
                .withTitle("Title for 80011113")
                .withDetails("Details for 80011113")
                .withPrimaryFaust("80011113")
                .withEditor(10)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-04-01")
                .withMaterialType(MaterialType.MOVIE)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.MOVIES_GR_1)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(List.of("80011113")),
                        new TaskDto()
                                .withTaskType(TaskType.MOVIE_NON_FICTION_GR1)
                                .withTaskFieldType(TaskFieldType.BKM)
                                .withTargetFausts(List.of("80011113"))
                ));
        response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase movie = mapper.readValue(response.readEntity(String.class), PromatCase.class);


        // Create a MULTIMEDIA case
        dto = new CaseRequest()
                .withTitle("Title for 80011114")
                .withDetails("Details for 80011114")
                .withPrimaryFaust("80011114")
                .withEditor(10)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-04-01")
                .withMaterialType(MaterialType.MULTIMEDIA)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.MULTIMEDIA_FEE)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(List.of("80011114")),
                        new TaskDto()
                                .withTaskType(TaskType.MULTIMEDIA_FEE)
                                .withTaskFieldType(TaskFieldType.BKM)
                                .withTargetFausts(List.of("80011114"))
                ));
        response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase multimedia = mapper.readValue(response.readEntity(String.class), PromatCase.class);


        CaseSummaryList cases = promatServiceConnector.listCases(new ListCasesParams().withMaterials("BOOK"));
        assertThat("At least one book", cases.getNumFound(), greaterThanOrEqualTo(1));
        assertThat("Only books", cases.getCases().stream().allMatch(promatCase -> promatCase.getMaterialType() == MaterialType.BOOK));

        cases = promatServiceConnector.listCases(new ListCasesParams().withMaterials("BOOK,MOVIE"));
        assertThat("At least one book and one movie", cases.getNumFound(), greaterThanOrEqualTo(2));
        assertThat("Not only books!", !cases.getCases().stream().allMatch(promatCase -> promatCase.getMaterialType() == MaterialType.BOOK));


        cases = promatServiceConnector.listCases(new ListCasesParams().withMaterials("BOOK,MULTIMEDIA"));
        assertThat("At least one book and one multimedia", cases.getNumFound(), greaterThanOrEqualTo(2));
        assertThat("Only books & multimedia", cases.getCases().stream().
                allMatch(promatCase -> promatCase.getMaterialType() == MaterialType.BOOK ||
                        promatCase.getMaterialType() == MaterialType.MULTIMEDIA));

        assertThat("At least one multimedia case was found",
                cases.getCases().stream().anyMatch(promatCase -> promatCase.getMaterialType() == MaterialType.MULTIMEDIA));

        // Delete case, in order not to mess up other tests.
        // Delete the case so that we dont mess up payments tests
        response = deleteResponse("v1/api/cases/"+book.getId());
        assertThat("status code", response.getStatus(), is(200));

        response = deleteResponse("v1/api/cases/"+movie.getId());
        assertThat("status code", response.getStatus(), is(200));

        response = deleteResponse("v1/api/cases/"+multimedia.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testRejectAssignmentAndReassignedToOtherReviewer() throws JsonProcessingException, PromatServiceConnectorException {

        // Create a new case
        CaseRequest dto = new CaseRequest()
                .withTitle("Title for 18001111")
                .withDetails("Details for 18001111")
                .withPrimaryFaust("18001111")
                .withEditor(10)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-03-23")
                .withMaterialType(MaterialType.BOOK);

        Response response = postResponse("v1/api/cases", dto);
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status code", response.getStatus(), is(201));

        // Update case - should return 200 OK
        dto = new CaseRequest().withReviewer(1);
        PromatCase updated = promatServiceConnector.updateCase(created.getId(), dto);
        assertThat("is assigned", updated.getStatus(), is(CaseStatus.ASSIGNED));

        // Then reject the case
        dto = new CaseRequest().withStatus(CaseStatus.REJECTED);
        updated = promatServiceConnector.updateCase(created.getId(), dto);
        assertThat("is rejected", updated.getStatus(), is(CaseStatus.REJECTED));

        // Then reassign to other reviewer.
        dto.setReviewer(2);
        dto.setStatus(null);
        updated = promatServiceConnector.updateCase(created.getId(), dto);
        assertThat("is reassigned", updated.getStatus(), is(CaseStatus.ASSIGNED));

        // Delete the case so that we dont mess up payments tests
        response = deleteResponse("v1/api/cases/"+created.getId());
        assertThat("deleted", response.getStatus(), is(200));

    }

    @Test
    public void testStatusFlowToExported() throws JsonProcessingException {

        // Create a new case
        CaseRequest dto = new CaseRequest()
                .withTitle("Title for 19001111")
                .withDetails("Details for 19001111")
                .withPrimaryFaust("19001111")
                .withEditor(10)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-03-30")
                .withMaterialType(MaterialType.BOOK);

        Response response = postResponse("v1/api/cases", dto);
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status code", response.getStatus(), is(201));

        // Send case to approval
        CaseRequest requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Approve the case
        requestDto = new CaseRequest().withStatus(CaseStatus.APPROVED);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Move to PENDING_MEETING
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_MEETING);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Move to PENDING_EXPORT
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_EXPORT);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Move to EXPORTED
        requestDto = new CaseRequest().withStatus(CaseStatus.EXPORTED);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Delete the case
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testStatusFlowToExportedFromApproved() throws JsonProcessingException {

        // Create a new case
        CaseRequest dto = new CaseRequest()
                .withTitle("Title for 20001111")
                .withDetails("Details for 20001111")
                .withPrimaryFaust("20001111")
                .withEditor(10)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-03-30")
                .withMaterialType(MaterialType.BOOK);

        Response response = postResponse("v1/api/cases", dto);
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status code", response.getStatus(), is(201));

        // Send case to approval
        CaseRequest requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Approve the case
        requestDto = new CaseRequest().withStatus(CaseStatus.APPROVED);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Move to PENDING_EXPORT
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_EXPORT);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Move to EXPORTED
        requestDto = new CaseRequest().withStatus(CaseStatus.EXPORTED);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Delete the case
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testStatusFlowToPendingRevertAndRevertedFromExported() throws JsonProcessingException {

        // Create a new case
        CaseRequest dto = new CaseRequest()
                .withTitle("Title for 21001111")
                .withDetails("Details for 21001111")
                .withPrimaryFaust("21001111")
                .withEditor(10)
                .withReviewer(1)
                .withNote("hej")
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-03-30")
                .withMaterialType(MaterialType.BOOK);

        Response response = postResponse("v1/api/cases", dto);
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status code", response.getStatus(), is(201));

        // Send case to approval
        CaseRequest requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Approve the case
        requestDto = new CaseRequest().withStatus(CaseStatus.APPROVED);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Move to PENDING_EXPORT
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_EXPORT);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Move to EXPORTED
        requestDto = new CaseRequest().withStatus(CaseStatus.EXPORTED);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Move to PENDING_REVERT
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_REVERT);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Move to REVERTED
        requestDto = new CaseRequest().withStatus(CaseStatus.REVERTED);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Delete the case
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testAssignFaustnumberWhenChangeToPendingExport() throws JsonProcessingException {

        // Create a new case
        CaseRequest dto = new CaseRequest()
                .withTitle("Title for 24001111")
                .withDetails("Details for 24001111")
                .withPrimaryFaust("24001111")
                .withEditor(10)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-03-30")
                .withNote("hej")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.asList(new TaskDto()
                    .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                    .withTaskFieldType(TaskFieldType.BRIEF)
                    .withTargetFausts(Arrays.asList("24001111"))
                ));

        Response response = postResponse("v1/api/cases", dto);
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status code", response.getStatus(), is(201));

        // Send case to approval
        CaseRequest requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Approve the case
        requestDto = new CaseRequest().withStatus(CaseStatus.APPROVED);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Move to PENDING_MEETING
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_MEETING);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        PromatCase updated = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("faust", updated.getTasks().get(0).getRecordId(), is(nullValue()));

        // Move to PENDING_EXPORT
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_EXPORT);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        updated = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("faust", updated.getTasks().get(0).getRecordId(), is("131990219"));

        // Delete the case
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testStatusFlowToPendingIssuesFromApproved() throws JsonProcessingException {

        // Create a new case
        CaseRequest dto = new CaseRequest()
                .withTitle("Title for 22001111")
                .withDetails("Details for 22001111")
                .withPrimaryFaust("22001111")
                .withEditor(10)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-03-30")
                .withMaterialType(MaterialType.BOOK);

        Response response = postResponse("v1/api/cases", dto);
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status code", response.getStatus(), is(201));

        // Send case to approval
        CaseRequest requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Approve the case
        requestDto = new CaseRequest().withStatus(CaseStatus.APPROVED);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Move to PENDING_ISSUES
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_ISSUES);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Delete the case
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testStatusFlowToPendingIssuesFromPendingMeeting() throws JsonProcessingException {

        // Create a new case
        CaseRequest dto = new CaseRequest()
                .withTitle("Title for 23001111")
                .withDetails("Details for 23001111")
                .withPrimaryFaust("23001111")
                .withEditor(10)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-03-30")
                .withMaterialType(MaterialType.BOOK);

        Response response = postResponse("v1/api/cases", dto);
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status code", response.getStatus(), is(201));

        // Send case to approval
        CaseRequest requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Approve the case
        requestDto = new CaseRequest().withStatus(CaseStatus.APPROVED);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Move to PENDING_MEETING
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_MEETING);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Move to PENDING_ISSUES
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_ISSUES);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Delete the case
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testReassignCase() throws JsonProcessingException {

        // Create a new case
        CaseRequest dto = new CaseRequest()
                .withTitle("Title for 25001111")
                .withDetails("Details for 25001111")
                .withPrimaryFaust("25001111")
                .withEditor(10)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-03-30")
                .withMaterialType(MaterialType.BOOK);

        Response response = postResponse("v1/api/cases", dto);
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status code", response.getStatus(), is(201));
        assertThat("assigned", created.getStatus(), is(CaseStatus.ASSIGNED));

        // Reassign case without specifying a new status
        CaseRequest requestDto = new CaseRequest()
                .withReviewer(2);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Reassign case and specify status ASSIGNED
        requestDto = new CaseRequest()
                .withReviewer(1)
                .withStatus(CaseStatus.ASSIGNED);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Delete the case
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testReassignCaseWithPendingIssues() throws JsonProcessingException {

        // Create a new case
        CaseRequest dto = new CaseRequest()
                .withTitle("Title for 26001111")
                .withDetails("Details for 26001111")
                .withPrimaryFaust("26001111")
                .withEditor(10)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-03-30")
                .withMaterialType(MaterialType.BOOK);

        Response response = postResponse("v1/api/cases", dto);
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status code", response.getStatus(), is(201));

        // Send case to approval
        CaseRequest requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Case has issues
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_ISSUES);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Reassign case without specifying a new status
        requestDto = new CaseRequest()
                .withReviewer(2);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Send case to approval
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Case has issues
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_ISSUES);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Reassign case and specify status ASSIGNED
        requestDto = new CaseRequest()
                .withReviewer(1)
                .withStatus(CaseStatus.ASSIGNED);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Delete the case
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testStatusFlowToPendingCloseFromPendingApproval() throws JsonProcessingException {

        // Create a new case
        CaseRequest dto = new CaseRequest()
                .withTitle("Title for 27001111")
                .withDetails("Details for 27001111")
                .withPrimaryFaust("27001111")
                .withEditor(10)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-03-30")
                .withMaterialType(MaterialType.BOOK);

        Response response = postResponse("v1/api/cases", dto);
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status code", response.getStatus(), is(201));

        // Send case to approval
        CaseRequest requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // CLose the case while including it in the next payroll
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_CLOSE);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Delete the case
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testStatusFlowToApprovedFromPendingExternal() throws JsonProcessingException {

        // Create a new case
        CaseRequest dto = new CaseRequest()
                .withTitle("Title for 28001111")
                .withDetails("Details for 28001111")
                .withPrimaryFaust("28001111")
                .withEditor(10)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-03-30")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Arrays.asList("28001111")),
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.METAKOMPAS)
                                .withTargetFausts(Arrays.asList(new String[] {"28001111"}))
                ));

        Response response = postResponse("v1/api/cases", dto);
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status code", response.getStatus(), is(201));

        // Send case to approval
        CaseRequest requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Approve the case
        requestDto = new CaseRequest().withStatus(CaseStatus.APPROVED);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Case should now be in PENDING_EXTERNAL and NOT be approved
        PromatCase updated = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("case status", updated.getStatus(), is(CaseStatus.PENDING_EXTERNAL));
        assertThat("approved", updated.getTasks().stream()
                .filter(t -> t.getTaskFieldType() == TaskFieldType.METAKOMPAS)
                .findFirst().orElseThrow()
                .getApproved(), is(nullValue()));

        // Move case to APPROVED from PENDING_EXTERNAL to bypass waiting for metakompas topics to be added
        // (mostly express cases)
        requestDto = new CaseRequest().withStatus(CaseStatus.APPROVED);
        response = postResponse("v1/api/cases/" + created.getId(), requestDto);
        assertThat("status code", response.getStatus(), is(200));

        // Case should now be in APPROVED and be approved
        updated = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("case status", updated.getStatus(), is(CaseStatus.APPROVED));
        assertThat("approved", updated.getTasks().stream()
                .filter(t -> t.getTaskFieldType() == TaskFieldType.METAKOMPAS)
                .findFirst().orElseThrow()
                .getApproved(), is(notNullValue()));

        // Delete the case
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    @Order(1)
    public void testGetOnlyCasesWithFaustForExport() throws PromatServiceConnectorException {
        CaseSummaryList fetched = promatServiceConnector.listCases(new ListCasesParams()
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withFormat(ListCasesParams.Format.EXPORT));
        assertThat("Number of cases to export", fetched.getNumFound(), is(1));
        assertThat("numFound", fetched.getNumFound(), is(1));
    }

    @Test
    public void testQueryByCreator() throws JsonProcessingException {

        // Create a new case
        CaseRequest dto = new CaseRequest()
                .withTitle("Title for 28001112")
                .withDetails("Details for 28001112")
                .withPrimaryFaust("28001112")
                .withEditor(10)
                .withCreator(10)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-03-30")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Collections.singletonList("28001112")),
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.METAKOMPAS)
                                .withTargetFausts(Collections.singletonList("28001112"))));

        Response response = postResponse("v1/api/cases", dto);
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status code", response.getStatus(), is(201));

        response = getResponse("v1/api/cases", Map.of("creator", 10));
        CaseSummaryList cases = mapper.readValue(response.readEntity(String.class), CaseSummaryList.class);
        assertThat("Number of cases belonging to creator '10'", cases.getNumFound(),
                is(greaterThanOrEqualTo(1)));
        for (PromatCase pc : cases.getCases()) {
            assertThat("Creator is '10'", pc.getCreator().getId(), is(10));
        }

        // Delete the case
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testQueryByIdAndPublisher() throws JsonProcessingException {

        // Create a new case
        CaseRequest dto = new CaseRequest()
                .withTitle("Title for 27100104")
                .withDetails("Details for 27100104")
                .withPrimaryFaust("27100104")
                .withEditor(10)
                .withCreator(10)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-07-30")
                .withMaterialType(MaterialType.BOOK)
                .withPublisher("Publisher for 27100104")
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Collections.singletonList("27100104")),
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.METAKOMPAS)
                                .withTargetFausts(Collections.singletonList("27100104"))));

        Response response = postResponse("v1/api/cases", dto);

        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status code", response.getStatus(), is(201));

        // Create another new case
        dto = new CaseRequest()
                .withTitle("Title for 38352253")
                .withDetails("Details for 38352253")
                .withPrimaryFaust("38352253")
                .withEditor(10)
                .withCreator(10)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-07-30")
                .withMaterialType(MaterialType.BOOK)
                .withPublisher("Publisher for 38352253")
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(List.of("38352253", "38352296")),
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.METAKOMPAS)
                                .withTargetFausts(List.of("38352253", "38352296"))));

        response = postResponse("v1/api/cases", dto);
        PromatCase created_2 = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status code", response.getStatus(), is(201));

        // Query by id with faust 24699773: Expected is at least the case just created.
        response = getResponse("v1/api/cases", Map.of("id", 27100104));
        CaseSummaryList cases = mapper.readValue(response.readEntity(String.class), CaseSummaryList.class);
        assertThat(cases.getNumFound(), is(greaterThanOrEqualTo(1)));
        assertThat("The newly created first case with this faust is one of them",
                cases.getCases().stream().map(PromatCase::getId).collect(Collectors.toList()).contains(created.getId()));

        // Query by isbn 9788764432589: Expected is at least the case just created.
        response = getResponse("v1/api/cases", Map.of("id", "9788764432589"));
        cases = mapper.readValue(response.readEntity(String.class), CaseSummaryList.class);
       
        assertThat(cases.getNumFound(), is(greaterThanOrEqualTo(1)));
        assertThat("The newly created first case with this isbn is one of them",
                cases.getCases().stream().map(PromatCase::getId).collect(Collectors.toList()).contains(created.getId()));

        // Query by ean 5053083221386: Expected is at least the case just created.
        response = getResponse("v1/api/cases", Map.of("id", "5053083221386"));
        cases = mapper.readValue(response.readEntity(String.class), CaseSummaryList.class);
        assertThat(cases.getNumFound(), is(greaterThanOrEqualTo(1)));
        assertThat("The newly created second case with this ean is one of them",
                cases.getCases().stream().map(PromatCase::getId).collect(Collectors.toList()).contains(created_2.getId()));

        // Query by publisher: Expected are both cases.
        response = getResponse("v1/api/cases", Map.of("publisher", "for"));
        cases = mapper.readValue(response.readEntity(String.class), CaseSummaryList.class);
        assertThat(cases.getNumFound(), is(greaterThanOrEqualTo(2)));
        assertThat("Both newly created cases, should be present",
                cases.getCases().stream().map(PromatCase::getId).collect(Collectors.toList())
                        .containsAll(List.of(created_2.getId(), created.getId())));

        // Query by publisher: Expected is only the first.
        response = getResponse("v1/api/cases", Map.of("publisher", "27100104"));
        cases = mapper.readValue(response.readEntity(String.class), CaseSummaryList.class);
        List<Integer> caseIds = cases.getCases().stream().map(PromatCase::getId).collect(Collectors.toList());
        assertThat(cases.getNumFound(), is(greaterThanOrEqualTo(1)));
        assertThat("The first of the cases, should be present",
                caseIds.contains(created.getId()));

        assertThat("The second one of the two cases, should NOT be present",
                not(caseIds.contains(created_2.getId())));

        // Delete case 1
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));

        // Delete case 2
        response = deleteResponse("v1/api/cases/" + created_2.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testThatEmaterialForDownloadIsRegisteredProperly() throws JsonProcessingException {

        // Create a new case
        CaseRequest dto = new CaseRequest()
                .withTitle("Title for 39449668")
                .withDetails("Details for 39449668")
                .withPrimaryFaust("39449668")
                .withEditor(10)
                .withCreator(10)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-07-30")
                .withMaterialType(MaterialType.BOOK)
                .withPublisher("Publisher for 39449668")
                .withTasks(Collections.singletonList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Collections.singletonList("39449668"))));

        // Create a another new case
        CaseRequest dto2 = new CaseRequest()
                .withTitle("Title for 39449669")
                .withDetails("Details for 39449669")
                .withPrimaryFaust("39449669")
                .withEditor(10)
                .withCreator(10)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-07-30")
                .withMaterialType(MaterialType.BOOK)
                .withPublisher("Publisher for 39449669")
                .withTasks(Collections.singletonList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Collections.singletonList("39449669"))));

        Response response = postResponse("v1/api/cases", dto);
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status code", response.getStatus(), is(201));

        // Check that there now is a download url.
        assertThat(created.getFulltextLink(), is("http://host.testcontainers.internal:" + wireMockServer.port() +
                "?faust=39449668"));

        // Check that for the next one, no url is present.
        response = postResponse("v1/api/cases", dto2);
        PromatCase created2 = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("status code", response.getStatus(), is(201));
        assertThat("No e-content for this faust could be found at creation time.",created2.getFulltextLink(), nullValue());

        // Delete the cases so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));

        response = deleteResponse("v1/api/cases/" + created2.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testThatGdprFieldsIsNeverExposedOnCases() throws JsonProcessingException {

        // Create case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("5001111")
                .withTitle("Title for 5001111")
                .withMaterialType(MaterialType.BOOK)
                .withReviewer(1)
                .withEditor(10)
                .withDeadline("2021-08-21")
                .withSubjects(Arrays.asList(3, 4));

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        assertGdprFields(created);

        // Get the case
        response = getResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
        PromatCase fetched = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        assertGdprFields(fetched);

        // Update the case
        dto = new CaseRequest()
                .withTitle("Title for 5001111 - rettet");

        response = postResponse("v1/api/cases/" + created.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        PromatCase updated = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        assertGdprFields(updated);

        // Search the case
        response = getResponse("v1/api/cases", Map.of("faust", "5001111"));
        assertThat("status code", response.getStatus(), is(200));
        CaseSummaryList found = mapper.readValue(response.readEntity(String.class), CaseSummaryList.class);
        assertThat("found the case", found.getCases().size(), is(1));

        assertGdprFields(found.getCases().get(0));
    }

    private void assertGdprFields(PromatCase promatCase) {
        assertThat("reviewer is set", promatCase.getReviewer(), is(notNullValue()));
        assertThat("editor is set", promatCase.getEditor(), is(notNullValue()));

        assertThat("reviewer address is not set", promatCase.getReviewer().getAddress(), is(nullValue()));
        assertThat("reviewer email is not set", promatCase.getReviewer().getEmail(), is(nullValue()));
        assertThat("reviewer phone is not set", promatCase.getReviewer().getPhone(), is(nullValue()));

        assertThat("reviewer private address is not set", promatCase.getReviewer().getPrivateAddress(), is(nullValue()));
        assertThat("reviewer private email is not set", promatCase.getReviewer().getPrivateEmail(), is(nullValue()));
        assertThat("reviewer private phone is not set", promatCase.getReviewer().getPrivatePhone(), is(nullValue()));

        assertThat("editor email is not set", promatCase.getEditor().getEmail(), is(nullValue()));
        assertThat("editor phone is not set", promatCase.getEditor().getPhone(), is(nullValue()));
    }

    @Test
    public void testRepeatedCaseCreation() throws IOException, InterruptedException {
        final int REVIEWER_ID = 3;
        final int EDITOR_ID = 11;

        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("5004322")
                .withTitle("Title for 5004322")
                .withMaterialType(MaterialType.BOOK)
                .withEditor(EDITOR_ID)
                .withDeadline("2021-08-28")
                .withSubjects(Arrays.asList(3, 4));

        // Create case.
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase aCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Try to assign the case by incorrectly posting it again
        dto.setReviewer(REVIEWER_ID);
        response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(409));

        // Now assign a reviewer the correct way
        response = postResponse("v1/api/cases/" + aCase.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));

        // Cleanup
        response = deleteResponse("v1/api/cases/" + aCase.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testSearchCaseWithPendingClose() throws IOException, InterruptedException {

        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("5004422")
                .withTitle("Title for 5004422")
                .withMaterialType(MaterialType.BOOK)
                .withReviewer(3)
                .withCreator(11)
                .withEditor(11)
                .withDeadline("2021-09-09")
                .withSubjects(Arrays.asList(3, 4));

        // Create case.
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase aCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        CaseRequest statusDto = new CaseRequest()
                .withStatus(CaseStatus.PENDING_APPROVAL);
        assertThat("set to PENDING_APPROVAL", postResponse("v1/api/cases/" + aCase.getId(), statusDto).getStatus(), is(200));

        statusDto = new CaseRequest()
                .withStatus(CaseStatus.PENDING_CLOSE);
        assertThat("set to PENDING_CLOSE", postResponse("v1/api/cases/" + aCase.getId(), statusDto).getStatus(), is(200));

        // Search the case using faust
        response = getResponse("v1/api/cases", Map.of("faust", "5004422"));
        assertThat("status code", response.getStatus(), is(200));
        CaseSummaryList found = mapper.readValue(response.readEntity(String.class), CaseSummaryList.class);
        assertThat("found the case", found.getCases().size(), is(1));

        // Search the case using id
        response = getResponse("v1/api/cases", Map.of("id", "5004422"));
        assertThat("status code", response.getStatus(), is(200));
        found = mapper.readValue(response.readEntity(String.class), CaseSummaryList.class);
        assertThat("found the case", found.getCases().size(), is(1));

        // Cleanup
        response = deleteResponse("v1/api/cases/" + aCase.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testSearchAndViewClosedCases() throws IOException, InterruptedException, PromatServiceConnectorException {

        // Create case.
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("5004522")
                .withTitle("CASE A")
                .withMaterialType(MaterialType.BOOK)
                .withReviewer(3)
                .withCreator(11)
                .withEditor(11)
                .withDeadline("2021-09-09")
                .withSubjects(Arrays.asList(3, 4))
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Arrays.asList("5004522")),
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.DESCRIPTION)
                                .withTargetFausts(Arrays.asList(new String[] {"5004522"}))
                ));

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase caseA = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Close case A
        dto = new CaseRequest().withStatus(CaseStatus.CLOSED);
        response = postResponse("v1/api/cases/" + caseA.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));

        // Create another case with same faustnumber
        dto = new CaseRequest()
                .withPrimaryFaust("5004522")
                .withTitle("CASE B")
                .withMaterialType(MaterialType.BOOK)
                .withReviewer(3)
                .withCreator(11)
                .withEditor(11)
                .withDeadline("2021-09-09")
                .withSubjects(Arrays.asList(3, 4))
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Arrays.asList("5004522")),
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.DESCRIPTION)
                                .withTargetFausts(Arrays.asList(new String[] {"5004522"}))
                ));

        response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase caseB = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Search can find only find case B when searching specifically on faust
        response = getResponse("v1/api/cases", Map.of("faust", "5004522"));
        assertThat("status code", response.getStatus(), is(200));
        CaseSummaryList found = mapper.readValue(response.readEntity(String.class), CaseSummaryList.class);
        assertThat("found one case", found.getCases().size(), is(1));
        assertThat("correct case", found.getCases().get(0).getTitle().equals("CASE B"));

        // Search can find case A and case B when searching on id
        response = getResponse("v1/api/cases", Map.of("id", "5004522"));
        assertThat("status code", response.getStatus(), is(200));
        found = mapper.readValue(response.readEntity(String.class), CaseSummaryList.class);
        assertThat("found two cases", found.getCases().size(), is(2));
        assertThat("found case A", found.getCases().stream().anyMatch(c -> c.getTitle().equals("CASE A")));
        assertThat("found case B", found.getCases().stream().anyMatch(c -> c.getTitle().equals("CASE B")));

        // Caseview (html/xml) of faust 5004522 finds case B and does not explode
        String actual = promatServiceConnector.getCaseview("5004522", "HTML", true, StandardCharsets.ISO_8859_1);
        assertThat("got caseview", actual.contains("CASE B"));

        // Cleanup
        response = deleteResponse("v1/api/cases/" + caseA.getId());
        assertThat("status code", response.getStatus(), is(200));
        response = deleteResponse("v1/api/cases/" + caseB.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testSetKeepEditor() throws IOException {

        // Note: Since the keepEditor flag is not exposed (it is a strictly internal value
        //       which should never be used or set from outside - and may change if we decide
        //       to handle the editor-reassignment differently in the future), we cannot check
        //       that it actually gets set.. so this test only checks that the service does not
        //       blow up in a spectacular fashion when changing status to PENDING_ISSUES

        // Create case.
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("5004622")
                .withTitle("Title of 5004622")
                .withMaterialType(MaterialType.BOOK)
                .withReviewer(3)
                .withCreator(11)
                .withEditor(11)
                .withDeadline("2021-09-13")
                .withSubjects(Arrays.asList(3, 4))
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Arrays.asList("5004522")),
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.DESCRIPTION)
                                .withTargetFausts(Arrays.asList(new String[] {"5004522"}))
                ));

        // Create the case and check that the keepEditor field is not exposed
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        String caseAsString = response.readEntity(String.class);
        assertThat("keepEditor is not exposed", caseAsString.contains("keepEditor"), is(false));
        PromatCase aCase = mapper.readValue(caseAsString, PromatCase.class);

        // Send case to approval, then back to the reviewer
        dto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), dto)
                .getStatus(), is(200));
        dto.setStatus(CaseStatus.PENDING_ISSUES);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), dto)
                .getStatus(), is(200));

        // Cleanup
        response = deleteResponse("v1/api/cases/" + aCase.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testSetAssignedDateOnAssigned() throws IOException {

        // Initial check to make sure we dont get false results (test is added late)
        Response response = getResponse("v1/api/cases/6");
        assertThat("status code", response.getStatus(), is(200));
        PromatCase aCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("case has status ASSIGNED", aCase.getStatus(), is(CaseStatus.ASSIGNED));
        assertThat("case has assigned date in the past", aCase.getAssigned(), is(LocalDate.parse("2020-11-11")));

        // Set a new reviewer
        CaseRequest dto = new CaseRequest().withReviewer(2);
        response = postResponse("v1/api/cases/6", dto);
        assertThat("status code", response.getStatus(), is(200));

        // Make sure the case now have today's date as assigned date
        response = getResponse("v1/api/cases/6");
        assertThat("status code", response.getStatus(), is(200));
        aCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("case has status ASSIGNED", aCase.getStatus(), is(CaseStatus.ASSIGNED));
        assertThat("case has assigned date = now", aCase.getAssigned(), is(LocalDate.now()));

        // Reset reviewer (other tests may depend on this case)
        dto = new CaseRequest().withReviewer(1);
        response = postResponse("v1/api/cases/6", dto);
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testRevertedCaseIsInactive() throws JsonProcessingException {

        // Create first case
        CaseRequest requestDto = new CaseRequest()
                .withTitle("Title for 90005678")
                .withDetails("Details for 90005678")
                .withPrimaryFaust("90005678")
                .withEditor(10)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-03-30")
                .withMaterialType(MaterialType.BOOK);
        Response response = postResponse("v1/api/cases", requestDto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase firstCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Send case to approval
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        assertThat("status code", postResponse("v1/api/cases/" + firstCase.getId(), requestDto).getStatus(), is(200));

        // Approve the case
        requestDto = new CaseRequest().withStatus(CaseStatus.APPROVED);
        assertThat("status code", postResponse("v1/api/cases/" + firstCase.getId(), requestDto).getStatus(), is(200));

        // Move to PENDING_EXPORT
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_EXPORT);
        assertThat("status code", postResponse("v1/api/cases/" + firstCase.getId(), requestDto).getStatus(), is(200));

        // Move to EXPORTED
        requestDto = new CaseRequest().withStatus(CaseStatus.EXPORTED);
        assertThat("status code", postResponse("v1/api/cases/" + firstCase.getId(), requestDto).getStatus(), is(200));

        // Move to PENDING_REVERT
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_REVERT);
        assertThat("status code", postResponse("v1/api/cases/" + firstCase.getId(), requestDto).getStatus(), is(200));

        // Move to REVERTED
        requestDto = new CaseRequest().withStatus(CaseStatus.REVERTED);
        assertThat("status code", postResponse("v1/api/cases/" + firstCase.getId(), requestDto).getStatus(), is(200));

        // Create a new case, reusing the primary faust of the first case
        requestDto = new CaseRequest()
                .withTitle("New title for 90005678")
                .withDetails("New details for 90005678")
                .withPrimaryFaust("90005678")
                .withEditor(10)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-03-30")
                .withMaterialType(MaterialType.BOOK);

        response = postResponse("v1/api/cases", requestDto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase secondCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Make sure that the first case can not be reactivated (since the faustnumber is now in use)
        requestDto = new CaseRequest().withStatus(CaseStatus.CREATED);
        assertThat("status code", postResponse("v1/api/cases/" + firstCase.getId(), requestDto).getStatus(), is(409));

        // Close the first case
        requestDto = new CaseRequest().withStatus(CaseStatus.CLOSED);
        assertThat("status code", postResponse("v1/api/cases/" + firstCase.getId(), requestDto).getStatus(), is(200));

        // First case can still not be reactivated
        requestDto = new CaseRequest().withStatus(CaseStatus.CREATED);
        assertThat("status code", postResponse("v1/api/cases/" + firstCase.getId(), requestDto).getStatus(), is(409));

        // Delete the first case
        assertThat("status code", deleteResponse("v1/api/cases/" + firstCase.getId()).getStatus(), is(200));

        // Even now, the first case can not be reactivated
        requestDto = new CaseRequest().withStatus(CaseStatus.CREATED);
        assertThat("status code", postResponse("v1/api/cases/" + firstCase.getId(), requestDto).getStatus(), is(409));

        // Delete the second case (cleanup)
        assertThat("status code", deleteResponse("v1/api/cases/" + secondCase.getId()).getStatus(), is(200));
    }

    @Test
    public void testInactiveCaseCanBeReactivated() throws JsonProcessingException {

        // Create case
        CaseRequest requestDto = new CaseRequest()
                .withTitle("Title for 90009012")
                .withDetails("Details for 90009012")
                .withPrimaryFaust("90009012")
                .withEditor(10)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2021-03-30")
                .withMaterialType(MaterialType.BOOK);
        Response response = postResponse("v1/api/cases", requestDto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase aCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Send case to approval
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), requestDto).getStatus(), is(200));

        // Approve the case
        requestDto = new CaseRequest().withStatus(CaseStatus.APPROVED);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), requestDto).getStatus(), is(200));

        // Move to PENDING_EXPORT
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_EXPORT);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), requestDto).getStatus(), is(200));

        // Move to EXPORTED
        requestDto = new CaseRequest().withStatus(CaseStatus.EXPORTED);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), requestDto).getStatus(), is(200));

        // Move to PENDING_REVERT
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_REVERT);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), requestDto).getStatus(), is(200));

        // Move to REVERTED
        requestDto = new CaseRequest().withStatus(CaseStatus.REVERTED);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), requestDto).getStatus(), is(200));

        // Reactivate the case
        requestDto = new CaseRequest().withStatus(CaseStatus.CREATED);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), requestDto).getStatus(), is(200));

        // Close the case
        requestDto = new CaseRequest().withStatus(CaseStatus.CLOSED);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), requestDto).getStatus(), is(200));

        // Reactivate the case
        requestDto = new CaseRequest().withStatus(CaseStatus.CREATED);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), requestDto).getStatus(), is(200));

        // Delete the case
        assertThat("status code", deleteResponse("v1/api/cases/" + aCase.getId()).getStatus(), is(200));

        // Reactivate the case
        requestDto = new CaseRequest().withStatus(CaseStatus.CREATED);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), requestDto).getStatus(), is(200));

        // Delete the case (final cleanup)
        assertThat("status code", deleteResponse("v1/api/cases/" + aCase.getId()).getStatus(), is(200));
    }

    @Test
    public void testFaustActiveEndpoint() throws JsonProcessingException {
        // Setup
        // Create case
        CreateStatusDto createStatusDto;

        CaseRequest requestDto = new CaseRequest()
                .withTitle("Title for 00000001")
                .withDetails("Details for 00000001")
                .withPrimaryFaust("00000001")
                .withEditor(10)
                .withReviewer(1)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline("2022-03-30")
                .withMaterialType(MaterialType.BOOK);
        Response response = postResponse("v1/api/cases", requestDto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase aCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Test
        response = getResponse("v1/api/cases/active/" + aCase.getPrimaryFaust());
        assertThat("status code", response.getStatus(), is(200));
        createStatusDto = mapper.readValue(response.readEntity(String.class), CreateStatusDto.class);
        assertThat("createStatus", createStatusDto.getCreateStatus(), is(CreateStatus.IN_ACTIVE_CASE));

        // Send case to approval
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), requestDto).getStatus(), is(200));

        // Approve the case
        requestDto = new CaseRequest().withStatus(CaseStatus.APPROVED);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), requestDto).getStatus(), is(200));

        // Move to PENDING_EXPORT
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_EXPORT);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), requestDto).getStatus(), is(200));

        // Move to EXPORTED
        requestDto = new CaseRequest().withStatus(CaseStatus.EXPORTED);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), requestDto).getStatus(), is(200));

        // Move to PENDING_REVERT
        requestDto = new CaseRequest().withStatus(CaseStatus.PENDING_REVERT);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), requestDto).getStatus(), is(200));

        // Test IN_ACTIVE_CASE
        response = getResponse("v1/api/cases/active/" + aCase.getPrimaryFaust());
        assertThat("status code", response.getStatus(), is(200));
        createStatusDto = mapper.readValue(response.readEntity(String.class), CreateStatusDto.class);
        assertThat("createStatus", createStatusDto.getCreateStatus(), is(CreateStatus.IN_ACTIVE_CASE));

        // Move to REVERTED
        requestDto = new CaseRequest().withStatus(CaseStatus.REVERTED);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), requestDto).getStatus(), is(200));

        // Test READY_FOR_CREATION
        response = getResponse("v1/api/cases/active/" + aCase.getPrimaryFaust());
        assertThat("status code", response.getStatus(), is(200));
        createStatusDto = mapper.readValue(response.readEntity(String.class), CreateStatusDto.class);
        assertThat("createStatus", createStatusDto.getCreateStatus(), is(CreateStatus.READY_FOR_CREATION));

        // Reactivate the case
        requestDto = new CaseRequest().withStatus(CaseStatus.CREATED);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), requestDto).getStatus(), is(200));

        // Test IN_ACTIVE_CASE
        response = getResponse("v1/api/cases/active/" + aCase.getPrimaryFaust());
        assertThat("status code", response.getStatus(), is(200));
        createStatusDto = mapper.readValue(response.readEntity(String.class), CreateStatusDto.class);
        assertThat("createStatus", createStatusDto.getCreateStatus(), is(CreateStatus.IN_ACTIVE_CASE));

        // Close the case
        requestDto = new CaseRequest().withStatus(CaseStatus.CLOSED);
        assertThat("status code", postResponse("v1/api/cases/" + aCase.getId(), requestDto).getStatus(), is(200));

        // Test READY_FOR_CREATION
        response = getResponse("v1/api/cases/active/" + aCase.getPrimaryFaust());
        assertThat("status code", response.getStatus(), is(200));
        createStatusDto = mapper.readValue(response.readEntity(String.class), CreateStatusDto.class);
        assertThat("createStatus", createStatusDto.getCreateStatus(), is(CreateStatus.READY_FOR_CREATION));

        // Delete the case (final cleanup)
        assertThat("status code", deleteResponse("v1/api/cases/" + aCase.getId()).getStatus(), is(200));
    }
}
