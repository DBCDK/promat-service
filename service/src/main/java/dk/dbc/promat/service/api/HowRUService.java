package dk.dbc.promat.service.api;

import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("")
@Stateless
public class HowRUService {
    @GET
    @Path("howru")
    @Produces({MediaType.APPLICATION_JSON})
    public Response howRU() { return Response.ok().build(); }
}
