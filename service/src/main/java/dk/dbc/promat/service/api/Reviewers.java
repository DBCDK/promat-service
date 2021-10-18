/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.connector.culr.CulrConnectorException;
import dk.dbc.promat.service.Repository;
import dk.dbc.promat.service.dto.ReviewerList;
import dk.dbc.promat.service.dto.ReviewerRequest;
import dk.dbc.promat.service.dto.ReviewerWithWorkloads;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.ReviewerView;
import dk.dbc.promat.service.persistence.Subject;
import dk.dbc.promat.service.templating.NotificationFactory;
import dk.dbc.promat.service.templating.model.ReviewerDataChanged;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DefaultValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Stateless
@Path("")
public class Reviewers {
    private static final Logger LOGGER = LoggerFactory.getLogger(Reviewers.class);

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @Inject
    NotificationFactory notificationFactory;

    @EJB
    Repository repository;

    @Inject
    CulrHandler culrHandler;

    @Inject
    AuditLogHandler auditLogHandler;

    @GET
    @Path("reviewers/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    @RolesAllowed({"authenticated-user"})
    public Response getReviewer(@PathParam("id") Integer id, @Context UriInfo uriInfo) {
        LOGGER.info("reviewers/{} (GET)", id);

        try {
            final Reviewer reviewer = entityManager.find(Reviewer.class, id);
            if (reviewer == null) {
                auditLogHandler.logTraceReadForToken("Request for full profile", uriInfo, 0, 404);
                return Response.status(404).build();
            }

            auditLogHandler.logTraceReadForToken("View full profile", uriInfo, reviewer.getPaycode(), 200);
            return Response.ok(reviewer).build();
        }
        catch (Exception e) {
            LOGGER.error("Exception in /reviewers when requesting id {}", id);
            LOGGER.error("Exception was: {}\n{}", e.getMessage(), e.getStackTrace());
            return ServiceErrorDto.Failed("Unexpected exception when trying to find reviewer");
        }
    }

    @POST
    @Path("reviewers")
    @Produces({MediaType.APPLICATION_JSON})
    @RolesAllowed({"authenticated-user"})
    public Response createReviewer(ReviewerRequest reviewerRequest, @Context UriInfo uriInfo) throws CulrConnectorException {
        LOGGER.info("reviewers (POST)");

        final String cprNumber = reviewerRequest.getCprNumber();
        if (cprNumber == null || cprNumber.isBlank()) {
            return ServiceErrorDto.InvalidRequest("Missing required field in the request data",
                    "Field 'cprNumber' must be supplied when creating a new reviewer");
        }

        final Integer paycode = reviewerRequest.getPaycode();
        if (paycode == null) {
            return ServiceErrorDto.InvalidRequest("Missing required field in the request data",
                    "Field 'paycode' must be supplied when creating a new reviewer");
        }

        final String culrId;
        try {
            culrId = culrHandler.createCulrAccount(cprNumber, String.valueOf(paycode));
        } catch (ServiceErrorException e) {
            return Response.status(500).entity(e.getServiceErrorDto()).build();
        }

        try {
            final Reviewer entity = new Reviewer()
                    .withActive(reviewerRequest.isActive() != null ? reviewerRequest.isActive() : true)  // New users defaults to active
                    .withFirstName(reviewerRequest.getFirstName())
                    .withLastName(reviewerRequest.getLastName())
                    .withEmail(reviewerRequest.getEmail())
                    .withPhone(reviewerRequest.getPhone())
                    .withPrivateEmail(reviewerRequest.getPrivateEmail())
                    .withPrivatePhone(reviewerRequest.getPrivatePhone())
                    .withInstitution(reviewerRequest.getInstitution())
                    .withPaycode(reviewerRequest.getPaycode())
                    .withAddress(reviewerRequest.getAddress())
                    .withPrivateAddress(reviewerRequest.getPrivateAddress())
                    .withHiatus_begin(reviewerRequest.getHiatusBegin())
                    .withHiatus_end(reviewerRequest.getHiatusEnd())
                    .withSubjects(repository.resolveSubjects(reviewerRequest.getSubjects()))
                    .withAccepts(reviewerRequest.getAccepts())
                    .withCapacity(reviewerRequest.getCapacity())
                    .withSubjectNotes(repository.checkSubjectNotes(reviewerRequest.getSubjectNotes(), reviewerRequest.getSubjects()));


            entity.setCulrId(culrId);

            entityManager.persist(entity);
            entityManager.flush();

            auditLogHandler.logTraceCreateForToken("Created new user", uriInfo, entity.getPaycode(), 201);
            LOGGER.info("Created new reviewer with ID {}", entity.getId());
            return Response.status(201)
                    .entity(entity)
                    .build();
        } catch (ServiceErrorException e) {
            return Response.status(400).entity(e.getServiceErrorDto()).build();
        }
    }

    @GET
    @Path("reviewers")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAllReviewers(@QueryParam("deadline") LocalDate deadline) throws JsonProcessingException {
        LOGGER.info("reviewers (GET)");
        final ObjectMapper objectMapper = new JsonMapperProvider().getObjectMapper();

        if (deadline == null) {
            final TypedQuery<Reviewer> query = entityManager.createNamedQuery(
                    Reviewer.GET_ALL_REVIEWERS_NAME, Reviewer.class);
            ReviewerList listOfReviewers = new ReviewerList<>()
                    .withReviewers(query.getResultList());
            return Response.ok(objectMapper.writerWithView(ReviewerView.Summary.class).writeValueAsString(listOfReviewers))
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
        ReviewerList<ReviewerWithWorkloads> listOfReviewers = new ReviewerList<ReviewerWithWorkloads>().withReviewers(reviewers);
        return Response.ok(objectMapper.writerWithView(ReviewerView.Summary.class).writeValueAsString(listOfReviewers))
                .build();
    }

    @PUT
    @Path("reviewers/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    @RolesAllowed({"authenticated-user"})
    public Response updateReviewer(@PathParam("id") final Integer id, ReviewerRequest reviewerRequest,
                                   @QueryParam("notify") @DefaultValue("false") final Boolean notify,
                                   @Context UriInfo uriInfo) {
        Notification notification = null;

        LOGGER.info("reviewers/{} (PUT), notify:{}", id, notify);

        try {

            // Find the existing user
            final Reviewer reviewer = entityManager.find(Reviewer.class, id);
            if (reviewer == null) {
                LOGGER.info("Reviewer with id {} does not exists", id);
                auditLogHandler.logTraceUpdateForToken("Request for update of profile", uriInfo, 0, 404);
                return Response.status(404).build();
            }

            if(notify) {
                // Create the notification now, before we fill in the changed fields in reviewer.
                 notification = notificationFactory.notificationOf(new ReviewerDataChanged()
                        .withReviewerRequest(reviewerRequest)
                        .withReviewer(reviewer)
                );
            }

            // Update by patching
            if(reviewerRequest.isActive() != null) {
                reviewer.setActive(reviewerRequest.isActive());
                reviewer.setActiveChanged(Date.from(Instant.now()));
                if( reviewerRequest.isActive() ) {
                    reviewer.setDeactivated(null);
                }
            }
            if(reviewerRequest.getAccepts() != null) {
                reviewer.setAccepts(reviewerRequest.getAccepts());
            }
            if(reviewerRequest.getAddress() != null) {
                reviewer.setAddress(reviewerRequest.getAddress());
            }
            if(reviewerRequest.getPrivateAddress() != null) {
                reviewer.setPrivateAddress(reviewerRequest.getPrivateAddress());
            }
            if(reviewerRequest.getEmail() != null) {
                reviewer.setEmail(reviewerRequest.getEmail());
            }
            if(reviewerRequest.getPrivateEmail() != null) {
                reviewer.setPrivateEmail(reviewerRequest.getPrivateEmail());
            }
            if(reviewerRequest.getFirstName() != null) {
                reviewer.setFirstName(reviewerRequest.getFirstName());
            }
            if(reviewerRequest.getLastName() != null) {
                reviewer.setLastName(reviewerRequest.getLastName());
            }
            if(reviewerRequest.getHiatusBegin() != null) {
                reviewer.setHiatusBegin(reviewerRequest.getHiatusBegin());
            }
            if(reviewerRequest.getHiatusEnd() != null) {
                reviewer.setHiatusEnd(reviewerRequest.getHiatusEnd());
            }
            if(reviewerRequest.getInstitution() != null) {
                reviewer.setInstitution(reviewerRequest.getInstitution());
            }
            if(reviewerRequest.getPaycode() != null) {
                if( !reviewerRequest.getPaycode().equals(reviewer.getPaycode()) ) {
                    Map<String, String> paycodeChanges = new HashMap<>();
                    paycodeChanges.put("Current value", reviewer.getPaycode().toString());
                    paycodeChanges.put("New value", reviewerRequest.getPaycode().toString());
                    auditLogHandler.logTraceUpdateForToken("Change of paycode (owning id)", uriInfo, reviewer.getPaycode(), paycodeChanges);
                }
                reviewer.setPaycode(reviewerRequest.getPaycode());
            }
            if(reviewerRequest.getPhone() != null) {
                reviewer.setPhone(reviewerRequest.getPhone());
            }
            if(reviewerRequest.getPrivatePhone() != null) {
                reviewer.setPrivatePhone(reviewerRequest.getPrivatePhone());
            }
            if(reviewerRequest.getSubjects() != null) {

                // If subjects were removed, make sure that associated notes are also removed.
                reviewer.setSubjectNotes(repository.resolveSubjectNotes(reviewerRequest.getSubjects(), reviewer.getSubjectNotes()));
                reviewer.setSubjects(repository.resolveSubjects(reviewerRequest.getSubjects()));
            }
            if(reviewerRequest.getSubjectNotes() != null) {

                // If both subjects and subjectnotes were changed, make sure that the supplied subjectnotes refer to
                // subjects present in the new subjectlist.
                if(reviewerRequest.getSubjects() != null) {
                    reviewer.setSubjectNotes(repository.checkSubjectNotes(reviewerRequest.getSubjectNotes(), reviewerRequest.getSubjects()));
                } else {

                    // Validate against existing subjects instead.
                    reviewer.setSubjectNotes(
                            repository.resolveSubjectNotes(
                                    reviewer.getSubjects().stream().map(Subject::getId).collect(Collectors.toList()),
                                    reviewerRequest.getSubjectNotes()));
                }
            }
            if(reviewerRequest.getCapacity() != null) {
                reviewer.setCapacity(reviewerRequest.getCapacity());
            }
            if(reviewerRequest.getNote() != null) {
                reviewer.setNote(reviewerRequest.getNote());
            }

            if (notify && notification != null) {
                entityManager.persist(notification);
            }

            auditLogHandler.logTraceUpdateForToken("Update and view full profile", uriInfo, reviewer.getPaycode(), 200);
            return Response.status(200)
                    .entity(reviewer)
                    .build();
        } catch (ServiceErrorException e) {
            return Response.status(e.getHttpStatus()).entity(e.getServiceErrorDto()).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
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
