package dk.dbc.promat.service.taxonomy;

import com.fasterxml.jackson.core.type.TypeReference;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.taxonomy.dto.PathTranslator;
import dk.dbc.promat.service.taxonomy.dto.Subject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static dk.dbc.promat.service.TestUtils.getResource;
import static dk.dbc.promat.service.taxonomy.RecordServiceMocks.addToAgencyDump;
import static dk.dbc.promat.service.taxonomy.RecordServiceMocks.resetAgencyDump;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TaxonomyTreeIT extends ContainerTest {

    @Test
    public void testTaxonomyChange() throws IOException {
        List<String> somePath = List.of("handling", "hovedperson(er) - beskrivelse", "hovedpersonens karaktertræk");

        // check that 'observerende' is not in the taxonomy'
        List<Subject> subjects = getList(somePath);
        assertThat("No subject 'observerende'", subjects.stream().noneMatch(subject -> subject.getTitle().equals("observerende")));

        // Now add a new record to the dump
        addToAgencyDump(getResource("/taxonomy/records/19487032.json").replace('\n', ' '));

        // Reload cache
        deleteResponse("v1/api/taxonomy/cache");

        // 'observerende' is now in the taxonomy''
        subjects = getList(somePath);
        assertThat("Subject 'observerende' now present", subjects.stream().anyMatch(subject -> subject.getTitle().equals("observerende")));

        // Reset as not to interfere with other tests.
        resetAgencyDump();
        deleteResponse("v1/api/taxonomy/cache");
    }

    @Test
    public void testAlias() {
        List<String> expectedTitles = List.of("boerkrigen 1899-1902", "1-99", "1000-1009");
        List<String> actualTitles = getListByAlias(PathTranslator.I1).stream().map(Subject::getTitle).toList();
        assertThat(actualTitles, is(expectedTitles));
    }

    private List<Subject> getList(List<String> path) {
        return postAndAssert(
                "v1/api/taxonomy/subtree",
                path,
                new TypeReference<List<Subject>>() {},
                Response.Status.OK);
    }

    private List<Subject> getListByAlias(PathTranslator path) {
        return getResponse(
                "v1/api/taxonomy/subtree/" + path,
                new TypeReference<List<Subject>>() {},
                Response.Status.OK);
    }


}
