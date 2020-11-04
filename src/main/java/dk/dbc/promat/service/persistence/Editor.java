package dk.dbc.promat.service.persistence;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.Objects;

@Entity
@DiscriminatorValue("EDITOR")
public class Editor extends PromatUser {
    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + firstName.hashCode();
        result = 31 * result + lastName.hashCode();
        result = 31 * result + email.hashCode();
        result = 31 * result + (phone != null ? phone.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Editor editor = (Editor) o;

        if (!id.equals(editor.id)) return false;
        if (!firstName.equals(editor.firstName)) return false;
        if (!lastName.equals(editor.lastName)) return false;
        if (!email.equals(editor.email)) return false;
        return !Objects.equals(phone, editor.phone);
    }

    @Override
    public String toString() {
        return "Editor{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}
