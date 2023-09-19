package dk.dbc.promat.service.dto;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class CaseRequestTest {
    @Test
    void equals() {
        // Note: This test makes little sense right now, but properties may be added again
        // later on which needs testing, so we let the test stay as a placeholder
        final CaseRequest caseRequestDto1 = new CaseRequest();
        final CaseRequest caseRequestDto2 = new CaseRequest();
        assertThat(caseRequestDto1, is(caseRequestDto2));
    }
}
