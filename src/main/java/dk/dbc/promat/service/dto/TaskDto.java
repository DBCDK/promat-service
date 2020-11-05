package dk.dbc.promat.service.dto;

import dk.dbc.promat.service.persistence.Paycode;
import dk.dbc.promat.service.persistence.TaskType;

import java.util.List;
import java.util.Objects;

public class TaskDto {

    private TaskType typeOfTask;

    private Paycode paycode;

    private List<String> targetFausts;

    public TaskType getTypeOfTask() {
        return typeOfTask;
    }

    public void setTypeOfTask(TaskType typeOfTask) {
        this.typeOfTask = typeOfTask;
    }

    public Paycode getPaycode() {
        return paycode;
    }

    public void setPaycode(Paycode paycode) {
        this.paycode = paycode;
    }

    public List<String> getTargetFausts() {
        return targetFausts;
    }

    public void setTargetFausts(List<String> targetFausts) {
        this.targetFausts = targetFausts;
    }

    public TaskDto withTypeOfTask(TaskType typeOfTask) {
        this.typeOfTask = typeOfTask;
        return this;
    }

    public TaskDto withPaycode(Paycode paycode) {
        this.paycode = paycode;
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
        return typeOfTask == taskDto.typeOfTask &&
                paycode == taskDto.paycode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeOfTask, paycode);
    }

    @Override
    public String toString() {
        return "TaskDto{" +
                "typeOfTask=" + typeOfTask +
                ", paycode=" + paycode +
                '}';
    }
}
