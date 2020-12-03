/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

public enum ServiceErrorCode {
    INVALID_REQUEST,
    CASE_EXISTS,
    INVALID_STATE,
    FAILED,
    NOT_FOUND,
    FORBIDDEN
}
