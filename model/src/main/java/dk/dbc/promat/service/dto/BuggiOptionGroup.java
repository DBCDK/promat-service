package dk.dbc.promat.service.dto;

import java.util.List;

// Group of Buggi options exposed by GET /buggi/options. minValue/maxValue is the value range for the
// group's options; 0 (where allowed) means "not chosen".
public record BuggiOptionGroup(String name, String marcSubfieldCode, Integer minValue, Integer maxValue,
                               List<BuggiOption> options) {
}
