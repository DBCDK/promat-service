package dk.dbc.promat.service.dto;

import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.persistence.TaskType;

import java.util.List;
import java.util.Objects;

public class TaskDto {

    private TaskType taskType;

    private TaskFieldType taskFieldType;

    private List<String> targetFausts;

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public TaskFieldType getTaskFieldType() {
        return taskFieldType;
    }

    public void setTaskFieldType(TaskFieldType taskFieldType) {
        this.taskFieldType = taskFieldType;
    }

    public List<String> getTargetFausts() {
        return targetFausts;
    }

    public void setTargetFausts(List<String> targetFausts) {
        this.targetFausts = targetFausts;
    }

    public TaskDto withTaskType(TaskType taskType) {
        this.taskType = taskType;
        return this;
    }

    public TaskDto withTaskFieldType(TaskFieldType taskFieldType) {
        this.taskFieldType = taskFieldType;
        return this;
    }

    public TaskDto withTargetFausts(List<String> targetFausts) {
        this.targetFausts = targetFausts;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        TaskDto taskDto = (TaskDto) o;
        return taskType == taskDto.taskType &&
               taskFieldType == taskDto.taskFieldType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskType, taskFieldType);
    }

    @Override
    public String toString() {
        return "TaskDto{" +
                "taskType=" + taskType +
                "taskFieldType=" + taskFieldType +
                '}';
    }
}
