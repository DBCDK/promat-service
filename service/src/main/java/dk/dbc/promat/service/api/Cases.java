/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.connector.openformat.OpenFormatConnectorException;
import dk.dbc.opennumberroll.OpennumberRollConnector;
import dk.dbc.opennumberroll.OpennumberRollConnectorException;
import dk.dbc.promat.service.Repository;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.dto.CaseSummaryList;
import dk.dbc.promat.service.dto.CriteriaOperator;
import dk.dbc.promat.service.dto.Dto;
import dk.dbc.promat.service.dto.ListCasesParams;
import dk.dbc.promat.service.dto.RecordDto;
import dk.dbc.promat.service.dto.RecordsListDto;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.dto.TaskDto;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.CaseView;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.PromatMessage;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.Subject;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.templating.CaseviewXmlTransformer;
import dk.dbc.promat.service.templating.NotificationFactory;
import dk.dbc.promat.service.templating.model.AssignReviewer;
import dk.dbc.promat.service.templating.Renderer;
import java.time.LocalDateTime;

import org.eclipse.microprofile.config.inject.ConfigProperty;
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
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Stateless
@Path("")
public class Cases {
    private static final Logger LOGGER = LoggerFactory.getLogger(Cases.class);

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @EJB
    Repository repository;

    @EJB
    NotificationFactory notificationFactory;

    @EJB
    Records records;

    @Inject
    private OpennumberRollConnector opennumberRollConnector;

    @Inject
    @ConfigProperty(name = "OPENNUMBERROLL_NUMBERROLLNAME")
    String openNumberrollRollName;

    // Default number of results when getting cases
    private static final int DEFAULT_CASES_LIMIT = 100;

    // Set of allowed states when changing reviewer
    private static final Set<CaseStatus> REVIEWER_CHANGE_ALLOWED_STATES =
            Set.of(CaseStatus.CREATED, CaseStatus.REJECTED);

    // Set of allowed states when approving tasks
    private static final Set<CaseStatus> APPROVE_TASKS_ALLOWED_STATES =
            Set.of(CaseStatus.PENDING_EXTERNAL, CaseStatus.APPROVED);

    @POST
    @Path("cases")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCase(CaseRequest dto) {
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

        // Check for acceptable status code
        if( dto.getStatus() != null ) {
            switch( dto.getStatus() ) {
                case CREATED:  // Default status
                case ASSIGNED: // A check is made later to make sure we can mark the case as assigned
                    break;
                default:
                    LOGGER.info("Attempt to create case with invalid state {}", dto.getStatus());
                    return ServiceErrorDto.InvalidState(String.format(
                            "Case status %s is not allowed when creating a new case", dto.getStatus()));
            }
        }

        // Map foreign entities and create tasks
        List<Subject> subjects;
        Reviewer reviewer;
        Editor editor;
        Editor creator;
        List<PromatTask> tasks;
        ArrayList<String> relatedFausts = new ArrayList<>(dto.getRelatedFausts() == null
                ? new ArrayList<>()
                : dto.getRelatedFausts());
        try {
            // Check that the new case does not use any faustnumbers used on any other active case
            checkValidFaustNumbers(dto);

            // Map simple entities
            subjects = repository.resolveSubjects(dto.getSubjects());
            reviewer = resolveReviewer(dto.getReviewer());
            editor = resolveEditor(dto.getEditor());
            creator = resolveEditor(dto.getCreator());

            // Create tasks (and update related fausts if needed)
            tasks = createTasks(dto.getPrimaryFaust(), dto.getTasks(), relatedFausts);

        } catch(ServiceErrorException serviceErrorException) {
            LOGGER.info("Received serviceErrorException while mapping entities: {}", serviceErrorException.getMessage());
            return Response.status(serviceErrorException.getHttpStatus()).entity(serviceErrorException.getServiceErrorDto()).build();
        }

        // Handle possible change of case status due to assigning an editor.
        //
        // If a reviewer has been assigned, then set or modify the status of the case and the assigned field.
        // If no reviwer is given, check that the status is not ASSIGNED - that would be a mess
        LocalDate assigned = dto.getAssigned() == null ? null : LocalDate.parse(dto.getAssigned());
        CaseStatus status = dto.getStatus() == null ? CaseStatus.CREATED : dto.getStatus();
        if( reviewer != null ) {
            assigned = LocalDate.now();
            status = CaseStatus.ASSIGNED;
        } else {
            if( status.equals(CaseStatus.ASSIGNED) ) {
                LOGGER.info("Attempt to set status ASSIGNED with no reviewer");
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
            .withTasks(tasks)
            .withAuthor(dto.getAuthor())
            .withCreator(creator)
            .withPublisher(dto.getPublisher())
            .withWeekCode(dto.getWeekCode())
            .withFulltextLink(dto.getFulltextLink())
            .withNote(dto.getNote());

            entityManager.persist(entity);
            if (entity.getStatus() == CaseStatus.ASSIGNED) {
                notifyOnReviewerChanged(entity);
            }

            // Set the "are there new Messages?" pins for reviewer and editor
            entity.setNewMessagesToEditor(areThereNewMessages(entity.getId(),
                    PromatMessage.Direction.REVIEWER_TO_EDITOR));
            entity.setNewMessagesToReviewer(areThereNewMessages(entity.getId(),
                    PromatMessage.Direction.EDITOR_TO_REVIEWER));

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
                return ServiceErrorDto.NotFound("Case not found",
                        String.format("Requested case %s does not exist", id));
            }

            // Set the "are there new Messages?" pins for reviewer and editor
            requested.setNewMessagesToEditor(
                    areThereNewMessages(id, PromatMessage.Direction.REVIEWER_TO_EDITOR));
            requested.setNewMessagesToReviewer(
                    areThereNewMessages(id, PromatMessage.Direction.EDITOR_TO_REVIEWER));

            return Response.status(200).entity(requested).build();
        } catch(Exception exception) {
            LOGGER.error("Caught exception: {}", exception.getMessage());
            return ServiceErrorDto.Failed(exception.getMessage());
        }
    }

    @GET
    @Path("cases/{id}/fulltext")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getFulltext(@PathParam("id") final Integer id) {
        LOGGER.info("cases/{}/fulltext", id);

        try {
            final PromatCase promatCase = entityManager.find(PromatCase.class, id);
            if (promatCase == null) {
                return ServiceErrorDto.NotFound("Case not found",
                        String.format("Requested case %s does not exist", id));
            }
            if (promatCase.getFulltextLink() == null) {
                return ServiceErrorDto.InvalidRequest("Unable to get fulltext",
                        String.format("Requested case %s does not contain a fulltext link", id));
            }

            final FulltextHandler fulltextHandler = new FulltextHandler(promatCase.getFulltextLink());
            final StreamingOutput streamingFulltext = fulltextHandler::getFulltext;
            return Response.status(200)
                    .header("Content-Disposition", String.format("attachment; filename=\"%s\"",
                            fulltextHandler.getFilename()))
                    .entity(streamingFulltext).build();
        } catch(Exception exception) {
            LOGGER.error("Caught exception: {}", exception.getMessage());
            return ServiceErrorDto.Failed(exception.getMessage());
        }
    }



    @GET
    @Path("cases/{format}/{faust}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_HTML})
    public Response getView(@PathParam("format") @DefaultValue("HTML") final ClassviewFormat format, @PathParam("faust") final String faust) {
        LOGGER.info("cases/{}/{}", faust, format);

        try {
            TypedQuery query = entityManager.createNamedQuery(PromatCase.GET_CASE_BY_FAUST_NAME, PromatCase.class);
            query.setParameter("faust", faust);
            List<PromatCase> cases = query.getResultList();
            if (cases == null || cases.size() == 0) {
                LOGGER.info("No case with faust {}", faust);
                return ServiceErrorDto.NotFound("No case with this primary- or relatedfaust or no case in required states",
                        String.format("No case with primary- or relatedfaust %s or in required states exists", faust));
            }
            if (cases.size() > 1) {
                LOGGER.error("Too many cases with faust {} ({} cases)", faust, cases.size());
                return ServiceErrorDto.Failed(String.format("Too many cases with primary- or relatedfaust %s exists", faust));
            }
            if (cases.get(0).getTasks() == null || cases.get(0).getTasks().size() == 0) {
                LOGGER.error("Case with faust {} has no tasks", faust);
                return ServiceErrorDto.NotFound("Case has no tasks",
                        String.format("Case with primary- or relatedfaust %s has no tasks", faust));
            }

            // Case must have a status that ensures that there is valid data
            if (!Arrays.asList(CaseStatus.PENDING_EXTERNAL, CaseStatus.APPROVED, CaseStatus.PENDING_MEETING,
                    CaseStatus.PENDING_EXPORT, CaseStatus.EXPORTED).contains(cases.get(0).getStatus())) {
                return ServiceErrorDto.NotFound("Not found or not in valid state",
                        String.format("No case with faust %s or a status that guarantees valid data is found", faust));
            }

            var relatedFausts = new ArrayList<>(cases.get(0).getRelatedFausts());
            relatedFausts.add(cases.get(0).getPrimaryFaust());
            relatedFausts.remove(faust);

            Renderer renderer = new Renderer();
            Map<String, Object> models = new HashMap<>();
            models.put("promatCase", cases.get(0));
            models.put("requestedFaustnumber", faust);
            models.put("relatedFaustnumbers", relatedFausts);

            switch(format) {
                case HTML:
                    String html = renderer.render("promatcase_view_html.jte", models);
                    return Response.status(200)
                            .header("Content-Type", "text/html; charset=utf-8")
                            .entity(html).build();
                case XML:
                    CaseviewXmlTransformer transformer = new CaseviewXmlTransformer();
                    byte[] transformed = transformer.toXml(faust, cases.get(0));
                    return Response.status(200)
                            .header("Content-Type", "text/xml; charset=ISO-8859-1")
                            .entity(transformed).build();
                default:
                    return ServiceErrorDto.Failed(String.format("No handling of CaseviewFormat.", format));
            }
        } catch(Exception exception) {
            LOGGER.error("Caught exception: {}", exception.getMessage());
            return ServiceErrorDto.Failed(exception.getMessage());
        }
    }

    @GET
    @Path("cases")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listCases(@QueryParam("format") @DefaultValue("SUMMARY") final CaseFormat format,
                              @QueryParam("faust") final String faust,
                              @QueryParam("status") final String status,
                              @QueryParam("reviewer") final Integer reviewer,
                              @QueryParam("editor") final Integer editor,
                              @QueryParam("title") final String title,
                              @QueryParam("author") final String author,
                              @QueryParam("trimmedWeekcode") final String trimmedWeekcode,
                              @QueryParam("trimmedWeekcodeOperator") @DefaultValue("EQUAL")
                                  final CriteriaOperator trimmedWeekcodeOperator,
                              @QueryParam("weekCode") final String weekCode,
                              @QueryParam("limit") final Integer limit,
                              @QueryParam("from") final Integer from,
                              @QueryParam("to") final Integer to,
                              @QueryParam("materials") final String materials,
                              @QueryParam("order") final ListCasesParams.Order order) {

        final ListCasesParams listCasesParams = new ListCasesParams()
                .withFaust(faust)
                .withStatus(status)
                .withReviewer(reviewer)
                .withEditor(editor)
                .withTitle(title)
                .withAuthor(author)
                .withWeekCode(weekCode)
                .withTrimmedWeekcode(trimmedWeekcode)
                .withTrimmedWeekcodeOperator(trimmedWeekcodeOperator)
                .withFormat(ListCasesParams.Format.valueOf(format.toString()))
                .withLimit(limit)
                .withFrom(from)
                .withTo(to)
                .withMaterials(materials)
                .withOrder(order);

        LOGGER.info("GET cases/ {}", listCasesParams);

        // Select and return cases
        try {
            final CaseSummaryList caseList = listCases(listCasesParams);

            // Set the "are there new Messages?" pins for reviewer and editor.
            caseList.getCases().forEach(promatCase -> {
                promatCase.setNewMessagesToEditor(
                        areThereNewMessages(promatCase.getId(), PromatMessage.Direction.REVIEWER_TO_EDITOR));
                promatCase.setNewMessagesToReviewer(
                        areThereNewMessages(promatCase.getId(), PromatMessage.Direction.EDITOR_TO_REVIEWER));
            });

            // Return the found cases
            // Note that the http status is set to 404 (NOT FOUND) if no case matched the query
            // this is to allow a quick(er) check for existing cases by using HEAD and checking
            // the statuscode instead of deserializing the response body and looking at numFound
            final ObjectMapper objectMapper = new JsonMapperProvider().getObjectMapper();
            return Response.status(caseList.getNumFound() > 0 ? 200 : 404)
                    .entity(objectMapper.writerWithView(format.getViewClass())
                            .writeValueAsString(caseList)).build();
        } catch (ServiceErrorException e) {
            return Response.status(e.getHttpStatus()).entity(e.getServiceErrorDto()).build();
        } catch(Exception exception) {
            LOGGER.error("Caught exception: {}", exception.getMessage());
            return ServiceErrorDto.Failed(exception.getMessage());
        }
    }

    public CaseSummaryList listCases(ListCasesParams params) throws ServiceErrorException {
        // Initialize query and criteriabuilder
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery criteriaQuery = builder.createQuery();
        final Root<PromatCase> root = criteriaQuery.from(PromatCase.class);
        criteriaQuery.select(root);

        // List of all predicates to be AND'ed together on the final query
        final List<Predicate> allPredicates = new ArrayList<>();

        // Get case with given primary or related
        final String faust = params.getFaust();
        if (faust != null && !faust.isBlank()) {
            final Predicate primaryFaustPredicat = builder.equal(root.get("primaryFaust"), builder.literal(faust));
            final Predicate relatedFaustsPredicat = builder.isTrue(builder.function("JsonbContainsFromString", Boolean.class, root.get("relatedFausts"), builder.literal(faust)));
            final Predicate faustPredicate = builder.or(primaryFaustPredicat, relatedFaustsPredicat);

            // And status not CLOSED or DONE
            final CriteriaBuilder.In<CaseStatus> inClause = builder.in(root.get("status"));
            inClause.value(CaseStatus.CLOSED);
            inClause.value(CaseStatus.EXPORTED);
            inClause.value(CaseStatus.DELETED);
            Predicate statusPredicate = builder.not(inClause);

            allPredicates.add(builder.and(faustPredicate, statusPredicate));
        }

        // Get cases with given set of statuses
        final String status = params.getStatus();
        if (status != null && !status.isBlank()) {
            // Allthough jax.rs actually supports having multiple get arguments with the same name
            // "?status=CREATED&status=ASSIGNED" this is not a safe implementation since other
            // frameworks (React/NextJS or others) may have difficulties handling this. So instead
            // a list of statuses is expected to be given as a comma separated list

            final List<Predicate> statusPredicates = new ArrayList<>();
            for (String oneStatus : status.split(",")) {
                try {
                    statusPredicates.add(builder.equal(root.get("status"), CaseStatus.valueOf(oneStatus)));
                } catch (IllegalArgumentException ex) {
                    final ServiceErrorDto error = new ServiceErrorDto()
                            .withCode(ServiceErrorCode.INVALID_REQUEST)
                            .withCause("Invalid case status")
                            .withDetails(String.format("Unknown case status: %s", oneStatus));
                    throw new ServiceErrorException(error.getCause()).withHttpStatus(400);
                }
            }
            allPredicates.add(builder.or(statusPredicates.toArray(Predicate[]::new)));
        } else {
            // If no status specified: Set a status "not" deleted predicate
            allPredicates.add(builder.notEqual(root.get("status"), CaseStatus.DELETED));
        }

        // Get cases with given reviewer
        final Integer reviewer = params.getReviewer();
        if (reviewer != null && reviewer > 0) {
            allPredicates.add(builder.equal(root.get("reviewer").get("id"), reviewer));
        }

        // Get cases with given editor
        final Integer editor = params.getEditor();
        if (editor != null && editor > 0) {
            allPredicates.add(builder.equal(root.get("editor").get("id"), editor));
        }

        // Get cases with a title that matches (entire, or part of) the given title
        final String title = params.getTitle();
        if (title != null && !title.isBlank()) {
            allPredicates.add(builder
                    .like(builder
                            .lower(root
                                    .get("title")), builder.literal("%" + title.toLowerCase() + "%")));
        }

        // Get cases with an author that matches (entire, or part of) the given author
        final String author = params.getAuthor();
        if (author != null && !author.isBlank()) {
            allPredicates.add(builder
                    .like(builder
                            .lower(root
                                    .get("author")), builder.literal("%" + author.toLowerCase() + "%")));
        }

        final String trimmedWeekcode = params.getTrimmedWeekcode();
        if (trimmedWeekcode != null && !trimmedWeekcode.isBlank()) {
            allPredicates.add(PredicateFactory.fromBinaryOperator(params.getTrimmedWeekcodeOperator(),
                    root.get("trimmedWeekCode"), trimmedWeekcode, builder));
        }

        final String weekCode = params.getWeekCode();
        if (weekCode != null && !weekCode.isBlank()) {
            allPredicates.add(builder.equal(builder.lower(root.get("weekCode")), weekCode.toLowerCase()));
        }

        // Get cases with these (commaseparated) materials
        final String materials = params.getMaterials();
        if (materials != null && !materials.isBlank()) {
            final List<Predicate> materialsPredicates = new ArrayList<>();
            for (String oneMaterial : materials.split(",")) {
                try {
                    materialsPredicates.add(builder.equal(root.get("materialType"), MaterialType.valueOf(oneMaterial)));
                } catch (IllegalArgumentException ex) {
                    final ServiceErrorDto error = new ServiceErrorDto()
                            .withCode(ServiceErrorCode.INVALID_REQUEST)
                            .withCause("Invalid material type")
                            .withDetails(String.format("Unknown material: %s", oneMaterial));
                    throw new ServiceErrorException(error.getCause()).withHttpStatus(400);
                }
            }
            allPredicates.add(builder.or(materialsPredicates.toArray(Predicate[]::new)));
        }

        // If a starting id has been given, add this
        final Integer from = params.getFrom();
        if (from != null) {
            allPredicates.add(builder.gt(root.get("id"), builder.literal(from)));
        }

        // If an ending id has been given, add this
        final Integer to = params.getTo();
        if (to != null) {
            allPredicates.add(builder.lt(root.get("id"), builder.literal(to)));
        }

        // Combine all where clauses together with AND and add them to the query
        if (allPredicates.size() > 0) {
            Predicate finalPredicate = builder.and(allPredicates.toArray(Predicate[]::new));
            criteriaQuery.where(finalPredicate);
        }

        // Add ordering
        ListCasesParams.Order order = params.getOrder();
        if( order == ListCasesParams.Order.DESCENDING ) {
            criteriaQuery.orderBy(builder.desc(root.get("id")));
        } else {
            criteriaQuery.orderBy(builder.asc(root.get("id")));
        }

        // Add limits
        final TypedQuery<PromatCase> query = entityManager.createQuery(criteriaQuery);
        query.setMaxResults(params.getLimit() == null ? DEFAULT_CASES_LIMIT : params.getLimit());

        // Execute the query
        // TODO: 12/01/2021 Rename CaseSummaryList to CaseList
        final CaseSummaryList caseList = new CaseSummaryList();
        caseList.getCases().addAll(query.getResultList());
        caseList.setNumFound(caseList.getCases().size());
        return caseList;
    }


    @POST
    @Path("cases/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response patchCase(@PathParam("id") final Integer id, CaseRequest dto) {
        LOGGER.info("cases/{} (POST) body: {}", id, dto);

        repository.getExclusiveAccessToTable(PromatCase.TABLE_NAME);
        repository.getExclusiveAccessToTable(PromatTask.TABLE_NAME);
        PromatCase existing = null;

        try {

            // Update strategy is effectively a PATCH update, only a selected number of fields
            // can be updated on a case, some forbidden fields will trigger an error but most
            // fields not accepted will be silently ignored.

            // Errorchecking when trying to update fields managed by the backend solely
            if(dto.getAssigned() != null) {
                LOGGER.info("Attempt to set 'assigned' on case {}", id);
                return ServiceErrorDto.InvalidRequest("Forbidden field", "Setting the value of 'assigned' is not allowed");
            }

            // Fetch an existing entity with the given id
            existing = entityManager.find(PromatCase.class, id);
            if( existing == null ) {
                LOGGER.info("No such case {}", id);
                return ServiceErrorDto.NotFound("No such case", String.format("Case with id %d does not exist", id));
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
                    return ServiceErrorDto.FaustInUse(String.format("Case with primary or related faust %s and status not DONE or CLOSED exists", dto.getPrimaryFaust()));
                }
                existing.setPrimaryFaust(dto.getPrimaryFaust());
            }
            if(dto.getRelatedFausts() != null) {
                if(!Faustnumbers.checkNoOpenCaseWithFaust(entityManager, existing.getId(), dto.getRelatedFausts().toArray(String[]::new))) {
                    LOGGER.info("Case with primary or related faust {} and state <> CLOSED|DONE exists", dto.getPrimaryFaust());
                    return ServiceErrorDto.FaustInUse(String.format("Case with primary or related fausts %s and status not DONE or CLOSED exists", dto.getRelatedFausts()));
                }
                existing.setRelatedFausts(dto.getRelatedFausts());
            }
            if(dto.getNote() != null) {
                existing.setNote(dto.getNote());
            }
            if(dto.getReviewer() != null) {
                Integer reviewer_id = existing.getReviewer() == null ? null : existing.getReviewer().getId();
                if (!dto.getReviewer().equals(reviewer_id)) {
                    if(REVIEWER_CHANGE_ALLOWED_STATES.contains(existing.getStatus())) {
                        existing.setReviewer(resolveReviewer(dto.getReviewer()));
                        notifyOnReviewerChanged(existing);
                        existing.setStatus(calculateStatus(existing, CaseStatus.ASSIGNED));
                    } else {
                        throw new ServiceErrorException("Not allowed to set status ASSIGNED when case is not in CREATED, REJECTED or nor reviewer is set")
                                .withDetails("Attempt to set status of case to ASSIGNED when case is not in status CREATED, REJECTED or there is no reviewer set")
                                .withHttpStatus(400)
                                .withCode(ServiceErrorCode.INVALID_REQUEST);
                    }
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
                CaseStatus status = calculateStatus(existing, dto.getStatus());
                if(status == CaseStatus.PENDING_CLOSE) {
                    approveBkmTasks(existing);
                } else if(APPROVE_TASKS_ALLOWED_STATES.contains(status)) {
                    approveTasks(existing);
                }
                existing.setStatus(status);

                // If status changed to PENDING_EXPORT, the case must be enriched with a new faustnumber
                if( status == CaseStatus.PENDING_EXPORT ) {
                    assignFaustnumber(existing);
                }
            }
            if(dto.getCreator() != null) {
                if (existing.getCreator() != null && !existing.getCreator().getId().equals(dto.getCreator())) {
                    return ServiceErrorDto.Forbidden("Change in 'creator'","'Creator' cannot be changed, once set.");
                } else {
                    existing.setCreator(resolveEditor(dto.getCreator()));
                }
            }
            if(dto.getWeekCode() != null) {
                existing.setWeekCode(dto.getWeekCode());
            }
            if(dto.getAuthor() != null) {
                existing.setAuthor(dto.getAuthor());
            }
            if(dto.getPublisher() != null) {
                existing.setPublisher(dto.getPublisher());
            }
            if (dto.getFulltextLink() != null) {
                existing.setFulltextLink(dto.getFulltextLink());
            }

            // Set the "are there new Messages?" pins for reviewer and editor
            existing.setNewMessagesToReviewer(areThereNewMessages(existing.getId(), PromatMessage.Direction.EDITOR_TO_REVIEWER));
            existing.setNewMessagesToEditor(areThereNewMessages(existing.getId(), PromatMessage.Direction.REVIEWER_TO_EDITOR));
            return Response.ok(existing).build();
        } catch(ServiceErrorException serviceErrorException) {
            LOGGER.info("Received serviceErrorException while mapping entities: {}", serviceErrorException.getMessage());

            // Some type of illegal update was Detected. All changes must be rolled back.
            // SO detach case from entitymanager, and thereby in effect do a rollback.
            entityManager.detach(existing);
            return Response.status(serviceErrorException.getHttpStatus()).entity(serviceErrorException.getServiceErrorDto()).build();
        } catch(Exception exception) {
            LOGGER.error("Caught exception: {}", exception.getMessage());

            // Updating the case went wrong for some other reason. All changes must be rolled back.
            // SO detach case from entitymanager, and thereby in effect do a rollback.
            entityManager.detach(existing);

            return ServiceErrorDto.Failed(exception.getMessage());
        }
    }

    @POST
    @Path("cases/{id}/tasks")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addTask(@PathParam("id") final Integer id, TaskDto dto) {
        LOGGER.info("cases/{}/tasks (POST) body: {}", id, dto);

        // Lock case and task tables
        repository.getExclusiveAccessToTable(PromatCase.TABLE_NAME);
        repository.getExclusiveAccessToTable(PromatTask.TABLE_NAME);

        try {

            // Make sure we got all required fields
            validateTaskDto(dto);

            // Fetch the case
            PromatCase promatCase = entityManager.find(PromatCase.class, id);
            if(promatCase == null) {
                LOGGER.info("No case with id {}", id);
                return ServiceErrorDto.NotFound("No such case", String.format("No case with id %d exists", id));
            }

            // Check that we do not add any targetfausts that is in use
            if(dto.getTargetFausts() != null) {
                if(!Faustnumbers.checkNoOpenCaseWithFaust(entityManager, promatCase.getId(), dto.getTargetFausts().toArray(String[]::new))) {
                    LOGGER.info("Case contains a task with one or more targetFaust {} used by other active cases", dto.getTargetFausts());
                    return ServiceErrorDto.FaustInUse("One or more targetFausts is used by other active cases");
                }
            }

            // Create the new task
            PromatTask task = new PromatTask()
                    .withTaskType(dto.getTaskType())
                    .withTaskFieldType(dto.getTaskFieldType())
                    .withData(dto.getData())  // null is allowed here since it is the default value anyway
                    .withCreated(LocalDate.now())
                    .withPayCategory(Repository.getPayCategoryForTaskFieldTypeOfTaskType(dto.getTaskType(), dto.getTaskFieldType()))
                    .withTargetFausts(dto.getTargetFausts());

            // Add the new task
            promatCase.getTasks().add(task);

            // Add new targetfausts
            if(dto.getTargetFausts() != null && dto.getTargetFausts().size() > 0) {
                for(String faust : dto.getTargetFausts()) {
                    if(!promatCase.getRelatedFausts().contains(faust)) {
                        promatCase.getRelatedFausts().add(faust);
                    }
                }
            }

            return Response.status(201)
                    .entity(task)
                    .build();
        } catch(ServiceErrorException serviceErrorException) {
            LOGGER.info("Received serviceErrorException while mapping entities: {}", serviceErrorException.getMessage());
            return Response.status(serviceErrorException.getHttpStatus()).entity(serviceErrorException.getServiceErrorDto()).build();
        } catch(Exception exception) {
            LOGGER.error("Caught exception: {}", exception.getMessage());
            return ServiceErrorDto.Failed(exception.getMessage());
        }
    }

    @DELETE
    @Path("cases/{id}")
    public Response deleteCase(@PathParam("id") final Integer id) {
        repository.getExclusiveAccessToTable(PromatCase.TABLE_NAME);

        // Fetch the case
        PromatCase promatCase = entityManager.find(PromatCase.class, id);
        if(promatCase == null) {
            LOGGER.info("No case with id {}", id);
            return ServiceErrorDto.NotFound("No such case", String.format("No case with id %d exists", id));
        }

        promatCase.setStatus(CaseStatus.DELETED);
        return Response.ok().build();
    }

    @POST
    @Path("drafts")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @JsonView({CaseView.Summary.class})
    public Response addDraftOrUpdateExistingCase(CaseRequest caseRequest) throws Exception {
        LOGGER.info("POST /drafts: {}", caseRequest);

        if (caseRequest.getPrimaryFaust() == null || caseRequest.getPrimaryFaust().isBlank()) {
            return ServiceErrorDto.InvalidRequest("Missing required field in the request data",
                    "Field 'primaryFaust' must be supplied when creating a new case draft");
        }
        if (caseRequest.getReviewer() != null) {
            return ServiceErrorDto.InvalidRequest("Illegal value in request",
                    "Drafts cannot be created with an assigned reviewer");
        }

        final Dto dto = records.resolveId(caseRequest.getPrimaryFaust());
        if (!(dto instanceof RecordsListDto)) {
            return Response.status(400).entity(dto).build();
        }

        final RecordsListDto recordsList = (RecordsListDto) dto;
        final List<RecordDto> relatedRecords = recordsList.getNumFound() == 0 ?
                Collections.emptyList() : recordsList.getRecords();
        final Set<PromatCase> existingCases = new HashSet<>();

        for (RecordDto relatedRecord : relatedRecords) {
            // Lookup existing open cases...
            final String faust = relatedRecord.getFaust();
            if (faust != null && !faust.isBlank()) {
                final CaseSummaryList caseList = listCases(new ListCasesParams()
                        .withFaust(relatedRecord.getFaust()));
                if (caseList.getNumFound() > 0) {
                    existingCases.addAll(caseList.getCases());
                }
            }
        }

        if (existingCases.isEmpty()) {
            // No matching cases found, create new case draft...

            // force case draft status to CREATED
            caseRequest.setStatus(CaseStatus.CREATED);
            return createCase(caseRequest);
        }

        if (existingCases.size() > 1) {
            final ServiceErrorDto error = new ServiceErrorDto()
                    .withCode(ServiceErrorCode.FAUST_IN_USE)
                    .withCause(String.format("multiple case candidates found for faust number %s",
                            caseRequest.getPrimaryFaust()))
                    .withDetails(existingCases.stream()
                            .map(PromatCase::getId)
                            .collect(Collectors.toList())
                            .toString());
            return Response.status(409).entity(error).build();
        }

        // Update existing case...

        repository.getExclusiveAccessToTable(PromatCase.TABLE_NAME);

        final PromatCase existingCase = existingCases.iterator().next();

        // Race condition check
        if (!Faustnumbers.checkNoOpenCaseWithFaust(entityManager, existingCase.getId(), caseRequest.getPrimaryFaust())) {
            return ServiceErrorDto.FaustInUse(String.format(
                    "Case with faust number %s already exists", caseRequest.getPrimaryFaust()));
        }

        entityManager.refresh(existingCase);
        if (!existingCase.getPrimaryFaust().equals(caseRequest.getPrimaryFaust())) {
            if (existingCase.getRelatedFausts() == null) {
                existingCase.setRelatedFausts(List.of(caseRequest.getPrimaryFaust()));
                LOGGER.info("Set related fausts for existing case {}: {}",
                        existingCase.getId(), existingCase.getRelatedFausts());
            } else if (!existingCase.getRelatedFausts().contains(caseRequest.getPrimaryFaust())) {
                final ArrayList<String> relatedFausts = new ArrayList<>(existingCase.getRelatedFausts());
                relatedFausts.add(caseRequest.getPrimaryFaust());
                existingCase.setRelatedFausts(relatedFausts);
                LOGGER.info("Updated related fausts for existing case {}: {}",
                        existingCase.getId(), existingCase.getRelatedFausts());
            }
        }
        existingCase.setFulltextLink(caseRequest.getFulltextLink());
        LOGGER.info("Updated existing case {}", existingCase.getId());

        return Response.status(200).entity(existingCase).build();
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
                    .withDetails(String.format("Field 'reviewer' contains user id %d which does not exist", reviewerId))
                    .withHttpStatus(400);
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
                    .withDetails(String.format("Field 'editor' contains user id %d which does not exist", editorId))
                    .withHttpStatus(400);
        }
        return editor;
    }

    private List<PromatTask> createTasks(String primaryFaust, List<TaskDto> taskDtos, List<String> relatedFausts) throws ServiceErrorException {
        ArrayList<PromatTask> tasks = new ArrayList<>();

        if( taskDtos != null && taskDtos.size() > 0) {
            for(TaskDto task : taskDtos) {
                validateTaskDto(task);

                tasks.add(new PromatTask()
                        .withTaskType(task.getTaskType())
                        .withTaskFieldType(task.getTaskFieldType())
                        .withPayCategory(
                                Repository.getPayCategoryForTaskFieldTypeOfTaskType(task.getTaskType(), task.getTaskFieldType()))
                        .withCreated(LocalDate.now())
                        .withTargetFausts(task.getTargetFausts() == null ? null : task.getTargetFausts()));

                if( task.getTargetFausts() != null ) {
                    for(String faust : task.getTargetFausts() ) {
                        if(!relatedFausts.contains(faust) && !faust.equals(primaryFaust)) {
                            relatedFausts.add(faust);
                        }
                    }
                }
            }
        }

        return tasks;
    }

    private void validateTaskDto(TaskDto dto) throws ServiceErrorException {
        if(dto.getTaskType() == null || dto.getTaskFieldType() == null || dto.getTargetFausts() == null || dto.getTargetFausts().size() == 0) {
            LOGGER.info("Task dto is missing the taskType and/or taskFieldType field");
            throw new ServiceErrorException("Task dto is missing the taskType and/or taskFieldType field")
                    .withCause("Missing required field(s) in the request data")
                    .withDetails("Fields 'taskType' and 'taskFieldType' must be supplied when creating a new task")
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withHttpStatus(400);
        }
    }

    private void checkValidFaustNumbers(CaseRequest dto) throws ServiceErrorException {

        // Check that no existing case exists with the same primary or related faustnumber
        // as the given primary faustnumber and a state other than CLOSED or DONE
        if(!Faustnumbers.checkNoOpenCaseWithFaust(entityManager, dto.getPrimaryFaust())) {
            LOGGER.info("Case with primary or related Faust {} and state <> CLOSED|DONE exists", dto.getPrimaryFaust());
            throw new ServiceErrorException(String.format("Case with primary or related faust %s and status not DONE or CLOSED exists", dto.getPrimaryFaust()))
                    .withHttpStatus(409)
                    .withCode(ServiceErrorCode.FAUST_IN_USE)
                    .withCause("Faustnumber is in use")
                    .withDetails(String.format("Case with primary or related faust %s and status not DONE or CLOSED exists", dto.getPrimaryFaust()));
        }

        // Check that no existing case exists with the same primary or related faustnumber
        // as the given relasted fasutnumbers and a state other than CLOSED or DONE
        if(dto.getRelatedFausts() != null && dto.getRelatedFausts().size() > 0) {
            if(!Faustnumbers.checkNoOpenCaseWithFaust(entityManager, dto.getRelatedFausts().toArray(String[]::new))) {
                LOGGER.info("Case with primary or related {} and state <> CLOSED|DONE exists", dto.getRelatedFausts());
                throw new ServiceErrorException(String.format("Case with primary or related faust %s and status not DONE or CLOSED exists", dto.getRelatedFausts()))
                        .withHttpStatus(409)
                        .withCode(ServiceErrorCode.FAUST_IN_USE)
                        .withCause("Faustnumber is in use")
                        .withDetails(String.format("Case with primary or related faust %s and status not DONE or CLOSED exists", dto.getRelatedFausts()));
            }
        }

        // Check that no existing case exists with the same primary or related faustnumber
        // as the given faustnumbers on any task, and a state other than CLOSED or DONE
        if( dto.getTasks() != null) {
            for(TaskDto task : dto.getTasks()) {
                if(task.getTargetFausts() != null) {
                    if(!Faustnumbers.checkNoOpenCaseWithFaust(entityManager, task.getTargetFausts().toArray(String[]::new))) {
                        LOGGER.info("Case contains a task with one or more targetFaust {} used by other active cases", task.getTargetFausts());
                        throw new ServiceErrorException(String.format("Case contains tasks with one or more targetfausts %s used by other active cases", task.getTargetFausts()))
                                .withHttpStatus(409)
                                .withCode(ServiceErrorCode.FAUST_IN_USE)
                                .withCause("Faustnumber is in use")
                                .withDetails(String.format("Case contains tasks with one or more targetfausts %s used by other active cases", task.getTargetFausts()));
                    }
                }
            }
        }
    }

    private void notifyOnReviewerChanged(PromatCase promatCase)
            throws NotificationFactory.ValidateException, OpenFormatConnectorException {

        if (promatCase.getId() == null) {
            entityManager.flush();
        }

        Notification notification = notificationFactory
                .notificationOf(new AssignReviewer().withPromatCase(promatCase).withNote(promatCase.getNote()));
        PromatMessage message = new PromatMessage()
                .withCaseId(promatCase.getId())
                .withMessageText(notification.getBodyText())
                .withDirection(PromatMessage.Direction.EDITOR_TO_REVIEWER)
                .withAuthor(
                        PromatMessage.Author.fromPromatUser(promatCase.getEditor())
                )
                .withCreated(LocalDateTime.now())
                .withIsRead(Boolean.FALSE);
        entityManager.persist(notification);
        entityManager.persist(message);
    }

    private CaseStatus calculateStatus(PromatCase existing, CaseStatus proposedStatus) throws ServiceErrorException {
        switch(proposedStatus) {

            case CLOSED:
                return CaseStatus.CLOSED;

            case EXPORTED:
                if (existing.getStatus() != CaseStatus.PENDING_EXPORT) {
                    throw new ServiceErrorException("Not allowed to set status EXPORTED when case is not in PENDING_EXPORT")
                            .withDetails("Attempt to set status of case to EXPORTED when case is not in status PENDING_EXPORT")
                            .withHttpStatus(400)
                            .withCode(ServiceErrorCode.INVALID_REQUEST);
                }
                return CaseStatus.EXPORTED;

            case REVERTED:
                if (existing.getStatus() != CaseStatus.PENDING_REVERT) {
                    throw new ServiceErrorException("Not allowed to set status REVERTED when case is not in pending revert")
                            .withDetails("Attempt to set status of case to REVERTED when case is not in status PENDING_REVERT")
                            .withHttpStatus(400)
                            .withCode(ServiceErrorCode.INVALID_REQUEST);
                }
                return CaseStatus.REVERTED;

            case CREATED:
                if (existing.getReviewer() != null) {
                    return CaseStatus.ASSIGNED;
                } else {
                    return CaseStatus.CREATED;
                }

            case ASSIGNED:
                if (existing.getReviewer() != null && Set.of(CaseStatus.CREATED, CaseStatus.REJECTED).contains(existing.getStatus())) {
                    return CaseStatus.ASSIGNED;
                } else {
                    throw new ServiceErrorException("Not allowed to set status ASSIGNED when case is not in CREATED, REJECTED or nor reviewer is set")
                            .withDetails("Attempt to set status of case to ASSIGNED when case is not in status CREATED, REJECTED or there is no reviewer set")
                            .withHttpStatus(400)
                            .withCode(ServiceErrorCode.INVALID_REQUEST);
                }

            case REJECTED:
                if (existing.getStatus() == CaseStatus.ASSIGNED) {
                    return CaseStatus.REJECTED;
                } else {
                    throw new ServiceErrorException("Not allowed to set status REJECTED when case is not in ASSIGNED")
                            .withDetails("Attempt to set status of case to REJECTED when case is not in status ASSIGNED")
                            .withHttpStatus(400)
                            .withCode(ServiceErrorCode.INVALID_REQUEST);
                }

            case PENDING_CLOSE:
                return CaseStatus.PENDING_CLOSE;

            case APPROVED:
                if (existing.getStatus() != CaseStatus.PENDING_APPROVAL) {
                    throw new ServiceErrorException("Not allowed to set status APPROVED when case is not in PENDING_APPROVAL")
                            .withDetails("Attempt to set status of case to APPROVED when case is not in status PENDING_APPROVAL")
                            .withHttpStatus(400)
                            .withCode(ServiceErrorCode.INVALID_REQUEST);
                }


                if (existing.getTasks().stream()
                        .filter(task -> task.getTaskFieldType() == TaskFieldType.METAKOMPAS && task.getApproved() == null)
                        .count() != 0) {
                    return CaseStatus.PENDING_EXTERNAL;
                } else {
                    return CaseStatus.APPROVED;
                }


            case PENDING_APPROVAL:
                if (existing.getStatus() != CaseStatus.ASSIGNED && existing.getStatus() != CaseStatus.PENDING_ISSUES) {
                    throw new ServiceErrorException("Not allowed to set status PENDING_APPROVAL when case is not assigned")
                            .withDetails("Attempt to set status of case to PENDING_APPROVAL when case is not in status ASSIGNED")
                            .withHttpStatus(400)
                            .withCode(ServiceErrorCode.INVALID_REQUEST);
                }
                return CaseStatus.PENDING_APPROVAL;

            case PENDING_ISSUES:
                if (existing.getStatus() != CaseStatus.PENDING_APPROVAL) {
                    throw new ServiceErrorException("Not allowed to set status PENDING_ISSUES when case is not in PENDING_APPROVAL")
                            .withDetails("Attempt to set status of case to PENDING_ISSUES when case is not in status PENDING_APPROVAL")
                            .withHttpStatus(400)
                            .withCode(ServiceErrorCode.INVALID_REQUEST);
                }
                return CaseStatus.PENDING_ISSUES;

            case PENDING_EXPORT:
                if (existing.getStatus() != CaseStatus.EXPORTED && existing.getStatus() != CaseStatus.PENDING_MEETING && existing.getStatus() != CaseStatus.APPROVED ) {
                    throw new ServiceErrorException("Not allowed to set status PENDING_EXPORT when case is not in EXPORTED, PENDING_MEETING or APPROVED")
                            .withDetails("Attempt to set status of case to PENDING_EXPORT when case is not in status EXPORTED, PENDING_MEETING or APPROVED")
                            .withHttpStatus(400)
                            .withCode(ServiceErrorCode.INVALID_REQUEST);
                }
                return CaseStatus.PENDING_EXPORT;

            case PENDING_MEETING:
                if (existing.getStatus() != CaseStatus.APPROVED) {
                    throw new ServiceErrorException("Not allowed to set status PENDING_MEETING when case is not approved")
                            .withDetails("Attempt to set status of case to PENDING_MEETING when case is not in status APPROVED")
                            .withHttpStatus(400)
                            .withCode(ServiceErrorCode.INVALID_REQUEST);
                }
                return CaseStatus.PENDING_MEETING;

            case PENDING_REVERT:
                if (existing.getStatus() != CaseStatus.EXPORTED) {
                    throw new ServiceErrorException("Not allowed to set status PENDING_REVERT when case is not exported")
                            .withDetails("Attempt to set status of case to PENDING_REVERT when case is not in status EXPORTED")
                            .withHttpStatus(400)
                            .withCode(ServiceErrorCode.INVALID_REQUEST);
                }
                return CaseStatus.PENDING_REVERT;

            default:
                throw new ServiceErrorException("Unknown or forbidden status")
                        .withDetails(String.format("Attempt to set status %s on case %d which is forbidden or impossible",
                                proposedStatus.name(), existing.getId()))
                        .withHttpStatus(400)
                        .withCode(ServiceErrorCode.INVALID_REQUEST);
        }
    }

    private void approveTasks(PromatCase existing) {
        for (PromatTask task : new ArrayList<>(existing.getTasks())) {
            if (task.getTaskFieldType() != TaskFieldType.METAKOMPAS) {
                task.setApproved(LocalDate.now());
            }
        }
    }

    private void approveBkmTasks(PromatCase existing) {
        for (PromatTask task : existing.getTasks()) {
            if (task.getTaskFieldType().equals(TaskFieldType.BKM)) {
                task.setApproved(LocalDate.now());
            }
        }
    }

    private Boolean areThereNewMessages(Integer caseId, PromatMessage.Direction direction) {
        TypedQuery<Integer> query =
                entityManager.createNamedQuery(PromatMessage.GET_NEWS_FOR_CASE, Integer.class);
        query.setParameter("direction", direction);
        query.setParameter("caseId", caseId);

        return query.getResultList().size() > 0;
    }

    private void assignFaustnumber(PromatCase existing) throws OpennumberRollConnectorException {
        for(PromatTask task : existing.getTasks().stream()
                .filter(task -> task.getTaskFieldType() == TaskFieldType.BRIEF)
                .collect(Collectors.toList())) {
            if( task.getRecordId() == null || task.getRecordId().isEmpty() ) {
                OpennumberRollConnector.Params params = new OpennumberRollConnector.Params();
                params.withRollName(openNumberrollRollName);
                task.setRecordId(opennumberRollConnector.getId(params));
                LOGGER.info("Assigned new faustnumber {} to task with id {} on case with id {}", task.getRecordId(), task.getRecordId(), existing.getId());
            }
        }
    }
}
