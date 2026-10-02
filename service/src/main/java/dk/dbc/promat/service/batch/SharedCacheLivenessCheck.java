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
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Watches for threads stuck on an EclipseLink shared-cache lock (see ADR 0007). Normal waits on these
 * locks last milliseconds.
 * <ul>
 *   <li>Any thread - a request or a batch job - waiting for SHARED_CACHE_LOG_AFTER_MINUTES is logged,
 *       with its stack and the locked cache entries. That is the evidence needed to find the cause.</li>
 *   <li>Only a batch job (an EJB timer thread) waiting for SHARED_CACHE_BATCH_MAX_WAIT_MINUTES makes
 *       /health/live report DOWN, so Kubernetes restarts the pod. Requests have been seen to get free by
 *       themselves after about 15 minutes, and restarting for them would restart the pods all the time.
 *       A stuck batch job has waited for hours and blocks all later runs, and only a restart clears it.</li>
 * </ul>
 * A thread only counts if it is seen waiting on every probe for the whole period.
 */
@Liveness
@ApplicationScoped
public class SharedCacheLivenessCheck implements HealthCheck {
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedCacheLivenessCheck.class);

    @Inject
    SharedCacheInspector sharedCacheInspector;

    @Inject
    @ConfigProperty(name = "SHARED_CACHE_LOG_AFTER_MINUTES", defaultValue = "5")
    long logAfterMinutes;

    @Inject
    @ConfigProperty(name = "SHARED_CACHE_BATCH_MAX_WAIT_MINUTES", defaultValue = "15")
    long batchMaxWaitMinutes;

    // Thread id -> when the thread was first seen waiting on a cache lock, in the current unbroken wait
    private final Map<Long, Instant> waitingSince = new ConcurrentHashMap<>();
    private final Set<Long> logged = ConcurrentHashMap.newKeySet();

    @Override
    public HealthCheckResponse call() {
        return check(Thread.getAllStackTraces(), Instant.now());
    }

    HealthCheckResponse check(Map<Thread, StackTraceElement[]> threads, Instant now) {
        Map<Long, Thread> waiting = new HashMap<>();
        threads.forEach((thread, stack) -> {
            if (isWaitingOnCacheLock(thread, stack)) {
                waiting.put(thread.threadId(), thread);
            }
        });
        // A thread that is no longer waiting starts over the next time it waits
        waitingSince.keySet().retainAll(waiting.keySet());
        logged.retainAll(waiting.keySet());
        waiting.keySet().forEach(id -> waitingSince.putIfAbsent(id, now));

        Instant logLimit = now.minus(Duration.ofMinutes(logAfterMinutes));
        Instant batchLimit = now.minus(Duration.ofMinutes(batchMaxWaitMinutes));
        boolean newlyLogged = false;
        HealthCheckResponseBuilder response = HealthCheckResponse.named("shared-cache-locks");
        boolean down = false;
        for (Map.Entry<Long, Instant> entry : waitingSince.entrySet()) {
            Thread thread = waiting.get(entry.getKey());
            StackTraceElement[] stack = threads.get(thread);
            Instant since = entry.getValue();
            if (since.isBefore(logLimit) && logged.add(entry.getKey())) {
                newlyLogged = true;
                LOGGER.error("Thread '{}' has been waiting on a shared JPA cache lock since {}. Stack:\n{}",
                        thread.getName(), since,
                        Arrays.stream(stack)
                                .map(element -> "\tat " + element)
                                .collect(Collectors.joining("\n")));
            }
            if (isBatchJob(stack) && since.isBefore(batchLimit)) {
                down = true;
                response.withData(thread.getName(), "batch job waiting on a cache lock since " + since);
            }
        }
        if (newlyLogged) {
            logLockedCacheEntries();
        }
        if (down) {
            LOGGER.error("A batch job is stuck on a shared JPA cache lock. Reporting DOWN on liveness.");
            return response.down().build();
        }
        return response.up().build();
    }

    // Scheduled batch jobs run on EJB timer threads
    static boolean isBatchJob(StackTraceElement[] stack) {
        return Arrays.stream(stack).anyMatch(element ->
                "com.sun.ejb.containers.EJBTimerService".equals(element.getClassName()));
    }

    // Waiting inside EclipseLink's WriteLockManager.acquireLocksForClone - where the hung threads have been parked
    static boolean isWaitingOnCacheLock(Thread thread, StackTraceElement[] stack) {
        Thread.State state = thread.getState();
        if (state != Thread.State.WAITING && state != Thread.State.TIMED_WAITING) {
            return false;
        }
        return Arrays.stream(stack).anyMatch(element ->
                "org.eclipse.persistence.internal.helper.WriteLockManager".equals(element.getClassName())
                        && "acquireLocksForClone".equals(element.getMethodName()));
    }

    // Who holds the cache locks - the evidence needed to find the cause.
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
