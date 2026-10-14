package dk.dbc.promat.service.connectors;

// Keeps update-service transport/status failures distinguishable from mapping
// and persistence failures - they're returned to the caller as 502.
public class CatalogingUpdateConnectorException extends Exception {
    public CatalogingUpdateConnectorException(String message) {
        super(message);
    }

    public CatalogingUpdateConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
