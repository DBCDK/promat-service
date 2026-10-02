package dk.dbc.promat.service.dto;

public class Tag {
    private String name;
    private Integer value;

    public Tag() {
    }

    public Tag(String name, Integer value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "name='" + name + "', value='" + value + "'";
    }
}
