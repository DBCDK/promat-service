/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ReviewersTest {

    @Test
    void workloadDateIntervals() {
        final LocalDate date = LocalDate.of(2020, 12, 1);
        final Reviewers.WorkloadDateIntervals workloadDateIntervals = Reviewers.WorkloadDateIntervals.from(date);
        assertThat("week begin", workloadDateIntervals.getWeekBegin(),
                is(LocalDate.of(2020, 11, 30)));
        assertThat("week end", workloadDateIntervals.getWeekEnd(),
                is(LocalDate.of(2020, 12, 6)));
        assertThat("week before begin", workloadDateIntervals.getWeekBeforeBegin(),
                is(LocalDate.of(2020, 11, 23)));
        assertThat("week before end", workloadDateIntervals.getWeekBeforeEnd(),
                is(LocalDate.of(2020, 11, 29)));
        assertThat("week after begin", workloadDateIntervals.getWeekAfterBegin(),
                is(LocalDate.of(2020, 12, 7)));
        assertThat("week after end", workloadDateIntervals.getWeekAfterEnd(),
                is(LocalDate.of(2020, 12, 13)));
    }
}