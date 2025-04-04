package dk.dbc.promat.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import dk.dbc.promat.service.persistence.Address;
import dk.dbc.promat.service.persistence.EditorView;
import dk.dbc.promat.service.persistence.Reviewer;

import dk.dbc.promat.service.persistence.ReviewerView;
import dk.dbc.promat.service.persistence.SubjectNote;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewerRequest implements Dto {
    private Boolean active;
    @Deprecated(since = "Will not be used for reviewer creation after switch to professional login")
    private String cprNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String privateEmail;
    private String privatePhone;
    private String institution;
    private Integer paycode;
    private Address address;
    private String note;
    private Address privateAddress;

    private List<Integer> subjects;
    private List<SubjectNote> subjectNotes;
    private List<Reviewer.Accepts> accepts;
    private Integer capacity;

    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate hiatusBegin;

    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate hiatusEnd;

    protected String agency;
    protected String userId;

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public ReviewerRequest withActive(boolean active) {
        this.active = active;
        return this;
    }

    public String getCprNumber() {
        return cprNumber;
    }

    public void setCprNumber(String cprNumber) {
        this.cprNumber = cprNumber;
    }

    public ReviewerRequest withCprNumber(String cprNumber) {
        this.cprNumber = cprNumber;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public ReviewerRequest withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public ReviewerRequest withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ReviewerRequest withEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public ReviewerRequest withPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getPrivateEmail() {
        return privateEmail;
    }

    public void setPrivateEmail(String email) {
        this.privateEmail = email;
    }

    public ReviewerRequest withPrivateEmail(String email) {
        this.privateEmail = email;
        return this;
    }

    public String getPrivatePhone() {
        return privatePhone;
    }

    public void setPrivatePhone(String phone) {
        this.privatePhone = phone;
    }

    public ReviewerRequest withPrivatePhone(String phone) {
        this.privatePhone = phone;
        return this;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public ReviewerRequest withInstitution(String institution) {
        this.institution = institution;
        return this;
    }

    public Integer getPaycode() {
        return paycode;
    }

    public void setPaycode(Integer paycode) {
        this.paycode = paycode;
    }

    public ReviewerRequest withPaycode(Integer paycode) {
        this.paycode = paycode;
        return this;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public ReviewerRequest withAddress(Address address) {
        this.address = address;
        return this;
    }

    public Address getPrivateAddress() {
        return privateAddress;
    }

    public void setPrivateAddress(Address address) {
        this.privateAddress = address;
    }

    public ReviewerRequest withPrivateAddress(Address address) {
        this.privateAddress = address;
        return this;
    }

    public LocalDate getHiatusBegin() {
        return hiatusBegin;
    }

    public void setHiatusBegin(LocalDate hiatusBegin) {
        this.hiatusBegin = hiatusBegin;
    }

    public ReviewerRequest withHiatusBegin(LocalDate hiatus_begin) {
        this.hiatusBegin = hiatus_begin;
        return this;
    }

    public LocalDate getHiatusEnd() {
        return hiatusEnd;
    }

    public void setHiatusEnd(LocalDate hiatusEnd) {
        this.hiatusEnd = hiatusEnd;
    }

    public ReviewerRequest withHiatusEnd(LocalDate hiatus_end) {
        this.hiatusEnd = hiatus_end;
        return this;
    }

    public List<Integer> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<Integer> subjects) {
        this.subjects = subjects;
    }

    public ReviewerRequest withSubjects(List<Integer> subjects) {
        this.subjects = subjects;
        return this;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public List<Reviewer.Accepts> getAccepts() {
        return accepts;
    }

    public void setAccepts(List<Reviewer.Accepts> accepts) {
        this.accepts = accepts;
    }

    public ReviewerRequest withAccepts(List<Reviewer.Accepts> accepts) {
        this.accepts = accepts;
        return this;
    }

    public ReviewerRequest withCapacity(Integer capacity) {
        this.capacity = capacity;
        return this;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public ReviewerRequest withNote(String note) {
        this.note = note;
        return this;
    }

    public List<SubjectNote> getSubjectNotes() {
        return subjectNotes;
    }

    public void setSubjectNotes(List<SubjectNote> subjectNotes) {
        this.subjectNotes = subjectNotes;
    }

    public ReviewerRequest withSubjectNotes(List<SubjectNote> subjectNotes) {
        this.subjectNotes = subjectNotes;
        return this;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public ReviewerRequest withAgency(String agency) {
        this.agency = agency;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public ReviewerRequest withUserId(String userId) {
        this.userId = userId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ReviewerRequest that = (ReviewerRequest) o;
        return Objects.equals(active, that.active) && Objects.equals(cprNumber, that.cprNumber) && Objects.equals(firstName, that.firstName) && Objects.equals(lastName, that.lastName) && Objects.equals(email, that.email) && Objects.equals(phone, that.phone) && Objects.equals(privateEmail, that.privateEmail) && Objects.equals(privatePhone, that.privatePhone) && Objects.equals(institution, that.institution) && Objects.equals(paycode, that.paycode) && Objects.equals(address, that.address) && Objects.equals(note, that.note) && Objects.equals(privateAddress, that.privateAddress) && Objects.equals(subjects, that.subjects) && Objects.equals(subjectNotes, that.subjectNotes) && Objects.equals(accepts, that.accepts) && Objects.equals(capacity, that.capacity) && Objects.equals(hiatusBegin, that.hiatusBegin) && Objects.equals(hiatusEnd, that.hiatusEnd) && Objects.equals(agency, that.agency) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(active, cprNumber, firstName, lastName, email, phone, privateEmail, privatePhone, institution, paycode, address, note, privateAddress, subjects, subjectNotes, accepts, capacity, hiatusBegin, hiatusEnd, agency, userId);
    }

    @Override
    public String toString() {
        return "ReviewerRequest{" +
                "active=" + active +
                ", cprNumber='" + cprNumber + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", privateEmail='" + privateEmail + '\'' +
                ", privatePhone='" + privatePhone + '\'' +
                ", institution='" + institution + '\'' +
                ", paycode=" + paycode +
                ", address=" + address +
                ", note='" + note + '\'' +
                ", privateAddress=" + privateAddress +
                ", subjects=" + subjects +
                ", subjectNotes=" + subjectNotes +
                ", accepts=" + accepts +
                ", capacity=" + capacity +
                ", hiatusBegin=" + hiatusBegin +
                ", hiatusEnd=" + hiatusEnd +
                ", agency='" + agency + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}
