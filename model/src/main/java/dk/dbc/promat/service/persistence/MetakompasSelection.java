package dk.dbc.promat.service.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

// A reviewer's Metakompas subject selection for one (task, faust) pair. `data` holds the
// selection as a plain JSON array (see dk.dbc.promat.service.dto.MetakompasSelectionEntry),
// read back and (de)serialized by the application rather than queried at the SQL level - same
// choice as TaxonomySnapshot.data, made for the same reason (sidesteps an EclipseLink/PGobject
// jsonb binding issue).
@Entity
@IdClass(TaskFaustId.class)
@Table(name = MetakompasSelection.TABLE_NAME)
public class MetakompasSelection {
    public static final String TABLE_NAME = "metakompas_selection";

    @Id
    @Column(name = "task_id")
    private Integer taskId;

    @Id
    private String faust;

    @Column(nullable = false)
    private String data;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public String getFaust() {
        return faust;
    }

    public void setFaust(String faust) {
        this.faust = faust;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public MetakompasSelection withTaskId(Integer taskId) {
        this.taskId = taskId;
        return this;
    }

    public MetakompasSelection withFaust(String faust) {
        this.faust = faust;
        return this;
    }

    public MetakompasSelection withData(String data) {
        this.data = data;
        return this;
    }

    public MetakompasSelection withUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetakompasSelection that = (MetakompasSelection) o;
        return Objects.equals(taskId, that.taskId)
                && Objects.equals(faust, that.faust)
                && Objects.equals(data, that.data)
                && Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, faust, data, updatedAt);
    }

    @Override
    public String toString() {
        return "MetakompasSelection{" +
                "taskId=" + taskId +
                ", faust='" + faust + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
