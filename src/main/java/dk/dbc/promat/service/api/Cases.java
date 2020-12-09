/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

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
            return ServiceErrorDto.InvalidRequest("Missing required field in the request data", "Field 'title' must be supplied when creating a new case");
        }
        if( dto.getPrimaryFaust() == null || dto.getPrimaryFaust().isEmpty() ) {
            LOGGER.info("Request dto is missing 'primaryFaust' field");
            return ServiceErrorDto.InvalidRequest("Missing required field in the request data", "Field 'primaryFaust' must be supplied when creating a new case");
        }
        if( dto.getMaterialType() == null ) {
            LOGGER.info("Request dto is missing 'materialType' field");
            return ServiceErrorDto.InvalidRequest("Missing required field in the request data", "Field 'materialType' must be supplied when creating a new case");
        }

        repository.getExclusiveAccessToTable(PromatCase.TABLE_NAME);

        // Check that no existing case exists with the same primary or related faustnumber
        // as the given primary faustnumber and a state other than CLOSED or DONE
        if(!Faustnumbers.checkNoOpenCaseWithFaust(entityManager, dto.getPrimaryFaust())) {
            LOGGER.info("Case with primary or related Faust {} and state <> CLOSED|DONE exists", dto.getPrimaryFaust());
            return ServiceErrorDto.CaseExists(String.format("Case with primary or related faust {} and status not DONE or CLOSED exists", dto.getPrimaryFaust()));
        }

        // Check that no existing case exists with the same primary or related faustnumber
        // as the given relasted fasutnumbers and a state other than CLOSED or DONE
        if(dto.getRelatedFausts() != null && dto.getRelatedFausts().size() > 0) {
            if(!Faustnumbers.checkNoOpenCaseWithFaust(entityManager, dto.getRelatedFausts().toArray(String[]::new))) {
                LOGGER.info("Case with primary or related {} and state <> CLOSED|DONE exists", dto.getRelatedFausts());
                return ServiceErrorDto.CaseExists(String.format("Case with primary or related faust {} and status not DONE or CLOSED exists", dto.getRelatedFausts()));
            }
        }

        // Check for acceptable status code
        if( dto.getStatus() != null ) {
            switch( dto.getStatus() ) {
                case CREATED:  // Default status
                case ASSIGNED: // A check is made later to make sure we can mark the case as assigned
                    break;
                default:
                    LOGGER.info("Attempt to create case with invalid state {}", dto.getStatus());
                    return ServiceErrorDto.InvalidState(String.format("Case status {} is not allowed when creating a new case", dto.getStatus()));
            }
        }

        // Map foreign entities and create tasks
        List<Subject> subjects;
        Reviewer reviewer;
        Editor editor;
        List<PromatTask> tasks;
        ArrayList<String> relatedFausts = new ArrayList<>(dto.getRelatedFausts() == null
                ? new ArrayList<>()
                : dto.getRelatedFausts());
        try {
            subjects = repository.resolveSubjects(dto.getSubjects());
            reviewer = resolveReviewer(dto.getReviewer());
            editor = resolveEditor(dto.getEditor());
            tasks = createTasks(dto.getTasks(), relatedFausts);
        } catch(ServiceErrorException serviceErrorException) {
            LOGGER.info("Received serviceErrorException while mapping entities: {}", serviceErrorException.getMessage());
            return Response.status(400).entity(serviceErrorException.getServiceErrorDto()).build();
        }

        // Handle possible change of case status due to assigning an editor.
        //
        // If an reviewer has been assigned, then set or modify the status of the case and the assigned field.
        // If no reviwer is given, check that the status is not ASSIGNED - that would be a mess
        LocalDate assigned = dto.getAssigned() == null ? null : LocalDate.parse(dto.getAssigned());
        CaseStatus status = dto.getStatus() == null ? CaseStatus.CREATED : dto.getStatus();
        if( reviewer != null ) {
            assigned = LocalDate.now();
            status = CaseStatus.ASSIGNED;
        } else {
            if( status.equals(CaseStatus.ASSIGNED) ) {
                LOGGER.info("Attempt to set status ASSIGNED with no reviewer", dto.getReviewer());
                return ServiceErrorDto.InvalidState("Case status ASSIGNED is not possible without a reviewer");
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
            return ServiceErrorDto.Failed(exception.getMessage());
        }
    }

    @GET
    @Path("cases/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCase(@PathParam("id") final Integer id) {
        LOGGER.info("cases/{}", id);

        // Find and return the requested case
        try {

            PromatCase requested = entityManager.find(PromatCase.class, id);
            if( requested == null ) {
                LOGGER.info("Requested case {} does not exist", id);
                return ServiceErrorDto.NotFound("Case not found", String.format("Requested case {} does not exist", id));
            }

            return Response.status(200).entity(requested).build();
        } catch(Exception exception) {
            LOGGER.error("Caught exception: {}", exception.getMessage());
            return ServiceErrorDto.Failed(exception.getMessage());
        }
    }

    @GET
    @Path("cases")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listCases(@QueryParam("faust") final String faust,
                              @QueryParam("status") final String status,
                              @QueryParam("reviewer") final Integer reviewer,
                              @QueryParam("editor") final Integer editor,
                              @QueryParam("title") final String title,
                              @QueryParam("limit") final Integer limit,
                              @QueryParam("from") final Integer from) {
        LOGGER.info("cases/?faust={}|status={}|editor={}|title={}|limit={}|from={}",
                faust == null ? "null" : faust,
                status == null ? "null" : status,
                reviewer == null ? "null" : reviewer,
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
                        return ServiceErrorDto.InvalidRequest("Invalid case status code", String.format("Unknown case status={}", oneStatus));
                    }
                }

                allPredicates.add(builder.or(statusPredicates.toArray(Predicate[]::new)));
            }

            // Get cases with given reviewer
            if(reviewer != null && reviewer > 0) {
                allPredicates.add(builder.equal(root.get("reviewer").get("id"), reviewer));
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
            return ServiceErrorDto.Failed(exception.getMessage());
        }
    }

    @POST
    @Path("cases/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response patchCase(@PathParam("id") final Integer id, CaseRequestDto dto) {
        LOGGER.info("cases/{} (POST) body: {}", id, dto);

        repository.getExclusiveAccessToTable(PromatCase.TABLE_NAME);

        try {

            // Update strategy is effectively a PATCH update, only a selected number of fields
            // can be updated on a case, some forbidden fields will trigger an error but most
            // fields not accepted will be silently ignored.

            // Errorchecking when trying to update fields managed by the backend solely
            if(dto.getAssigned() != null) {
                LOGGER.info("Attempt to set 'assigned' on case {}", id);
                return ServiceErrorDto.InvalidRequest("Forbidden field", String.format("Setting the value of 'assigned' is not allowed"));
            }
            if(dto.getStatus() != null && dto.getStatus() != CaseStatus.CLOSED && dto.getStatus() != CaseStatus.CREATED) {
                LOGGER.info("Attempt to set forbidden 'status' on case {}", id);
                return ServiceErrorDto.InvalidRequest("Forbidden status", String.format("Setting the value of 'status' to other statuses than CLOSED or CREATED is not allowed"));
            }

            // Fetch an existing entity with the given id
            PromatCase existing = entityManager.find(PromatCase.class, id);
            if( existing == null ) {
                LOGGER.info("No such case {}", id);
                return ServiceErrorDto.NotFound("No such case", String.format("Case with id {} does not exist", id));
            }

            // Update fields
            if(dto.getTitle() != null) {
                existing.setTitle(dto.getTitle());
            }
            if(dto.getDetails() != null) {
                existing.setDetails(dto.getDetails());
            }
            if(dto.getPrimaryFaust() != null) {
                if(!Faustnumbers.checkNoOpenCaseWithFaust(entityManager, existing.getId(), dto.getPrimaryFaust())) {
                    LOGGER.info("Case with primary or related faust {} and state <> CLOSED|DONE exists", dto.getPrimaryFaust());
                    return ServiceErrorDto.CaseExists(String.format("Case with primary or related faust {} and status not DONE or CLOSED exists", dto.getPrimaryFaust()));
                }
                existing.setPrimaryFaust(dto.getPrimaryFaust());
            }
            if(dto.getRelatedFausts() != null) {
                if(!Faustnumbers.checkNoOpenCaseWithFaust(entityManager, existing.getId(), dto.getRelatedFausts().toArray(String[]::new))) {
                    LOGGER.info("Case with primary or related faust {} and state <> CLOSED|DONE exists", dto.getPrimaryFaust());
                    return ServiceErrorDto.CaseExists(String.format("Case with primary or related fausts {} and status not DONE or CLOSED exists", dto.getRelatedFausts()));
                }
                existing.setRelatedFausts(dto.getRelatedFausts());
            }
            if(dto.getReviewer() != null) {
                existing.setReviewer(resolveReviewer(dto.getReviewer()));
                if(existing.getStatus() == CaseStatus.CREATED) {
                    existing.setStatus(CaseStatus.ASSIGNED);
                }
            }
            if(dto.getEditor() != null) {
                existing.setEditor(resolveEditor(dto.getEditor()));
            }
            if(dto.getSubjects() != null) {
                existing.setSubjects(repository.resolveSubjects(dto.getSubjects()));
            }
            if(dto.getDeadline() != null) {
                existing.setDeadline(LocalDate.parse(dto.getDeadline()));
            }
            if(dto.getMaterialType() != null) {
                existing.setMaterialType(dto.getMaterialType());
            }
            if(dto.getStatus() != null) {
                if(dto.getStatus() == CaseStatus.CLOSED) {
                    existing.setStatus(CaseStatus.CLOSED);
                } else {
                    if( existing.getTasks().stream().filter(task -> task.getData() == null || task.getData().isEmpty()).count() == 0) {
                        if( existing.getTasks().stream().filter(task -> task.getApproved() == null).count() == 0) {
                            existing.setStatus(CaseStatus.APPROVED);
                        } else {
                            existing.setStatus(CaseStatus.PENDING_APPROVAL);
                        }
                    } else {
                        existing.setStatus(CaseStatus.ASSIGNED);
                    }
                    // Todo: We may neeed more status handling here when the lifecycle of a task is better defined
                }
            }

            return Response.ok(existing).build();
        } catch(ServiceErrorException serviceErrorException) {
            LOGGER.info("Received serviceErrorException while mapping entities: {}", serviceErrorException.getMessage());
            return Response.status(400).entity(serviceErrorException.getServiceErrorDto()).build();
        }
        catch(Exception exception) {
            LOGGER.error("Caught exception: {}", exception.getMessage());
            return ServiceErrorDto.Failed(exception.getMessage());
        }
    }

    public Reviewer resolveReviewer(Integer reviewerId) throws ServiceErrorException {
        if(reviewerId == null) {
            return null;
        }

        Reviewer reviewer = entityManager.find(Reviewer.class, reviewerId);
        if( reviewer == null ) {
            LOGGER.info("Attempt to resolve reviewer {} failed. No such user", reviewerId);
            throw new ServiceErrorException("Attempt to resolve reviewer failed")
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withCause("No such reviewer")
                    .withDetails(String.format("Field 'reviewer' contains user id {} which does not exist", reviewerId));
        }
        return reviewer;
    }

    public Editor resolveEditor(Integer editorId) throws ServiceErrorException {
        if(editorId == null) {
            return null;
        }

        Editor editor = entityManager.find(Editor.class, editorId);
        if( editor == null ) {
            LOGGER.info("Attempt to resolve editor {} failed. No such user", editorId);
            throw new ServiceErrorException("Attempt to resolve editor failed")
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withCause("No such editor")
                    .withDetails(String.format("Field 'editor' contains user id {} which does not exist", editorId));
        }
        return editor;
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

    private List<PromatTask> createTasks(List<TaskDto> taskDtos, List<String> relatedFausts) throws ServiceErrorException {
        ArrayList<PromatTask> tasks = new ArrayList<>();

        if( taskDtos != null && taskDtos.size() > 0) {
            for(TaskDto task : taskDtos) {
                if(task.getTaskType() == null || task.getTaskFieldType() == null) {
                    LOGGER.info("Task dto is missing the taskType and/or taskFieldType field");
                    throw new ServiceErrorException("Task dto is missing the taskType and/or taskFieldType field")
                            .withCause("Missing required field(s) in the request data")
                            .withDetails("Fields 'taskType' and 'taskFieldType' must be supplied when creating a new task")
                            .withCode(ServiceErrorCode.INVALID_REQUEST);
                }

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

        return tasks;
    }
}
