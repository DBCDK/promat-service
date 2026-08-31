package dk.dbc.promat.service.api;

import dk.dbc.promat.service.taxonomy.TaxonomyCache;
import dk.dbc.promat.service.taxonomy.TaxonomyException;
import dk.dbc.promat.service.taxonomy.dto.PathTranslator;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.List;

// This class is BOTH an EJB (@Stateless, see TaxonomyKafkaPersistence.java for what that
// means) AND a JAX-RS ("Jakarta RESTful Web Services") resource (@Path). JAX-RS is the spec
// that turns annotated methods into HTTP endpoints - @Path("taxonomy") here sets the base
// URL segment for every method in this class, combining with PromatApplication's own
// @ApplicationPath("v1/api") to produce paths like /v1/api/taxonomy/tree (see the individual
// @Path values on each method below). Being registered is a separate step, though - see
// PromatApplication.getClasses(), which lists this class explicitly (this project doesn't use
// automatic classpath scanning for REST resources).
@Stateless
@Path("taxonomy")
public class TaxonomyService  {

    private static final int SEARCH_RESULT_LIMIT = 50;

    private TaxonomyCache taxonomyCache;

    // This no-arg constructor looks pointless (it's never called directly by application
    // code), but it's required by the EJB/CDI spec: the container needs a way to create a
    // proxy instance of this class for things like interceptors and lazy initialization, and
    // that proxy creation mechanism requires a no-arg constructor to exist, even though the
    // REAL constructor below (the one with @Inject) is what actually gets used to build a
    // fully-initialized instance. `protected` (rather than public) signals "this exists for
    // the framework, not for you to call".
    protected TaxonomyService() {}

    // Constructor injection, same pattern as TaxonomyBuilderProducer.java - the EJB container
    // calls this constructor itself, supplying a TaxonomyCache automatically.
    @Inject
    public TaxonomyService(TaxonomyCache taxonomyCache) {
        this.taxonomyCache = taxonomyCache;
    }


    // @GET/@POST/@DELETE mark which HTTP method a given Java method responds to. @Path can
    // also appear per-method (appended to the class-level @Path) - GET /v1/api/taxonomy/tree
    // routes here. @Produces("application/json") sets the HTTP response's Content-Type header
    // and tells JAX-RS to serialize whatever this method returns (via the returned
    // Response's .entity(...), ultimately a plain Java Map here - see Taxonomy.getRoot()) into
    // JSON, using Jackson under the hood (registered as a JAX-RS provider elsewhere in this
    // project - see JsonMapperProvider/PromatApplication).
    @GET
    @Path("tree")
    @Produces("application/json")
    public Response getTaxonomy() {
        // Response.ok().entity(...).build() is JAX-RS's builder-style API for constructing an
        // HTTP response: ok() sets status 200, .entity(...) sets the response body (before
        // JSON serialization happens), .build() finalizes it into an actual Response object
        // to return. Using this builder instead of just returning the Map directly gives full
        // control over status codes - see the error-handling methods below, which use
        // Response.status(...) instead of .ok() for non-200 responses.
        return Response.ok().entity(taxonomyCache.get().getRoot()).build();
    }

    @GET
    @Path("structure")
    @Produces("application/json")
    public Response getTaxonomyStructure() {
        return Response.ok().entity(taxonomyCache.get().getStructure()).build();
    }

    // @Consumes("application/json") is @Produces's mirror image for the REQUEST side: it
    // tells JAX-RS this method expects a JSON request body, which then gets deserialized into
    // this method's parameter (`List<String> path`) before the method body runs - the
    // opposite direction of the same Jackson-based (de)serialization used for the response.
    @POST
    @Path("subtree")
    @Consumes("application/json")
    @Produces("application/json")
    public Response getTaxonomySubtree(List<String> path) {
        try {
            return Response.ok().entity(taxonomyCache.get().getList(path.toArray(String[]::new))).build();
        } catch (IllegalArgumentException e) {
            // Response.status(...) lets you set an arbitrary HTTP status - here, translating
            // a Java exception (an unrecognized taxonomy path) into a proper 404 Not Found for
            // API clients, rather than letting the exception propagate and become a generic
            // 500 Internal Server Error.
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    // @PathParam extracts a value from the URL path itself, matched against the {alias}
    // placeholder in @Path("subtree/{alias}") below - e.g. GET /v1/api/taxonomy/subtree/foo
    // calls this method with alias bound to "foo". Note the parameter's declared type here is
    // PathTranslator, not String - JAX-RS supports automatically converting a raw path segment
    // into any type that has a matching constructor/factory method (see PathTranslator.java
    // for how that conversion actually works), instead of every method that needs one having
    // to parse a plain String itself.
    @GET
    @Path("subtree/{alias}")
    @Produces("application/json")
    public Response getTaxonomySubtreeByAlias(@PathParam("alias") PathTranslator alias) {
        try {
            return Response.ok().entity(taxonomyCache.get().getList(alias.getPathValue().toArray(String[]::new))).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path("cache")
    public Response clearCache() {
        try {
            taxonomyCache.refresh();
        } catch (TaxonomyException | IOException e) {
            // A single catch block handling two different exception types (Java 7+'s
            // "multi-catch" syntax, `catch (TypeA | TypeB e)`) - used when the handling logic
            // is identical either way, to avoid writing the same catch body twice.
            return Response.serverError().entity(e.getMessage()).build();
        }
        return Response.ok().build();
    }

    /**
     * Searches for subjects by title within a given taxonomy subtree, capped at
     * {@value SEARCH_RESULT_LIMIT} results. Intended for subtrees too large to
     * usefully render as a plain list/dropdown (e.g. "handling->handler om" with
     * thousands of subjects) - the frontend decides when to use search versus
     * fetching the whole subtree via {@link #getTaxonomySubtree}.
     *
     * @param query search term, matched case-insensitively as a substring of the subject title
     * @param path  the taxonomy path to search within, same shape as {@link #getTaxonomySubtree}
     */
    // @QueryParam("q") binds a URL query-string parameter (?q=...) to a method parameter -
    // this method takes BOTH a query param (query) and a JSON request body (path), which
    // JAX-RS is happy to combine: query params and headers come from the URL/HTTP metadata,
    // while at most one parameter can be bound to the request body.
    @POST
    @Path("subtree/search")
    @Consumes("application/json")
    @Produces("application/json")
    public Response searchTaxonomySubtree(@QueryParam("q") String query, List<String> path) {
        if (query == null || query.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Query parameter 'q' is required").build();
        }

        try {
            return Response.ok()
                    .entity(taxonomyCache.get().searchList(path.toArray(String[]::new), query, SEARCH_RESULT_LIMIT))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }
}
