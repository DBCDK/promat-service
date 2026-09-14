package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.type.TypeReference;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.connector.PromatServiceConnectorException;
import dk.dbc.promat.service.connector.PromatServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.dto.MetakompasSelectionEntry;
import dk.dbc.promat.service.dto.Tag;
import dk.dbc.promat.service.dto.TagList;
import dk.dbc.promat.service.dto.TaskDto;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.persistence.TaskType;
import dk.dbc.promat.service.taxonomy.dto.Subject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import static dk.dbc.promat.service.TestUtils.getResource;
import static dk.dbc.promat.service.taxonomy.RecordServiceMocks.addToAgencyDump;
import static dk.dbc.promat.service.taxonomy.RecordServiceMocks.resetAgencyDump;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

// Covers the endpoints persisting what a reviewer actually selected from the Metakompas
// taxonomy tree and the Buggi tag vocabulary for a given task (PUT/GET
// tasks/{taskId}/metakompas|buggi). One selection per task, shared across all of its target
// fausts, same as every other task type's data. Separate from CasesIT (which still covers the
// legacy POST cases/{pid}/buggi endpoint) to keep this feature's tests together.
public class CaseTaskSelectionIT extends ContainerTest {

    @Test
    void testBuggiSelectionSharedAcrossTargetFausts() throws PromatServiceConnectorException, IOException {
        String firstFaust = "94001111";
        String secondFaust = "94001112";
        PromatCase aCase = postAndAssert("v1/api/cases",
                makeRequestWithTargetFausts(firstFaust, TaskFieldType.BUGGI, firstFaust, secondFaust),
                PromatCase.class, CREATED);
        int taskId = ContainerTest.findTaskByFieldType(aCase, TaskFieldType.BUGGI).getId();

        TagList tags = new TagList(new Tag("first", 1));
        promatServiceConnector.putBuggiSelection(taskId, tags);

        TagList read = promatServiceConnector.getBuggiSelection(taskId);
        assertThat(read.getTags().get(0).getName(), is("first"));

        // A later write is a plain overwrite of the one shared value - same as PromatTask.data
        // behaves for every other task type - not independent per faust.
        TagList secondTags = new TagList(new Tag("second", 2));
        promatServiceConnector.putBuggiSelection(taskId, secondTags);
        TagList readAfterOverwrite = promatServiceConnector.getBuggiSelection(taskId);
        assertThat(readAfterOverwrite.getTags().get(0).getName(), is("second"));

        // Also confirm it comes back inline on the full case view, in the same task.data field
        // every other task type uses
        PromatCase fullCase = promatServiceConnector.getCase(aCase.getId());
        PromatTask task = ContainerTest.findTaskByFieldType(fullCase, TaskFieldType.BUGGI);
        assertThat(mapper.readValue(task.getData(), TagList.class).getTags().get(0).getName(), is("second"));

        deleteResponse("v1/api/cases/" + aCase.getId());
    }

    // The legacy cases/{pid}/buggi endpoint resolves its task by faust, but the write itself is
    // task-scoped like everywhere else - approving via either target faust of the same task
    // writes the one shared selection.
    @Test
    void testLegacyBuggiEndpointSharesSelectionAcrossTargetFausts() throws PromatServiceConnectorException {
        String descriptor = "870170-BASIS:";
        String firstFaust = "94001113";
        String secondFaust = "94001114";
        PromatCase aCase = postAndAssert("v1/api/cases",
                makeRequestWithTargetFausts(firstFaust, TaskFieldType.BUGGI, firstFaust, secondFaust),
                PromatCase.class, CREATED);
        int taskId = ContainerTest.findTaskByFieldType(aCase, TaskFieldType.BUGGI).getId();

        promatServiceConnector.approveBuggiTask(descriptor + firstFaust, new TagList(new Tag("legacy-first", 1)));
        promatServiceConnector.approveBuggiTask(descriptor + secondFaust, new TagList(new Tag("legacy-second", 2)));

        TagList read = promatServiceConnector.getBuggiSelection(taskId);
        assertThat(read.getTags().get(0).getName(), is("legacy-second"));

        deleteResponse("v1/api/cases/" + aCase.getId());
    }

    @Test
    void testBuggiSelectionValidation() throws PromatServiceConnectorException {
        String faust = "94001116";
        // A BKM task, not BUGGI - the endpoint should reject writes/reads targeting it as BUGGI
        PromatCase aCase = postAndAssert("v1/api/cases", makeRequest(faust, TaskFieldType.BKM), PromatCase.class, CREATED);
        int taskId = ContainerTest.findTaskByFieldType(aCase, TaskFieldType.BKM).getId();

        assertPromatThrows(BAD_REQUEST, () -> promatServiceConnector.putBuggiSelection(taskId, new TagList(new Tag("x", 1))));
        assertPromatThrows(NOT_FOUND, () -> promatServiceConnector.putBuggiSelection(999999, new TagList(new Tag("x", 1))));

        deleteResponse("v1/api/cases/" + aCase.getId());
    }

    @Test
    void testMetakompasSelection() throws IOException, PromatServiceConnectorException {
        List<String> path = List.of("handling", "hovedperson(er) - beskrivelse", "hovedpersonens karaktertræk");
        addToAgencyDump(getResource("/taxonomy/records/19487032.json").replace('\n', ' '));
        deleteResponse("v1/api/taxonomy/cache");
        try {
            List<Subject> subjects = postAndAssert("v1/api/taxonomy/subtree", path, new TypeReference<List<Subject>>() {}, Response.Status.OK);
            Subject observerende = subjects.stream()
                    .filter(s -> s.getTitle().equals("observerende"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Fixture record did not add expected subject 'observerende'"));

            String faust = "94001115";
            PromatCase aCase = postAndAssert("v1/api/cases", makeRequest(faust, TaskFieldType.METAKOMPAS), PromatCase.class, CREATED);
            int taskId = ContainerTest.findTaskByFieldType(aCase, TaskFieldType.METAKOMPAS).getId();

            MetakompasSelectionEntry entry = new MetakompasSelectionEntry()
                    .withPath(path)
                    .withId(observerende.getId())
                    .withTitle(observerende.getTitle())
                    .withNote(observerende.getNote())
                    .withOftenUsed(observerende.isOftenUsed())
                    .withRef(observerende.getRef());

            List<MetakompasSelectionEntry> saved = promatServiceConnector.putMetakompasSelection(taskId, List.of(entry));
            assertThat(saved.get(0).getTitle(), is("observerende"));

            List<MetakompasSelectionEntry> read = promatServiceConnector.getMetakompasSelection(taskId);
            assertThat(read.get(0).getTitle(), is("observerende"));

            PromatCase fullCase = promatServiceConnector.getCase(aCase.getId());
            PromatTask task = ContainerTest.findTaskByFieldType(fullCase, TaskFieldType.METAKOMPAS);
            List<MetakompasSelectionEntry> inlined = mapper.readValue(task.getData(), new TypeReference<List<MetakompasSelectionEntry>>() {});
            assertThat(inlined.get(0).getTitle(), is("observerende"));

            // A subject id that doesn't exist at the (valid) path should be rejected
            MetakompasSelectionEntry bogus = new MetakompasSelectionEntry().withPath(path).withId(-999).withTitle("does-not-exist");
            assertPromatThrows(BAD_REQUEST, () -> promatServiceConnector.putMetakompasSelection(taskId, List.of(bogus)));

            // A path that doesn't exist in the tree at all should also be rejected
            MetakompasSelectionEntry badPath = new MetakompasSelectionEntry().withPath(List.of("not", "a", "real", "path")).withId(1).withTitle("nope");
            assertPromatThrows(BAD_REQUEST, () -> promatServiceConnector.putMetakompasSelection(taskId, List.of(badPath)));

            deleteResponse("v1/api/cases/" + aCase.getId());
        } finally {
            resetAgencyDump();
            deleteResponse("v1/api/taxonomy/cache");
        }
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

    // Single task of the given type, targeting more than one faust - the shape needed to
    // confirm a selection is shared across all of a task's target fausts.
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
