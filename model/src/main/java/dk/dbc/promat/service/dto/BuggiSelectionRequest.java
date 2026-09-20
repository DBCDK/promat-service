package dk.dbc.promat.service.dto;

// Request DTO for PUT /tasks/{taskId}/buggi. The client sends stable option ids from
// /buggi/options plus the selected value; the backend resolves the rest of the metadata.
public class BuggiSelectionRequest {
    private Integer id;
    private Integer value;

    public BuggiSelectionRequest() {
    }

    public BuggiSelectionRequest(Integer id, Integer value) {
        this.id = id;
        this.value = value;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
