/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.persistence.TaskType;

import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskDto {

    private TaskType taskType;

    private TaskFieldType taskFieldType;

    private List<String> targetFausts;

    private String data;

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

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
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

    public TaskDto withData(String data) {
        this.data = data;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        TaskDto taskDto = (TaskDto) o;
        return taskType == taskDto.taskType &&
                taskFieldType == taskDto.taskFieldType &&
                Objects.equals(targetFausts, taskDto.targetFausts) &&
                Objects.equals(data, taskDto.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskType, taskFieldType, targetFausts, data);
    }

    @Override
    public String toString() {
        return "TaskDto{" +
                "taskType=" + taskType +
                ", taskFieldType=" + taskFieldType +
                ", targetFausts=" + targetFausts +
                ", data='" + data + '\'' +
                '}';
    }
}
