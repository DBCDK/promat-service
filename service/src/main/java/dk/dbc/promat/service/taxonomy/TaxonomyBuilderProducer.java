package dk.dbc.promat.service.taxonomy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Optional;

@ApplicationScoped
public class TaxonomyBuilderProducer {
    Optional<String> recordService;
    Duration readTimeout;

    @Inject
    public TaxonomyBuilderProducer(@ConfigProperty(name = "RECORD_SERVICE") Optional<String> recordService,
                                   @ConfigProperty(name = "TOPICS_FETCH_READ_TIMEOUT", defaultValue = "PT20S") Duration readTimeout) {
        this.recordService = recordService;
        this.readTimeout = readTimeout;

    }

    @Produces
    public TaxonomyBuilder produce() {
        return recordService
                .map(url -> (TaxonomyBuilder) new DM2Builder(url, readTimeout))

                // DM3 builder will eventually be initiated here. (RECORD_SERVICE url is null).
                .orElse(null);
    }
}