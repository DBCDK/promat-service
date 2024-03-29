package dk.dbc.promat.service.connector;

import dk.dbc.httpclient.HttpClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;

/**
 * Promat service connector factory
 * <p>
 * Synopsis:
 * </p>
 * <pre>
 *    // New instance
 *    PromatServiceConnector psc = PromatServiceConnector.create("http://promat-service");
 *
 *    // Singleton instance in CDI enabled environment
 *    {@literal @}Inject
 *    PromatServiceConnectorFactory factory;
 *    ...
 *    PromatServiceConnector psc = factory.getInstance();
 *
 *    // or simply
 *    {@literal @}Inject
 *    PromatServiceConnector psc;
 * </pre>
 * <p>
 * CDI case depends on the promat service baseurl being defined as
 * the value of either a system property or environment variable
 * named PROMAT_SERVICE_URL.
 * </p>
 */
@ApplicationScoped
public class PromatServiceConnectorFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(PromatServiceConnectorFactory.class);

    public static PromatServiceConnector create(String promatServiceBaseUrl) {
        final Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonConfig())
                .register(new JacksonFeature()));
        LOGGER.info("Creating PromatServiceConnector for: {}", promatServiceBaseUrl);
        return new PromatServiceConnector(client, promatServiceBaseUrl);
    }

    @Inject
    @ConfigProperty(name = "PROMAT_SERVICE_URL")
    private String promatServiceBaseUrl;

    PromatServiceConnector promatServiceConnector;

    @PostConstruct
    public void initializeConnector() {
        promatServiceConnector = PromatServiceConnectorFactory.create(promatServiceBaseUrl);
    }

    @Produces
    public PromatServiceConnector getInstance() {
        return promatServiceConnector;
    }

    @PreDestroy
    public void tearDownConnector() {
        promatServiceConnector.close();
    }
}
