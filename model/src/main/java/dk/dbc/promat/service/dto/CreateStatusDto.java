package dk.dbc.promat.service.dto;

public class CreateStatusDto {
    CreateStatus createStatus;

    public CreateStatus getCreateStatus() {
        return this.createStatus;
    }

    public void setCreateStatus(CreateStatus createStatus) {
        this.createStatus = createStatus;
    }

    public CreateStatusDto withCreateStatus(boolean bool) {
        return this.withCreateStatus(bool ? CreateStatus.READY_FOR_CREATION : CreateStatus.IN_ACTIVE_CASE);
    }

    public CreateStatusDto withCreateStatus(CreateStatus createStatus) {
        this.createStatus = createStatus;
        return this;
    }

}
