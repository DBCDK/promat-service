/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class CaseRequestDtoTest {
    @Test
    void equals() {
        final CaseRequestDto caseRequestDto1 = new CaseRequestDto().withRecordId("42424242");
        final CaseRequestDto caseRequestDto2 = new CaseRequestDto().withRecordId("42424242");
        assertThat(caseRequestDto1, is(caseRequestDto2));
    }
}