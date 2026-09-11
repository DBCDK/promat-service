package dk.dbc.promat.service.taxonomy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Optional;

// Decides which TaxonomyBuilder implementation gets injected into TaxonomyCache, used by
// ScheduledTaxonomyUpdater.
@ApplicationScoped
public class TaxonomyBuilderProducer {
    private final Optional<String> recordService;
    private final Duration readTimeout;

    @Inject
    public TaxonomyBuilderProducer(@ConfigProperty(name = "RECORD_SERVICE") Optional<String> recordService,
                                   @ConfigProperty(name = "TOPICS_FETCH_READ_TIMEOUT", defaultValue = "PT20S") Duration readTimeout) {
        this.recordService = recordService;
        this.readTimeout = readTimeout;
    }

    // DM2Builder if RECORD_SERVICE is set; otherwise null (TaxonomyCache treats a null builder
    // as "never refreshes", logging an error instead of crashing).
    @Produces
    public TaxonomyBuilder produce() {
        return recordService
                .filter(url -> !url.isBlank())
                .map(url -> (TaxonomyBuilder) new DM2Builder(url, readTimeout))
                .orElse(null);
    }
}
