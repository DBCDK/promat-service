package dk.dbc.promat.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Persisted BUGGI task data is a list of BuggiSelectionEntry (name/value plus id/marcSubfieldCode/
// requiresNonzeroValue used only when saving); registration only needs name/value, so unknown
// fields must be ignored here rather than rejected.
@JsonIgnoreProperties(ignoreUnknown = true)
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
