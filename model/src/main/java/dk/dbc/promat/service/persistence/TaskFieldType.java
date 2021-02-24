/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.persistence;

public enum TaskFieldType {
    BRIEF,
    DESCRIPTION,
    EVALUATION,
    COMPARISON,
    RECOMMENDATION,
    @Deprecated
    BIBLIOGRAPHIC, // Todo: Obsolete, remove when no tasks exists in the db with this taskfieldtype
    TOPICS,
    BKM,
    EXPRESS,
    METAKOMPAS,
    @Deprecated
    GENRE, // Todo: Obsolete, remove when no tasks exists in the db with this taskfieldtype
    AGE,
    MATLEVEL
}
