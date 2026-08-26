package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.dto.UserRole;
import dk.dbc.promat.service.persistence.PromatUser;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class UsersIT extends ContainerTest  {

    @Test
    void resolveReviewer() throws JsonProcessingException {
        final Response response = getResponse("v1/api/users/role", "3-4-5-6-7");
        assertThat("response status", response.getStatus(), is(200));

        final UserRole userRole = mapper.readValue(response.readEntity(String.class), UserRole.class);
        assertThat("user role", userRole, is(new UserRole(2, PromatUser.Role.REVIEWER)));
    }

    @Test
    void resolveEditor() throws JsonProcessingException {
        final Response response = getResponse("v1/api/users/role", "2-3-4-5-6");
        assertThat("response status", response.getStatus(), is(200));

        final UserRole userRole = mapper.readValue(response.readEntity(String.class), UserRole.class);
        assertThat("user role", userRole, is(new UserRole(13, PromatUser.Role.EDITOR)));
    }

    @Test
    void resolveEditorViaEntraDbcLogin() throws JsonProcessingException {
        /* userinfo for this token returns userId="klnp@dbc.dk" and initials="klnp" (entraDbc
           login), whereas the "dbcidp" login for the same user returns userId="klnp". Since
           resolution is keyed on the "initials" claim - which the IdP asserts consistently
           regardless of login path - both must resolve to the same promatuser row. */
        final Response response = getResponse("v1/api/users/role", "6-5-4-3-2");
        assertThat("response status", response.getStatus(), is(200));

        final UserRole userRole = mapper.readValue(response.readEntity(String.class), UserRole.class);
        assertThat("user role", userRole, is(new UserRole(13, PromatUser.Role.EDITOR)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"4-5-6-7-8", "5-6-7-8-9", "5-4-3-2-1", "3-2-1-0-9", "4-3-2-1-0"})
    void resolveUsersWithInvalidRights(String authToken) {
        Response response = getResponse("v1/api/users/role", authToken);
        assertThat("response status", response.getStatus(), is(401));
    }

    @Test
    void resolveUserFailsWhenInitialsDoNotMatch() {
        /* userinfo for this token returns initials="nomatch" - no promatuser row has
           userId="nomatch", so resolution must fail. */
        final Response response = getResponse("v1/api/users/role", "7-6-5-4-3");
        assertThat("response status", response.getStatus(), is(401));
    }

    @Test
    void resolveUserFailsForNonDbcDkUserIdDomain() throws JsonProcessingException {
        /* userinfo for this token returns userId="klnp@otherdomain.com" (an unrelated, non-dbc.dk
           email domain) with initials="klnp" agreeing with its local-part. Even so, an
           email-shaped userId outside the dbc.dk domain must be rejected before the DB lookup. */
        final Response response = getResponse("v1/api/users/role", "8-7-6-5-4");
        assertThat("response status", response.getStatus(), is(401));

        final ServiceErrorDto error = mapper.readValue(response.readEntity(String.class), ServiceErrorDto.class);
        assertThat("error cause", error.getCause(), is("Untrusted userId domain"));
    }

    @Test
    void resolveUserFailsWhenInitialsDisagreeWithUserId() throws JsonProcessingException {
        /* userinfo for this token returns userId="klnp@dbc.dk" but initials="different" - the
           cross-claim consistency check must reject this before ever querying the database. */
        final Response response = getResponse("v1/api/users/role", "9-8-7-6-5");
        assertThat("response status", response.getStatus(), is(401));

        final ServiceErrorDto error = mapper.readValue(response.readEntity(String.class), ServiceErrorDto.class);
        assertThat("error cause", error.getCause(), is("Inconsistent identity claims"));
    }
}
