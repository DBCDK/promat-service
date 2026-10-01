package dk.dbc.promat.service.batch;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of running batch jobs, so {@link BatchJobLivenessCheck} can tell a job that is stuck
 * from one that is just slow. A job reports when it starts, after each item it processes, and when it
 * finishes. A job is stalled when it has made no progress for a given duration.
 * See ADR 0007 for why this exists.
 */
@ApplicationScoped
public class BatchJobMonitor {

    public static class Run {
        private final String job;
        private final Thread thread;
        private final Instant started;
        private volatile Instant lastProgress;
        private volatile boolean reported;

        Run(String job, Thread thread, Instant started) {
            this.job = job;
            this.thread = thread;
            this.started = started;
            this.lastProgress = started;
        }

        public String getJob() {
            return job;
        }

        public Thread getThread() {
            return thread;
        }

        public Instant getStarted() {
            return started;
        }

        public Instant getLastProgress() {
            return lastProgress;
        }

        // True the first time it is called, so a stalled run is only logged once
        boolean markReported() {
            if (reported) {
                return false;
            }
            reported = true;
            return true;
        }
    }

    private final Map<String, Run> runs = new ConcurrentHashMap<>();

    public void started(String job) {
        runs.put(job, new Run(job, Thread.currentThread(), Instant.now()));
    }

    public void progressed(String job) {
        Run run = runs.get(job);
        if (run != null) {
            run.lastProgress = Instant.now();
        }
    }

    public void finished(String job) {
        runs.remove(job);
    }

    public List<Run> stalledRuns(Duration maxIdle) {
        Instant limit = Instant.now().minus(maxIdle);
        return runs.values().stream()
                .filter(run -> run.lastProgress.isBefore(limit))
                .toList();
    }
}
