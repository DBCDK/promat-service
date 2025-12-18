package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.promat.service.Repository;
import dk.dbc.promat.service.dto.ReviewerList;
import dk.dbc.promat.service.dto.ReviewerRequest;
import dk.dbc.promat.service.dto.ReviewerWithWorkloads;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.JsonMapperProvider;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.ReviewerDataStash;
import dk.dbc.promat.service.persistence.ReviewerView;
import dk.dbc.promat.service.persistence.Subject;
import dk.dbc.promat.service.templating.NotificationFactory;
import dk.dbc.promat.service.templating.model.HiatusReset;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.DefaultValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Arrays;

import static dk.dbc.promat.service.api.Users.IDP_EDITOR_RIGHT_NAME;
import static dk.dbc.promat.service.api.Users.IDP_PRODUCT_NAME;
import static dk.dbc.promat.service.api.Users.IDP_REVIEWER_RIGHT_NAME;

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
    AuditLogHandler auditLogHandler;

    @GET
    @Path("reviewers/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    @RolesAllowed({"authenticated-user", IDP_PRODUCT_NAME + "-" + IDP_EDITOR_RIGHT_NAME, IDP_PRODUCT_NAME + "-" + IDP_REVIEWER_RIGHT_NAME})
    public Response getReviewer(@PathParam("id") Integer id, @Context UriInfo uriInfo) {
        LOGGER.info("reviewers/{} (GET)", id);

        try {
            final Reviewer reviewer = entityManager.find(Reviewer.class, id);
            if (reviewer == null) {
                auditLogHandler.logTraceReadForToken("Request for full profile", uriInfo, 0, 404);
                return Response.status(404).build();
            }

            auditLogHandler.logTraceReadForToken("View full reviewer profile", uriInfo, reviewer.getPaycode(), 200);
            return Response.ok(reviewer).build();
        } catch (Exception e) {
            LOGGER.error("Exception in /reviewers when requesting id {}", id);
            LOGGER.error("Exception was: {}\n{}", e.getMessage(), e.getStackTrace());
            return ServiceErrorDto.Failed("Unexpected exception when trying to find reviewer");
        }
    }

    private static final String MISSING_REQUIRED_FIELD = "Missing required field in the request data";

    @POST
    @Path("reviewers")
    @Produces({MediaType.APPLICATION_JSON})
    @RolesAllowed({"authenticated-user", IDP_PRODUCT_NAME + "-" + IDP_EDITOR_RIGHT_NAME})
    public Response createReviewer(ReviewerRequest reviewerRequest, @Context UriInfo uriInfo) {
        LOGGER.info("reviewers (POST)");

        final Integer paycode = reviewerRequest.getPaycode();
        if (paycode == null) {
            return ServiceErrorDto.InvalidRequest(MISSING_REQUIRED_FIELD,
                    "Field 'paycode' must be supplied when creating a new reviewer");
        }

        if (reviewerRequest.getEmail() == null && reviewerRequest.getPrivateEmail() == null) {
            return ServiceErrorDto.InvalidRequest(MISSING_REQUIRED_FIELD,
                    "Field 'email' or 'private email' must be supplied when creating a new reviewer");
        }

        final String agency = reviewerRequest.getAgency();
        if ( agency == null || agency.isBlank()) {
            return ServiceErrorDto.InvalidRequest(MISSING_REQUIRED_FIELD,
                    "Field 'agency' must be supplied and not be blank when creating a new reviewer");
        }

        final String userId = reviewerRequest.getUserId();
        if ( userId == null || userId.isBlank()) {
            return ServiceErrorDto.InvalidRequest(MISSING_REQUIRED_FIELD,
                    "Field 'userId' must be supplied and not be blank when creating a new reviewer");
        }

        try {
            final Reviewer entity = new Reviewer()
                    .withActive(reviewerRequest.isActive() == null || reviewerRequest.isActive())  // New users defaults to active
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
                    .withSubjectNotes(repository.checkSubjectNotes(reviewerRequest.getSubjectNotes(), reviewerRequest.getSubjects()))
                    .withAgency(agency)
                    .withUserId(userId);

            entityManager.persist(entity);
            entityManager.flush();

            auditLogHandler.logTraceCreateForToken("Created new reviewer", uriInfo, entity.getPaycode(), 201);
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
            ReviewerList<Reviewer> listOfReviewers = new ReviewerList<>()
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
                .toList();
        ReviewerList<ReviewerWithWorkloads> listOfReviewers = new ReviewerList<ReviewerWithWorkloads>().withReviewers(reviewers);
        return Response.ok(objectMapper.writerWithView(ReviewerView.Summary.class).writeValueAsString(listOfReviewers))
                .build();
    }

    @PUT
    @Path("reviewers/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    @RolesAllowed({"authenticated-user", IDP_PRODUCT_NAME + "-" + IDP_EDITOR_RIGHT_NAME, IDP_PRODUCT_NAME + "-" + IDP_REVIEWER_RIGHT_NAME})
    public Response updateReviewer(@PathParam("id") final Integer id, ReviewerRequest reviewerRequest,
                                   @QueryParam("notify") @DefaultValue("false") final boolean notify,
                                   @Context UriInfo uriInfo) {

        LOGGER.info("reviewers/{} (PUT), notify:{} request:{}", id, notify, reviewerRequest);
        try {

            // Find the existing user
            Reviewer reviewer = entityManager.find(Reviewer.class, id);
            if (reviewer == null) {
                LOGGER.info("Reviewer with id {} does not exists", id);
                auditLogHandler.logTraceUpdateForToken("Request for update of profile", uriInfo, 0, 404);
                return Response.status(404).build();
            }

            // If notification is required
            if (notify) {
                stash(reviewer, id);
            }

            // Update by patching
            if (reviewerRequest.isActive() != null) {
                reviewer.setActive(reviewerRequest.isActive());
                reviewer.setActiveChanged(Date.from(Instant.now()));
                if (reviewerRequest.isActive()) {
                    reviewer.setDeactivated(null);
                }
            }
            if (reviewerRequest.getAccepts() != null) {
                reviewer.setAccepts(reviewerRequest.getAccepts());
            }
            if (reviewerRequest.getAddress() != null) {
                reviewer.setAddress(reviewerRequest.getAddress());
            }
            if (reviewerRequest.getPrivateAddress() != null) {
                reviewer.setPrivateAddress(reviewerRequest.getPrivateAddress());
            }
            if (reviewerRequest.getEmail() != null) {
                reviewer.setEmail(reviewerRequest.getEmail());
            }
            if (reviewerRequest.getPrivateEmail() != null) {
                reviewer.setPrivateEmail(reviewerRequest.getPrivateEmail());
            }
            if (reviewerRequest.getFirstName() != null) {
                reviewer.setFirstName(reviewerRequest.getFirstName());
            }
            if (reviewerRequest.getLastName() != null) {
                reviewer.setLastName(reviewerRequest.getLastName());
            }
            if (reviewerRequest.getHiatusBegin() != null) {
                reviewer.setHiatusBegin(reviewerRequest.getHiatusBegin());
            }
            if (reviewerRequest.getHiatusEnd() != null) {
                reviewer.setHiatusEnd(reviewerRequest.getHiatusEnd());
            }
            if (reviewerRequest.getInstitution() != null) {
                reviewer.setInstitution(reviewerRequest.getInstitution());
            }
            if (reviewerRequest.getPaycode() != null) {
                if (!reviewerRequest.getPaycode().equals(reviewer.getPaycode())) {
                    Map<String, String> paycodeChanges = new HashMap<>();
                    paycodeChanges.put("Current value", reviewer.getPaycode().toString());
                    paycodeChanges.put("New value", reviewerRequest.getPaycode().toString());
                    auditLogHandler.logTraceUpdateForToken("Change of paycode (owning id)", uriInfo, reviewer.getPaycode(), paycodeChanges);
                }
                reviewer.setPaycode(reviewerRequest.getPaycode());
            }
            if (reviewerRequest.getPhone() != null) {
                reviewer.setPhone(reviewerRequest.getPhone());
            }
            if (reviewerRequest.getPrivatePhone() != null) {
                reviewer.setPrivatePhone(reviewerRequest.getPrivatePhone());
            }
            if (reviewerRequest.getSubjects() != null) {

                // If subjects were removed, make sure that associated notes are also removed.
                reviewer.setSubjectNotes(repository.resolveSubjectNotes(reviewerRequest.getSubjects(), reviewer.getSubjectNotes()));
                reviewer.setSubjects(repository.resolveSubjects(reviewerRequest.getSubjects()));
            }
            if (reviewerRequest.getSubjectNotes() != null) {

                // If both subjects and subjectnotes were changed, make sure that the supplied subjectnotes refer to
                // subjects present in the new subjectlist.
                if (reviewerRequest.getSubjects() != null) {
                    reviewer.setSubjectNotes(repository.checkSubjectNotes(reviewerRequest.getSubjectNotes(), reviewerRequest.getSubjects()));
                } else {

                    // Validate against existing subjects instead.
                    reviewer.setSubjectNotes(
                            repository.resolveSubjectNotes(
                                    reviewer.getSubjects().stream().map(Subject::getId).toList(),
                                    reviewerRequest.getSubjectNotes()));
                }
            }
            if (reviewerRequest.getCapacity() != null) {
                reviewer.setCapacity(reviewerRequest.getCapacity());
            }
            if (reviewerRequest.getNote() != null) {
                reviewer.setNote(reviewerRequest.getNote());
            }
            if (reviewerRequest.getAgency() != null) {
                reviewer.setAgency(reviewerRequest.getAgency());
            }
            if (reviewerRequest.getUserId() != null) {
                reviewer.setUserId(reviewerRequest.getUserId());
            }

            auditLogHandler.logTraceUpdateForToken("Update and view full reviewer profile", uriInfo, reviewer.getPaycode(), 200);
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
     * "Stash" the reviewer data. That is: Save the full reviewer object as a marshalled json.
     * The purpose: To have a baseline representing the reviewer before editing was begun.
     * @param reviewer the reviewer to stash
     * @param id of the reviewer.
     */
    private void stash(Reviewer reviewer, Integer id) {

        // Assume that eventually we will need to post a reviewerdata changed notification to the editors.
        TypedQuery<ReviewerDataStash> query = entityManager
                .createNamedQuery(ReviewerDataStash.GET_STASH_FROM_REVIEWER, ReviewerDataStash.class);
        query.setParameter("reviewerId", id);
        Optional<ReviewerDataStash> stash = query.getResultList().stream().findFirst();


        // If there is a stash: Settle for just updating the timestamp on reviewer data.
        stash.ifPresentOrElse(reviewerDataStash -> reviewer.withLastChanged(LocalDateTime.now()), () -> {

            // else create a new stash
            LOGGER.info("Stash for reviewer with id {} does not exists. Creating it.", id);
            ReviewerDataStash newStash;
            try {
                newStash = ReviewerDataStash.fromReviewer(reviewer);
                entityManager.persist(newStash);
                reviewer.withLastChanged(LocalDateTime.now());
            } catch (JsonProcessingException e) {
                LOGGER.error("Failed to serialize reviewer from stash:", e);
            }
        });
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

    @POST
    @Path("reviewers/{id}/resethiatus")
    @Produces({MediaType.APPLICATION_JSON})
    @RolesAllowed({"authenticated-user", IDP_PRODUCT_NAME + "-" + IDP_EDITOR_RIGHT_NAME, IDP_PRODUCT_NAME + "-" + IDP_REVIEWER_RIGHT_NAME})
    public Response resetHiatus(@PathParam("id") final Integer id,
                                @QueryParam("notify") @DefaultValue("false") final boolean notify,
                                @Context UriInfo uriInfo) {

        // Find the existing user
        final Reviewer reviewer = entityManager.find(Reviewer.class, id);
        if (reviewer == null) {
            LOGGER.info("Reviewer with id {} does not exists", id);
            auditLogHandler.logTraceUpdateForToken("Request for update of profile",
                    uriInfo, 0, 404);
            return Response.status(404).build();
        }

        reviewer.setHiatusBegin(null);
        reviewer.setHiatusEnd(null);

        try {
            // notify
            if (notify) {
                // Create the notification now, before we fill in the changed fields in reviewer.
                Notification notification = notificationFactory.notificationOf(new HiatusReset().withReviewer(reviewer));
                entityManager.persist(notification);
            }

            auditLogHandler.logTraceUpdateForToken("Hiatus reset", uriInfo, reviewer.getId(), 200);
            LOGGER.info("Hiatus for reviewer with ID {} was reset", reviewer.getId());
            return Response.ok(reviewer)
                    .build();
        } catch (NotificationFactory.ValidateException e) {
            LOGGER.error("ResetHiatus failed:", e);
            return Response.serverError().entity(e.getMessage()).build();
        } catch (Exception e) {
            LOGGER.error("Unexpected error when resetting hiatus:", e);
            return Response.serverError().entity(e.getMessage()).build();
        }

    }

    @GET
    @Path("reviewers/accepttypes")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAcceptTypes() {
        LOGGER.info("reviewers/accepttypes (GET)");
        List<Reviewer.AcceptsDto> acceptTypes = Arrays.stream(Reviewer.Accepts.values())
                .map(Reviewer.AcceptsDto::from)
                .toList();
        return Response.ok(acceptTypes).build();
    }
}
