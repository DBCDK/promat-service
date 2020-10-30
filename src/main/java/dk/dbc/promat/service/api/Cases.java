package dk.dbc.promat.service.api;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.promat.service.persistence.Case;
import dk.dbc.promat.service.dto.CaseRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@Path("cases")
public class Cases {
    private static final Logger LOGGER = LoggerFactory.getLogger(Cases.class);
    private final JSONBContext jsonbContext = new JSONBContext();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRecords(CaseRequestDto dto) throws Exception {
        LOGGER.info("cases/{}", dto);  //Todo: add proper toString method in the dto

        try {
            // Todo: Create and persist entity with case
            Case entity = new Case();

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
