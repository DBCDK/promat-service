package dk.dbc.promat.service.dto;

import java.util.List;

// Group of Buggi options exposed by GET /buggi/options.
public class BuggiOptionGroup {
    private String name;
    private String marcSubfieldCode;
    private Boolean requiresNonzeroValue;
    private List<BuggiOption> options;

    public BuggiOptionGroup() {
    }

    public BuggiOptionGroup(String name, String marcSubfieldCode, Boolean requiresNonzeroValue, List<BuggiOption> options) {
        this.name = name;
        this.marcSubfieldCode = marcSubfieldCode;
        this.requiresNonzeroValue = requiresNonzeroValue;
        this.options = options;
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

    public List<BuggiOption> getOptions() {
        return options;
    }

    public void setOptions(List<BuggiOption> options) {
        this.options = options;
    }
}
