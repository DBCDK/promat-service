package dk.dbc.promat.service.persistence;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Entity
public class PromatTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private TaskType taskType;

    private TaskFieldType taskFieldType;

    private LocalDate created;

    private String paycode;

    private LocalDate approved;

    private LocalDate payed;

    private String data;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonStringArrayConverter.class)
    private List<String> targetFausts;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

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

    public LocalDate getCreated() {
        return created;
    }

    public void setCreated(LocalDate created) {
        this.created = created;
    }

    public String getPaycode() {
        return paycode;
    }

    public void setPaycode(String paycode) {
        this.paycode = paycode;
    }

    public LocalDate getApproved() {
        return approved;
    }

    public void setApproved(LocalDate approved) {
        this.approved = approved;
    }

    public LocalDate getPayed() {
        return payed;
    }

    public void setPayed(LocalDate payed) {
        this.payed = payed;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public List<String> getTargetFausts() {
        return targetFausts;
    }

    public void setTargetFausts(List<String> targetFausts) {
        this.targetFausts = targetFausts;
    }

    public PromatTask withId(int id) {
        this.id = id;
        return this;
    }

    public PromatTask withTaskType(TaskType taskType) {
        this.taskType = taskType;
        return this;
    }

    public PromatTask withTaskFieldType(TaskFieldType taskFieldType) {
        this.taskFieldType = taskFieldType;
        return this;
    }

    public PromatTask withCreated(LocalDate created) {
        this.created = created;
        return this;
    }

    public PromatTask withPayCode(String paycode) {
        this.paycode = paycode;
        return this;
    }

    public PromatTask withApproved(LocalDate approved) {
        this.approved = approved;
        return this;
    }

    public PromatTask withPayed(LocalDate payed) {
        this.payed = payed;
        return this;
    }


    public PromatTask withData(String data) {
        this.data = data;
        return this;
    }

    public PromatTask withTargetFausts(List<String> targetFausts) {
        this.targetFausts = targetFausts;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        PromatTask task = (PromatTask) o;
        return id == task.id &&
                taskType == task.taskType &&
                taskFieldType == task.taskFieldType &&
                created.equals(task.created) &&
                paycode.equals(task.paycode) &&
                Objects.equals(approved, task.approved) &&
                Objects.equals(payed, task.payed) &&
                Objects.equals(data, task.data) &&
                Objects.equals(targetFausts, task.targetFausts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, taskType, taskFieldType, created, paycode, approved, payed, data, targetFausts);
    }
}
