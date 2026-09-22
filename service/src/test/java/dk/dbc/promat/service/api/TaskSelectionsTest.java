package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.promat.service.connectors.CatalogingUpdateConnector;
import dk.dbc.promat.service.connectors.CatalogingUpdateConnectorException;
import dk.dbc.promat.service.dto.BuggiSelectionRequest;
import dk.dbc.promat.service.dto.MetakompasSelectionRequest;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.taskdata.BuggiSelectionEntry;
import dk.dbc.promat.service.taskdata.MetakompasTaskData;
import dk.dbc.promat.service.taxonomy.TaxonomyCache;
import dk.dbc.promat.service.taxonomy.dto.Subject;
import dk.dbc.promat.service.taxonomy.dto.Taxonomy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TaskSelectionsTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void rejectsMetakompasSelectionWithInvalidPath() {
        TaskSelections taskSelections = taskSelectionsWithTaxonomy();

        ServiceErrorException exception = assertThrows(ServiceErrorException.class, () ->
                taskSelections.writeMetakompasSelection(new PromatTask(), List.of(
                        new MetakompasSelectionRequest()
                                .withPath(List.of("handling", "not a real category"))
                                .withSuggestions(List.of("new word")))));

        assertThat(exception.getHttpStatus(), is(400));
        assertThat(exception.getServiceErrorDto().getCode(), is(ServiceErrorCode.INVALID_REQUEST));
        assertThat(exception.getServiceErrorDto().getCause(), is("Invalid metakompas path"));
    }

    @Test
    public void resolvesSelectedIdsByIdEvenWhenRequestPathIsDifferentButValid() throws ServiceErrorException {
        TaskSelections taskSelections = taskSelectionsWithTaxonomy();

        MetakompasTaskData result = taskSelections.writeMetakompasSelection(new PromatTask(), List.of(
                new MetakompasSelectionRequest()
                        .withPath(List.of("stemning", "dramatisk"))
                        .withIds(List.of(42))));

        assertThat(result.getEntries().getFirst().getPath(), contains("ramme", "genre"));
    }

    @Test
    public void acceptsSuggestionsWhenPathExists() throws ServiceErrorException {
        TaskSelections taskSelections = taskSelectionsWithTaxonomy();

        MetakompasTaskData result = taskSelections.writeMetakompasSelection(new PromatTask(), List.of(
                new MetakompasSelectionRequest()
                        .withPath(List.of("handling", "handler om"))
                        .withSuggestions(List.of("new word", "another word"))));

        assertThat(result.getSuggestions().getFirst().getPath(), contains("handling", "handler om"));
        assertThat(result.getSuggestions().getFirst().getText(), is("new word"));
        assertThat(result.getSuggestions().get(1).getPath(), contains("handling", "handler om"));
        assertThat(result.getSuggestions().get(1).getText(), is("another word"));
    }

    @Test
    public void storesMetakompasSelectionDataAsParseableJson() throws Exception {
        TaskSelections taskSelections = taskSelectionsWithTaxonomy();
        PromatTask task = new PromatTask();

        taskSelections.writeMetakompasSelection(task, List.of(
                new MetakompasSelectionRequest()
                        .withPath(List.of("handling", "handler om"))
                        .withIds(List.of(42))
                        .withSuggestions(List.of("new word"))));

        MetakompasTaskData data = OBJECT_MAPPER.readValue(task.getData(), MetakompasTaskData.class);

        assertThat(data.getEntries().getFirst().getId(), is(42));
        assertThat(data.getEntries().getFirst().getTitle(), is("krimi"));
        assertThat(data.getSuggestions().getFirst().getPath(), contains("handling", "handler om"));
        assertThat(data.getSuggestions().getFirst().getText(), is("new word"));
    }

    @Test
    public void storesBuggiSelectionDataAsParseableJson() throws Exception {
        TaskSelections taskSelections = taskSelectionsWithTaxonomy();
        PromatTask task = new PromatTask();

        taskSelections.writeBuggiSelection(task, List.of(new BuggiSelectionRequest(8, 2)));

        List<BuggiSelectionEntry> data = OBJECT_MAPPER.readValue(task.getData(), new TypeReference<>() {});

        assertThat(data.getFirst().getId(), is(8));
        assertThat(data.getFirst().getName(), is("spændende"));
        assertThat(data.getFirst().getMarcSubfieldCode(), is("n"));
        assertThat(data.getFirst().getRequiresNonzeroValue(), is(true));
        assertThat(data.getFirst().getValue(), is(2));
    }

    @Test
    public void approveReadingExperienceRejectsEmptySelection() {
        TaskSelections taskSelections = taskSelectionsWithEntityManager(new PromatTask()
                .withId(123)
                .withTaskFieldType(TaskFieldType.BUGGI)
                .withData(""));

        ServiceErrorException exception = assertThrows(ServiceErrorException.class, () ->
                taskSelections.approveReadingExperience(123));

        assertThat(exception.getHttpStatus(), is(400));
        assertThat(exception.getServiceErrorDto().getCause(), is("Empty selection"));
    }

    @Test
    public void approveReadingExperienceRegistersNonEmptyBuggiSelectionAndApprovesTask() throws Exception {
        PromatTask task = new PromatTask()
                .withId(123)
                .withTaskFieldType(TaskFieldType.BUGGI)
                .withTargetFausts(List.of("12345678"));
        TaskSelections taskSelections = taskSelectionsWithEntityManager(task);
        taskSelections.writeBuggiSelection(task, List.of(new BuggiSelectionRequest(8, 2)));

        taskSelections.approveReadingExperience(123);

        verify(taskSelections.catalogingUpdateConnector).updateRecord(eq("870970:12345678"), anyString());
        assertThat(task.getApproved(), is(notNullValue()));
    }

    @Test
    public void approveReadingExperienceDoesNotApproveTaskWhenUpdateServiceFails() throws Exception {
        PromatTask task = new PromatTask()
                .withId(123)
                .withTaskFieldType(TaskFieldType.BUGGI)
                .withTargetFausts(List.of("12345678"));
        TaskSelections taskSelections = taskSelectionsWithEntityManager(task);
        taskSelections.writeBuggiSelection(task, List.of(new BuggiSelectionRequest(8, 2)));
        doThrow(new CatalogingUpdateConnectorException("update-service is down"))
                .when(taskSelections.catalogingUpdateConnector).updateRecord(anyString(), anyString());

        ServiceErrorException exception = assertThrows(ServiceErrorException.class, () ->
                taskSelections.approveReadingExperience(123));

        assertThat(exception.getHttpStatus(), is(502));
        assertThat(task.getApproved(), is((Object) null));
    }

    private TaskSelections taskSelectionsWithTaxonomy() {
        Taxonomy taxonomy = new Taxonomy();
        taxonomy.put(new Subject()
                .withId(42)
                .withTitle("krimi")
                .withPath(List.of("ramme", "genre")), "ramme", "genre");

        TaxonomyCache taxonomyCache = new TaxonomyCache();
        taxonomyCache.set(taxonomy);

        TaskSelections taskSelections = new TaskSelections();
        taskSelections.taxonomyCache = taxonomyCache;
        return taskSelections;
    }

    @SuppressWarnings("unchecked")
    private TaskSelections taskSelectionsWithEntityManager(PromatTask task) {
        TypedQuery<PromatCase> query = mock(TypedQuery.class);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(new PromatCase());

        EntityManager entityManager = mock(EntityManager.class);
        when(entityManager.find(PromatTask.class, 123)).thenReturn(task);
        when(entityManager.createNamedQuery(PromatCase.GET_CASE_WITH_TASK_ID_NAME, PromatCase.class)).thenReturn(query);

        TaskSelections taskSelections = new TaskSelections();
        taskSelections.entityManager = entityManager;
        taskSelections.catalogingUpdateConnector = mock(CatalogingUpdateConnector.class);
        taskSelections.updateRecordLibraryId = "870970";
        return taskSelections;
    }
}
