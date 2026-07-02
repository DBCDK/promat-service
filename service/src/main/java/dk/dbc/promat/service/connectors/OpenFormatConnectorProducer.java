package dk.dbc.promat.service.connectors;

import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import java.time.Duration;

@ApplicationScoped
public class OpenFormatConnectorProducer {
    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response -> response.getStatus() == 500)
            .withDelay(Duration.ofSeconds(5))
            .withMaxRetries(3);

    protected OpenFormatConnectorProducer() {}

    @Produces
    public static OpenFormatConnector produce(@ConfigProperty(name = "OPENFORMAT_SERVICE_URL") String baseUrl) {
        jakarta.ws.rs.client.Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
        FailSafeHttpClient failSafeHttpClient = FailSafeHttpClient.create(client, RETRY_POLICY);
        return new OpenFormatConnector(failSafeHttpClient, baseUrl);
    }

    static void dispose(@Disposes OpenFormatConnector connector) {
        connector.close();
    }
}
