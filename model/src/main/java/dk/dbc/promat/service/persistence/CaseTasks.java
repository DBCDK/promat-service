package dk.dbc.promat.service.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@SuppressWarnings("java:S100")
@Entity
public class CaseTasks {
    public static final String TABLE_NAME = "casetasks";

    @SuppressWarnings("java:S116")
    @Id
    private int task_id;

    @SuppressWarnings("java:S116")
    @Id
    private int case_id;

    public int getTask_id() {
        return task_id;
    }

    @SuppressWarnings("java:S117")
    public void setTask_id(int task_id) {
        this.task_id = task_id;
    }

    public int getCase_id() {
        return case_id;
    }

    @SuppressWarnings("java:S117")
    public void setCase_id(int case_id) {
        this.case_id = case_id;
    }
}
