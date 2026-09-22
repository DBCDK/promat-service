package dk.dbc.promat.service.taskdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// One Metakompas subject picked by a reviewer, with its tree path attached - deliberately not
// a reference into Taxonomy's category tree, so renaming/restructuring it never requires
// migrating old selections.
//
// Persisted as part of MetakompasTaskData in PromatTask.data, and returned as-is in the
// PUT /tasks/{taskId}/metakompas response - nothing here needs hiding from the client (oftenUsed/
// ref are already public via GET /taxonomy/tree), so one class serves both roles. ignoreUnknown
// guards against future drift between those two roles, the way Tag needed it once
// BuggiSelectionEntry grew fields Tag didn't expect.
@JsonIgnoreProperties(ignoreUnknown = true)
public record MetakompasSelectionEntry(List<String> path, Integer id, String title, List<String> note,
                                        Boolean oftenUsed, String ref) {
}
