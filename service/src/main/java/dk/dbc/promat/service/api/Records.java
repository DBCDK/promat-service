package dk.dbc.promat.service.api;

import dk.dbc.promat.service.connectors.FaustResolverException;
import dk.dbc.promat.service.connectors.FbiApiConnectorException;
import dk.dbc.promat.service.dto.RecordDto;
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

    // A faust always resolves to at most one record, unlike getRecords(), which
    // also has to serve ISBN/barcode lookups that can genuinely match several -
    // so this returns a single record directly instead of a list of one.
    @GET
    @Path("faust/{faust}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRecordByFaust(@PathParam("faust") final String faust) {
        LOGGER.info("getRecords/faust/{}", faust);

        try {
            RecordDto record = recordsProvider.getRecordByFaust(faust);
            if (record == null) {
                return Response.status(404).build();
            }
            return Response.ok(record).build();
        } catch (FbiApiConnectorException | RecordServiceConnectorException e) {
            LOGGER.error("Failed to get record for faust {}", faust, e);
            return Response.status(400).entity(e).build();
        }
    }

    // JAX-RS prefers a literal path segment ("search") over a template
    // variable ("{id}") when both could match, so this doesn't get routed
    // to getRecords() with id="search".
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
