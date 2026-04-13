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
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.List;

@Stateless
@Path("taxonomy")
public class TaxonomyService  {

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
}
