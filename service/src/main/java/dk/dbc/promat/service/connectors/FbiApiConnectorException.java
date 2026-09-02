package dk.dbc.promat.service.connectors;

// A checked exception (extends Exception, not RuntimeException), so every
// method that can fail to talk to fbi-api - network error, bad response,
// GraphQL error - must either handle it or declare "throws
// FbiApiConnectorException". That makes fbi-api failures visible in every
// method signature that touches the connector, instead of being an
// unchecked surprise at runtime.
public class FbiApiConnectorException extends Exception {
    public FbiApiConnectorException(String message) {
        super(message);
    }
    public FbiApiConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
