package dk.dbc.promat.service.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Guards against manifestationByPid.graphql and FbiApiHandler.Manifestation silently drifting
 * apart in one specific direction: a record component with no matching query field just
 * deserializes to null, with no exception - the reverse (a query field with no matching record
 * component) already fails loudly since Jackson rejects unrecognized properties by default.
 */
class FbiApiHandlerQueryFieldsTest {

    @Test
    void manifestationQueryFields_matchManifestationRecordFields() throws Exception {
        final String query = loadResource("/graphql/manifestationByPid.graphql");
        final Set<String> queryFields = topLevelFields(query, "manifestation");

        final Class<?> manifestationClass = Class.forName("dk.dbc.promat.service.api.FbiApiHandler$Manifestation");
        final Set<String> recordFields = new LinkedHashSet<>();
        for (RecordComponent component : manifestationClass.getRecordComponents()) {
            recordFields.add(component.getName());
        }

        assertThat("manifestationByPid.graphql's top-level fields must match FbiApiHandler.Manifestation's " +
                "record components, or fbi-api data silently stops being mapped", queryFields, is(recordFields));
    }

    // Extracts the direct child field names of "<rootField>(...) { ... }" from a GraphQL query,
    // ignoring nested subselections - good enough for this query's flat, argument-free field list.
    private static Set<String> topLevelFields(String query, String rootField) {
        final int rootStart = query.indexOf(rootField + "(");
        final int braceStart = query.indexOf('{', rootStart);

        int depth = 0;
        int bodyStart = -1;
        int bodyEnd = -1;
        for (int i = braceStart; i < query.length(); i++) {
            final char c = query.charAt(i);
            if (c == '{') {
                depth++;
                if (depth == 1) {
                    bodyStart = i + 1;
                }
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    bodyEnd = i;
                    break;
                }
            }
        }
        final String body = query.substring(bodyStart, bodyEnd);

        final Set<String> fields = new LinkedHashSet<>();
        int localDepth = 0;
        int i = 0;
        while (i < body.length()) {
            final char c = body.charAt(i);
            if (c == '{') {
                localDepth++;
                i++;
            } else if (c == '}') {
                localDepth--;
                i++;
            } else if (localDepth == 0 && Character.isJavaIdentifierStart(c)) {
                int j = i + 1;
                while (j < body.length() && Character.isJavaIdentifierPart(body.charAt(j))) {
                    j++;
                }
                fields.add(body.substring(i, j));
                i = j;
            } else {
                i++;
            }
        }
        return fields;
    }

    private static String loadResource(String path) throws IOException {
        try (InputStream is = FbiApiHandlerQueryFieldsTest.class.getResourceAsStream(path)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
