/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.persistence;

public enum CaseStatus {
    CREATED,
    REJECTED,
    ASSIGNED,
    PENDING_APPROVAL,
    PENDING_ISSUES,
    APPROVED,
    DONE,
    CLOSED
}
