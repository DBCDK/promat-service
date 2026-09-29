package dk.dbc.promat.service.api;

import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.connector.PromatServiceConnectorException;
import dk.dbc.promat.service.connector.PromatServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.promat.service.dto.BuggiSelectionRequest;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.dto.MetakompasSelectionRequest;
import dk.dbc.promat.service.dto.Tag;
import dk.dbc.promat.service.dto.TagList;
import dk.dbc.promat.service.dto.TaskDto;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.persistence.TaskType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

// Covers the PUT tasks/{taskId}/metakompas|buggi endpoints and the legacy POST cases/{pid}/buggi
// endpoint's selection sharing across target fausts. CasesIT covers the rest of the legacy endpoint.
public class CaseTaskSelectionIT extends ContainerTest {

    // The IT container doesn't set METAKOMPAS_REGISTRATION, so it runs the default METAKOMPASSET, as prod
    // does until the switch - saving selections in Promat is rejected then. What the endpoints do in
    // PROMAT mode is covered by MetakompasAndBuggiTaskSelectionsTest.
    @Test
    void testSelectionsRejectedWhileRegistrationHappensInMetakompasset() throws PromatServiceConnectorException {
        String faust = "94001111";
        PromatCase aCase = postAndAssert("v1/api/cases",
                makeRequest(faust, TaskFieldType.METAKOMPAS, TaskFieldType.BUGGI), PromatCase.class, CREATED);
        int metakompasTaskId = ContainerTest.findTaskByFieldType(aCase, TaskFieldType.METAKOMPAS).getId();
        int buggiTaskId = ContainerTest.findTaskByFieldType(aCase, TaskFieldType.BUGGI).getId();

        assertPromatThrows(CONFLICT, () -> promatServiceConnector.putMetakompasSelection(metakompasTaskId,
                new MetakompasSelectionRequest().withIds(List.of())));
        assertPromatThrows(CONFLICT, () -> promatServiceConnector.putBuggiSelection(buggiTaskId,
                List.of(new BuggiSelectionRequest(1, 1))));

        deleteResponse("v1/api/cases/" + aCase.getId());
    }

    // The legacy endpoint resolves its task by faust, but the write is task-scoped - approving
    // via either faust writes the one shared selection.
    @Test
    void testLegacyBuggiEndpointSharesSelectionAcrossTargetFausts() throws PromatServiceConnectorException, IOException {
        String descriptor = "870170-BASIS:";
        String firstFaust = "94001113";
        String secondFaust = "94001114";
        PromatCase aCase = postAndAssert("v1/api/cases",
                makeRequestWithTargetFausts(firstFaust, TaskFieldType.BUGGI, firstFaust, secondFaust),
                PromatCase.class, CREATED);
        int taskId = ContainerTest.findTaskByFieldType(aCase, TaskFieldType.BUGGI).getId();

        promatServiceConnector.approveBuggiTask(descriptor + firstFaust, new TagList(new Tag("legacy-first", 1)));
        promatServiceConnector.approveBuggiTask(descriptor + secondFaust, new TagList(new Tag("legacy-second", 2)));

        PromatCase fullCase = promatServiceConnector.getCase(aCase.getId());
        PromatTask task = ContainerTest.findTaskByFieldType(fullCase, TaskFieldType.BUGGI);
        assertThat(mapper.readValue(task.getData(), TagList.class).getTags().get(0).getName(), is("legacy-second"));

        deleteResponse("v1/api/cases/" + aCase.getId());
    }

    @Test
    void testBuggiSelectionValidation() throws PromatServiceConnectorException {
        String faust = "94001116";
        // A BKM task, not BUGGI - the endpoint should reject writing to it
        PromatCase aCase = postAndAssert("v1/api/cases", makeRequest(faust, TaskFieldType.BKM), PromatCase.class, CREATED);
        int taskId = ContainerTest.findTaskByFieldType(aCase, TaskFieldType.BKM).getId();

        assertPromatThrows(BAD_REQUEST, () -> promatServiceConnector.putBuggiSelection(taskId, List.of(new BuggiSelectionRequest(1, 1))));
        assertPromatThrows(NOT_FOUND, () -> promatServiceConnector.putBuggiSelection(999999, List.of(new BuggiSelectionRequest(1, 1))));

        deleteResponse("v1/api/cases/" + aCase.getId());
    }

    private CaseRequest makeRequest(String faust, TaskFieldType... tasks) {
        return new CaseRequest()
                .withTitle("Title for " + faust)
                .withDetails("Details for " + faust)
                .withPrimaryFaust(faust)
                .withEditor(10)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline(LocalDate.now().plus(10, ChronoUnit.DAYS).toString())
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.stream(tasks)
                        .map(tf -> new TaskDto().withTaskFieldType(tf).withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES).withTargetFausts(List.of(faust)))
                        .toList());
    }

    // A single task targeting more than one faust, for the shared-selection tests.
    private CaseRequest makeRequestWithTargetFausts(String primaryFaust, TaskFieldType taskFieldType, String... targetFausts) {
        return makeRequest(primaryFaust)
                .withTasks(List.of(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(taskFieldType)
                        .withTargetFausts(List.of(targetFausts))));
    }

    private void assertPromatThrows(Response.Status status, Callable<?> callable) {
        try {
            callable.call();
            Assertions.fail("Expected call to throw, but it returned normally");
        } catch (PromatServiceConnectorUnexpectedStatusCodeException pe) {
            Assertions.assertEquals(status.getStatusCode(), pe.getStatusCode(),
                    "Client was expected to throw an exception containing status code " + status);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
