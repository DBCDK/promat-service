package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.SubjectList;
import dk.dbc.promat.service.entity.Subject;
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
