package dk.dbc.promat.service.api;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.promat.service.entity.Subject;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@Path("")
public class Subjects {
    @PersistenceContext(unitName = "promatPU")
    EntityManager entityManager;
    private final JSONBContext jsonbContext = new JSONBContext();

    protected static final String LIST_SUBJECTS = "subjects";

    @GET
    @Path(LIST_SUBJECTS)
    @Produces(MediaType.APPLICATION_JSON)
    public Response listSubjects() throws JSONBException {
        TypedQuery<Subject> query = entityManager.createNamedQuery(Subject.GET_SUBJECTS_LIST_NAME, Subject.class);
        return Response.ok(jsonbContext.marshall(query.getResultList())).build();
    }
}
