/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.rest;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.promat.service.persistence.Case;
import dk.dbc.promat.service.persistence.PromatEntityManager;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Stateless
@Path("/test")
public class TestResource {
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    @Inject @PromatEntityManager
    EntityManager entityManager;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getExistingCases(
            @QueryParam("faust") List<String> faust) throws JSONBException {
        // When matching array values using the postgresql jsonb contains
        // operator, arrays are required on both the left and right hand
        // side of the expression.
        // Therefore "List<String> faust" is used even though we are only
        // interested in checking a single value.

        final List<Case> getExistingCases = entityManager.createNamedQuery("getExistingCases", Case.class)
                .setParameter("faust", JSONB_CONTEXT.marshall(faust))
                .getResultList();
        return Response.ok().entity(getExistingCases).build();
    }
}
