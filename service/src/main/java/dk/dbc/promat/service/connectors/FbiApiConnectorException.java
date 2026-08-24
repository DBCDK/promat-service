package dk.dbc.promat.service.connectors;

public class FbiApiConnectorException extends Exception {
    public FbiApiConnectorException(String message) {
        super(message);
    }
    public FbiApiConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
