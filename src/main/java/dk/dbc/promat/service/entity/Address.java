package dk.dbc.promat.service.entity;

import javax.persistence.Embeddable;

@Embeddable
public class Address {
    private String address1;
    private String address2;
    private Integer zip;
    private String city;

    public String getAddress1() {
        return address1;
    }

    public String getAddress2() {
        return address2;
    }

    public Integer getZip() {
        return zip;
    }

    public String getCity() {
        return city;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public void setZip(Integer zip) {
        this.zip = zip;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Address withAddress1(String address1) {
        this.address1 = address1;
        return this;
    }

    public Address withAddress2(String address2) {
        this.address2 = address2;
        return this;
    }

    public Address withZip(Integer zip) {
        this.zip = zip;
        return this;
    }

    public Address withCity(String city) {
        this.city = city;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Address address = (Address) o;

        if (!address1.equals(address.address1)) return false;
        if (address2 != null ? !address2.equals(address.address2) : address.address2 != null) return false;
        if (!zip.equals(address.zip)) return false;
        return city.equals(address.city);
    }

    @Override
    public int hashCode() {
        int result = address1.hashCode();
        result = 31 * result + (address2 != null ? address2.hashCode() : 0);
        result = 31 * result + zip.hashCode();
        result = 31 * result + city.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Address{" +
                "address1='" + address1 + '\'' +
                ", address2='" + address2 + '\'' +
                ", zip=" + zip +
                ", city='" + city + '\'' +
                '}';
    }
}
