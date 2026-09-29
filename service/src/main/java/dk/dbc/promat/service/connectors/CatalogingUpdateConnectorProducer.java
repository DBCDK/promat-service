package dk.dbc.promat.service.connectors;

import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;

@ApplicationScoped
public class CatalogingUpdateConnectorProducer {
    // No automatic retries: registration is a write, and a failure is returned to the reviewer, who can retry
    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>().withMaxRetries(0);

    protected CatalogingUpdateConnectorProducer() {}

    // CDI producer that keeps update-service configuration and client construction
    // out of the scheduled sender. This follows the existing connector producer
    // pattern used elsewhere in promat-service.
    @Produces
    public static CatalogingUpdateConnector produce(
            @ConfigProperty(name = "UPDATE_SERVICE_URL", defaultValue = "") String baseUrl,
            @ConfigProperty(name = "UPDATE_SCHEMA_NAME", defaultValue = "") String schemaName,
            @ConfigProperty(name = "UPDATE_NETPUNKT_GROUP", defaultValue = "") String netpunktGroup,
            @ConfigProperty(name = "UPDATE_NETPUNKT_USER", defaultValue = "") String netpunktUser,
            @ConfigProperty(name = "UPDATE_NETPUNKT_PASSWORD", defaultValue = "") String netpunktPassword) {
        return produce(baseUrl, schemaName, netpunktGroup, netpunktUser, netpunktPassword, UserAgent.forInternalRequests());
    }

    public static CatalogingUpdateConnector produce(String baseUrl, String schemaName, String netpunktGroup,
                                                    String netpunktUser, String netpunktPassword, UserAgent userAgent) {
        FailSafeHttpClient failSafeHttpClient = FailSafeHttpClient.create(
                HttpClient.newClient(new ClientConfig()
                        .register(new JacksonFeature())
                        .property(ClientProperties.CONNECT_TIMEOUT, 5000)
                        .property(ClientProperties.READ_TIMEOUT, 30000)),
                userAgent, RETRY_POLICY);
        return new CatalogingUpdateConnector(failSafeHttpClient, baseUrl, schemaName, netpunktGroup, netpunktUser,
                netpunktPassword);
    }

    static void dispose(@Disposes CatalogingUpdateConnector connector) {
        connector.close();
    }
}
