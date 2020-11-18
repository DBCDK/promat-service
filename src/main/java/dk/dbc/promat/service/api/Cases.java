package dk.dbc.promat.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.promat.service.dto.CaseRequestDto;
import dk.dbc.promat.service.dto.CaseSummaryList;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.dto.TaskDto;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.CaseView;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.Repository;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.Subject;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskType;
import dk.dbc.promat.service.rest.JsonMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
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
import javax.ws.rs.PATCH;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(Cases.class);

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @EJB Repository repository;

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
            LOGGER.info("Request dto is missing 'title' field");
            ServiceErrorDto err = new ServiceErrorDto()
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withCause("Missing required field in the request data")
                    .withDetails("Field 'title' must be supplied when creating a new case");
            return Response.status(400).entity(err).build();
        }
        if( dto.getPrimaryFaust() == null || dto.getPrimaryFaust().isEmpty() ) {
            LOGGER.info("Request dto is missing 'primaryFaust' field");
            ServiceErrorDto err = new ServiceErrorDto()
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withCause("Missing required field in the request data")
                    .withDetails("Field 'primaryFaust' must be supplied when creating a new case");
            return Response.status(400).entity(err).build();
        }
        if( dto.getMaterialType() == null ) {
            LOGGER.info("Request dto is missing 'materialType' field");
            ServiceErrorDto err = new ServiceErrorDto()
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withCause("Missing required field in the request data")
                    .withDetails("Field 'materialType' must be supplied when creating a new case");
            return Response.status(400).entity(err).build();
        }

        repository.getExclusiveAccessToTable(PromatCase.TABLE_NAME);

        // Check that no existing case exists with the same primary or related faustnumber
        // as the given primary faustnumber and a state other than CLOSED or DONE
        Query q = entityManager.createNativeQuery("SELECT CheckNoOpenCaseWithFaust(?)");
        q.setParameter(1, dto.getPrimaryFaust());
        if((boolean) q.getSingleResult() == false) {
            LOGGER.info("Case with primary or related Faust {} and state <> CLOSED|DONE exists", dto.getPrimaryFaust());
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
                    LOGGER.info("Case with primary or related {} and state <> CLOSED|DONE exists", faust);
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
                    LOGGER.info("Attempt to resolve subject {} failed. No such subject", subjectId);
                    ServiceErrorDto err = new ServiceErrorDto()
                            .withCode(ServiceErrorCode.INVALID_REQUEST)
                            .withCause("No such subject")
                            .withDetails(String.format("Field 'subject' contains id {} which does not exist", subjectId));
                    return Response.status(400).entity(err).build();
                }
                subjects.add(subject);
            }
        }

        // Map reviewer id to existing reviewer (promatuser)
        // If an reviewer has been assigned, then set or modify the status of the case and the assigned field.
        // If no reviwer is given, check that the status is not ASSIGNED - that would be a mess
        Reviewer reviewer = null;
        LocalDate assigned = dto.getAssigned() == null ? null : LocalDate.parse(dto.getAssigned());
        CaseStatus status = dto.getStatus() == null ? CaseStatus.CREATED : dto.getStatus();
        if( dto.getReviewer() != null ) {
            reviewer = entityManager.find(Reviewer.class, dto.getReviewer());
            if(reviewer == null) {
                LOGGER.info("Attempt to resolve reviewer {} failed. No such user", dto.getReviewer());
                ServiceErrorDto err = new ServiceErrorDto()
                        .withCode(ServiceErrorCode.INVALID_REQUEST)
                        .withCause("No such reviewer")
                        .withDetails(String.format("Field 'reviewer' contains user id {} which does not exist", dto.getReviewer()));
                return Response.status(400).entity(err).build();
            }
            assigned = LocalDate.now();
            status = CaseStatus.ASSIGNED;
        } else {
            if( status.equals(CaseStatus.ASSIGNED) ) {
                LOGGER.info("Attempt to set status ASSIGNED with no reviewer", dto.getReviewer());
                ServiceErrorDto err = new ServiceErrorDto()
                        .withCode(ServiceErrorCode.INVALID_STATE)
                        .withCause("Invalid state")
                        .withDetails("Case status ASSIGNED is not possible without a reviewer");
                return Response.status(400).entity(err).build();
            }
        }

        // Map editor to existing editor (promatuser)
        Editor editor = null;
        if( dto.getEditor() != null ) {
            editor= entityManager.find(Editor.class, dto.getEditor());
            if( editor == null ) {
                LOGGER.info("Attempt to resolve editor {} failed. No such user", dto.getEditor());
                ServiceErrorDto err = new ServiceErrorDto()
                        .withCode(ServiceErrorCode.INVALID_REQUEST)
                        .withCause("No such editor")
                        .withDetails(String.format("Field 'editor' contains user id {} which does not exist", dto.getEditor()));
                return Response.status(400).entity(err).build();
            }
        }

        // We may have to add more related faustnumbers when creating tasks
        ArrayList<String> relatedFausts = new ArrayList<>();
        relatedFausts.addAll(dto.getRelatedFausts() == null ? new ArrayList<>() : dto.getRelatedFausts());

        // Create tasks if any is given
        ArrayList<PromatTask> tasks = new ArrayList<>();
        if( dto.getTasks() != null ) {
            for(TaskDto task : dto.getTasks()) {
                tasks.add(new PromatTask()
                        .withTaskType(task.getTaskType())
                        .withTaskFieldType(task.getTaskFieldType())
                        .withPayCode(getPaycodeForTaskType(task.getTaskType()))
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
            .withEditor(editor)
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
            LOGGER.error("Caught exception: {}", exception.getMessage());
            throw exception;
        }
    }

    @GET
    @Path("cases")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listCases(@QueryParam("faust") final String faust,
                              @QueryParam("status") final String status,
                              @QueryParam("editor") final Integer editor,
                              @QueryParam("title") final String title,
                              @QueryParam("limit") final Integer limit,
                              @QueryParam("from") final Integer from) throws Exception {
        LOGGER.info("cases/?faust={}|status={}|editor={}|title={}|limit={}|from={}",
                faust == null ? "null" : faust,
                status == null ? "null" : status,
                editor == null ? "null" : editor,
                title == null ? "null" : title,
                limit == null ? "null" : limit,
                from == null ? "null" : from);

        // Select and return cases
        try {

            // Initialize query and criteriabuilder
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery criteriaQuery = builder.createQuery();
            Root<PromatCase> root = criteriaQuery.from(PromatCase.class);
            criteriaQuery.select(root);

            // List of all predicates to be AND'ed together on the final query
            List<Predicate> allPredicates = new ArrayList<>();

            // Get case with given primary or related
            if(faust != null && !faust.isBlank() && !faust.isEmpty()) {

                Predicate primaryFaustPredicat = builder.equal(root.get("primaryFaust"), builder.literal(faust));
                Predicate relatedFaustsPredicat = builder.isTrue(builder.function("JsonbContainsFromString", Boolean.class, root.get("relatedFausts"), builder.literal(faust)));
                Predicate faustPredicate = builder.or(primaryFaustPredicat, relatedFaustsPredicat);

                // And status not CLOSED or DONE
                CriteriaBuilder.In<CaseStatus> inClause = builder.in(root.get("status"));
                inClause.value(CaseStatus.CLOSED);
                inClause.value(CaseStatus.DONE);
                Predicate statusPredicate = builder.not(inClause);

                allPredicates.add(builder.and(faustPredicate, statusPredicate));
            }

            // Get cases with given set of statuses
            if(status != null && !status.isBlank() && !status.isEmpty()) {

                // Allthough jax.rs actually supports having multiple get arguments with the same name
                // "?status=CREATED&status=ASSIGNED" this is not a safe implementation since other
                // frameworks (React/NextJS or others) may have difficulties handling this. So instead
                // a list of statuses is expected to be given as a comma separated list

                List<Predicate> statusPredicates = new ArrayList<>();
                for(String oneStatus : status.split(",")) {
                    try {
                        statusPredicates.add(builder.equal(root.get("status"), CaseStatus.valueOf(oneStatus)));
                    } catch (IllegalArgumentException ex) {
                        LOGGER.info("Invalid status code '{}' in request for cases with status", oneStatus);
                        ServiceErrorDto err = new ServiceErrorDto()
                                .withCode(ServiceErrorCode.INVALID_REQUEST)
                                .withCause("Request failed")
                                .withDetails("Invalid case status code");
                        return Response.serverError().entity(err).build();
                    }
                }

                allPredicates.add(builder.or(statusPredicates.toArray(Predicate[]::new)));
            }

            // Get cases with given editor
            if(editor != null && editor > 0) {
                allPredicates.add(builder.equal(root.get("editor").get("id"), editor));
            }

            // Get cases with a title that matches (entire, or part of) the given title
            if(title != null && !title.isBlank() && !title.isEmpty()) {
                allPredicates.add(builder
                        .like(builder
                                .lower(root
                                        .get("title")), builder.literal("%" + title.toLowerCase() + "%")));
            }

            // If a starting id has been given, add this
            if( from != null ) {
                allPredicates.add(builder.gt(root.get("id"), builder.literal(from)));
            }

            // Combine all where clauses together with AND and add them to the query
            if(allPredicates.size() > 0) {
                Predicate finalPredicate = builder.and(allPredicates.toArray(Predicate[]::new));
                criteriaQuery.where(finalPredicate);
            }

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
            LOGGER.error("Caught exception: {}", exception.getMessage());
            throw exception;
        }
    }

    private String getPaycodeForTaskType(TaskType taskType) {
        switch(taskType) {

            case GROUP_1_LESS_THAN_100_PAGES: return "1956";
            case GROUP_2_100_UPTO_199_PAGES: return "1957";
            case GROUP_3_200_UPTO_499_PAGES: return "1958";
            case GROUP_4_500_OR_MORE_PAGES: return "1959";

            case MOVIES_GR_1: return "1980";
            case MOVIES_GR_2: return "1981";
            case MOVIES_GR_3: return "1982";

            case MULTIMEDIA_FEE: return "1954";
            case MULTIMEDIA_FEE_GR2: return "1985";

            case MOVIE_NON_FICTION_GR1: return "1979";
            case MOVIE_NON_FICTION_GR2: return "1983";
            case MOVIE_NON_FICTION_GR3: return "1984";

            case NO_REVIEW: return "1961";

            case MUSIC_FEE: return "1234";

            case BKM: return "1962";

            case METAKOMPAS: return "1987";

            case BUGGI: return "0000";  // Todo: Update return value when the paycode is known

            case NONE:
            default:
                return "";
        }
    }

    @POST
    @Path("cases/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response patchCase(@PathParam("id") final Integer id, CaseRequestDto dto) {
        LOGGER.info("cases/{} (POST) body: {}", id, dto);

        try {

            // Update strategy is effectively a PATCH update, only a selected number of fields
            // can be updated on a case, some forbidden fields will trigger an error but most
            // fields not accepted will be silently ignored.

            // Errorchecking when trying to update fields managed by the backend solely
            if(dto.getAssigned() != null) {
                LOGGER.info("Attempt to set 'assigned' on case {}", id);
                ServiceErrorDto err = new ServiceErrorDto()
                        .withCode(ServiceErrorCode.INVALID_REQUEST)
                        .withCause("Forbidden field")
                        .withDetails(String.format("Setting the value of 'assigned' is not allowed"));
                return Response.status(404).entity(err).build();
            }
            if(dto.getStatus() != null) {
                LOGGER.info("Attempt to set 'status' on case {}", id);
                ServiceErrorDto err = new ServiceErrorDto()
                        .withCode(ServiceErrorCode.INVALID_REQUEST)
                        .withCause("Forbidden field")
                        .withDetails(String.format("Setting the value of 'status' is not allowed"));
                return Response.status(404).entity(err).build();
            }

            // Fetch an existing entity with the given id
            PromatCase existing = entityManager.find(PromatCase.class, id);
            if( existing == null ) {
                LOGGER.info("No such case {}", id);
                ServiceErrorDto err = new ServiceErrorDto()
                        .withCode(ServiceErrorCode.INVALID_REQUEST)
                        .withCause("No such case")
                        .withDetails(String.format("Case with id {} does not exist", id));
                return Response.status(404).entity(err).build();
            }

            // Update fields
            if(dto.getTitle() != null) {
                existing.setTitle(dto.getTitle());
            }
            if(dto.getDetails() != null) {
                existing.setDetails(dto.getDetails());
            }
            if(dto.getPrimaryFaust() != null) {
                existing.setPrimaryFaust(dto.getPrimaryFaust());
            }
            if(dto.getRelatedFausts() != null) {
                existing.setRelatedFausts(dto.getRelatedFausts());
            }
            if(dto.getReviewer() != null) {
                // Todo: Resolve and update reviewer
            }
            if(dto.getEditor() != null) {
                // Todo: Resolve and update editor
            }
            if(dto.getSubjects() != null) {
                // Todo: Resolve and update subjects
            }
            if(dto.getDeadline() != null) {
                existing.setDeadline(LocalDate.parse(dto.getDeadline()));
            }
            if(dto.getMaterialType() != null) {
                existing.setMaterialType(dto.getMaterialType());
            }

            // Todo: Handle fields that could/should change implicitly when other fields change value.
            //       * assigned;
            //       * status;

            return Response.ok().build();
        } catch(Exception exception) {
            LOGGER.error("Caught exception: {}", exception.getMessage());
            throw exception;
        }
    }
}
