/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.persistence;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class Address {
    @JsonView({CaseView.Case.class, ReviewerView.Reviewer.class})
    private String address1;

    @JsonView({CaseView.Case.class, ReviewerView.Reviewer.class})
    private String address2;

    @JsonView({CaseView.Case.class, ReviewerView.Reviewer.class})
    private String zip;

    @JsonView({CaseView.Case.class, ReviewerView.Reviewer.class})
    private String city;

    @JsonView({CaseView.Case.class, ReviewerView.Reviewer.class})
    private Boolean selected = null;

    public String getAddress1() {
        return address1;
    }

    public String getAddress2() {
        return address2;
    }

    public String getZip() {
        return zip;
    }

    public String getCity() {
        return city;
    }

    public boolean getSelected() {
        return selected != null && selected == true ? true : false;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public Address withAddress1(String address1) {
        this.address1 = address1;
        return this;
    }

    public Address withAddress2(String address2) {
        this.address2 = address2;
        return this;
    }

    public Address withZip(String zip) {
        this.zip = zip;
        return this;
    }

    public Address withCity(String city) {
        this.city = city;
        return this;
    }

    public Address withSelected(Boolean selected) {
        this.selected = selected;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(address1, address.address1) && Objects.equals(address2, address.address2) && Objects.equals(zip, address.zip) && Objects.equals(city, address.city) && Objects.equals(selected, address.selected);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address1, address2, zip, city, selected);
    }

    @Override
    public String toString() {
        return "Address{" +
                "address1='" + address1 + '\'' +
                ", address2='" + address2 + '\'' +
                ", zip='" + zip + '\'' +
                ", city='" + city + '\'' +
                ", selected=" + selected +
                '}';
    }
}
