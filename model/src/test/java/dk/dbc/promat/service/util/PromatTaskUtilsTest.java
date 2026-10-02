package dk.dbc.promat.service.util;

import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class PromatTaskUtilsTest {

    @Test
    void approvedCaseWithAllTasksApprovedIsApproved() {
        assertThat(PromatTaskUtils.approvedCaseStatus(caseWith(approved(TaskFieldType.BRIEF),
                approved(TaskFieldType.READING_EXPERIENCE_ADULT))), is(CaseStatus.APPROVED));
    }

    @Test
    void approvedCaseWaitsForReadingExperienceTasks() {
        assertThat(PromatTaskUtils.approvedCaseStatus(caseWith(approved(TaskFieldType.BRIEF),
                unapproved(TaskFieldType.READING_EXPERIENCE_CHILD))), is(CaseStatus.PENDING_READING_EXPERIENCE));
    }

    @Test
    void approvedCaseWaitsForMetakompassetFirst() {
        assertThat(PromatTaskUtils.approvedCaseStatus(caseWith(unapproved(TaskFieldType.METAKOMPAS),
                unapproved(TaskFieldType.READING_EXPERIENCE_ADULT))), is(CaseStatus.PENDING_EXTERNAL));
    }

    @Test
    void unapprovedInternalTasksDontMakeTheCaseWait() {
        // Internal tasks are approved together with the case
        assertThat(PromatTaskUtils.approvedCaseStatus(caseWith(unapproved(TaskFieldType.BRIEF))), is(CaseStatus.APPROVED));
    }

    private static PromatCase caseWith(PromatTask... tasks) {
        return new PromatCase().withTasks(List.of(tasks));
    }

    private static PromatTask approved(TaskFieldType taskFieldType) {
        return new PromatTask().withTaskFieldType(taskFieldType).withApproved(LocalDate.now());
    }

    private static PromatTask unapproved(TaskFieldType taskFieldType) {
        return new PromatTask().withTaskFieldType(taskFieldType);
    }
}
