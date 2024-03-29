package dk.dbc.promat.service.persistence;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

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
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Editor editor = (Editor) o;

        if (!id.equals(editor.id)) return false;
        if (active != editor.active) return false;
        if (!firstName.equals(editor.firstName)) return false;
        if (!lastName.equals(editor.lastName)) return false;
        if (email != null ? !email.equals(editor.email) : editor.email != null) return false;
        if (activeChanged != null ? !activeChanged.equals(editor.activeChanged) : editor.activeChanged != null) return false;
        if (deactivated != null ? !deactivated.equals(editor.activeChanged) : editor.deactivated != null)  return false;
        return phone != null ? phone.equals(editor.phone) : editor.phone == null;
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
                '}';
    }
}
