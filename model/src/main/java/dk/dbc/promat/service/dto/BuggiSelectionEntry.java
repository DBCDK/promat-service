package dk.dbc.promat.service.dto;

// Resolved Buggi selection stored in PromatTask.data and returned from PUT /tasks/{taskId}/buggi.
// Keeps the stable option id together with the metadata needed for later cataloging registration.
public class BuggiSelectionEntry {
    private Integer id;
    private String name;
    private String subfieldCode;
    private Boolean requiresNonzeroValue;
    private Integer value;

    public BuggiSelectionEntry() {
    }

    public BuggiSelectionEntry(Integer id, String name, String subfieldCode, Boolean requiresNonzeroValue, Integer value) {
        this.id = id;
        this.name = name;
        this.subfieldCode = subfieldCode;
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

    public String getSubfieldCode() {
        return subfieldCode;
    }

    public void setSubfieldCode(String subfieldCode) {
        this.subfieldCode = subfieldCode;
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
