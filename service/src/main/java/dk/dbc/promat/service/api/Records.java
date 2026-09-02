package dk.dbc.promat.service.api;

import dk.dbc.promat.service.connectors.FaustResolverException;
import dk.dbc.promat.service.connectors.FbiApiConnectorException;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

// @Path("records") on the class + @Path(...) on each method combine into
// the full route, e.g. GET /v1/api/records/{id} and GET /v1/api/records/search
// (the /v1/api prefix is configured elsewhere, in PromatApplication). This
// class is intentionally thin: it only does HTTP-shaped things (read query
// params, turn exceptions into a 400 response) and immediately hands off
// to RecordsProvider for the actual logic - a common pattern so the "web"
// layer stays simple and testable business logic lives elsewhere.
@Stateless
@Path("records")
public class Records {
    private static final Logger LOGGER = LoggerFactory.getLogger(Records.class);
    private RecordsProvider recordsProvider;

    public Records() {}

    @Inject
    public Records(RecordsProvider recordsProvider) {
        this.recordsProvider = recordsProvider;
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRecords(@PathParam("id") final String id) {
        LOGGER.info("getRecords/{}", id);

        // Find every record with that belongs to any work that matches the given id
        try {
            return Response.ok(recordsProvider.getRecords(id)).build();


        } catch (FaustResolverException | FbiApiConnectorException | RecordServiceConnectorException e) {
            LOGGER.error("Failed to get records for id {}", id, e);
            return Response.status(400).entity(e).build();
        }
    }

    // JAX-RS resolves a literal path segment like "search" in preference to
    // a template variable like "{id}" when both could match the same URL -
    // so a request to /v1/api/records/search is routed here, not to
    // getRecords() with id="search". Order in the source file doesn't
    // matter for this.
    @GET
    @Path("search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(@QueryParam("title") String title,
                            @QueryParam("creator") String creator,
                            @QueryParam("results") @DefaultValue("100") Integer results) {
        LOGGER.info("getRecords/search title={} creator={} results={}", title, creator, results);

        try {
            return Response.ok(recordsProvider.search(title, creator, results)).build();
        } catch (FbiApiConnectorException e) {
            LOGGER.error("Failed to search for records with title {} and creator {}", title, creator, e);
            return Response.status(400).entity(e).build();
        }
    }
}
