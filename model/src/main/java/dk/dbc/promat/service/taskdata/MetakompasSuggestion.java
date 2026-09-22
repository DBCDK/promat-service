package dk.dbc.promat.service.taskdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// A reviewer-typed suggestion for a word not (yet) in the Metakompas taxonomy, tied to the
// category path it was suggested under - never resolved against the taxonomy tree and never
// assigned an id.
//
// Persisted as part of MetakompasTaskData in PromatTask.data, and returned as-is in the
// PUT /tasks/{taskId}/metakompas response - same fields either way, so one class serves both
// roles. ignoreUnknown guards against future drift between those two roles.
@JsonIgnoreProperties(ignoreUnknown = true)
public record MetakompasSuggestion(List<String> path, String text) {
}
