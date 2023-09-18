package dk.dbc.promat.service.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class CaseTasks {
    public static final String TABLE_NAME = "casetasks";

    @Id
    private int task_id;

    @Id
    private int case_id;

    public int getTask_id() {
        return task_id;
    }

    public void setTask_id(int task_id) {
        this.task_id = task_id;
    }

    public int getCase_id() {
        return case_id;
    }

    public void setCase_id(int case_id) {
        this.case_id = case_id;
    }
}
