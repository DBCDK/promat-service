package dk.dbc.promat.service.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import dk.dbc.promat.service.dto.ReviewerWithWorkloads;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SqlResultSetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
            "FROM promatuser pu LEFT JOIN promatcase pc ON pc.reviewer_id=pu.id AND pc.status not in ('CREATED', 'DELETED', 'REJECTED', 'PENDING_CLOSE') AND pc.deadline BETWEEN ?7 AND ?8 " +
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

    @PrePersist
    @PreUpdate
    private void beforeUpdate() {
        if( address != null && privateAddress != null ) {
            if( address.getSelected() == true && privateAddress.getSelected() == true ) {
                privateAddress.setSelected(false);
            } else if( address.getSelected() == false && privateAddress.getSelected() == false ) {
                address.setSelected(true);
            }
        }
    }

    @Embedded
    @JsonView({ReviewerView.Reviewer.class})
    protected Address address;

    @AttributeOverrides({
        @AttributeOverride(name="address1",column=@Column(name="privateAddress1")),
        @AttributeOverride(name="address2",column=@Column(name="privateAddress2")),
        @AttributeOverride(name="zip",column=@Column(name="privateZip")),
        @AttributeOverride(name="city",column=@Column(name="privateCity")),
        @AttributeOverride(name="selected",column=@Column(name="privateSelected")),
    })
    @Embedded
    @JsonView({ReviewerView.Reviewer.class})
    protected Address privateAddress;

    @JsonView({CaseView.Summary.class, CaseView.Case.class, ReviewerView.Summary.class, ReviewerView.Reviewer.class})
    protected String institution;

    @JsonView({CaseView.Summary.class, CaseView.Case.class, ReviewerView.Summary.class})
    protected Integer paycode;

    @JsonView({CaseView.Summary.class, CaseView.Case.class, ReviewerView.Summary.class})
    @JsonProperty("hiatus_begin")
    @Column(name = "hiatus_begin")
    protected LocalDate hiatusBegin;

    @JsonView({CaseView.Summary.class, CaseView.Case.class, ReviewerView.Summary.class})
    @JsonProperty("hiatus_end")
    @Column(name = "hiatus_end")
    protected LocalDate hiatusEnd;

    protected String note;

    protected Integer capacity;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable (
            name = "ReviewerSubjects",
            joinColumns = @JoinColumn(name = "reviewer_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    @JsonView({CaseView.Summary.class, CaseView.Case.class, ReviewerView.Summary.class})
    protected Collection<Subject> subjects;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable (
            name = "ReviewerSubjectNotes",
            joinColumns = @JoinColumn(name = "reviewer_id"),
            inverseJoinColumns = @JoinColumn(name = "subjectnote_id")
    )
    @JsonView({CaseView.Summary.class, CaseView.Case.class, ReviewerView.Summary.class})
    protected Collection<SubjectNote> subjectNotes;

    @Convert(converter = AcceptsListToJsonArrayConverter.class)
    @JsonView({CaseView.Summary.class, CaseView.Case.class, ReviewerView.Summary.class})
    protected List<Accepts> accepts;

    @JsonView({ReviewerView.Reviewer.class})
    protected String privateEmail;

    @JsonView({ReviewerView.Reviewer.class})
    protected String privatePhone;

    // lastChanged is NOT part of equals and hashcode.
    // This is totally on purpose, as there is no need no
    // check the update datetimestamp apart from all other
    // changes going on.
    @JsonView({ReviewerView.Reviewer.class})
    protected LocalDateTime lastChanged;

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Address getPrivateAddress() {
        return privateAddress;
    }

    public void setPrivateAddress(Address address) {
        this.privateAddress = address;
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

    public LocalDate getHiatusBegin() {
        return hiatusBegin;
    }

    public void setHiatusBegin(LocalDate hiatus_begin) {
        this.hiatusBegin = hiatus_begin;
    }

    public Reviewer withHiatusBegin(LocalDate hiatus_begin) {
        this.hiatusBegin = hiatus_begin;
        return this;
    }

    public LocalDate getHiatusEnd() {
        return hiatusEnd;
    }

    public void setHiatusEnd(LocalDate hiatus_end) {
        this.hiatusEnd = hiatus_end;
    }

    public Reviewer withHiatusEnd(LocalDate hiatus_end) {
        this.hiatusEnd = hiatus_end;
        return this;
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

    public String getNote() { return note; }

    public void setNote(String note) { this.note = note; }

    public String getPrivateEmail() {
        return privateEmail;
    }

    public void setPrivateEmail(String email) {
        this.privateEmail = email;
    }

    public String getPrivatePhone() {
        return privatePhone;
    }

    public void setPrivatePhone(String phone) {
        this.privatePhone = phone;
    }

    public Collection<SubjectNote> getSubjectNotes() {
        return subjectNotes;
    }

    public void setSubjectNotes(Collection<SubjectNote> subjectNotes) {
        this.subjectNotes = subjectNotes;
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

    public Reviewer withPrivateEmail(String email) {
        this.privateEmail = email;
        return this;
    }

    public Reviewer withPrivatePhone(String phone) {
        this.privatePhone = phone;
        return this;
    }

    public Reviewer withAddress(Address address) {
        this.address = address;
        return this;
    }

    public Reviewer withPrivateAddress(Address address) {
        this.privateAddress = address;
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
        this.hiatusBegin = hiatus_begin;
        return this;
    }

    public Reviewer withHiatus_end(LocalDate hiatus_end) {
        this.hiatusEnd = hiatus_end;
        return this;
    }

    public Reviewer withAccepts(List<Accepts> accepts) {
        this.accepts = accepts;
        return this;
    }

    public Reviewer withNote(String note) {
        this.note = note;
        return this;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Reviewer withCapacity(Integer capacity) {
        this.capacity = capacity;
        return this;
    }

    public Reviewer withSubjectNotes(List<SubjectNote> subjectNotes) {
        this.subjectNotes = subjectNotes;
        return this;
    }

    public LocalDateTime getLastChanged() {
        return lastChanged;
    }

    public Reviewer withLastChanged(LocalDateTime lastChanged) {
        this.lastChanged = lastChanged;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Reviewer)) {
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
        if (privateEmail != null ? !privateEmail.equals(reviewer.privateEmail) : reviewer.privateEmail != null) {
            return false;
        }
        if (privatePhone != null ? !privatePhone.equals(reviewer.privatePhone) : reviewer.privatePhone != null) {
            return false;
        }
        if (address != null ? !address.equals(reviewer.address) : reviewer.address != null) {
            return false;
        }
        if (privateAddress != null ? !privateAddress.equals(reviewer.privateAddress) : reviewer.privateAddress != null) {
            return false;
        }
        if (institution != null ? !institution.equals(reviewer.institution) : reviewer.institution != null) {
            return false;
        }
        if (paycode != null ? !paycode.equals(reviewer.paycode) : reviewer.paycode != null) {
            return false;
        }
        if (hiatusBegin != null ? !hiatusBegin.equals(reviewer.hiatusBegin) : reviewer.hiatusBegin != null) {
            return false;
        }
        if (hiatusEnd != null ? !hiatusEnd.equals(reviewer.hiatusEnd) : reviewer.hiatusEnd != null) {
            return false;
        }
        if (note != null ? !note.equals(reviewer.note) : reviewer.note != null) {
            return false;
        }
        if (capacity != null ? !capacity.equals(reviewer.capacity) : reviewer.capacity != null) {
            return false;
        }
        if (subjects != null ? !subjects.equals(reviewer.subjects) : reviewer.subjects != null) {
            return false;
        }
        if (accepts != null ? !accepts.equals(reviewer.accepts) : reviewer.accepts != null) {
            return false;
        }
        if (activeChanged != null ? !activeChanged.equals(reviewer.activeChanged) : reviewer.activeChanged != null) {
            return false;
        }
        if (deactivated != null ? !deactivated.equals(reviewer.activeChanged) : reviewer.deactivated != null) {
            return false;
        }
        return subjectNotes != null ? subjectNotes.equals(reviewer.subjectNotes) : reviewer.subjectNotes == null;
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
        result = 31 * result + (privateEmail != null ? privateEmail.hashCode() : 0);
        result = 31 * result + (privatePhone != null ? privatePhone.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (privateAddress != null ? privateAddress.hashCode() : 0);
        result = 31 * result + institution.hashCode();
        result = 31 * result + paycode.hashCode();
        result = 31 * result + (hiatusBegin != null ? hiatusBegin.hashCode() : 0);
        result = 31 * result + (hiatusEnd != null ? hiatusEnd.hashCode() : 0);
        result = 31 * result + (note != null ? note.hashCode() : 0);
        result = 31 * result + (capacity != null ? capacity.hashCode() : 0);
        result = 31 * result + (subjects != null ? subjects.hashCode() : 0);
        result = 31 * result + (accepts != null ? accepts.hashCode() : 0);
        result = 31 * result + (subjectNotes != null ? subjectNotes.hashCode() : 0);
        result = 31 * result + (activeChanged != null ? activeChanged.hashCode() : 0);
        result = 31 * result + (deactivated != null ? deactivated.hashCode() : 0);
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
                ", privateEmail='" + privateEmail + '\'' +
                ", privatePhone='" + privatePhone + '\'' +
                ", address=" + address +
                ", privateAddress=" + privateAddress +
                ", institution='" + institution + '\'' +
                ", paycode=" + paycode +
                ", hiatus_begin=" + hiatusBegin +
                ", hiatus_end=" + hiatusEnd +
                ", subjects=" + subjects +
                ", accepts=" + accepts +
                ", note='" + note + '\'' +
                ", capacity=" + capacity +
                ", subjectNotes=" + subjectNotes +
                ", activeChanged='" + activeChanged + '\'' +
                ", deactivated='" + deactivated + '\'' +
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
        reviewerWithWorkloads.setPrivateEmail(privateEmail);
        reviewerWithWorkloads.setAddress(address);
        reviewerWithWorkloads.setPrivateAddress(privateAddress);
        reviewerWithWorkloads.setInstitution(institution);
        reviewerWithWorkloads.setPaycode(paycode);
        reviewerWithWorkloads.setSubjects(subjects);
        reviewerWithWorkloads.setHiatusBegin(hiatusBegin);
        reviewerWithWorkloads.setHiatusEnd(hiatusEnd);
        reviewerWithWorkloads.setAccepts(accepts);
        reviewerWithWorkloads.setNote(note);
        reviewerWithWorkloads.setCapacity(capacity);
        reviewerWithWorkloads.setPhone(phone);
        reviewerWithWorkloads.setPrivatePhone(privatePhone);
        reviewerWithWorkloads.setSubjectNotes(subjectNotes);
        return reviewerWithWorkloads;
    }
}
