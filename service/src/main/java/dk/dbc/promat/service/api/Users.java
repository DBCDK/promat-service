package dk.dbc.promat.service.api;

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
    public JsonWebToken callerPrincipal;

    public static final String IDP_PRODUCT_NAME = "PROMAT";
    public static final String IDP_EDITOR_RIGHT_NAME = "EDITOR";
    public static final String IDP_REVIEWER_RIGHT_NAME = "REVIEWER";

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

        /* "initials" is asserted by the IdP regardless of login path (dbcidp or entraDbc), unlike
           "userId" which differs in format between the two (e.g. "klnp" vs "klnp@dbc.dk") - so we
           match promatuser rows on initials instead, without needing to care which idpUsed applies. */
        Optional<String> initials = callerPrincipal.claim("initials");
        Optional<String> userId = callerPrincipal.claim("userId");
        Optional<String> agency = callerPrincipal.claim("netpunktAgency");
        if (initials.isEmpty() || userId.isEmpty() || agency.isEmpty()) {
            return Response.status(401).entity(
                    new ServiceErrorDto()
                            .withCause("No initials, userId or agency")
                            .withDetails("Received request for user role without initials, userId and/or agency")
                            .withCode(ServiceErrorCode.FORBIDDEN)).build();
        }

        /* Sanity checks on the IdP's own claims, before trusting "initials" for the DB lookup:
           - if "userId" is email-shaped (entraDbc login), its domain must be dbc.dk - we don't
             want to accept an identity federated from an arbitrary/unrecognized domain
           - "initials" must equal the local-part of "userId" regardless of domain (e.g.
             "userId=klnp@dbc.dk" or "userId=klnp" must both have "initials=klnp") - this guards
             against relying on "initials" alone if it's ever populated inconsistently with the
             rest of the token */
        final String userIdLocalPart;
        if (userId.get().contains("@")) {
            final String userIdDomain = userId.get().substring(userId.get().indexOf('@') + 1);
            if (!userIdDomain.equalsIgnoreCase("dbc.dk")) {
                LOGGER.error("getUserRoleFromAuthToken userId {} is not from the dbc.dk domain", userId.get());
                return Response.status(401).entity(
                        new ServiceErrorDto()
                                .withCause("Untrusted userId domain")
                                .withDetails(String.format("userId %s is not from the dbc.dk domain", userId.get()))
                                .withCode(ServiceErrorCode.FORBIDDEN)).build();
            }
            userIdLocalPart = userId.get().substring(0, userId.get().indexOf('@'));
        } else {
            userIdLocalPart = userId.get();
        }
        if (!userIdLocalPart.equalsIgnoreCase(initials.get())) {
            LOGGER.error("getUserRoleFromAuthToken initials {} does not match userId {}", initials.get(), userId.get());
            return Response.status(401).entity(
                    new ServiceErrorDto()
                            .withCause("Inconsistent identity claims")
                            .withDetails(String.format("initials %s does not match userId %s", initials.get(), userId.get()))
                            .withCode(ServiceErrorCode.FORBIDDEN)).build();
        }

        final TypedQuery<UserRole> query = entityManager.createNamedQuery(PromatUser.GET_USER_ROLE_BY_AGENCY_AND_USERID, UserRole.class);
        query.setParameter(1, initials.get());
        query.setParameter(2, agency.get());

        final List<UserRole> userRole = query.getResultList();
        if (userRole.isEmpty()) {
            LOGGER.error("getUserRoleFromAuthToken returned empty list when searching with initials {} and agency {}", initials.get(), agency.get());
            return Response.status(401).entity(
                    new ServiceErrorDto()
                            .withCause("User not authorized")
                            .withDetails(String.format("initials/agency %s/%s was not found in the set of known Promat users", initials.get(), agency.get()))
                            .withCode(ServiceErrorCode.NOT_FOUND)).build();
        }
        if (userRole.size() > 1) {
            LOGGER.error("getUserRoleFromAuthToken returned list with more than 1 user when searching with initials {} and agency {}", initials.get(), agency.get());
            return Response.status(401).entity(
                    new ServiceErrorDto()
                            .withCause("User not authorized")
                            .withDetails(String.format("initials/agency %s/%s returned multiple known Promat users", initials.get(), agency.get()))
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
        return switch (role) {
            case REVIEWER -> IDP_REVIEWER_RIGHT_NAME;
            case EDITOR -> IDP_EDITOR_RIGHT_NAME;
        };
    }
}
