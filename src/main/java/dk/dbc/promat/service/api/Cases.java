package dk.dbc.promat.service.api;

import dk.dbc.invariant.InvariantUtil;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.Case;
import dk.dbc.promat.service.dto.CaseRequestDto;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.Subject;
import dk.dbc.promat.service.persistence.Task;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

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
            ServiceErrorDto err = new ServiceErrorDto()
                    .withCode(ServiceErrorCode.INVALID_REQUEST_BODY)
                    .withCause("Missing required field in the request data")
                    .withDetails("Field 'title' must be supplied when creating a new case");
            return Response.status(400).entity(err).build();
        }
        if( dto.getPrimaryFaust() == null || dto.getPrimaryFaust().isEmpty() ) {
            ServiceErrorDto err = new ServiceErrorDto()
                    .withCode(ServiceErrorCode.INVALID_REQUEST_BODY)
                    .withCause("Missing required field in the request data")
                    .withDetails("Field 'primaryFaust' must be supplied when creating a new case");
            return Response.status(400).entity(err).build();
        }

        // Todo: Check if there is any existing cases for the same primary faustnumber
        //       not in status CLOSED or DONE

        // Map subject ids to existing subjects
        ArrayList<Subject> subjects = new ArrayList<>();
        for(int subjectId : dto.getSubjects()) {
            Subject subject = entityManager.find(Subject.class, subjectId);
            if( subject == null ) {
                ServiceErrorDto err = new ServiceErrorDto()
                        .withCode(ServiceErrorCode.INVALID_REQUEST_BODY)
                        .withCause("No such subject")
                        .withDetails(String.format("Field 'subject' contains id {} which does not exist", subjectId));
                return Response.status(400).entity(err).build();
            }
            subjects.add(subject);
        }

        // Create case
        try {
            Case entity = new Case()
            .withTitle(dto.getTitle())
            .withDetails(dto.getDetails())
            .withPrimaryFaust(dto.getPrimaryFaust())
            .withRelatedFausts(Arrays.asList(dto.getRelatedFausts()))
            .withReviewer(dto.getReviewer())
            .withSubjects(subjects)
            .withCreated(LocalDate.now())
            .withDeadline(LocalDate.parse(dto.getDeadline()))
            .withAssigned(LocalDate.parse(dto.getAssigned()))
            .withStatus(CaseStatus.CREATED)
            .withMaterialType(dto.getMaterialType())
            .withTasks(new ArrayList<>());

            entityManager.persist(entity);

            // 201 CREATED
            return Response.status(201)
                    .entity(entity)
                    .build();
        } catch(Exception exception) {
            LOGGER.error("Caught exception: {}", exception.getMessage());
            throw exception;
        }
    }
}
