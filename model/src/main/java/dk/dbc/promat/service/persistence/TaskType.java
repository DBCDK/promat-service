/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.persistence;

public enum TaskType {

    GROUP_1_LESS_THAN_100_PAGES,
    GROUP_2_100_UPTO_199_PAGES,
    GROUP_3_200_UPTO_499_PAGES,
    GROUP_4_500_OR_MORE_PAGES,

    MOVIES_GR_1,
    MOVIES_GR_2,
    MOVIES_GR_3,

    MULTIMEDIA_FEE,
    MULTIMEDIA_FEE_GR2,

    MOVIE_NON_FICTION_GR1,
    MOVIE_NON_FICTION_GR2,
    MOVIE_NON_FICTION_GR3,
}
