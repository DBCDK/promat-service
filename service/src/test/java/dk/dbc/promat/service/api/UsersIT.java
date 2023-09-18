/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.UserRole;
import dk.dbc.promat.service.persistence.PromatUser;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

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
}
