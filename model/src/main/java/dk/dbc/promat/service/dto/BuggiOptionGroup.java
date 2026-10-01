package dk.dbc.promat.service.dto;

import java.util.List;

// Group of Buggi options exposed by GET /buggi/options. minValue/maxValue is the value range for the
// group's options; 0 (where allowed) means "not chosen".
public class BuggiOptionGroup {
    private String name;
    private String marcSubfieldCode;
    private Integer minValue;
    private Integer maxValue;
    private List<BuggiOption> options;

    public BuggiOptionGroup() {
    }

    public BuggiOptionGroup(String name, String marcSubfieldCode, Integer minValue, Integer maxValue, List<BuggiOption> options) {
        this.name = name;
        this.marcSubfieldCode = marcSubfieldCode;
        this.minValue = minValue;
        this.maxValue = maxValue;
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

    public Integer getMinValue() {
        return minValue;
    }

    public void setMinValue(Integer minValue) {
        this.minValue = minValue;
    }

    public Integer getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Integer maxValue) {
        this.maxValue = maxValue;
    }

    public List<BuggiOption> getOptions() {
        return options;
    }

    public void setOptions(List<BuggiOption> options) {
        this.options = options;
    }
}
