package dk.dbc.promat.service.taskdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Resolved Buggi selection stored in PromatTask.data and returned from PUT /tasks/{taskId}/buggi.
// Keeps the stable option id together with MARC registration metadata: marcSubfieldCode, and
// requiresNonzeroValue (the tag is only registered if its value isn't 0). Neither is needed by the
// client - the input control is rendered from the group's range in GET /buggi/options.
// ignoreUnknown guards against future drift between those two roles.
@JsonIgnoreProperties(ignoreUnknown = true)
public record BuggiSelectionEntry(Integer id, String name, String marcSubfieldCode, Boolean requiresNonzeroValue, Integer value) {
}
