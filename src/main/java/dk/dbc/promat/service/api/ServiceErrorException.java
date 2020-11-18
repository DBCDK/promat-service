package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.ServiceErrorDto;

public class ServiceErrorException extends Exception {

    private ServiceErrorDto serviceErrorDto = new ServiceErrorDto();

    public ServiceErrorException(String reason) {
        super(reason);
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
}
