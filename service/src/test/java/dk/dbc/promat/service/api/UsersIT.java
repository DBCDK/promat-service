package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.UserRole;
import dk.dbc.promat.service.persistence.PromatUser;
import dk.dbc.promat.service.persistence.Reviewer;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class UsersIT extends ContainerTest  {

    @Test
    void resolveReviewer() throws JsonProcessingException {
        final Response response = getResponse("v1/api/users/41/role", "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));

        final UserRole userRole = mapper.readValue(response.readEntity(String.class), UserRole.class);
        assertThat("user role", userRole, is(new UserRole(1, PromatUser.Role.REVIEWER)));
    }

    @Test
    void resolveEditor() throws JsonProcessingException {
        final Response response = getResponse("v1/api/users/51/role", "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));

        final UserRole userRole = mapper.readValue(response.readEntity(String.class), UserRole.class);
        assertThat("user role", userRole, is(new UserRole(10, PromatUser.Role.EDITOR)));
    }

    @Test
    void culrIdNotFoundInPromat() {
        final Response response = getResponse("v1/api/users/61/role");
        assertThat("response status", response.getStatus(), is(401));
    }

    @Test
    void localIdNotFoundInCulr() {
        final Response response = getResponse("v1/api/users/52/role");
        assertThat("response status", response.getStatus(), is(401));
    }

    @Test
    void localIdNotMatchingCulrId() {
        final Response response = getResponse("v1/api/users/54/role");
        assertThat("response status", response.getStatus(), is(401));
    }

    @Test
    void resolveReviewerByUserId() throws JsonProcessingException {
        Response response = getResponse("v1/api/users/role", "3-4-5-6-7");
        assertThat("response status", response.getStatus(), is(200));

        final UserRole userRole = mapper.readValue(response.readEntity(String.class), UserRole.class);
        assertThat("user role", userRole, is(new UserRole(2, PromatUser.Role.REVIEWER)));
    }

    @Test
    void resolveEditorByUserId() throws JsonProcessingException {
        final Response response = getResponse("v1/api/users/role", "2-3-4-5-6");
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
}
