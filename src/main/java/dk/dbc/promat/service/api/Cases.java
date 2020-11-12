package dk.dbc.promat.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.promat.service.dto.CaseSummaryList;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.dto.TaskDto;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.dto.CaseRequestDto;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.CaseView;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.Subject;
import dk.dbc.promat.service.persistence.Task;
import dk.dbc.promat.service.rest.JsonMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Stateless
@Path("")
public class Cases {
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();
    private static final Logger LOGGER = LoggerFactory.getLogger(Cases.class);

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    // Default number of results when getting cases
    private static final int DEFAULT_CASES_LIMIT = 100;

    @POST
    @Path("cases")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postCase(CaseRequestDto dto) {
        LOGGER.info("cases/ (POST) body: {}", dto);

        // Check for required data when creating a new case
        if( dto.getTitle() == null || dto.getTitle().isEmpty() ) {
            LOGGER.error("Request dto is missing 'title' field");
            ServiceErrorDto err = new ServiceErrorDto()
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withCause("Missing required field in the request data")
                    .withDetails("Field 'title' must be supplied when creating a new case");
            return Response.status(400).entity(err).build();
        }
        if( dto.getPrimaryFaust() == null || dto.getPrimaryFaust().isEmpty() ) {
            LOGGER.error("Request dto is missing 'primaryFaust' field");
            ServiceErrorDto err = new ServiceErrorDto()
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withCause("Missing required field in the request data")
                    .withDetails("Field 'primaryFaust' must be supplied when creating a new case");
            return Response.status(400).entity(err).build();
        }
        if( dto.getMaterialType() == null ) {
            LOGGER.error("Request dto is missing 'materialType' field");
            ServiceErrorDto err = new ServiceErrorDto()
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withCause("Missing required field in the request data")
                    .withDetails("Field 'materialType' must be supplied when creating a new case");
            return Response.status(400).entity(err).build();
        }

        // Check that no existing case exists with the same primary or related faustnumber
        // as the given primary faustnumber and a state other than CLOSED or DONE
        Query q = entityManager.createNativeQuery("SELECT CheckNoOpenCaseWithFaust(?)");
        q.setParameter(1, dto.getPrimaryFaust());
        if((boolean) q.getSingleResult() == false) {
            LOGGER.error("Case with primary or related Faust {} and state <> CLOSED|DONE exists", dto.getPrimaryFaust());
            ServiceErrorDto err = new ServiceErrorDto()
                    .withCode(ServiceErrorCode.CASE_EXISTS)
                    .withCause("Case exists")
                    .withDetails(String.format("Case with primary or related faust {} and status not DONE or CLOSED exists", dto.getPrimaryFaust()));
            return Response.status(409).entity(err).build();
        }

        // Check that no existing case exists with the same primary or related faustnumber
        // as the given relasted fasutnumbers and a state other than CLOSED or DONE
        if(dto.getRelatedFausts() != null && dto.getRelatedFausts().size() > 0) {
            q = entityManager.createNativeQuery("SELECT CheckNoOpenCaseWithFaust(?)");
            for(String faust : dto.getRelatedFausts()) {
                q.setParameter(1, faust);
                if((boolean) q.getSingleResult() == false) {
                    LOGGER.error("Case with primary or related {} and state <> CLOSED|DONE exists", faust);
                    ServiceErrorDto err = new ServiceErrorDto()
                            .withCode(ServiceErrorCode.CASE_EXISTS)
                            .withCause("Case exists")
                            .withDetails(String.format("Case with primary or related faust {} and status not DONE or CLOSED exists", dto.getPrimaryFaust()));
                    return Response.status(409).entity(err).build();
                }
            }
        }

        // Check for acceptable status code
        if( dto.getStatus() != null ) {
            switch( dto.getStatus() ) {
                case CREATED:  // Default status
                case ASSIGNED: // A check is made later to make sure we can mark the case as assigned
                    break;
                default:
                    ServiceErrorDto err = new ServiceErrorDto()
                            .withCode(ServiceErrorCode.INVALID_STATE)
                            .withCause("Invalid state")
                            .withDetails(String.format("Case status {} is not allowed when creating a new case", dto.getStatus()));
                    return Response.status(400).entity(err).build();
            }
        }

        // Map subject ids to existing subjects
        ArrayList<Subject> subjects = new ArrayList<>();
        if( dto.getSubjects() != null ) {
            for(int subjectId : dto.getSubjects()) {
                Subject subject = entityManager.find(Subject.class, subjectId);
                if(subject == null) {
                    LOGGER.error("Attempt to resolve subject {} failed. No such subject", subjectId);
                    ServiceErrorDto err = new ServiceErrorDto()
                            .withCode(ServiceErrorCode.INVALID_REQUEST)
                            .withCause("No such subject")
                            .withDetails(String.format("Field 'subject' contains id {} which does not exist", subjectId));
                    return Response.status(400).entity(err).build();
                }
                subjects.add(subject);
            }
        }

        // Map reviewer id to existing reviewer.
        // If an reviewer has been assigned, then set or modify the status of the case and the assigned field.
        // If no reviwer is given, check that the status is not ASSIGNED - that would be a mess
        Reviewer reviewer = null;
        LocalDate assigned = dto.getAssigned() == null ? null : LocalDate.parse(dto.getAssigned());
        CaseStatus status = dto.getStatus() == null ? CaseStatus.CREATED : dto.getStatus();
        if( dto.getReviewer() != null ) {
            reviewer = entityManager.find(Reviewer.class, dto.getReviewer());
            if(reviewer == null) {
                LOGGER.error("Attempt to resolve reviewer {} failed. No such reviewer", dto.getReviewer());
                ServiceErrorDto err = new ServiceErrorDto()
                        .withCode(ServiceErrorCode.INVALID_REQUEST)
                        .withCause("No such reviewer")
                        .withDetails(String.format("Field 'reviewer' contains id {} which does not exist", dto.getReviewer()));
                return Response.status(400).entity(err).build();
            }
            assigned = LocalDate.now();
            status = CaseStatus.ASSIGNED;
        } else {
            if( status.equals(CaseStatus.ASSIGNED) ) {
                LOGGER.error("Attempt to set status ASSIGNED with no reviewer", dto.getReviewer());
                ServiceErrorDto err = new ServiceErrorDto()
                        .withCode(ServiceErrorCode.INVALID_STATE)
                        .withCause("Invalid state")
                        .withDetails("Case status ASSIGNED is not possible without a reviewer");
                return Response.status(400).entity(err).build();
            }
        }

        // We may have to add more related faustnumbers when creating tasks
        ArrayList<String> relatedFausts = new ArrayList<>();
        relatedFausts.addAll(dto.getRelatedFausts() == null ? new ArrayList<>() : dto.getRelatedFausts());

        // Create tasks if any is given
        ArrayList<Task> tasks = new ArrayList<>();
        if( dto.getTasks() != null ) {
            for(TaskDto task : dto.getTasks()) {
                tasks.add(new Task()
                        .withPaycode(task.getPaycode())
                        .withTypeOfTask(task.getTypeOfTask())
                        .withCreated(LocalDate.now())
                        .withTargetFausts(task.getTargetFausts() == null ? null : task.getTargetFausts()));

                if( task.getTargetFausts() != null ) {
                    for(String faust : task.getTargetFausts() ) {
                        if(!relatedFausts.contains(faust)) {
                            relatedFausts.add(faust);
                        }
                    }
                }
            }
        }

        // Create case
        try {
            PromatCase entity = new PromatCase()
            .withTitle(dto.getTitle())
            .withDetails(dto.getDetails() == null ? "" : dto.getDetails())
            .withPrimaryFaust(dto.getPrimaryFaust())
            .withRelatedFausts(relatedFausts)
            .withReviewer(reviewer)
            .withSubjects(subjects)
            .withCreated(LocalDate.now())
            .withDeadline(dto.getDeadline() == null ? null : LocalDate.parse(dto.getDeadline()))
            .withAssigned(assigned)
            .withStatus(status)
            .withMaterialType(dto.getMaterialType())
            .withTasks(tasks);

            entityManager.persist(entity);

            // 201 CREATED
            LOGGER.info("Created new case for primaryFaust {}", entity.getPrimaryFaust());
            return Response.status(201)
                    .entity(entity)
                    .build();
        } catch(Exception exception) {
            LOGGER.error("Caught unexpected exception: {} of type {}", exception.getMessage(), exception.toString());
            ServiceErrorDto err = new ServiceErrorDto()
                    .withCode(ServiceErrorCode.FAILED)
                    .withCause("Request failed")
                    .withDetails(exception.getMessage());
            return Response.serverError().entity(err).build();
        }
    }

    @GET
    @Path("cases/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCase(@PathParam("id") final Integer id) throws Exception {
        LOGGER.info("cases/{}", id);

        // Find and return the requested case
        try {

            PromatCase requested = entityManager.find(PromatCase.class, id);
            if( requested == null ) {
                LOGGER.info("Requested case {} does not exist", id);
                return Response.status(404).build();
            }

            return Response.status(200).entity(requested).build();
        } catch(Exception exception) {
            LOGGER.info("Caught exception: {}", exception.getMessage());
            throw exception;
        }
    }

    @GET
    @Path("cases")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listCases(@QueryParam("faust") final String faust,
                              @QueryParam("status") final String status,
                              @QueryParam("limit") final Integer limit,
                              @QueryParam("from") final Integer from) throws Exception {
        LOGGER.info("cases/?faust={}|status={}|limit={}|from={}",
                faust,
                status == null ? "null" : status,
                limit == null ? "null" : limit,
                from == null ? "null" : from);

        // Prevent accidental 'fetch all cases' if no filters has been given
        if((faust == null || faust.isEmpty()) && (status == null || status.isEmpty())) {
            ServiceErrorDto err = new ServiceErrorDto()
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withCause("Missing a required field in the request data")
                    .withDetails("At least one filter must be given (faust, status)");
            return Response.status(400).entity(err).build();
        }

        // Select and return cases
        try {

            // Initialize query and criteriabuilder
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery criteriaQuery = builder.createQuery();
            Root<PromatCase> root = criteriaQuery.from(PromatCase.class);
            criteriaQuery.select(root);

            // List of all predicates to be AND'ed together on the final query
            List<Predicate> allPredicates = new ArrayList<>();

            // Add relevant clauses
            if( faust != null ) {

                // Get case with given primary or related
                Predicate primaryFaustPredicat = builder.equal(root.get("primaryFaust"), builder.literal(faust));
                Predicate relatedFaustsPredicat = builder.isTrue(builder.function("JsonbContainsFromString", Boolean.class, root.get("relatedFausts"), builder.literal(faust)));
                Predicate faustPredicate = builder.or(primaryFaustPredicat, relatedFaustsPredicat);

                // And status not CLOSED or DONE
                CriteriaBuilder.In<CaseStatus> inClause = builder.in(root.get("status"));
                inClause.value(CaseStatus.CLOSED);
                inClause.value(CaseStatus.DONE);
                Predicate statusPredicate = builder.not(inClause);

                allPredicates.add(builder.and(faustPredicate, statusPredicate));

            } else if( status != null ) {

                // Allthough jax.rs actually supports having multiple get arguments with the same name
                // "?status=CREATED&status=ASSIGNED" this is not a safe implementation since other
                // frameworks (React/NextJS or others) may have difficulties handling this. So instead
                // a list of statuses is expected to be given as a comma separated list

                // Get cases with the given set of statuses
                List<Predicate> statusPredicates = new ArrayList<>();
                for(String oneStatus : status.split(",")) {
                    statusPredicates.add(builder.equal(root.get("status"), CaseStatus.valueOf(oneStatus)));
                }

                allPredicates.add(builder.or(statusPredicates.toArray(Predicate[]::new)));
            }

            // If a starting id has been given, add this
            if( from != null ) {
                allPredicates.add(builder.gt(root.get("id"), builder.literal(from)));
            }

            // Combine all where clauses together with AND and add them to the query
            Predicate finalPredicate = builder.and(allPredicates.toArray(Predicate[]::new));
            criteriaQuery.where(finalPredicate);

            // Complete the query by adding limits and ordering
            criteriaQuery.orderBy(builder.asc(root.get("id")));
            TypedQuery<PromatCase> query = entityManager.createQuery(criteriaQuery);
            query.setMaxResults(limit == null ? DEFAULT_CASES_LIMIT : limit);

            // Execute the query
            CaseSummaryList cases = new CaseSummaryList();
            cases.getCases().addAll(query.getResultList());
            cases.setNumFound(cases.getCases().size());

            // Return the found cases as a list of CaseSummary entities
            //
            // Note that the http status is set to 404 (NOT FOUND) if no case matched the query
            // this is to allow a quick(er) check for existing cases by using HEAD and checking
            // the statuscode instead of deserializing the response body and looking at numFound
            ObjectMapper mapper = new JsonMapperProvider().getObjectMapper();
            return Response.status(cases.getNumFound() > 0 ? 200 : 404)
                    .entity(mapper.writerWithView(CaseView.CaseSummary.class)
                            .writeValueAsString(cases)).build();

        } catch(Exception exception) {
            LOGGER.info("Caught exception: {}", exception.getMessage());
            throw exception;
        }
    }
}
