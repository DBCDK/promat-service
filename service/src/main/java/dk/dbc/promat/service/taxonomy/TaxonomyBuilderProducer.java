package dk.dbc.promat.service.taxonomy;

import dk.dbc.promat.service.persistence.PromatEntityManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Optional;

// Decides which TaxonomyBuilder implementation gets injected wherever one is needed
// (TaxonomyCache) - callers never know or care whether they got a DbTaxonomyBuilder or a
// DM2Builder.
@ApplicationScoped
public class TaxonomyBuilderProducer {
    private final EntityManager entityManager;
    private final Optional<String> recordService;
    private final Duration readTimeout;
    private final Optional<String> taxonomyKafkaBootstrapServers;
    private final Optional<String> taxonomyKafkaTopic;

    @Inject
    public TaxonomyBuilderProducer(@PromatEntityManager EntityManager entityManager,
                                   @ConfigProperty(name = "RECORD_SERVICE") Optional<String> recordService,
                                   @ConfigProperty(name = "TOPICS_FETCH_READ_TIMEOUT", defaultValue = "PT20S") Duration readTimeout,
                                   @ConfigProperty(name = "TAXONOMY_KAFKA_BOOTSTRAP_SERVERS") Optional<String> taxonomyKafkaBootstrapServers,
                                   @ConfigProperty(name = "TAXONOMY_KAFKA_TOPIC") Optional<String> taxonomyKafkaTopic) {
        this.entityManager = entityManager;
        this.recordService = recordService;
        this.readTimeout = readTimeout;
        this.taxonomyKafkaBootstrapServers = taxonomyKafkaBootstrapServers;
        this.taxonomyKafkaTopic = taxonomyKafkaTopic;
    }

    // Prefer the Kafka-backed DbTaxonomyBuilder whenever both Kafka settings are configured;
    // otherwise fall back to the older HTTP-based DM2Builder if RECORD_SERVICE is set;
    // otherwise null (TaxonomyCache treats a null builder as "never refreshes", logging an
    // error instead of crashing).
    @Produces
    public TaxonomyBuilder produce() {
        if (isConfigured(taxonomyKafkaBootstrapServers) && isConfigured(taxonomyKafkaTopic)) {
            return new DbTaxonomyBuilder(entityManager);
        }
        return recordService
                .filter(url -> !url.isBlank())
                .map(url -> (TaxonomyBuilder) new DM2Builder(url, readTimeout))
                .orElse(null);
    }

    private static boolean isConfigured(Optional<String> value) {
        return value.filter(v -> !v.isBlank()).isPresent();
    }
}
