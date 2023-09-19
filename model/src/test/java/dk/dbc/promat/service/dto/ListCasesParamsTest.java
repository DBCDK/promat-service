package dk.dbc.promat.service.dto;

import dk.dbc.promat.service.persistence.CaseStatus;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ListCasesParamsTest {
    @Test
    void parameters() {
        final HashMap<String, Object> expected = new HashMap<>();
        expected.put("editor", 1);
        expected.put("faust", "myFaust");
        expected.put("format", ListCasesParams.Format.EXPORT);
        expected.put("from", 2);
        expected.put("limit", 3);
        expected.put("reviewer", 4);
        expected.put("status", "APPROVED,PENDING_EXPORT");
        expected.put("title", "myTitle");

        final ListCasesParams listCasesParams = new ListCasesParams()
                .withEditor(1)
                .withFaust("myFaust")
                .withFormat(ListCasesParams.Format.EXPORT)
                .withFrom(2)
                .withLimit(3)
                .withReviewer(4)
                .withStatus(CaseStatus.APPROVED)
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withTitle("myTitle");

        assertThat(listCasesParams, is(expected));
    }
}
