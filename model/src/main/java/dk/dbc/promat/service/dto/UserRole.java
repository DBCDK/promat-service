/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.promat.service.persistence.PromatUser;

public class UserRole {
    private final int id;
    private final PromatUser.Role role;

    @JsonCreator
    public UserRole(@JsonProperty("id") int id, @JsonProperty("role") PromatUser.Role role) {
        this.id = id;
        this.role = role;
    }

    public UserRole(int id, String role) {
        this(id, PromatUser.Role.valueOf(role));
    }

    public int getId() {
        return id;
    }

    public PromatUser.Role getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserRole userRole = (UserRole) o;

        if (id != userRole.id) {
            return false;
        }
        return role == userRole.role;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (role != null ? role.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UserRole{" +
                "id=" + id +
                ", role=" + role +
                '}';
    }
}
