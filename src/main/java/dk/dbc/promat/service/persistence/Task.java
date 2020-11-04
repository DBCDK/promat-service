package dk.dbc.promat.service.persistence;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDate;
import java.util.Objects;

@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private TaskType typeOfTask;

    private LocalDate created;

    private Paycode paycode;

    private LocalDate approved;

    private LocalDate payed;

    private String data;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TaskType getTypeOfTask() {
        return typeOfTask;
    }

    public void setTypeOfTask(TaskType typeOfTask) {
        this.typeOfTask = typeOfTask;
    }

    public LocalDate getCreated() {
        return created;
    }

    public void setCreated(LocalDate created) {
        this.created = created;
    }

    public Paycode getPaycode() {
        return paycode;
    }

    public void setPaycode(Paycode paycode) {
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

    public Task withId(int id) {
        this.id = id;
        return this;
    }

    public Task withTypeOfTask(TaskType typeOfTask) {
        this.typeOfTask = typeOfTask;
        return this;
    }

    public Task withCreated(LocalDate created) {
        this.created = created;
        return this;
    }

    public Task withPaycode(Paycode paycode) {
        this.paycode = paycode;
        return this;
    }

    public Task withApproved(LocalDate approved) {
        this.approved = approved;
        return this;
    }

    public Task withPayed(LocalDate payed) {
        this.payed = payed;
        return this;
    }


    public Task withData(String data) {
        this.data = data;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id &&
                typeOfTask == task.typeOfTask &&
                created.equals(task.created) &&
                paycode == task.paycode &&
                Objects.equals(approved, task.approved) &&
                Objects.equals(payed, task.payed) &&
                Objects.equals(data, task.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, typeOfTask, created, paycode, approved, payed, data);
    }
}
