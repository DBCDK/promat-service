package dk.dbc.promat.service.persistence;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
@NamedQueries({
        @NamedQuery(
                name = Reviewer.GET_ALL_REVIEWERS_NAME,
                query = Reviewer.GET_ALL_REVIEWERS_QUERY
        )
})
@Entity
@DiscriminatorValue("REVIEWER")
public class Reviewer extends PromatUser {
    public static final String GET_ALL_REVIEWERS_NAME =
            "Reviewers.get.all";
    public static final String GET_ALL_REVIEWERS_QUERY =
            "SELECT reviewer FROM Reviewer reviewer ORDER BY reviewer.id ASC";


    @Embedded
    private Address address;
    private String institution;
    private Integer paycode;

    private LocalDate hiatus_begin;
    private LocalDate hiatus_end;

    @OneToMany
    @JoinTable (
            name = "ReviewerSubjects",
            joinColumns = @JoinColumn(name = "reviewer_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    private Collection<Subject> subjects;

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public Integer getPaycode() {
        return paycode;
    }

    public void setPaycode(Integer paycode) {
        this.paycode = paycode;
    }

    public LocalDate getHiatus_begin() {
        return hiatus_begin;
    }

    public void setHiatus_begin(LocalDate hiatus_begin) {
        this.hiatus_begin = hiatus_begin;
    }

    public LocalDate getHiatus_end() {
        return hiatus_end;
    }

    public void setHiatus_end(LocalDate hiatus_end) {
        this.hiatus_end = hiatus_end;
    }

    public Collection<Subject> getSubjects() {
        return subjects;
    }

    public void setSubjects(Collection<Subject> subjects) {
        this.subjects = subjects;
    }

    public Reviewer withId(Integer id) {
        this.id = id;
        return this;
    }

    public Reviewer withActive(boolean active) {
        this.active = active;
        return this;
    }

    public Reviewer withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public Reviewer withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public Reviewer withEmail(String email) {
        this.email = email;
        return this;
    }

    public Reviewer withAddress(Address address) {
        this.address = address;
        return this;
    }

    public Reviewer withInstitution(String institution) {
        this.institution = institution;
        return this;
    }

    public Reviewer withPaycode(Integer paycode) {
        this.paycode = paycode;
        return this;
    }

    public Reviewer withSubjects(List<Subject> subjects) {
        this.subjects = subjects;
        return this;
    }

    public Reviewer withHiatus_begin(LocalDate hiatus_begin) {
        this.hiatus_begin = hiatus_begin;
        return this;
    }

    public Reviewer withHiatus_end(LocalDate hiatus_end) {
        this.hiatus_end = hiatus_end;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Reviewer reviewer = (Reviewer) o;

        if (!id.equals(reviewer.id)) return false;
        if (active != reviewer.active) return false;
        if (!firstName.equals(reviewer.firstName)) return false;
        if (!lastName.equals(reviewer.lastName)) return false;
        if (!email.equals(reviewer.email)) return false;
        if (phone != null ? !phone.equals(reviewer.phone) : reviewer.phone != null) return false;
        if (!address.equals(reviewer.address)) return false;
        if (!institution.equals(reviewer.institution)) return false;
        if (!paycode.equals(reviewer.paycode)) return false;
        if (hiatus_begin != null ? !hiatus_begin.equals(reviewer.hiatus_begin) : reviewer.hiatus_begin != null)
            return false;
        if (hiatus_end != null ? !hiatus_end.equals(reviewer.hiatus_end) : reviewer.hiatus_end != null) return false;
        return subjects != null ? subjects.equals(reviewer.subjects) : reviewer.subjects == null;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (active ? 1 : 0);
        result = 31 * result + firstName.hashCode();
        result = 31 * result + lastName.hashCode();
        result = 31 * result + email.hashCode();
        result = 31 * result + (phone != null ? phone.hashCode() : 0);
        result = 31 * result + address.hashCode();
        result = 31 * result + institution.hashCode();
        result = 31 * result + paycode.hashCode();
        result = 31 * result + (hiatus_begin != null ? hiatus_begin.hashCode() : 0);
        result = 31 * result + (hiatus_end != null ? hiatus_end.hashCode() : 0);
        result = 31 * result + (subjects != null ? subjects.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Reviewer{" +
                "id=" + id +
                ", active=" + active +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", address=" + address +
                ", institution='" + institution + '\'' +
                ", paycode=" + paycode +
                ", hiatus_begin=" + hiatus_begin +
                ", hiatus_end=" + hiatus_end +
                ", subjects=" + subjects +
                '}';
    }
}
