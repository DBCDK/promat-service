package dk.dbc.promat.service.connectors;

// Keeps update-service transport/status failures distinguishable from mapping
// and persistence failures in the scheduler logs/error messages.
public class CatalogingUpdateConnectorException extends Exception {
    public CatalogingUpdateConnectorException(String message) {
        super(message);
    }

    public CatalogingUpdateConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
