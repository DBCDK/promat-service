package dk.dbc.promat.service.batch;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class BatchJobLivenessCheckTest {
    private BatchJobMonitor monitor;
    private BatchJobLivenessCheck check;

    // Payara provides the MicroProfile Health implementation at runtime - this minimal one stands in for it
    @BeforeAll
    static void setResponseProvider() {
        HealthCheckResponse.setResponseProvider(() -> new HealthCheckResponseBuilder() {
            private String name;
            private boolean up;
            private final Map<String, Object> data = new HashMap<>();

            @Override public HealthCheckResponseBuilder name(String name) { this.name = name; return this; }
            @Override public HealthCheckResponseBuilder withData(String key, String value) { data.put(key, value); return this; }
            @Override public HealthCheckResponseBuilder withData(String key, long value) { data.put(key, value); return this; }
            @Override public HealthCheckResponseBuilder withData(String key, boolean value) { data.put(key, value); return this; }
            @Override public HealthCheckResponseBuilder up() { up = true; return this; }
            @Override public HealthCheckResponseBuilder down() { up = false; return this; }
            @Override public HealthCheckResponseBuilder status(boolean up) { this.up = up; return this; }
            @Override public HealthCheckResponse build() {
                return new HealthCheckResponse(name, up ? HealthCheckResponse.Status.UP : HealthCheckResponse.Status.DOWN,
                        data.isEmpty() ? Optional.empty() : Optional.of(data));
            }
        });
    }

    @BeforeEach
    void setup() {
        monitor = new BatchJobMonitor();
        check = new BatchJobLivenessCheck();
        check.batchJobMonitor = monitor;
        check.sharedCacheInspector = mock(SharedCacheInspector.class);
        check.maxIdleMinutes = 15;
    }

    @Test
    void upWhenNoJobIsRunning() {
        assertThat(check.call().getStatus(), is(HealthCheckResponse.Status.UP));
    }

    @Test
    void upWhileRunningJobMakesProgress() {
        monitor.started("job");
        monitor.progressed("job");
        assertThat(check.call().getStatus(), is(HealthCheckResponse.Status.UP));
    }

    @Test
    void downWhenRunningJobHasStalled() {
        monitor.started("job");
        // A negative idle limit makes any running job count as stalled
        check.maxIdleMinutes = -1;
        HealthCheckResponse response = check.call();
        assertThat(response.getStatus(), is(HealthCheckResponse.Status.DOWN));
        assertThat(response.getData().orElseThrow().containsKey("job"), is(true));
    }

    @Test
    void cacheLocksAreLoggedOncePerStalledRun() {
        monitor.started("job");
        check.maxIdleMinutes = -1;
        check.call();
        check.call();
        verify(check.sharedCacheInspector, times(1)).findLockedEntries();
    }

    @Test
    void upAgainWhenStalledJobFinishes() {
        monitor.started("job");
        check.maxIdleMinutes = -1;
        assertThat(check.call().getStatus(), is(HealthCheckResponse.Status.DOWN));
        monitor.finished("job");
        assertThat(check.call().getStatus(), is(HealthCheckResponse.Status.UP));
    }

    @Test
    void stalledRunIsOnlyReportedOnce() {
        monitor.started("job");
        BatchJobMonitor.Run run = monitor.stalledRuns(Duration.ofMinutes(-1)).get(0);
        assertThat(run.markReported(), is(true));
        assertThat(run.markReported(), is(false));
    }

    @Test
    void monitorOnlyReturnsStalledRuns() {
        monitor.started("job");
        assertThat(monitor.stalledRuns(Duration.ofMinutes(15)), is(empty()));
        assertThat(monitor.stalledRuns(Duration.ofMinutes(-1)), hasSize(1));
        monitor.finished("job");
        assertThat(monitor.stalledRuns(Duration.ofMinutes(-1)), is(empty()));
    }
}
