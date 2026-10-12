package dk.dbc.promat.service.dto;

// Request DTO for PUT /tasks/{taskId}/reading-experience/child. The client sends stable option ids from
// /buggi/options plus the selected value; the backend resolves the rest of the metadata.
public record BuggiSelectionRequest(Integer id, Integer value) {
}
