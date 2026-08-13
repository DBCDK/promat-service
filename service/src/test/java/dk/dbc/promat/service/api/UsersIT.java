package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.ContainerTest;
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
    void resolveEditorViaEntraDbcEmailUserId() throws JsonProcessingException {
        // userinfo for this token returns userId="klnp@dbc.dk" (entraDbc login), whereas the
        // promatuser row is stored with userId="klnp" - this must still resolve via the
        // "@dbc.dk" local-part fallback in PromatUser.GET_USER_ROLE_BY_AGENCY_AND_USERID_QUERY.
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
    void resolveEditorViaEntraDbcEmailUserIdFailsWhenLocalPartDoesNotMatch() {
        // userinfo for this token returns userId="nomatch@dbc.dk" - no promatuser row has
        // userId="nomatch", so the "@dbc.dk" fallback must still fail to resolve a user.
        final Response response = getResponse("v1/api/users/role", "7-6-5-4-3");
        assertThat("response status", response.getStatus(), is(401));
    }

    @Test
    void resolveEditorViaEntraDbcEmailUserIdFailsForNonDbcDkDomain() {
        // userinfo for this token returns userId="klnp@otherdomain.com" - the local-part matches
        // the promatuser row (id=13, userId="klnp"), but since the domain isn't "@dbc.dk" the
        // fallback must not kick in.
        final Response response = getResponse("v1/api/users/role", "8-7-6-5-4");
        assertThat("response status", response.getStatus(), is(401));
    }
}
