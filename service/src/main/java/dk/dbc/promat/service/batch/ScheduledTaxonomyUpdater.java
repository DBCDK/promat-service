package dk.dbc.promat.service.batch;

import dk.dbc.promat.service.taxonomy.TaxonomyCache;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// @Startup disabled temporarily
@Singleton
public class ScheduledTaxonomyUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTaxonomyUpdater.class);

    private static final long INTERVAL_MS = 15 * 60 * 1000L;
    private final TaxonomyCache taxonomyCache;

    @Resource
    TimerService timerService;

    private final String hostname;

    @Inject
    public ScheduledTaxonomyUpdater(
            @ConfigProperty(name = "HOSTNAME", defaultValue = "promat-service-0") String hostname,
            TaxonomyCache taxonomyCache) {
        this.hostname = hostname;
        this.taxonomyCache = taxonomyCache;
    }

    /**
     * This approach (as opposed to using @Scheduled) is used to ensure that multiple instances of the service
     * will not fire the somewhat (loadwise) heavy request to recordservice all at the same time.
     */
    @PostConstruct
    void init() {
        // See ScheduledNotificationSender.processNotifications() for what
        // this guard is for.
        if (RuntimeGuards.disableScheduledJobs()) {
            LOGGER.warn("Skipping taxonomy updater scheduling because {}=true", RuntimeGuards.DISABLE_SCHEDULED_JOBS_ENV);
            return;
        }
        int offsetMinutes = resolveOffsetMinutes();
        long initialDelay = computeInitialDelay(offsetMinutes);
        LOGGER.info("Taxonomy updater scheduled with offset {} min, first run in {} ms", offsetMinutes, initialDelay);
        timerService.createIntervalTimer(initialDelay, INTERVAL_MS, new TimerConfig(null, false));
        updateTaxonomy();
    }

    private int resolveOffsetMinutes() {
        int lastDash = hostname.lastIndexOf('-');
        if (lastDash >= 0) {
            try {
                return Integer.parseInt(hostname.substring(lastDash + 1));
            } catch (NumberFormatException e) {
                LOGGER.warn("Could not parse ordinal from hostname '{}', defaulting to offset 0", hostname);
            }
        }
        return 0;
    }

    private long computeInitialDelay(int offsetMinutes) {
        long nowMs = System.currentTimeMillis();
        long offsetMs = offsetMinutes * 60 * 1000L;
        return ((nowMs - offsetMs) / INTERVAL_MS + 1) * INTERVAL_MS + offsetMs - nowMs;
    }

    @Timeout
    public void updateTaxonomy() {
        // See ScheduledNotificationSender.processNotifications() for what
        // this guard is for.
        if (RuntimeGuards.disableScheduledJobs()) {
            LOGGER.warn("Skipping taxonomy refresh because {}=true", RuntimeGuards.DISABLE_SCHEDULED_JOBS_ENV);
            return;
        }
        try {
            taxonomyCache.refresh();
        } catch (Exception e) {
            LOGGER.error("Failed to build taxonomy", e);
        }
    }
}
