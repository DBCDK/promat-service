/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.connector;

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
        expected.put("format", PromatServiceConnector.ListCasesParams.Format.EXPORT);
        expected.put("from", 2);
        expected.put("limit", 3);
        expected.put("reviewer", 4);
        expected.put("status", "APPROVED,PENDING_EXPORT");
        expected.put("title", "myTitle");

        final PromatServiceConnector.ListCasesParams listCasesParams = new PromatServiceConnector.ListCasesParams()
                .withEditor(1)
                .withFaust("myFaust")
                .withFormat(PromatServiceConnector.ListCasesParams.Format.EXPORT)
                .withFrom(2)
                .withLimit(3)
                .withReviewer(4)
                .withStatus(CaseStatus.APPROVED)
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withTitle("myTitle");

        assertThat(listCasesParams, is(expected));
    }
}