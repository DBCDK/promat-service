package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.Dto;
import dk.dbc.promat.service.dto.RecordsListDto;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.Case;
import dk.dbc.promat.service.dto.CaseRequestDto;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;

@Stateless
@Path("cases")
public class Cases {
    private static final Logger LOGGER = LoggerFactory.getLogger(Cases.class);

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @POST
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

        // Check that no existing case exists with the same primary faustnumber
        // and a state other than CLOSED or DONE
        Query q = entityManager.createNativeQuery("SELECT CheckNoOpenCaseWithFaust(?)");
        q.setParameter(1, dto.getPrimaryFaust());
        if((boolean) q.getSingleResult() == false) {
            LOGGER.error("Case with primaryFaust {} and state <> CLOSED|DONE exists", dto.getPrimaryFaust());
            ServiceErrorDto err = new ServiceErrorDto()
                    .withCode(ServiceErrorCode.CASE_EXISTS)
                    .withCause("Case exists")
                    .withDetails(String.format("Case with primary faust {} and status not DONE or CLOSED exists", dto.getPrimaryFaust()));
            return Response.status(409).entity(err).build();
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

        // Map reviewer id to existing reviewer
        Reviewer reviewer = null;
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
        }

        // Create case
        try {
            Case entity = new Case()
            .withTitle(dto.getTitle())
            .withDetails(dto.getDetails() == null ? "" : dto.getDetails())
            .withPrimaryFaust(dto.getPrimaryFaust())
            .withRelatedFausts(Arrays.asList(dto.getRelatedFausts() == null ? new String[0] : dto.getRelatedFausts()))
            .withReviewer(reviewer)
            .withSubjects(subjects)
            .withCreated(LocalDate.now())
            .withDeadline(dto.getDeadline() == null ? null : LocalDate.parse(dto.getDeadline()))
            .withAssigned(dto.getAssigned() == null ? null : LocalDate.parse(dto.getAssigned()))
            .withStatus(dto.getStatus() == null ? CaseStatus.CREATED : dto.getStatus())
            .withMaterialType(dto.getMaterialType())
            .withTasks(new ArrayList<>());

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
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCase(@PathParam("id") final Integer id) throws Exception {
        LOGGER.info("getCase/{}", id);

        // Find and return the requested case
        try {

            Case requested = entityManager.find(Case.class, id);
            if( requested == null ) {
                LOGGER.info("Requested case {} does not exist", id);
                return Response.status(204).build();
            }

            return Response.status(200).entity(requested).build();
        } catch(Exception exception) {
            LOGGER.info("Caught exception: {}", exception.getMessage());
            throw exception;
        }
    }
}
