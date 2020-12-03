/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.dto.UserRole;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.PromatUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Stateless
@Path("users")
public class Users {
    private static final Logger LOGGER = LoggerFactory.getLogger(Users.class);

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @GET
    @Path("{culrId}/role")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getUserRole(@PathParam("culrId") String culrId) {
        final TypedQuery<UserRole> query = entityManager.createNamedQuery(PromatUser.GET_USER_ROLE, UserRole.class);
        query.setParameter(1, culrId);

        final List<UserRole> userRole = query.getResultList();
        if (userRole.isEmpty()) {
            return ServiceErrorDto.Forbidden("User not authorized",
                    String.format("ID %s was not found in the set of known users", culrId));
        }
        LOGGER.info("USER_ROLE: {}", userRole.get(0));

        return Response.ok(userRole.get(0)).build();
    }
}
