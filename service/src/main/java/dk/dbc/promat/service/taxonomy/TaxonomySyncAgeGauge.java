package dk.dbc.promat.service.taxonomy;

import dk.dbc.promat.service.batch.ScheduledTaxonomyKafkaSync;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Supplier;

// Exposes how stale this pod's own taxonomy Kafka sync is, on /metrics - a plain gauge for
// dashboards/alerting, not a HealthCheck, since a stale-but-populated taxonomy should page a
// human rather than restart or reroute traffic from an otherwise healthy pod. Same registration
// pattern as ProcessingGauge.java.
@Startup
@Singleton
public class TaxonomySyncAgeGauge {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaxonomySyncAgeGauge.class);

    // Distinguishes "never synced" from an actual age, without the gauge returning null.
    private static final long NEVER_SYNCED_YET = -1L;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    MetricRegistry metricRegistry;

    @EJB
    ScheduledTaxonomyKafkaSync scheduledTaxonomyKafkaSync;

    static final Metadata syncAgeGaugeMetadata = Metadata.builder()
            .withName("promat_service_taxonomy_last_sync_age")
            .withDescription("Seconds since this pod's taxonomy Kafka sync last completed successfully, or -1 if it never has")
            .withUnit(MetricUnits.SECONDS)
            .build();

    Supplier<Long> syncAgeGauge = () -> {
        LocalDateTime lastSuccessfulSyncAt = scheduledTaxonomyKafkaSync.getLastSuccessfulSyncAt();
        if (lastSuccessfulSyncAt == null) {
            return NEVER_SYNCED_YET;
        }
        return Duration.between(lastSuccessfulSyncAt, LocalDateTime.now()).toSeconds();
    };

    @PostConstruct
    public void register() {
        if (metricRegistry != null) {
            LOGGER.info("Registering {}", syncAgeGaugeMetadata.getName());
            metricRegistry.gauge(syncAgeGaugeMetadata, syncAgeGauge);
        } else {
            LOGGER.info("No injected metricRegistry. Unable to register {}", syncAgeGaugeMetadata.getName());
        }
    }
}
