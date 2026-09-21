package dk.dbc.promat.service.dto;

import java.util.List;

// Group of Buggi options exposed by GET /buggi/options.
public class BuggiOptionGroup {
    private String name;
    private String subfieldCode;
    private Boolean requiresNonzeroValue;
    private List<BuggiOption> options;

    public BuggiOptionGroup() {
    }

    public BuggiOptionGroup(String name, String subfieldCode, Boolean requiresNonzeroValue, List<BuggiOption> options) {
        this.name = name;
        this.subfieldCode = subfieldCode;
        this.requiresNonzeroValue = requiresNonzeroValue;
        this.options = options;
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

    public List<BuggiOption> getOptions() {
        return options;
    }

    public void setOptions(List<BuggiOption> options) {
        this.options = options;
    }
}
