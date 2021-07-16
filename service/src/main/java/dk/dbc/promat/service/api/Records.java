/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.Dto;
import dk.dbc.promat.service.dto.RecordsListDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@Path("records")
public class Records {
    private static final Logger LOGGER = LoggerFactory.getLogger(Records.class);

    @Inject
    public RecordsResolver recordsResolver;

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRecords(@PathParam("id") final String id) throws Exception {
        LOGGER.info("getRecords/{}", id);

        // Find every record with that belongs to any work that matches the given id
        try {
            Dto dto = recordsResolver.resolveId(id);
            if(dto.getClass().equals(RecordsListDto.class)) {
                return Response.ok(dto).build();
            }
            return Response.status(400).entity(dto).build();
        } catch(Exception exception) {
            LOGGER.info("Caught exception: {}", exception.getMessage());
            throw exception;
        }
    }
}
