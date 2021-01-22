/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EditorRequest implements Dto {

    private Boolean active;
    private String cprNumber;
    private String firstName;
    private String lastName;
    private String email;
    private Integer paycode;

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public EditorRequest withActive(boolean active) {
        this.active = active;
        return this;
    }

    public String getCprNumber() {
        return cprNumber;
    }

    public void setCprNumber(String cprNumber) {
        this.cprNumber = cprNumber;
    }

    public EditorRequest withCprNumber(String cprNumber) {
        this.cprNumber = cprNumber;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public EditorRequest withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public EditorRequest withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public EditorRequest withEmail(String email) {
        this.email = email;
        return this;
    }

    public Integer getPaycode() {
        return paycode;
    }

    public void setPaycode(Integer paycode) {
        this.paycode = paycode;
    }

    public EditorRequest withPaycode(Integer paycode) {
        this.paycode = paycode;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        EditorRequest that = (EditorRequest) o;
        return Objects.equals(active, that.active) && Objects.equals(cprNumber, that.cprNumber) && Objects.equals(firstName, that.firstName) && Objects.equals(lastName, that.lastName) && Objects.equals(email, that.email) && Objects.equals(paycode, that.paycode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(active, cprNumber, firstName, lastName, email, paycode);
    }

    @Override
    public String toString() {
        return "EditorRequest{" +
                "active=" + active +
                ", cprNumber='" + cprNumber + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", paycode=" + paycode +
                '}';
    }
}
