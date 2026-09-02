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
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;

import java.time.Duration;

// A CDI "producer": a factory class whose job is to build an object that
// other classes then get handed via plain @Inject (see FbiApiHandler,
// which just does `@Inject FbiApiConnector connector`). This is how you
// construct a class like FbiApiConnector - which needs config values and
// an HTTP client, not just a no-arg constructor - and still let the
// container manage its lifecycle (one shared instance per app, per
// @ApplicationScoped below).
@ApplicationScoped
public class FbiApiConnectorProducer {
    // Retry policy for talking to fbi-api: retry on network-level failures
    // (ProcessingException) or a 500 response, up to 3 times, waiting 5s
    // between attempts. This is applied automatically to every request the
    // connector makes (see FailSafeHttpClient below).
    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response -> response.getStatus() == 500)
            .withDelay(Duration.ofSeconds(5))
            .withMaxRetries(3);

    // fbi-api's login only supports the OAuth2 "password" grant, which
    // requires a username+password as well as a client id/secret. Promat
    // doesn't have per-request end users to authenticate as here, so it
    // always logs in as this fixed anonymous placeholder account.
    private static final String ANONYMOUS_USERNAME = "@";
    private static final String ANONYMOUS_PASSWORD = "@";

    protected FbiApiConnectorProducer() {}

    // @Produces marks this as the factory method CDI calls whenever
    // something needs an FbiApiConnector injected. The @ConfigProperty
    // parameters are filled in automatically from environment variables
    // (see scripts/common) - this is MicroProfile Config, the same
    // mechanism used all over this codebase to read env vars into Java.
    @Produces
    public static FbiApiConnector produce(
            @ConfigProperty(name = "FBI_API_URL") String baseUrl,
            @ConfigProperty(name = "FBI_API_LOGIN_URL", defaultValue = "https://login.bib.dk") String loginUrl,
            @ConfigProperty(name = "OAUTH2_CLIENT_ID") String clientId,
            @ConfigProperty(name = "OAUTH2_CLIENT_SECRET") String clientSecret) {
        return produce(baseUrl, loginUrl, clientId, clientSecret, UserAgent.forInternalRequests());
    }

    // Separate, non-CDI overload so tests (and FbiApiHandlerCli) can build a
    // connector directly with explicit values, without needing a full CDI
    // container running just to construct one object.
    public static FbiApiConnector produce(String baseUrl, String loginUrl, String clientId, String clientSecret,
                                           UserAgent userAgent) {
        Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature())
                .property(ClientProperties.CONNECT_TIMEOUT, 5000)   // 5 sec timeout.
                .property(ClientProperties.READ_TIMEOUT, 30000));   // 30 sec read timeout.);
        FailSafeHttpClient failSafeHttpClient = FailSafeHttpClient.create(client, userAgent, RETRY_POLICY);
        return new FbiApiConnector(failSafeHttpClient, baseUrl, loginUrl, clientId, clientSecret, ANONYMOUS_USERNAME, ANONYMOUS_PASSWORD);
    }

    // The producer's counterpart: @Disposes is called when the container
    // shuts the bean down, so the underlying HTTP client's connection pool
    // gets closed cleanly instead of leaking.
    static void dispose(@Disposes FbiApiConnector connector) {
        connector.close();
    }
}
