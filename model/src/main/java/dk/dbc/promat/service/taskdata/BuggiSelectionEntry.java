package dk.dbc.promat.service.taskdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Resolved Buggi selection stored in PromatTask.data and returned from PUT /tasks/{taskId}/reading-experience/child.
// Keeps the stable option id together with the MARC subfield code used when registering it. A value
// of 0 means "no value" (not chosen, or not filled in yet) and isn't registered.
@JsonIgnoreProperties(ignoreUnknown = true)
public record BuggiSelectionEntry(Integer id, String name, String marcSubfieldCode, Integer value) {
}
