/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.persistence;

import dk.dbc.promat.service.dto.ReviewerWithWorkloads;

import javax.persistence.ColumnResult;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SqlResultSetMapping;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@SqlResultSetMapping(
        name = "ReviewerWithWorkloadMapping",
        entities = @EntityResult(entityClass = Reviewer.class),
        columns = {
                @ColumnResult(name = "weekWorkload", type = Long.class),
                @ColumnResult(name = "weekBeforeWorkload", type = Long.class),
                @ColumnResult(name = "weekAfterWorkload", type = Long.class)})
@NamedQuery(
        name = Reviewer.GET_ALL_REVIEWERS_NAME,
        query = Reviewer.GET_ALL_REVIEWERS_QUERY)
@NamedNativeQuery(
        name = Reviewer.GET_ALL_REVIEWERS_WITH_WORKLOADS,
        query = Reviewer.GET_ALL_REVIEWERS_WITH_WORKLOADS_QUERY,
        resultSetMapping = "ReviewerWithWorkloadMapping")
@Entity
@DiscriminatorValue("REVIEWER")
public class Reviewer extends PromatUser {
    public static final String GET_ALL_REVIEWERS_NAME =
            "Reviewers.get.all";
    public static final String GET_ALL_REVIEWERS_QUERY =
            "SELECT reviewer FROM Reviewer reviewer ORDER BY reviewer.id ASC";

    public static final String GET_ALL_REVIEWERS_WITH_WORKLOADS =
            "Reviewers.getAllReviewersWithWorkloads";
    public static final String GET_ALL_REVIEWERS_WITH_WORKLOADS_QUERY =
            "SELECT pu.*," +
            "SUM(CASE WHEN (pc.deadline BETWEEN ?1 AND ?2) THEN 1 ELSE 0 END) AS weekWorkload," +
            "SUM(CASE WHEN (pc.deadline BETWEEN ?3 AND ?4) THEN 1 ELSE 0 END) AS weekBeforeWorkload," +
            "SUM(CASE WHEN (pc.deadline BETWEEN ?5 AND ?6) THEN 1 ELSE 0 END) AS weekAfterWorkload " +
            "FROM promatuser pu LEFT JOIN promatcase pc ON pc.reviewer_id=pu.id AND pc.status = 'ASSIGNED' AND pc.deadline BETWEEN ?7 AND ?8 " +
            "WHERE pu.role='REVIEWER' " +
            "GROUP BY pu.id";

    public enum Accepts {
        BKM,
        BOOK,
        EBOOK,
        EXPRESS,
        MOVIE,
        MULTIMEDIA,
        PS4,
        PS5,
        XBOX_ONE,
        XBOX_SERIES_X,
        NINTENDO_SWITCH,
        BIWEEKLY_ONLY
    }

    @Embedded
    protected Address address;
    protected String institution;
    protected Integer paycode;

    protected LocalDate hiatus_begin;
    protected LocalDate hiatus_end;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable (
            name = "ReviewerSubjects",
            joinColumns = @JoinColumn(name = "reviewer_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    protected Collection<Subject> subjects;

    @Convert(converter = AcceptsListToJsonArrayConverter.class)
    protected List<Accepts> accepts;

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

    public List<Accepts> getAccepts() {
        return accepts;
    }

    public void setAccepts(List<Accepts> accepts) {
        this.accepts = accepts;
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

    public Reviewer withPhone(String phone) {
        this.phone = phone;
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

    public Reviewer withAccepts(List<Accepts> accepts) {
        this.accepts = accepts;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Reviewer reviewer = (Reviewer) o;

        if (active != reviewer.active) {
            return false;
        }
        if (id != null ? !id.equals(reviewer.id) : reviewer.id != null) {
            return false;
        }
        if (culrId != null ? !culrId.equals(reviewer.culrId) : reviewer.culrId != null) {
            return false;
        }
        if (firstName != null ? !firstName.equals(reviewer.firstName) : reviewer.firstName != null) {
            return false;
        }
        if (lastName != null ? !lastName.equals(reviewer.lastName) : reviewer.lastName != null) {
            return false;
        }
        if (email != null ? !email.equals(reviewer.email) : reviewer.email != null) {
            return false;
        }
        if (phone != null ? !phone.equals(reviewer.phone) : reviewer.phone != null) {
            return false;
        }
        if (address != null ? !address.equals(reviewer.address) : reviewer.address != null) {
            return false;
        }
        if (institution != null ? !institution.equals(reviewer.institution) : reviewer.institution != null) {
            return false;
        }
        if (paycode != null ? !paycode.equals(reviewer.paycode) : reviewer.paycode != null) {
            return false;
        }
        if (hiatus_begin != null ? !hiatus_begin.equals(reviewer.hiatus_begin) : reviewer.hiatus_begin != null) {
            return false;
        }
        if (hiatus_end != null ? !hiatus_end.equals(reviewer.hiatus_end) : reviewer.hiatus_end != null) {
            return false;
        }
        if (subjects != null ? !subjects.equals(reviewer.subjects) : reviewer.subjects != null) {
            return false;
        }
        return accepts != null ? accepts.equals(reviewer.accepts) : reviewer.accepts == null;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (active ? 1 : 0);
        result = 31 * result + (culrId != null ? culrId.hashCode() : 0);
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (phone != null ? phone.hashCode() : 0);
        result = 31 * result + address.hashCode();
        result = 31 * result + institution.hashCode();
        result = 31 * result + paycode.hashCode();
        result = 31 * result + (hiatus_begin != null ? hiatus_begin.hashCode() : 0);
        result = 31 * result + (hiatus_end != null ? hiatus_end.hashCode() : 0);
        result = 31 * result + (subjects != null ? subjects.hashCode() : 0);
        result = 31 * result + (accepts != null ? accepts.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Reviewer{" +
                "id=" + id +
                ", active=" + active +
                ", culrId'=" + culrId + '\'' +
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
                ", accepts=" + accepts +
                '}';
    }

    public ReviewerWithWorkloads toReviewerWithWorkloads() {
        final ReviewerWithWorkloads reviewerWithWorkloads = new ReviewerWithWorkloads();
        reviewerWithWorkloads.setId(id);
        reviewerWithWorkloads.setActive(active);
        reviewerWithWorkloads.setCulrId(culrId);
        reviewerWithWorkloads.setFirstName(firstName);
        reviewerWithWorkloads.setLastName(lastName);
        reviewerWithWorkloads.setEmail(email);
        reviewerWithWorkloads.setAddress(address);
        reviewerWithWorkloads.setInstitution(institution);
        reviewerWithWorkloads.setPaycode(paycode);
        reviewerWithWorkloads.setSubjects(subjects);
        reviewerWithWorkloads.setHiatus_begin(hiatus_begin);
        reviewerWithWorkloads.setHiatus_end(hiatus_end);
        reviewerWithWorkloads.setAccepts(accepts);
        return reviewerWithWorkloads;
    }
}
