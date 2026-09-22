package dk.dbc.promat.service.connectors;

import dk.dbc.updateservice.UpdateServiceUpdateConnectorFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CatalogingUpdateConnectorProducer {
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
        return new CatalogingUpdateConnector(UpdateServiceUpdateConnectorFactory.create(baseUrl), schemaName, netpunktGroup,
                netpunktUser, netpunktPassword);
    }

    static void dispose(@Disposes CatalogingUpdateConnector connector) {
        // The underlying update-service REST connector owns HTTP resources.
        connector.close();
    }
}
