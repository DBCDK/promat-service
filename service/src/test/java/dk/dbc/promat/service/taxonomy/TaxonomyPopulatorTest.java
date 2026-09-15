package dk.dbc.promat.service.taxonomy;

import dk.dbc.promat.service.taxonomy.dto.Subject;
import dk.dbc.promat.service.taxonomy.dto.Taxonomy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public class TaxonomyPopulatorTest {

    @Test
    public void placesASubjectWithAResolvablePath() {
        Taxonomy taxonomy = new Taxonomy();
        Subject item = new Subject().withId(456).withTitle("krimi").withPath(List.of("ramme", "genre"));

        TaxonomyPopulator.populate(taxonomy, List.of(item));

        List<Subject> placed = taxonomy.getList("ramme", "genre");
        assertThat(placed, contains(new Subject().withId(456).withTitle("krimi")));
    }

    @Test
    public void skipsASubjectWithAMissingPathWithoutThrowing() {
        Taxonomy taxonomy = new Taxonomy();
        Subject missingPath = new Subject().withId(1).withTitle("no path").withPath(null);
        Subject emptyPath = new Subject().withId(2).withTitle("empty path").withPath(List.of());

        TaxonomyPopulator.populate(taxonomy, List.of(missingPath, emptyPath));

        assertThat(taxonomy.getList("ramme", "genre"), empty());
    }

    @Test
    public void skipsASubjectWhosePathDoesNotResolveButStillPlacesTheRest() {
        Taxonomy taxonomy = new Taxonomy();
        Subject unresolvable = new Subject().withId(1).withTitle("unimplemented category")
                .withPath(List.of("not", "a", "real", "path"));
        Subject resolvable = new Subject().withId(2).withTitle("dramatisk")
                .withPath(List.of("stemning", "dramatisk"));

        TaxonomyPopulator.populate(taxonomy, List.of(unresolvable, resolvable));

        assertThat(taxonomy.getList("stemning", "dramatisk"),
                contains(new Subject().withId(2).withTitle("dramatisk")));
    }

    @Test
    public void emptyInputProducesAnEmptyTaxonomyWithoutThrowing() {
        Taxonomy taxonomy = new Taxonomy();

        TaxonomyPopulator.populate(taxonomy, List.of());

        assertThat(taxonomy.getList("ramme", "genre"), is(empty()));
    }
}
