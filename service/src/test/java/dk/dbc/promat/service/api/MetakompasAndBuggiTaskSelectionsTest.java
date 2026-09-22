package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.promat.service.dto.BuggiSelectionRequest;
import dk.dbc.promat.service.dto.MetakompasSelectionRequest;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.taskdata.BuggiSelectionEntry;
import dk.dbc.promat.service.taskdata.MetakompasSelectionEntry;
import dk.dbc.promat.service.taskdata.MetakompasSuggestion;
import dk.dbc.promat.service.taskdata.MetakompasTaskData;
import dk.dbc.promat.service.taxonomy.TaxonomyCache;
import dk.dbc.promat.service.taxonomy.dto.Subject;
import dk.dbc.promat.service.taxonomy.dto.Taxonomy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MetakompasAndBuggiTaskSelectionsTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void rejectsMetakompasSelectionWithInvalidPath() {
        MetakompasAndBuggiTaskSelections taskSelections = taskSelectionsWithTaxonomy();

        ServiceErrorException exception = assertThrows(ServiceErrorException.class, () ->
                taskSelections.writeMetakompasSelection(new PromatTask(),
                        new MetakompasSelectionRequest()
                                .withSuggestions(List.of(new MetakompasSuggestion(
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
        MetakompasSelectionEntry entry = result.getEntries().getFirst();
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
                                new MetakompasSuggestion(List.of("handling", "handler om"), "new word"),
                                new MetakompasSuggestion(List.of("handling", "handler om"), "another word"))));

        assertThat(result.getSuggestions().getFirst().path(), contains("handling", "handler om"));
        assertThat(result.getSuggestions().getFirst().text(), is("new word"));
        assertThat(result.getSuggestions().get(1).path(), contains("handling", "handler om"));
        assertThat(result.getSuggestions().get(1).text(), is("another word"));
    }

    @Test
    public void storesMetakompasSelectionDataAsParseableJson() throws Exception {
        MetakompasAndBuggiTaskSelections taskSelections = taskSelectionsWithTaxonomy();
        PromatTask task = new PromatTask();

        taskSelections.writeMetakompasSelection(task,
                new MetakompasSelectionRequest()
                        .withIds(List.of(42))
                        .withSuggestions(List.of(new MetakompasSuggestion(List.of("handling", "handler om"), "new word"))));

        MetakompasTaskData data = OBJECT_MAPPER.readValue(task.getData(), MetakompasTaskData.class);

        assertThat(data.getEntries().getFirst().id(), is(42));
        assertThat(data.getEntries().getFirst().title(), is("krimi"));
        assertThat(data.getSuggestions().getFirst().path(), contains("handling", "handler om"));
        assertThat(data.getSuggestions().getFirst().text(), is("new word"));
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
        assertThat(data.getFirst().requiresNonzeroValue(), is(true));
        assertThat(data.getFirst().value(), is(2));
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
