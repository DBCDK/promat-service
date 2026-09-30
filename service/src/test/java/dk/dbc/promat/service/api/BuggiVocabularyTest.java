package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.BuggiOption;
import dk.dbc.promat.service.dto.BuggiOptionGroup;
import dk.dbc.promat.service.dto.BuggiSelectionRequest;
import dk.dbc.promat.service.taskdata.BuggiSelectionEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BuggiVocabularyTest {

    @Test
    void groupsHaveTheValueRangesUsedInMetakompasset() {
        Map<String, BuggiOptionGroup> groups = BuggiVocabulary.groups().stream()
                .collect(Collectors.toMap(BuggiOptionGroup::getName, group -> group));

        assertThat(groups.keySet().size(), is(4));
        assertRange(groups.get("Læsbarhed"), 1, 5);
        assertRange(groups.get("Fantasi/virkelighed"), 1, 5);
        assertRange(groups.get("Stemning"), 0, 5);
        assertRange(groups.get("Tema"), 0, 5);
    }

    @Test
    void optionIdsAreUnchanged() {
        // The ids are part of the API contract used by PUT /tasks/{taskId}/buggi
        List<Integer> ids = BuggiVocabulary.groups().stream()
                .flatMap(group -> group.getOptions().stream())
                .map(BuggiOption::getId)
                .toList();

        assertThat(ids, contains(IntStream.rangeClosed(1, 23).boxed().toArray(Integer[]::new)));
    }

    @Test
    void scaleValueMustBeSet() {
        // let/svær
        assertThrows(ServiceErrorException.class, () -> BuggiVocabulary.resolve(new BuggiSelectionRequest(1, 0)));
    }

    @Test
    void scaleValueIsResolvedAndAlwaysRegistered() throws ServiceErrorException {
        BuggiSelectionEntry entry = BuggiVocabulary.resolve(new BuggiSelectionRequest(1, 1));

        assertThat(entry.name(), is("let/svær"));
        assertThat(entry.marcSubfieldCode(), is("s"));
        assertThat(entry.requiresNonzeroValue(), is(false));
        assertThat(entry.value(), is(1));
    }

    @Test
    void moodMayBeNotChosen() throws ServiceErrorException {
        // spændende, value 0 = not chosen, so not registered
        BuggiSelectionEntry entry = BuggiVocabulary.resolve(new BuggiSelectionRequest(8, 0));

        assertThat(entry.marcSubfieldCode(), is("n"));
        assertThat(entry.requiresNonzeroValue(), is(true));
        assertThat(entry.value(), is(0));
    }

    @Test
    void valueAboveRangeIsRejected() {
        assertThrows(ServiceErrorException.class, () -> BuggiVocabulary.resolve(new BuggiSelectionRequest(8, 6)));
    }

    @Test
    void unknownOptionIsRejected() {
        assertThrows(ServiceErrorException.class, () -> BuggiVocabulary.resolve(new BuggiSelectionRequest(99, 1)));
    }

    private static void assertRange(BuggiOptionGroup group, int minValue, int maxValue) {
        assertThat(group.getName() + " minValue", group.getMinValue(), is(minValue));
        assertThat(group.getName() + " maxValue", group.getMaxValue(), is(maxValue));
    }
}
