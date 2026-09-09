package dk.dbc.promat.service.taxonomy;

import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.TaxonomySnapshot;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Supplier;

// Exposes how stale the taxonomy snapshot is, on /metrics - deliberately a plain gauge for
// dashboards/alerting, NOT a MicroProfile HealthCheck (@Liveness/@Readiness/@Startup are the
// only qualifiers that API version has - there's no "informational only" option there, and
// the aggregate /health endpoint is already used as a real startup/readiness gate elsewhere
// in this project's own test setup). A stale-but-present snapshot means the REST API is still
// fully functional (see DbTaxonomyBuilder) - this should page/alert a human, not restart or
// reroute traffic away from a healthy pod. Same registration pattern as ProcessingGauge.java.
@Startup
@Singleton
public class TaxonomySnapshotGauge {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaxonomySnapshotGauge.class);
    private static final int SNAPSHOT_ID = 1;

    // Sentinel returned when no snapshot row exists yet at all (e.g. right after a fresh
    // deploy, before the first successful Kafka sync) - distinguishes "never synced" from an
    // actual age in seconds, without the gauge needing to return something nullable.
    private static final long NO_SNAPSHOT_YET = -1L;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    MetricRegistry metricRegistry;

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    static final Metadata snapshotAgeGaugeMetadata = Metadata.builder()
            .withName("promat_service_taxonomy_snapshot_age")
            .withDescription("Seconds since the taxonomy snapshot was last successfully updated from Kafka, or -1 if no snapshot exists yet")
            .withUnit(MetricUnits.SECONDS)
            .build();

    Supplier<Long> snapshotAgeGauge = () -> {
        TaxonomySnapshot snapshot = entityManager.find(TaxonomySnapshot.class, SNAPSHOT_ID);
        if (snapshot == null) {
            return NO_SNAPSHOT_YET;
        }
        return Duration.between(snapshot.getUpdatedAt(), LocalDateTime.now()).toSeconds();
    };

    @PostConstruct
    public void register() {
        if (metricRegistry != null) {
            LOGGER.info("Registering {}", snapshotAgeGaugeMetadata.getName());
            metricRegistry.gauge(snapshotAgeGaugeMetadata, snapshotAgeGauge);
        } else {
            LOGGER.info("No injected metricRegistry. Unable to register {}", snapshotAgeGaugeMetadata.getName());
        }
    }
}
