package dk.dbc.promat.service.persistence;

import java.util.Arrays;

public enum CaseStatus {

    /*
        Initial state

        Transition from:    (none)
        Transition to:      ASSIGNED
        Payable:            No
        Visible in DBCKat   No
    */
    CREATED,

    /*
        Case has been assigned to a reviewer

        Transition from:    CREATED, REJECTED
        Transition to:      REJECTED, PENDING_APPROVAL
        Payable:            No
        Visible in DBCKat   No
    */
    ASSIGNED,

    /*
        Reviewer rejects the case.
        There are 2 reasons for rejecting a case:
            1) Not able/willing to write rewiew
            2) Case includes a BKM assessment, and the result was "not relevant for libraries"

        Transition from:    ASSIGNED
        Transition to:      ASSIGNED, PENDING_CLOSE
        Payable:            No
        Visible in DBCKat   No
    */
    REJECTED,

    /*
        Reviewer has completed the review and requests approval by an editor

        Transition from:    ASSIGNED
        Transition to:      PENDING_ISSUES, PENDING_EXTERNAL, APPROVED
        Payable:            No
        Visible in DBCKat   No
    */
    PENDING_APPROVAL,

    /*
        Editor has found issues with the review and requests changes to be made

        Transition from:    PENDING_APPROVAL
        Transition to:      PENDING_APPROVAL
        Payable:            No
        Visible in DBCKat   No
    */
    PENDING_ISSUES,

    /*
        Editor has approved the review, but there are still tasks that must be checked against
        an external system

        Transition from:    PENDING_APPROVAL
        Transition to:      APPROVED, PENDING_ISSUES
        Payable:            No
        Visible in DBCKat   No
    */
    PENDING_EXTERNAL,

    /*
        Editor has approved the review and/or all external checks has resolved into 'done'

        Transition from:    PENDING_APPROVAL, PENDING_EXTERNAL
        Transition to:      PENDING_MEETING, PENDING_ISSUES
        Payable:            Yes
        Visible in DBCKat   Yes
    */
    APPROVED,

    /*
        The record for the primary faustnumber has been completed, and the weekcode given herein
        matches the current (or earlier) week. Review is now a candidate for export

        Transition from:    APPROVED
        Transition to:      PENDING_EXPORT, PENDING_ISSUES
        Payable:            Yes
        Visible in DBCKat   Yes
    */
    PENDING_MEETING,

    /*
        Review has been selected for export by the editors

        Transition from:    PENDING_MEETING, EXPORTED
        Transition to:      EXPORTED, PENDING_ISSUES
        Payable:            Yes
        Visible in DBCKat   Yes
    */
    PENDING_EXPORT,

    /*
        Review is being processed by DataIO for export

        Transition from:    PENDING_EXPORT
        Transition to:      EXPORTED, PENDING_EXPORT
        Payable:            Yes
        Visible in DBCKat   Yes
    */
    PROCESSING,

    /*
        Review has been exported

        Transition from:    PENDING_EXPORT
        Transition to:      PENDING_REVERT
        Payable:            Yes
        Visible in DBCKat   Yes

        MOST REVIEWS WILL STAY IN THIS STATE
    */
    EXPORTED,

    /*
        Review has been exported but must be withdrawn and deleted in the datawell

        Transition from:    EXPORTED, PROCESSING
        Transition to:      REVERTED, PENDING_EXPORT
        Payable:            Yes
        Visible in DBCKat   Yes
    */
    PENDING_REVERT,

    /*
        Review has been deleted in the datawell

        Transition from:    PENDING_REVERT, PROCESSING
        Transition to:      PENDING_ISSUES, PENDING_EXPORT (if we allow this)
        Payable:            Yes
        Visible in DBCKat   Yes

        REVIEWS WILL STAY IN THIS STATE UNLESS LATER SELECTED FOR CHANGES OR EXPORT
    */
    REVERTED,

    /*
        BMK assessment resulted in the review not being library relevant. Case must be closed
        but only after the BMK assessment task has been payed

        Transition from:    PENDING_APPROVAL
        Transition to:      CLOSED
        Payable:            Yes (only BKM tasks)
        Visible in DBCKat   No
    */
    PENDING_CLOSE,

    /*
        Review has been closed

        Transition from:    (any)
        Transition to:      CREATED
        Payable:            No
        Visible in DBCKat   No

        REVIEWS WILL STAY IN THIS STATE UNLESS LATER CHANGED TO CREATED. AN ASSIGNED REVIEWER MUST BE REMOVED
    */
    CLOSED,

    /*
        Review has been deleted

        Transition from:    (any)
        Transition to:      CREATED
        Payable:            No
        Visible in DBCKat   No

        REVIEWS WILL STAY IN THIS STATE UNLESS LATER CHANGED TO CREATED. AN ASSIGNED REVIEWER MUST BE REMOVED
    */
    DELETED;

    public boolean in(CaseStatus... caseStatuses) {
        return Arrays.asList(caseStatuses).contains(this);
    }
}
