package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.MetakompasSelectionRequest;
import dk.dbc.promat.service.dto.MetakompasSelectionResult;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.taxonomy.TaxonomyCache;
import dk.dbc.promat.service.taxonomy.dto.Subject;
import dk.dbc.promat.service.taxonomy.dto.Taxonomy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TaskSelectionsTest {

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

        MetakompasSelectionResult result = taskSelections.writeMetakompasSelection(new PromatTask(), List.of(
                new MetakompasSelectionRequest()
                        .withPath(List.of("stemning", "dramatisk"))
                        .withIds(List.of(42))));

        assertThat(result.getEntries().getFirst().getPath(), contains("ramme", "genre"));
    }

    @Test
    public void acceptsSuggestionsWhenPathExists() throws ServiceErrorException {
        TaskSelections taskSelections = taskSelectionsWithTaxonomy();

        MetakompasSelectionResult result = taskSelections.writeMetakompasSelection(new PromatTask(), List.of(
                new MetakompasSelectionRequest()
                        .withPath(List.of("handling", "handler om"))
                        .withSuggestions(List.of("new word", "another word"))));

        assertThat(result.getSuggestions().getFirst().getPath(), contains("handling", "handler om"));
        assertThat(result.getSuggestions().getFirst().getText(), is("new word"));
        assertThat(result.getSuggestions().get(1).getPath(), contains("handling", "handler om"));
        assertThat(result.getSuggestions().get(1).getText(), is("another word"));
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
}
