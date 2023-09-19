package dk.dbc.promat.service.connector;

import dk.dbc.promat.service.dto.ServiceErrorDto;

public class PromatServiceConnectorUnexpectedStatusCodeException extends PromatServiceConnectorException {
    private final int statusCode;
    private final ServiceErrorDto serviceErrorDto;

    public PromatServiceConnectorUnexpectedStatusCodeException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.serviceErrorDto = null;
    }

    public PromatServiceConnectorUnexpectedStatusCodeException(ServiceErrorDto serviceError, int statusCode) {
        super(serviceError.getCause());
        this.statusCode = statusCode;
        this.serviceErrorDto = serviceError;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public ServiceErrorDto getServiceErrorDto() {
        return serviceErrorDto;
    }
}
