package dk.dbc.promat.service.api;

import dk.dbc.promat.service.persistence.Case;
import dk.dbc.promat.service.dto.CaseRequestDto;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.PromatEntityManager;
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
    public Response getRecords(CaseRequestDto dto) {
        LOGGER.info("cases/{}", dto);

        // Todo: Check if there is any existing cases for the same primary faustnumber
        //       not in status CLOSED or DONE

        try {
            Case entity = new Case()
            .withTitle(dto.getTitle())
            .withDetails(dto.getDetails())
            .withPrimaryFaust(dto.getPrimaryFaust())
            .withRelatedFausts(Arrays.asList(dto.getRelatedFausts()))
            .withReviewer(dto.getReviewer())
            .withSubjects(Arrays.asList(dto.getSubjects()))
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
            LOGGER.info("Caught exception: {}", exception.getMessage());
            throw exception;
        }
    }
}
