package dk.dbc.promat.service.taskdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Resolved Buggi selection stored in PromatTask.data and returned from PUT /tasks/{taskId}/buggi.
// Keeps the stable option id and MARC registration metadata together with requiresNonzeroValue,
// which the frontend needs to render the tag's input control; marcSubfieldCode isn't needed by
// the client but is harmless to expose, so one class serves both roles instead of two.
// ignoreUnknown guards against future drift between those two roles.
@JsonIgnoreProperties(ignoreUnknown = true)
public record BuggiSelectionEntry(Integer id, String name, String marcSubfieldCode, Boolean requiresNonzeroValue, Integer value) {
}
