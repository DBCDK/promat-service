package dk.dbc.promat.service.persistence;

import dk.dbc.commons.jpa.converter.StringListToJsonArrayConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@NamedQuery(
        name = PromatTask.GET_PAYMENT_HISTORY_NAME,
        query = PromatTask.GET_PAYMENT_HISTORY_QUERY)
@Entity
public class PromatTask {
    public static final String TABLE_NAME = "promattask";

    public static final String GET_PAYMENT_HISTORY_NAME =
            "PromatTask.get.payment.history";
    public static final String GET_PAYMENT_HISTORY_QUERY = "select distinct t.payed" +
            "                                                 from PromatTask t" +
            "                                                where t.payed is not null" +
            "                                                order by t.payed";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    private TaskType taskType;

    @Enumerated(EnumType.STRING)
    private TaskFieldType taskFieldType;

    private LocalDate created;

    @Enumerated(EnumType.STRING)
    private PayCategory payCategory;

    private LocalDate approved;

    private LocalDateTime payed;

    private String data;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = StringListToJsonArrayConverter.class)
    private List<String> targetFausts;

    private String recordId;

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

    public PayCategory getPayCategory() {
        return payCategory;
    }

    public void setPayCategory(PayCategory payCategory) {
        this.payCategory = payCategory;
    }

    public LocalDate getApproved() {
        return approved;
    }

    public void setApproved(LocalDate approved) {
        this.approved = approved;
    }

    public LocalDateTime getPayed() {
        return payed;
    }

    public void setPayed(LocalDateTime payed) {
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

    public PromatTask withPayCategory(PayCategory payCategory) {
        this.payCategory = payCategory;
        return this;
    }

    public PromatTask withApproved(LocalDate approved) {
        this.approved = approved;
        return this;
    }

    public PromatTask withPayed(LocalDateTime payed) {
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

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public boolean hasValidRecordId() {
        return recordId != null && !recordId.isEmpty();
    }

    public PromatTask withRecordId(String recordId) {
        this.recordId = recordId;
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
                payCategory == task.payCategory &&
                Objects.equals(approved, task.approved) &&
                Objects.equals(payed, task.payed) &&
                Objects.equals(data, task.data) &&
                Objects.equals(recordId, task.recordId) &&
                Objects.equals(targetFausts, task.targetFausts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, taskType, taskFieldType, created, payCategory, approved, payed, data, targetFausts, recordId);
    }
}
