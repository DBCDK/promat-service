package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.promat.service.dto.BuggiSelectionRequest;
import dk.dbc.promat.service.dto.MetakompasSelectionRequest;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.taskdata.BuggiSelectionEntry;
import dk.dbc.promat.service.taskdata.MetakompasTaskData;
import dk.dbc.promat.service.taxonomy.TaxonomyCache;
import dk.dbc.promat.service.taxonomy.dto.Subject;
import dk.dbc.promat.service.taxonomy.dto.Taxonomy;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetakompasAndBuggiTaskSelectionsTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void rejectsMetakompasSelectionWithInvalidPath() {
        MetakompasAndBuggiTaskSelections taskSelections = taskSelectionsWithTaxonomy();

        ServiceErrorException exception = assertThrows(ServiceErrorException.class, () ->
                taskSelections.writeMetakompasSelection(new PromatTask(),
                        new MetakompasSelectionRequest()
                                .withSuggestions(List.of(new MetakompasTaskData.Suggestion(
                                        List.of("handling", "not a real category"), "new word")))));

        assertThat(exception.getHttpStatus(), is(400));
        assertThat(exception.getServiceErrorDto().getCode(), is(ServiceErrorCode.INVALID_REQUEST));
        assertThat(exception.getServiceErrorDto().getCause(), is("Invalid metakompas path"));
    }

    @Test
    public void resolvesSelectedIdsById() throws ServiceErrorException {
        MetakompasAndBuggiTaskSelections taskSelections = taskSelectionsWithTaxonomy();

        MetakompasTaskData result = taskSelections.writeMetakompasSelection(new PromatTask(),
                new MetakompasSelectionRequest().withIds(List.of(42)));

        // Every field asserted with a value distinct from every other field's - path/note (both
        // List<String>) and title/ref (both String) are positional constructor args a swap
        // wouldn't fail to compile, so this is what actually catches that regression.
        MetakompasTaskData.Entry entry = result.getEntries().getFirst();
        assertThat(entry.id(), is(42));
        assertThat(entry.title(), is("krimi"));
        assertThat(entry.path(), contains("ramme", "genre"));
        assertThat(entry.note(), contains("En bemærkning om krimigenren"));
        assertThat(entry.oftenUsed(), is(true));
        assertThat(entry.ref(), is("665-g-042"));
    }

    @Test
    public void acceptsSuggestionsWhenPathExists() throws ServiceErrorException {
        MetakompasAndBuggiTaskSelections taskSelections = taskSelectionsWithTaxonomy();

        MetakompasTaskData result = taskSelections.writeMetakompasSelection(new PromatTask(),
                new MetakompasSelectionRequest()
                        .withSuggestions(List.of(
                                new MetakompasTaskData.Suggestion(List.of("handling", "handler om"), "new word"),
                                new MetakompasTaskData.Suggestion(List.of("handling", "handler om"), "another word"))));

        assertThat(result.getSuggestions().getFirst().path(), contains("handling", "handler om"));
        assertThat(result.getSuggestions().getFirst().title(), is("new word"));
        assertThat(result.getSuggestions().get(1).path(), contains("handling", "handler om"));
        assertThat(result.getSuggestions().get(1).title(), is("another word"));
    }

    @Test
    public void storesMetakompasSelectionDataAsParseableJson() throws Exception {
        MetakompasAndBuggiTaskSelections taskSelections = taskSelectionsWithTaxonomy();
        PromatTask task = new PromatTask();

        taskSelections.writeMetakompasSelection(task,
                new MetakompasSelectionRequest()
                        .withIds(List.of(42))
                        .withSuggestions(List.of(new MetakompasTaskData.Suggestion(List.of("handling", "handler om"), "new word"))));

        MetakompasTaskData data = OBJECT_MAPPER.readValue(task.getData(), MetakompasTaskData.class);

        assertThat(data.getEntries().getFirst().id(), is(42));
        assertThat(data.getEntries().getFirst().title(), is("krimi"));
        assertThat(data.getSuggestions().getFirst().path(), contains("handling", "handler om"));
        assertThat(data.getSuggestions().getFirst().title(), is("new word"));
    }

    @Test
    public void storesBuggiSelectionDataAsParseableJson() throws Exception {
        MetakompasAndBuggiTaskSelections taskSelections = taskSelectionsWithTaxonomy();
        PromatTask task = new PromatTask();

        taskSelections.writeBuggiSelection(task, List.of(new BuggiSelectionRequest(8, 2)));

        List<BuggiSelectionEntry> data = OBJECT_MAPPER.readValue(task.getData(), new TypeReference<>() {});

        assertThat(data.getFirst().id(), is(8));
        assertThat(data.getFirst().name(), is("spændende"));
        assertThat(data.getFirst().marcSubfieldCode(), is("n"));
        assertThat(data.getFirst().value(), is(2));
    }

    @Test
    public void removesDuplicateBuggiOptionsKeepingTheLastValue() throws Exception {
        List<BuggiSelectionEntry> saved = taskSelectionsWithTaxonomy().writeBuggiSelection(new PromatTask(), List.of(
                new BuggiSelectionRequest(8, 2),
                new BuggiSelectionRequest(1, 3),
                new BuggiSelectionRequest(8, 4)));

        assertThat(saved.stream().map(BuggiSelectionEntry::id).toList(), contains(8, 1));
        assertThat(saved.getFirst().value(), is(4));
    }

    @Test
    public void removesDuplicateMetakompasSubjectsAndSuggestions() throws Exception {
        MetakompasTaskData saved = taskSelectionsWithTaxonomy().writeMetakompasSelection(new PromatTask(),
                new MetakompasSelectionRequest()
                        .withIds(List.of(42, 42))
                        .withSuggestions(List.of(
                                new MetakompasTaskData.Suggestion(List.of("handling", "handler om"), "new word"),
                                new MetakompasTaskData.Suggestion(List.of("handling", "handler om"), "new word"))));

        assertThat(saved.getEntries().size(), is(1));
        assertThat(saved.getSuggestions().size(), is(1));
    }

    @Test
    public void dropsSuggestionRepeatingASelectedSubjectInTheSameCategory() throws Exception {
        // Subject 42 is "krimi" under ramme -> genre
        MetakompasTaskData saved = taskSelectionsWithTaxonomy().writeMetakompasSelection(new PromatTask(),
                new MetakompasSelectionRequest()
                        .withIds(List.of(42))
                        .withSuggestions(List.of(
                                new MetakompasTaskData.Suggestion(List.of("ramme", "genre"), "krimi"),
                                // Different capitalisation may be a different word (proper noun), so it's kept
                                new MetakompasTaskData.Suggestion(List.of("ramme", "genre"), "Krimi"),
                                // Same word, other category - kept
                                new MetakompasTaskData.Suggestion(List.of("handling", "handler om"), "krimi"))));

        assertThat(saved.getEntries().size(), is(1));
        assertThat(saved.getSuggestions().stream().map(MetakompasTaskData.Suggestion::title).toList(), contains("Krimi", "krimi"));
        assertThat(saved.getSuggestions().get(1).path(), contains("handling", "handler om"));
    }

    @Test
    public void resolvesTaskOfTheExpectedType() throws ServiceErrorException {
        PromatTask task = new PromatTask().withId(123).withTaskFieldType(TaskFieldType.READING_EXPERIENCE_ADULT);

        assertThat(taskSelectionsWithTask(task).resolveTaskForSelection(123, TaskFieldType.READING_EXPERIENCE_ADULT), is(task));
    }

    @Test
    public void rejectsSelectionOnTaskRegisteredInMetakompasset() {
        MetakompasAndBuggiTaskSelections taskSelections = taskSelectionsWithTask(
                new PromatTask().withId(123).withTaskFieldType(TaskFieldType.METAKOMPAS));

        ServiceErrorException exception = assertThrows(ServiceErrorException.class, () ->
                taskSelections.resolveTaskForSelection(123, TaskFieldType.READING_EXPERIENCE_ADULT));

        assertThat(exception.getHttpStatus(), is(400));
        assertThat(exception.getServiceErrorDto().getCause(), is("Wrong task type"));
    }

    @Test
    public void directDataWriteIsOnlyRejectedForTasksRegisteredInPromat() {
        MetakompasAndBuggiTaskSelections taskSelections = new MetakompasAndBuggiTaskSelections();

        assertThat(taskSelections.isDirectDataWriteAllowed(TaskFieldType.READING_EXPERIENCE_ADULT, "{}"), is(false));
        assertThat(taskSelections.isDirectDataWriteAllowed(TaskFieldType.READING_EXPERIENCE_CHILD, "[]"), is(false));
        assertThat(taskSelections.isDirectDataWriteAllowed(TaskFieldType.READING_EXPERIENCE_ADULT, ""), is(true));
        assertThat(taskSelections.isDirectDataWriteAllowed(TaskFieldType.METAKOMPAS, "true"), is(true));
        assertThat(taskSelections.isDirectDataWriteAllowed(TaskFieldType.BRIEF, "text"), is(true));
    }

    private MetakompasAndBuggiTaskSelections taskSelectionsWithTask(PromatTask task) {
        EntityManager entityManager = mock(EntityManager.class);
        when(entityManager.find(PromatTask.class, task.getId())).thenReturn(task);

        MetakompasAndBuggiTaskSelections taskSelections = new MetakompasAndBuggiTaskSelections();
        taskSelections.entityManager = entityManager;
        return taskSelections;
    }

    private MetakompasAndBuggiTaskSelections taskSelectionsWithTaxonomy() {
        Taxonomy taxonomy = new Taxonomy();
        taxonomy.put(new Subject()
                .withId(42)
                .withTitle("krimi")
                .withPath(List.of("ramme", "genre"))
                .withNote("En bemærkning om krimigenren")
                .withOftenUsed(true)
                .withRef("665-g-042"), "ramme", "genre");

        TaxonomyCache taxonomyCache = new TaxonomyCache();
        taxonomyCache.set(taxonomy);

        MetakompasAndBuggiTaskSelections taskSelections = new MetakompasAndBuggiTaskSelections();
        taskSelections.taxonomyCache = taxonomyCache;
        return taskSelections;
    }
}
