package dk.dbc.promat.service.dto;

public class ServiceErrorDto implements Dto {

    private ServiceErrorCode code;

    private String cause;

    private String details;

    public ServiceErrorCode getCode() {
        return code;
    }

    public void setCode(ServiceErrorCode code) {
        this.code = code;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public ServiceErrorDto withCode(ServiceErrorCode code) {
        this.code = code;
        return this;
    }

    public ServiceErrorDto withCause(String cause) {
        this.cause = cause;
        return this;
    }

    public ServiceErrorDto withDetails(String details) {
        this.details = details;
        return this;
    }
}
