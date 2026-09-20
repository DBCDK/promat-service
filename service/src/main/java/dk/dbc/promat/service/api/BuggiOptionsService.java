package dk.dbc.promat.service.api;

import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

// Buggi's option vocabulary (4 groups, 23 options total) has no external source - nothing
// syncs it and no admin screen edits it - so it's hardcoded directly here.
@Stateless
@Path("buggi")
public class BuggiOptionsService {

    @GET
    @Path("options")
    @Produces("application/json")
    public Response getOptions() {
        return Response.ok().entity(BuggiVocabulary.groups()).build();
    }
}
