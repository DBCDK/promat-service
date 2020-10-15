package dk.dbc.promat.service.api;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.promat.service.dto.RecordDto;
import dk.dbc.promat.service.dto.RecordsListDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;

@Stateless
@Path("records")
public class Records {
    private static final Logger LOGGER = LoggerFactory.getLogger(Records.class);
    private final JSONBContext jsonbContext = new JSONBContext();

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRecords(@PathParam("id") final String id) throws JSONBException {
        LOGGER.info("getRecords/{}", id);

        // Todo: resolve id and build list of real faust numbers
        RecordsListDto recordsListDto = new RecordsListDto()
                .withNumFound(1)
                .withRecords(Arrays.asList(new RecordDto()
                        .withFaust(id)
                        .withPrimary(true)));

        return Response.ok(jsonbContext.marshall(recordsListDto)).build();
    }
}
