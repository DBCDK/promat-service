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

    // CDI producer that keeps update-service configuration and client construction out of the
    // selection logic, following the connector producer pattern used elsewhere in promat-service.
    @Produces
    public static CatalogingUpdateConnector produce(
            @ConfigProperty(name = "UPDATE_SERVICE_URL") String baseUrl,
            // The same in every environment - which update-service is used is decided by UPDATE_SERVICE_URL
            @ConfigProperty(name = "UPDATE_PROVIDER_NAME", defaultValue = "fbs-update") String providerName,
            @ConfigProperty(name = "METAKOMPAS_NETPUNKT_GROUP") String metakompasGroup,
            @ConfigProperty(name = "METAKOMPAS_NETPUNKT_USER") String metakompasUser,
            @ConfigProperty(name = "METAKOMPAS_NETPUNKT_PASSWORD") String metakompasPassword,
            @ConfigProperty(name = "BUGGI_NETPUNKT_GROUP") String buggiGroup,
            @ConfigProperty(name = "BUGGI_NETPUNKT_USER") String buggiUser,
            @ConfigProperty(name = "BUGGI_NETPUNKT_PASSWORD") String buggiPassword) {
        return produce(baseUrl, providerName,
                new CatalogingUpdateConnector.NetpunktCredentials(metakompasGroup, metakompasUser, metakompasPassword),
                new CatalogingUpdateConnector.NetpunktCredentials(buggiGroup, buggiUser, buggiPassword),
                UserAgent.forInternalRequests());
    }

    public static CatalogingUpdateConnector produce(String baseUrl, String providerName,
                                                    CatalogingUpdateConnector.NetpunktCredentials metakompasCredentials,
                                                    CatalogingUpdateConnector.NetpunktCredentials buggiCredentials,
                                                    UserAgent userAgent) {
        FailSafeHttpClient failSafeHttpClient = FailSafeHttpClient.create(
                HttpClient.newClient(new ClientConfig()
                        .register(new JacksonFeature())
                        .property(ClientProperties.CONNECT_TIMEOUT, 5000)
                        .property(ClientProperties.READ_TIMEOUT, 30000)),
                userAgent, RETRY_POLICY);
        return new CatalogingUpdateConnector(failSafeHttpClient, baseUrl, providerName, metakompasCredentials, buggiCredentials);
    }

    static void dispose(@Disposes CatalogingUpdateConnector connector) {
        connector.close();
    }
}
