package dk.dbc.promat.service.persistence;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

import java.util.Date;
import java.util.Objects;

@NamedQueries(
        @NamedQuery(
                name = Editor.GET_ALL_EDITORS,
                query = Editor.GET_ALL_EDITORS_QUERY

        )
)
@Entity
@DiscriminatorValue("EDITOR")
public class Editor extends PromatUser {
    public static final String GET_ALL_EDITORS = "get.all.editors";
    public static final String GET_ALL_EDITORS_QUERY =
            "SELECT editor FROM Editor editor ORDER BY editor.id ASC";

    public Editor withActive(boolean active) {
        this.active = active;
        return this;
    }

    public Editor withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public Editor withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public Editor withEmail(String email) {
        this.email = email;
        return this;
    }

    public Editor withDeactivated(Date deactivated) {
        this.deactivated = deactivated;
        return this;
    }

    public Editor withAgency(String agency) {
        this.agency = agency;
        return this;
    }

    public Editor withUserId(String userId) {
        this.userId = userId;
        return this;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (active ? 1 : 0);
        result = 31 * result + firstName.hashCode();
        result = 31 * result + lastName.hashCode();
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (phone != null ? phone.hashCode() : 0);
        result = 31 * result + (activeChanged != null ? activeChanged.hashCode() : 0);
        result = 31 * result + (deactivated != null ? deactivated.hashCode() : 0);
        result = 31 * result + (agency != null ? agency.hashCode() : 0);
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Editor editor = (Editor) o;

        if (!Objects.equals(id, editor.id)) return false;
        if (!Objects.equals(active, editor.active)) return false;
        if (!Objects.equals(firstName, editor.firstName)) return false;
        if (!Objects.equals(lastName, editor.lastName)) return false;
        if (!Objects.equals(email, editor.email)) return false;
        if (!Objects.equals(activeChanged, editor.activeChanged)) return false;
        if (!Objects.equals(deactivated, editor.deactivated))  return false;
        if (!Objects.equals(agency, editor.agency))  return false;
        if (!Objects.equals(userId, editor.userId))  return false;
        return Objects.equals(phone, editor.phone);
    }

    @Override
    public String toString() {
        return "Editor{" +
                "id=" + id +
                ", active=" + active +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", activeChanged='" + activeChanged + '\'' +
                ", deactivated='" + deactivated + '\'' +
                ", agency='" + agency + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}
