package dk.dbc.promat.service.taxonomy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dk.dbc.marc.binding.MarcBinding;
import dk.dbc.promat.service.TestUtils;
import dk.dbc.promat.service.taxonomy.dto.PathSubject;
import dk.dbc.promat.service.taxonomy.dto.Taxonomy;
import dk.dbc.promat.service.taxonomy.dto.MarcUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import dk.dbc.promat.service.taxonomy.dto.SubjectBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class BuildTest extends TestUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    static  {
        MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        MAPPER.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
    }

    @Test
    public void testThatTreeIsBuiltCorrect() throws IOException {

        // Records as they are returned from recordservice dump:
        // One a line.
        FromFileBuilder builder = new FromFileBuilder(getPath("/taxonomy/records/agency/dump/190004.txt"));
        Taxonomy taxonomy = new Taxonomy();
        builder.buildTaxonomy(taxonomy);
        String actual = MAPPER.writeValueAsString(taxonomy.getRoot());
        String expected = getResource("/taxonomy/expectedTree.json");
        assertThat("Taxonomy is correct", actual, is(expected));
    }

    @Test
    public void testRegExp() {
        assertThat("Valid numeric interval", SubjectBuilder.INTERVAL_PATTERN.matcher("j 0 -    100").find(), is(true));
        assertThat("Not an numeric interval", SubjectBuilder.INTERVAL_PATTERN.matcher("p - 0").find(), is(false));
        assertThat("Not an numeric interval", SubjectBuilder.INTERVAL_PATTERN.matcher("p 100 - hej").find(), is(false));
        assertThat("Valid numeric interval", SubjectBuilder.INTERVAL_PATTERN.matcher("100000-100").find(), is(true));
        assertThat("Valid numeric interval", SubjectBuilder.INTERVAL_PATTERN.matcher("hej 99 -100 hej").find(), is(true));
        assertThat("Valid numeric interval", SubjectBuilder.INTERVAL_PATTERN.matcher("seksdageskrigen").find(), is(false));
    }

    @Test
    public void testVariousCornerCases() throws IOException, TaxonomyException {

        // Two taxonomy itemms
        MarcBinding rec19504336 = MarcUtils.toMarcBinding(getResource("/taxonomy/records/19504336.json"));
        List<PathSubject> subjectList19504336 = new SubjectBuilder().build(rec19504336);
        assertThat("Not empty", subjectList19504336.size(), is(2));
        PathSubject subject5024 =  subjectList19504336.stream()
                .filter(pathSubject -> pathSubject.getId() == 5024).findFirst().orElseThrow();
        assertThat("Path",  subject5024.getPath(), is(List.of("ramme", "handlingens tid udtrykt i ord")));

        PathSubject subject6873 =  subjectList19504336.stream()
                .filter(pathSubject -> pathSubject.getId() == 6873).findFirst().orElseThrow();
        assertThat("Path",  subject6873.getPath(), is(List.of("handling", "handler om")));


        // Another two taxonomy items
        MarcBinding rec19570452 = MarcUtils.toMarcBinding(getResource("/taxonomy/records/19570452.json"));
        List<PathSubject> subjectList19570452 = new SubjectBuilder().build(rec19570452);
        assertThat("Not empty", subjectList19570452.size(), is(2));

        // Test reference
        MarcBinding rec19486397 = MarcUtils.toMarcBinding(getResource("/taxonomy/records/19486397.json"));
        List<PathSubject> subjectList19486397 = new SubjectBuilder().build(rec19486397);
        PathSubject subject6192 =  subjectList19486397.stream()
                .filter(pathSubject -> pathSubject.getId() == 6192).findFirst().orElseThrow();
        assertThat("Path",  subject6192.getPath(), is(List.of("handling", "hovedperson(er) - beskrivelse", "om hovedpersonen")));


    }


}
