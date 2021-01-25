/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class CaseRequestTest {
    @Test
    void equals() {
        final CaseRequest caseRequestDto1 = new CaseRequest().withRecordId("42424242");
        final CaseRequest caseRequestDto2 = new CaseRequest().withRecordId("42424242");
        assertThat(caseRequestDto1, is(caseRequestDto2));
    }
}