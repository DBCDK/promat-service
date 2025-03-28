package dk.dbc.promat.service.api;

import dk.dbc.connector.culr.CulrConnectorException;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.dto.UserRole;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.PromatUser;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Stateless
@Path("users")
public class Users {
    private static final Logger LOGGER = LoggerFactory.getLogger(Users.class);

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @Inject
    CulrHandler culrHandler;

    @Inject
    public JsonWebToken callerPrincipal;

    public static final String IDP_PRODUCT_NAME = "PROMAT";
    public static final String IDP_EDITOR_RIGHT_NAME = "EDITOR";
    public static final String IDP_REVIEWER_RIGHT_NAME = "LEKTOR";  // Todo: This is not correct, fix when known

    @GET
    @Path("{culrId}/role")
    @Produces({MediaType.APPLICATION_JSON})
    @RolesAllowed({"authenticated-user", IDP_PRODUCT_NAME + "-" + IDP_EDITOR_RIGHT_NAME, IDP_PRODUCT_NAME + "-" + IDP_REVIEWER_RIGHT_NAME})
    public Response getUserRole(@PathParam("culrId") String culrId) throws CulrConnectorException {
        final TypedQuery<UserRole> query = entityManager.createNamedQuery(PromatUser.GET_USER_ROLE, UserRole.class);
        query.setParameter(1, culrId);

        final List<UserRole> userRole = query.getResultList();
        if (userRole.isEmpty()) {
            LOGGER.warn("getUserRole returned empty list when searching with culr id {}", culrId);
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

    @GET
    @Path("role")
    @Produces({MediaType.APPLICATION_JSON})
    @RolesAllowed({"authenticated-user", IDP_PRODUCT_NAME + "-" + IDP_EDITOR_RIGHT_NAME, IDP_PRODUCT_NAME + "-" + IDP_REVIEWER_RIGHT_NAME})
    public Response getUserRoleFromAuthToken() {

        // Check if we got no authtoken - this should not be possible since a role is required
        // but internal tests can call this endpoint outside an application server so no dependency
        // injection has taken place
        if (callerPrincipal == null) {
            return Response.status(401).entity(
                    new ServiceErrorDto()
                            .withCause("No authtoken")
                            .withDetails("Received request for user role without a (valid) authtoken")
                            .withCode(ServiceErrorCode.FORBIDDEN)).build();
        }

        Optional<String> userId = callerPrincipal.claim("userId");
        Optional<String> agency = callerPrincipal.claim("netpunktAgency");
        if (userId.isEmpty() || agency.isEmpty()) {
            return Response.status(401).entity(
                    new ServiceErrorDto()
                            .withCause("No userId or agency")
                            .withDetails("Received request for user role without a userId and/or agency")
                            .withCode(ServiceErrorCode.FORBIDDEN)).build();
        }

        // Extra verbose logging while transitioning from 'bibliotekslogin' to 'netpunktslogin'
        // Todo: Remove when transitioned
        LOGGER.info("getUserRoleFromAuthToken: userId={}, agency={}", userId, agency);

        final TypedQuery<UserRole> query = entityManager.createNamedQuery(PromatUser.GET_USER_ROLE_BY_AUTHTOKEN, UserRole.class);
        query.setParameter(1, userId.get());
        query.setParameter(2, agency.get());

        final List<UserRole> userRole = query.getResultList();
        if (userRole.isEmpty()) {
            LOGGER.error("getUserRoleFromAuthToken returned empty list when searching with user id {} and agency {}", userId.get(), agency.get());
            return Response.status(401).entity(
                    new ServiceErrorDto()
                            .withCause("User not authorized")
                            .withDetails(String.format("userId/agency %s/%s was not found in the set of known Promat users", userId.get(), agency.get()))
                            .withCode(ServiceErrorCode.NOT_FOUND)).build();
        }
        if (userRole.size() > 1) {
            LOGGER.error("getUserRoleFromAuthToken returned list with more than 1 user when searching with user id {} and agency {}", userId.get(), agency.get());
            return Response.status(401).entity(
                    new ServiceErrorDto()
                            .withCause("User not authorized")
                            .withDetails(String.format("userId/agency %s/%s returned multiple known Promat users", userId.get(), agency.get()))
                            .withCode(ServiceErrorCode.NOT_FOUND)).build();
        }

        Set<String> groups = callerPrincipal.getGroups();
        if (groups.isEmpty() || !groups.contains(IDP_PRODUCT_NAME + "-" + getRightNameForRole(userRole.get(0).getRole()))) {
            LOGGER.error("getUserRoleFromAuthToken with no or incorrect roles. Role is {}, but having groups {}", userRole.get(0).getRole().name(), groups);
            return Response.status(401).entity(
                    new ServiceErrorDto()
                            .withCause("No or incorrect roles")
                            .withDetails("Received request for user with no or incorrect roles.")
                            .withCode(ServiceErrorCode.FORBIDDEN)).build();
        }

        return Response.ok(userRole.get(0)).build();
    }

    private String getRightNameForRole(PromatUser.Role role) {
        switch (role) {
            case REVIEWER: return IDP_REVIEWER_RIGHT_NAME;
            case EDITOR: return IDP_EDITOR_RIGHT_NAME;
            default: return "(unknown)";
        }
    }
}
