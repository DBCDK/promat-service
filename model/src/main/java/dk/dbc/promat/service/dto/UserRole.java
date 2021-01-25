/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.promat.service.persistence.PromatUser;

public class UserRole {
    private final int id;
    private final PromatUser.Role role;
    private final String localId;

    @JsonCreator
    public UserRole(@JsonProperty("id") int id,
                    @JsonProperty("role") PromatUser.Role role,
                    @JsonProperty("localId") String localId) {
        this.id = id;
        this.role = role;
        this.localId = localId;
    }

    public UserRole(int id, String role, String localId) {
        this(id, PromatUser.Role.valueOf(role), localId);
    }

    public UserRole(int id, PromatUser.Role role) {
        this(id, role, null);
    }

    public int getId() {
        return id;
    }

    public PromatUser.Role getRole() {
        return role;
    }

    @JsonIgnore
    public String getLocalId() {
        return localId;
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
        if (role != userRole.role) {
            return false;
        }
        return localId != null ? localId.equals(userRole.localId) : userRole.localId == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + role.hashCode();
        result = 31 * result + (localId != null ? localId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UserRole{" +
                "id=" + id +
                ", role=" + role +
                ", localId='" + localId + '\'' +
                '}';
    }
}
