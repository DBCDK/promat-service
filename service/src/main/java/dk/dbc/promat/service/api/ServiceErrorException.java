package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.ServiceErrorDto;

public class ServiceErrorException extends Exception {

    private ServiceErrorDto serviceErrorDto = new ServiceErrorDto();

    private int httpStatus = 500;

    public ServiceErrorException(String reason) {
        super(reason);
        serviceErrorDto.setCause(reason);
    }

    public ServiceErrorException withHttpStatus(int status) {
        this.httpStatus = status;
        return this;
    }

    public ServiceErrorException withServiceErrorDto(ServiceErrorDto dto) {
        this.serviceErrorDto = dto;
        return this;
    }

    public ServiceErrorException withCode(ServiceErrorCode code) {
        this.serviceErrorDto.setCode(code);
        return this;
    }

    public ServiceErrorException withCause(String cause) {
        this.serviceErrorDto.setCause(cause);
        return this;
    }

    public ServiceErrorException withDetails(String details) {
        this.serviceErrorDto.setDetails(details);
        return this;
    }

    public ServiceErrorDto getServiceErrorDto() {
        return this.serviceErrorDto;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
