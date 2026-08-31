package dk.dbc.promat.service.taxonomy;

import dk.dbc.promat.service.persistence.PromatEntityManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Optional;

// @ApplicationScoped is a CDI "scope" annotation: one instance of this bean is created and
// reused for the whole application's lifetime (similar in spirit to @Singleton, but
// @ApplicationScoped is CDI's own concept rather than the EJB spec's - this class isn't an
// EJB at all, just a plain CDI-managed bean).
//
// This class exists purely to have a @Produces method (see below) - everything else about it
// (the constructor, the fields) just exists to gather the configuration that method needs.
@ApplicationScoped
public class TaxonomyBuilderProducer {
    private final EntityManager entityManager;
    private final Optional<String> recordService;
    private final Duration readTimeout;
    private final Optional<String> taxonomyKafkaBootstrapServers;
    private final Optional<String> taxonomyKafkaTopic;

    // Constructor injection: an alternative to field injection (the `@Inject` on a field
    // directly, as seen in most other classes in this codebase) where the container instead
    // calls this constructor, supplying every parameter itself. Advantages some teams prefer:
    // the fields can be `final` (as they are here - once constructed, they never change), and
    // a class built this way can be constructed and unit-tested with plain `new` + hand-made
    // arguments, without needing a full CDI container running.
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

    // @Produces is CDI's way of saying "when something asks to @Inject a TaxonomyBuilder,
    // call THIS method and hand out whatever it returns" - it turns a plain method into a
    // factory for a type (TaxonomyBuilder, the interface) that has no @Inject-able
    // constructor of its own, and lets the *decision* of which concrete implementation to use
    // live in one place, driven by configuration, instead of being hardcoded at every
    // injection site. Whoever injects a TaxonomyBuilder elsewhere in the codebase (TaxonomyCache)
    // has no idea whether they're getting a DbTaxonomyBuilder or a DM2Builder - and doesn't
    // need to.
    //
    // The actual choice: prefer the Kafka-backed DbTaxonomyBuilder whenever both Kafka
    // settings are configured: otherwise fall back to the older HTTP-based DM2Builder if
    // RECORD_SERVICE is set; otherwise return null (TaxonomyCache handles a null builder by
    // simply never refreshing, logging an error instead of crashing).
    @Produces
    public TaxonomyBuilder produce() {
        if (isConfigured(taxonomyKafkaBootstrapServers) && isConfigured(taxonomyKafkaTopic)) {
            return new DbTaxonomyBuilder(entityManager);
        }
        return recordService
                .filter(url -> !url.isBlank())
                // Optional.map(...) transforms the value inside the Optional if present,
                // leaving it empty otherwise - here, "if there's a non-blank RECORD_SERVICE
                // URL, turn it into a DM2Builder"; the (TaxonomyBuilder) cast is needed because
                // without it, Java would infer the Optional's type as Optional<DM2Builder>,
                // which wouldn't match this method's Optional<TaxonomyBuilder>-shaped return
                // path below.
                .map(url -> (TaxonomyBuilder) new DM2Builder(url, readTimeout))
                // Optional.orElse(null): "if empty, use null instead" - unwraps the Optional
                // into a plain (possibly-null) TaxonomyBuilder, since @Produces methods return
                // the bean type directly, not wrapped in Optional.
                .orElse(null);
    }

    private static boolean isConfigured(Optional<String> value) {
        return value.filter(v -> !v.isBlank()).isPresent();
    }
}
