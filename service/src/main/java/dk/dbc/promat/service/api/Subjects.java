package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.SubjectList;
import dk.dbc.promat.service.persistence.Subject;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Stateless
@Path("subjects")
public class Subjects {

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listSubjects() {
        TypedQuery<Subject> query = entityManager.createNamedQuery(Subject.GET_SUBJECTS_LIST_NAME, Subject.class);
        return Response.ok(new SubjectList().withSubjects(query.getResultList())).build();
    }
}
