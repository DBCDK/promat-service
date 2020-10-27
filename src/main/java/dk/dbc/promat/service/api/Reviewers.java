package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.ReviewerList;
import dk.dbc.promat.service.entity.Reviewer;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@Path("reviewers")
public class Reviewers {
    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAllReviewers() {
        TypedQuery<Reviewer> namedQuery = entityManager.createNamedQuery(Reviewer.GET_ALL_REVIEWERS_NAME, Reviewer.class);
        return Response.ok(new ReviewerList().withReviewers(namedQuery.getResultList())).build();
    }
}
