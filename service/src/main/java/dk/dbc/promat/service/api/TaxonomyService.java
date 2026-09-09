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

@Stateless
@Path("taxonomy")
public class TaxonomyService  {

    private static final int SEARCH_RESULT_LIMIT = 50;

    private TaxonomyCache taxonomyCache;

    protected TaxonomyService() {}

    @Inject
    public TaxonomyService(TaxonomyCache taxonomyCache) {
        this.taxonomyCache = taxonomyCache;
    }

    @GET
    @Path("tree")
    @Produces("application/json")
    public Response getTaxonomy() {
        return Response.ok().entity(taxonomyCache.get().getRoot()).build();
    }

    @GET
    @Path("structure")
    @Produces("application/json")
    public Response getTaxonomyStructure() {
        return Response.ok().entity(taxonomyCache.get().getStructure()).build();
    }

    @POST
    @Path("subtree")
    @Consumes("application/json")
    @Produces("application/json")
    public Response getTaxonomySubtree(List<String> path) {
        try {
            return Response.ok().entity(taxonomyCache.get().getList(path.toArray(String[]::new))).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

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
