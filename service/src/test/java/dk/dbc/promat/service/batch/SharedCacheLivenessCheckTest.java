package dk.dbc.promat.service.batch;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SharedCacheLivenessCheckTest {
    // What a request hung on an orphaned cache lock looks like
    private static final StackTraceElement[] REQUEST_WAIT = {
            new StackTraceElement("java.lang.Object", "wait0", "Object.java", -2),
            new StackTraceElement("org.eclipse.persistence.internal.helper.WriteLockManager", "acquireLocksForClone", "WriteLockManager.java", 184),
            new StackTraceElement("dk.dbc.promat.service.service.CaseSearch", "listCases", "CaseSearch.java", 124)
    };
    // ... and a scheduled batch job, which runs on an EJB timer thread
    private static final StackTraceElement[] BATCH_WAIT = {
            new StackTraceElement("java.lang.Object", "wait0", "Object.java", -2),
            new StackTraceElement("org.eclipse.persistence.internal.helper.WriteLockManager", "acquireLocksForClone", "WriteLockManager.java", 184),
            new StackTraceElement("dk.dbc.promat.service.batch.ScheduledCaseInformationUpdater", "updateCaseAssignedEditor", "ScheduledCaseInformationUpdater.java", 94),
            new StackTraceElement("com.sun.ejb.containers.EJBTimerService", "deliverTimeout", "EJBTimerService.java", 1208)
    };
    private static final StackTraceElement[] OTHER_WAIT = {
            new StackTraceElement("java.lang.Object", "wait0", "Object.java", -2),
            new StackTraceElement("java.util.concurrent.ThreadPoolExecutor", "getTask", "ThreadPoolExecutor.java", 1070)
    };
    private static final Instant T0 = Instant.parse("2026-10-02T10:00:00Z");

    private final CountDownLatch release = new CountDownLatch(1);
    private Thread waitingThread;
    private SharedCacheLivenessCheck check;

    @BeforeAll
    static void setResponseProvider() {
        TestHealthCheckResponses.install();
    }

    @BeforeEach
    void setup() throws InterruptedException {
        check = new SharedCacheLivenessCheck();
        check.sharedCacheInspector = mock(SharedCacheInspector.class);
        check.logAfterMinutes = 5;
        check.batchMaxWaitMinutes = 15;

        // A real thread in WAITING state - the stack it reports is supplied by each test
        waitingThread = new Thread(() -> {
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "waiting-test-thread");
        waitingThread.start();
        while (waitingThread.getState() != Thread.State.WAITING) {
            Thread.sleep(5);
        }
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        release.countDown();
        waitingThread.join();
    }

    @Test
    void upWhenNoThreadWaitsOnCacheLock() {
        assertThat(status(OTHER_WAIT, T0), is(HealthCheckResponse.Status.UP));
        assertThat(status(OTHER_WAIT, T0.plusSeconds(3600)), is(HealthCheckResponse.Status.UP));
        verify(check.sharedCacheInspector, never()).findLockedEntries();
    }

    @Test
    void stuckRequestIsLoggedButNeverReportedDown() {
        status(REQUEST_WAIT, T0);
        verify(check.sharedCacheInspector, never()).findLockedEntries();
        assertThat(status(REQUEST_WAIT, T0.plusSeconds(301)), is(HealthCheckResponse.Status.UP));
        verify(check.sharedCacheInspector, times(1)).findLockedEntries();
        assertThat(status(REQUEST_WAIT, T0.plusSeconds(3600)), is(HealthCheckResponse.Status.UP));
    }

    @Test
    void stuckBatchJobIsReportedDownAfterLimit() {
        status(BATCH_WAIT, T0);
        assertThat(status(BATCH_WAIT, T0.plusSeconds(600)), is(HealthCheckResponse.Status.UP));
        HealthCheckResponse response = check.check(Map.of(waitingThread, BATCH_WAIT), T0.plusSeconds(901));
        assertThat(response.getStatus(), is(HealthCheckResponse.Status.DOWN));
        assertThat(response.getData().orElseThrow().containsKey("waiting-test-thread"), is(true));
    }

    @Test
    void batchWaitStartsOverWhenThreadStopsWaiting() {
        status(BATCH_WAIT, T0);
        status(OTHER_WAIT, T0.plusSeconds(600));
        assertThat(status(BATCH_WAIT, T0.plusSeconds(1200)), is(HealthCheckResponse.Status.UP));
        assertThat(status(BATCH_WAIT, T0.plusSeconds(2101)), is(HealthCheckResponse.Status.DOWN));
    }

    @Test
    void lockedCacheEntriesAreLoggedOncePerStuckThread() {
        status(BATCH_WAIT, T0);
        status(BATCH_WAIT, T0.plusSeconds(301));
        status(BATCH_WAIT, T0.plusSeconds(331));
        status(BATCH_WAIT, T0.plusSeconds(901));
        verify(check.sharedCacheInspector, times(1)).findLockedEntries();
    }

    @Test
    void upAgainWhenStuckBatchJobIsGone() {
        status(BATCH_WAIT, T0);
        assertThat(status(BATCH_WAIT, T0.plusSeconds(901)), is(HealthCheckResponse.Status.DOWN));
        assertThat(check.check(Map.of(), T0.plusSeconds(931)).getStatus(), is(HealthCheckResponse.Status.UP));
    }

    @Test
    void runnableThreadPassingThroughIsNotWaiting() {
        assertThat(SharedCacheLivenessCheck.isWaitingOnCacheLock(Thread.currentThread(), REQUEST_WAIT), is(false));
    }

    @Test
    void batchJobIsRecognisedByTimerFrame() {
        assertThat(SharedCacheLivenessCheck.isBatchJob(BATCH_WAIT), is(true));
        assertThat(SharedCacheLivenessCheck.isBatchJob(REQUEST_WAIT), is(false));
    }

    private HealthCheckResponse.Status status(StackTraceElement[] stack, Instant now) {
        return check.check(Map.of(waitingThread, stack), now).getStatus();
    }
}
