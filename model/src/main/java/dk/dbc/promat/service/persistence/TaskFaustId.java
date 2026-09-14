package dk.dbc.promat.service.persistence;

import java.io.Serializable;
import java.util.Objects;

// IdClass companion for entities keyed by (task_id, faust) - a task can target more than one
// faust, and each (task, faust) pair has its own independent selection.
public class TaskFaustId implements Serializable {

    private Integer taskId;
    private String faust;

    public TaskFaustId() {
    }

    public TaskFaustId(Integer taskId, String faust) {
        this.taskId = taskId;
        this.faust = faust;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskFaustId that = (TaskFaustId) o;
        return Objects.equals(taskId, that.taskId) && Objects.equals(faust, that.faust);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, faust);
    }

    @Override
    public String toString() {
        return "TaskFaustId{" +
                "taskId=" + taskId +
                ", faust='" + faust + '\'' +
                '}';
    }
}
