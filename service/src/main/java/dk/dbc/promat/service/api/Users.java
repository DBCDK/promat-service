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

        Optional<String> userId = callerPrincipal.claim("userId");
        Optional<String> agency = callerPrincipal.claim("netpunktAgency");
        if (userId.isEmpty() || agency.isEmpty()) {
            return Response.status(401).entity(
                    new ServiceErrorDto()
                            .withCause("No userId or agency")
                            .withDetails("Received request for user role without a userId and/or agency")
                            .withCode(ServiceErrorCode.FORBIDDEN)).build();
        }

        /* An email-shaped userId means entraDbc login was used. By convention - not a DB constraint -
           promatuser.userId is stored in the format the netpunkt login flow uses
           (i.e. initials), so we use the "initials" claim instead, which the IdP asserts
           consistently regardless of login path, after the sanity checks below. A plain
           (non-email) userId is used as-is, unchanged, for backward compatibility. */
        final String userIdLookupKey;
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

            final Optional<String> initials = callerPrincipal.claim("initials");
            if (initials.isEmpty()) {
                return Response.status(401).entity(
                        new ServiceErrorDto()
                                .withCause("No initials")
                                .withDetails("Received request for user role with an email-shaped userId but no initials")
                                .withCode(ServiceErrorCode.FORBIDDEN)).build();
            }

            final String userIdLocalPart = userId.get().substring(0, userId.get().indexOf('@'));
            if (!userIdLocalPart.equalsIgnoreCase(initials.get())) {
                LOGGER.error("getUserRoleFromAuthToken initials {} does not match userId {}", initials.get(), userId.get());
                return Response.status(401).entity(
                        new ServiceErrorDto()
                                .withCause("Inconsistent identity claims")
                                .withDetails(String.format("initials %s does not match userId %s", initials.get(), userId.get()))
                                .withCode(ServiceErrorCode.FORBIDDEN)).build();
            }
            userIdLookupKey = initials.get();
        } else {
            userIdLookupKey = userId.get();
        }

        final TypedQuery<UserRole> query = entityManager.createNamedQuery(PromatUser.GET_USER_ROLE_BY_AGENCY_AND_USERID, UserRole.class);
        query.setParameter(1, userIdLookupKey);
        query.setParameter(2, agency.get());

        final List<UserRole> userRole = query.getResultList();
        if (userRole.isEmpty()) {
            LOGGER.error("getUserRoleFromAuthToken returned empty list when searching with userId {} and agency {}", userIdLookupKey, agency.get());
            return Response.status(401).entity(
                    new ServiceErrorDto()
                            .withCause("User not authorized")
                            .withDetails(String.format("userId/agency %s/%s was not found in the set of known Promat users", userIdLookupKey, agency.get()))
                            .withCode(ServiceErrorCode.NOT_FOUND)).build();
        }
        if (userRole.size() > 1) {
            LOGGER.error("getUserRoleFromAuthToken returned list with more than 1 user when searching with userId {} and agency {}", userIdLookupKey, agency.get());
            return Response.status(401).entity(
                    new ServiceErrorDto()
                            .withCause("User not authorized")
                            .withDetails(String.format("userId/agency %s/%s returned multiple known Promat users", userIdLookupKey, agency.get()))
                            .withCode(ServiceErrorCode.NOT_FOUND)).build();
        }

        Set<String> groups = callerPrincipal.getGroups();
        if (groups.isEmpty() || !groups.contains(IDP_PRODUCT_NAME + "-" + getRightNameForRole(userRole.getFirst().getRole()))) {
            LOGGER.error("getUserRoleFromAuthToken with no or incorrect roles. Role is {}, but having groups {}", userRole.getFirst().getRole().name(), groups);
            return Response.status(401).entity(
                    new ServiceErrorDto()
                            .withCause("No or incorrect roles")
                            .withDetails("Received request for user with no or incorrect roles.")
                            .withCode(ServiceErrorCode.FORBIDDEN)).build();
        }

        return Response.ok(userRole.getFirst()).build();
    }

    private String getRightNameForRole(PromatUser.Role role) {
        return switch (role) {
            case REVIEWER -> IDP_REVIEWER_RIGHT_NAME;
            case EDITOR -> IDP_EDITOR_RIGHT_NAME;
        };
    }
}
