package dk.dbc.promat.service.taskdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Resolved Buggi selection stored in PromatTask.data and returned from PUT /tasks/{taskId}/buggi.
// Keeps the stable option id and MARC registration metadata together with requiresNonzeroValue,
// which the frontend needs to render the tag's input control; marcSubfieldCode isn't needed by
// the client but is harmless to expose, so one class serves both roles instead of two.
// ignoreUnknown guards against future drift between those two roles.
@JsonIgnoreProperties(ignoreUnknown = true)
public class BuggiSelectionEntry {
    private Integer id;
    private String name;
    private String marcSubfieldCode;
    private Boolean requiresNonzeroValue;
    private Integer value;

    public BuggiSelectionEntry() {
    }

    public BuggiSelectionEntry(Integer id, String name, String marcSubfieldCode, Boolean requiresNonzeroValue, Integer value) {
        this.id = id;
        this.name = name;
        this.marcSubfieldCode = marcSubfieldCode;
        this.requiresNonzeroValue = requiresNonzeroValue;
        this.value = value;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMarcSubfieldCode() {
        return marcSubfieldCode;
    }

    public void setMarcSubfieldCode(String marcSubfieldCode) {
        this.marcSubfieldCode = marcSubfieldCode;
    }

    public Boolean getRequiresNonzeroValue() {
        return requiresNonzeroValue;
    }

    public void setRequiresNonzeroValue(Boolean requiresNonzeroValue) {
        this.requiresNonzeroValue = requiresNonzeroValue;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
