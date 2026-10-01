package dk.dbc.promat.service.batch;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reports DOWN on /health/live when a batch job has made no progress for BATCH_JOB_MAX_IDLE_MINUTES,
 * so Kubernetes restarts the pod. A restart is the only way to clear an orphaned EclipseLink
 * cache lock (see ADR 0007 and docs/postmortem-2026-08-19-promat-batch-hang.md).
 * Only reports on the job's own progress, never on external services, so an outage in the
 * database or fbi-api cannot cause restarts.
 */
@Liveness
@ApplicationScoped
public class BatchJobLivenessCheck implements HealthCheck {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchJobLivenessCheck.class);

    @Inject
    BatchJobMonitor batchJobMonitor;

    @Inject
    SharedCacheInspector sharedCacheInspector;

    @Inject
    @ConfigProperty(name = "BATCH_JOB_MAX_IDLE_MINUTES", defaultValue = "15")
    long maxIdleMinutes;

    @Override
    public HealthCheckResponse call() {
        List<BatchJobMonitor.Run> stalled = batchJobMonitor.stalledRuns(Duration.ofMinutes(maxIdleMinutes));
        HealthCheckResponseBuilder response = HealthCheckResponse.named("batch-jobs");
        if (stalled.isEmpty()) {
            return response.up().build();
        }
        for (BatchJobMonitor.Run run : stalled) {
            // Log the stuck thread's stack once - the evidence is lost when the pod restarts
            if (run.markReported()) {
                LOGGER.error("Batch job '{}' has made no progress since {} (started {}, thread '{}'). Reporting DOWN on liveness. Stack:\n{}",
                        run.getJob(), run.getLastProgress(), run.getStarted(), run.getThread().getName(),
                        Arrays.stream(run.getThread().getStackTrace())
                                .map(element -> "\tat " + element)
                                .collect(Collectors.joining("\n")));
                logLockedCacheEntries();
            }
            response.withData(run.getJob(), "no progress since " + run.getLastProgress());
        }
        return response.down().build();
    }

    // Who holds the cache lock the job is waiting on - the evidence needed to find the cause.
    // Uses EclipseLink internals, so a failure here must not break the liveness check.
    private void logLockedCacheEntries() {
        try {
            List<String> locked = sharedCacheInspector.findLockedEntries();
            if (locked.isEmpty()) {
                LOGGER.error("No locked entries found in the shared JPA cache");
            }
            locked.forEach(entry -> LOGGER.error("Locked shared JPA cache entry: {}", entry));
        } catch (Exception | LinkageError e) {
            LOGGER.error("Unable to inspect the shared JPA cache: {}", e.getMessage(), e);
        }
    }
}
