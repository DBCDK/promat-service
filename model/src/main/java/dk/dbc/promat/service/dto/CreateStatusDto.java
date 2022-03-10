package dk.dbc.promat.service.dto;

public class CreateStatusDto {
    CreateStatus createStatus;

    public CreateStatus getCreateStatus() {
        return this.createStatus;
    }

    public void setCreateStatus(CreateStatus createStatus) {
        this.createStatus = createStatus;
    }

    public CreateStatusDto withCreateStatus(boolean notFound) {
        return this.withCreateStatus(notFound ? CreateStatus.READY_FOR_CREATION : CreateStatus.IN_ACTIVE_CASE);
    }

    public CreateStatusDto withCreateStatus(CreateStatus createStatus) {
        this.createStatus = createStatus;
        return this;
    }

    @Override
    public String toString() {
        return "CreateStatusDto{" +
                "createStatus=" + createStatus +
                '}';
    }
}
