package dk.dbc.promat.service.dto;

import jakarta.ws.rs.core.Response;

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

    public static Response InvalidRequest(String cause, String details) {
        ServiceErrorDto err = new ServiceErrorDto()
                .withCode(ServiceErrorCode.INVALID_REQUEST)
                .withCause(cause)
                .withDetails(details);
        return Response.status(400).entity(err).build();
    }

    public static Response FaustInUse(String details) {
        ServiceErrorDto err = new ServiceErrorDto()
                .withCode(ServiceErrorCode.FAUST_IN_USE)
                .withCause("Case exists")
                .withDetails(details);
        return Response.status(409).entity(err).build();
    }

    public static Response InvalidState(String details) {
        ServiceErrorDto err = new ServiceErrorDto()
                .withCode(ServiceErrorCode.INVALID_STATE)
                .withCause("Invalid state")
                .withDetails(details);
        return Response.status(400).entity(err).build();
    }

    public static Response Failed(String details) {
        ServiceErrorDto err = new ServiceErrorDto()
                .withCode(ServiceErrorCode.FAILED)
                .withCause("Request failed")
                .withDetails(details);
        return Response.serverError().entity(err).build();
    }

    public static Response NotFound(String cause, String details) {
        ServiceErrorDto err = new ServiceErrorDto()
                .withCode(ServiceErrorCode.NOT_FOUND)
                .withCause(cause)
                .withDetails(details);
        return Response.status(404).entity(err).build();
    }

     public static Response Forbidden(String cause, String details) {
        ServiceErrorDto err = new ServiceErrorDto()
                .withCode(ServiceErrorCode.FORBIDDEN)
                .withCause(cause)
                .withDetails(details);
        return Response.status(401).entity(err).build();
    }
}
