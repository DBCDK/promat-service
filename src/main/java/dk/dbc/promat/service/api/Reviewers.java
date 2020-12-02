/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.ReviewerList;
import dk.dbc.promat.service.dto.ReviewerWithWorkloads;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.Reviewer;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@Path("reviewers")
public class Reviewers {
    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAllReviewers(@QueryParam("deadline") LocalDate deadline) {

        if (deadline == null) {
            final TypedQuery<Reviewer> query = entityManager.createNamedQuery(
                    Reviewer.GET_ALL_REVIEWERS_NAME, Reviewer.class);
            return Response.ok(new ReviewerList<>()
                    .withReviewers(query.getResultList()))
                    .build();
        }

        final WorkloadDateIntervals workloadDateIntervals = WorkloadDateIntervals.from(deadline);

        final TypedQuery<Object[]> query = entityManager.createNamedQuery(
                Reviewer.GET_ALL_REVIEWERS_WITH_WORKLOADS, Object[].class);
        query.setParameter(1, workloadDateIntervals.weekBegin);
        query.setParameter(2, workloadDateIntervals.weekEnd);
        query.setParameter(3, workloadDateIntervals.weekBeforeBegin);
        query.setParameter(4, workloadDateIntervals.weekBeforeEnd);
        query.setParameter(5, workloadDateIntervals.weekAfterBegin);
        query.setParameter(6, workloadDateIntervals.weekAfterEnd);
        query.setParameter(7, workloadDateIntervals.weekBeforeBegin);
        query.setParameter(8, workloadDateIntervals.weekAfterEnd);

        final List<ReviewerWithWorkloads> reviewers = query.getResultStream()
                .map(objects -> ((Reviewer) objects[0]).toReviewerWithWorkloads()
                        .withWeekWorkload((long) objects[1])
                        .withWeekBeforeWorkload((long) objects[2])
                        .withWeekAfterWorkload((long) objects[3]))
                .collect(Collectors.toList());
        return Response.ok(new ReviewerList<ReviewerWithWorkloads>().withReviewers(reviewers)).build();
    }

    /**
     * Determines begin and end dates for the week to which a given date belongs,
     * as well as begin and end dates for the weeks before and after the given date.
     */
    static class WorkloadDateIntervals {
        final LocalDate weekBegin;
        final LocalDate weekEnd;
        final LocalDate weekBeforeBegin;
        final LocalDate weekBeforeEnd;
        final LocalDate weekAfterBegin;
        final LocalDate weekAfterEnd;

        static WorkloadDateIntervals from(LocalDate date) {
            final DayOfWeek dayOfWeek = date.getDayOfWeek();
            // Step back start of the week before, ie. subtract dayofweek + 7 - 1
            final LocalDate weekBeforeBegin = date.minusDays(dayOfWeek.getValue() + 6);
            final LocalDate weekBeforeEnd = weekBeforeBegin.plusDays(6);
            final LocalDate weekBegin = weekBeforeEnd.plusDays(1);
            final LocalDate weekEnd = weekBegin.plusDays(6);
            final LocalDate weekAfterBegin = weekEnd.plusDays(1);
            final LocalDate weekAfterEnd = weekAfterBegin.plusDays(6);
            return new WorkloadDateIntervals(weekBegin, weekEnd,
                    weekBeforeBegin, weekBeforeEnd, weekAfterBegin, weekAfterEnd);
        }

        private WorkloadDateIntervals(LocalDate weekBegin, LocalDate weekEnd,
                                      LocalDate weekBeforeBegin, LocalDate weekBeforeEnd,
                                      LocalDate weekAfterBegin, LocalDate weekAfterEnd) {
            this.weekBegin = weekBegin;
            this.weekEnd = weekEnd;
            this.weekBeforeBegin = weekBeforeBegin;
            this.weekBeforeEnd = weekBeforeEnd;
            this.weekAfterBegin = weekAfterBegin;
            this.weekAfterEnd = weekAfterEnd;
        }

        public LocalDate getWeekBegin() {
            return weekBegin;
        }

        public LocalDate getWeekEnd() {
            return weekEnd;
        }

        public LocalDate getWeekBeforeBegin() {
            return weekBeforeBegin;
        }

        public LocalDate getWeekBeforeEnd() {
            return weekBeforeEnd;
        }

        public LocalDate getWeekAfterBegin() {
            return weekAfterBegin;
        }

        public LocalDate getWeekAfterEnd() {
            return weekAfterEnd;
        }

        @Override
        public String toString() {
            return "WorkloadDateIntervals{" +
                    "weekBegin=" + weekBegin +
                    ", weekEnd=" + weekEnd +
                    ", weekBeforeBegin=" + weekBeforeBegin +
                    ", weekBeforeEnd=" + weekBeforeEnd +
                    ", weekAfterBegin=" + weekAfterBegin +
                    ", weekAfterEnd=" + weekAfterEnd +
                    '}';
        }
    }
}
