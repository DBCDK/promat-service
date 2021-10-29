/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import dk.dbc.connector.culr.CulrConnectorException;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.dto.UserRole;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.PromatUser;

import javax.annotation.security.RolesAllowed;
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
import java.util.Optional;

@Stateless
@Path("users")
public class Users {
    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @Inject
    CulrHandler culrHandler;

    @GET
    @Path("{culrId}/role")
    @Produces({MediaType.APPLICATION_JSON})
    @RolesAllowed({"authenticated-user"})
    public Response getUserRole(@PathParam("culrId") String culrId) throws CulrConnectorException {
        final TypedQuery<UserRole> query = entityManager.createNamedQuery(PromatUser.GET_USER_ROLE, UserRole.class);
        query.setParameter(1, culrId);

        final List<UserRole> userRole = query.getResultList();
        if (userRole.isEmpty()) {
            final ServiceErrorDto serviceError = new ServiceErrorDto()
                    .withCause("User not authorized")
                    .withDetails(String.format("CULR ID %s was not found in the set of known Promat users", culrId))
                    .withCode(ServiceErrorCode.NOT_FOUND);
            return Response.status(401).entity(serviceError).build();
        }

        final Optional<ServiceErrorDto> verificationError =
                culrHandler.verifyCulrAccount(culrId, userRole.get(0).getLocalId());
        if (verificationError.isPresent()) {
            return Response.status(401).entity(verificationError.get()).build();
        }
        return Response.ok(userRole.get(0)).build();
    }
}
