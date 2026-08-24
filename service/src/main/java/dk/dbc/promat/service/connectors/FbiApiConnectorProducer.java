package dk.dbc.promat.service.connectors;

import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import java.time.Duration;

@ApplicationScoped
public class FbiApiConnectorProducer {
    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response -> response.getStatus() == 500)
            .withDelay(Duration.ofSeconds(5))
            .withMaxRetries(3);

    private static final String ANONYMOUS_USERNAME = "@";
    private static final String ANONYMOUS_PASSWORD = "@";

    protected FbiApiConnectorProducer() {}

    @Produces
    public static FbiApiConnector produce(
            @ConfigProperty(name = "FBI_API_URL") String baseUrl,
            @ConfigProperty(name = "FBI_API_LOGIN_URL") String loginUrl,
            @ConfigProperty(name = "FBI_API_CLIENT_ID") String clientId,
            @ConfigProperty(name = "FBI_API_CLIENT_SECRET") String clientSecret) {
        return produce(baseUrl, loginUrl, clientId, clientSecret, UserAgent.forInternalRequests());
    }

    public static FbiApiConnector produce(String baseUrl, String loginUrl, String clientId, String clientSecret,
                                           UserAgent userAgent) {
        Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
        FailSafeHttpClient failSafeHttpClient = FailSafeHttpClient.create(client, userAgent, RETRY_POLICY);
        return new FbiApiConnector(failSafeHttpClient, baseUrl, loginUrl, clientId, clientSecret, ANONYMOUS_USERNAME, ANONYMOUS_PASSWORD);
    }

    static void dispose(@Disposes FbiApiConnector connector) {
        connector.close();
    }
}
