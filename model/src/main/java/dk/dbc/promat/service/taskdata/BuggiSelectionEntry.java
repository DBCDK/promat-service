package dk.dbc.promat.service.taskdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Resolved Buggi selection stored in PromatTask.data and returned from PUT /tasks/{taskId}/buggi.
// Keeps the stable option id together with the MARC subfield code used when registering it. A value
// of 0 means "not chosen" and isn't registered - it's only allowed for groups whose range starts at 0
// (see GET /buggi/options). ignoreUnknown also lets selections saved with the former
// requiresNonzeroValue field be read.
@JsonIgnoreProperties(ignoreUnknown = true)
public record BuggiSelectionEntry(Integer id, String name, String marcSubfieldCode, Integer value) {
}
