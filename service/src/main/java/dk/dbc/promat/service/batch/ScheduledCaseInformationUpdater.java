package dk.dbc.promat.service.batch;

import dk.dbc.promat.service.cluster.ServerRole;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// Bean-managed concurrency: updateLock already prevents overlapping runs. With the default
// container write lock, one hung timer callback would silently block all later runs.
@Startup
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class ScheduledCaseInformationUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledCaseInformationUpdater.class);

    @Inject
    ServerRole serverRole;

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @EJB
    CaseInformationUpdater caseInformationUpdater;

    @Inject
    BatchJobMonitor batchJobMonitor;

    static final String CASE_UPDATE_JOB = "case-information-update";
    static final String EDITOR_RESET_JOB = "case-editor-reset";

    private static Lock updateLock = new ReentrantLock();

    // Since every update traverses all active cases, we should not run too often.
    // Run once every 10 minutes on digit 0 to match dataio which is running every
    // 10 minutes on digit 5.
    // Only run during working days and normal working hours
    // No outer transaction: each case is updated and committed in its own transaction (see ADR 0007)
    @Schedule(second = "0", minute = "*/10", hour = "6-18", dayOfWeek = "Mon-Fri", persistent = false)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void updateCaseInformation() {

        try {
            if(serverRole == ServerRole.PRIMARY) {

                // Prevent running multiple updates at once - since the update runs only every hour,
                // we should never encounter a lock - so if we do, something is frightfully wrong!
                if(!updateLock.tryLock()) {
                    LOGGER.error("Aborting update since update is already running. Check that the service is not locked or frozen!");
                    return;
                }

                batchJobMonitor.started(CASE_UPDATE_JOB);
                try {
                    for (Integer caseId : getCaseIdsForUpdate()) {
                        LOGGER.info("Updating case with id {}", caseId);
                        try {
                            caseInformationUpdater.updateCaseInformation(caseId);
                        } catch (Exception e) {
                            // Only this case is rolled back, the rest of the pass continues
                            LOGGER.error("Caught exception when trying to update case with id {}: {}", caseId, e.getMessage(), e);
                        }
                        batchJobMonitor.progressed(CASE_UPDATE_JOB);
                    }
                } catch(Exception e) {
                    LOGGER.error("Caught exception {}:{} when trying to update cases", e.getCause(), e.getMessage());
                    LOGGER.info("Exception: ", e);
                } finally {
                    batchJobMonitor.finished(CASE_UPDATE_JOB);
                    updateLock.unlock();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Caught exception in scheduled job 'updateCaseInformation()': {}", e.getMessage());
        }
    }

    public List<Integer> getCaseIdsForUpdate() {
        return entityManager.createNamedQuery(PromatCase.GET_CASE_IDS_FOR_UPDATE_NAME, Integer.class)
                .getResultList();
    }

    // Must not run at the same time as ScheduledUserUpdater (01:15), since concurrent
    // reads/writes of the same editors and reviewers can deadlock the shared JPA cache.
    @Schedule(second = "0", minute = "45", hour = "01", dayOfWeek = "Mon-Fri", persistent = false)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void updateCaseAssignedEditor() {

        try {
            if(serverRole == ServerRole.PRIMARY) {

                // Prevent running multiple updates at once
                if(!updateLock.tryLock()) {
                    LOGGER.error("Aborting update since update is already running. Check that the service is not locked or frozen!");
                    return;
                }

                batchJobMonitor.started(EDITOR_RESET_JOB);
                try {
                    for (Integer caseId : getCaseIdsWithInactiveEditor()) {
                        LOGGER.info("Clearing editor on case with id {}", caseId);
                        try {
                            caseInformationUpdater.clearEditor(caseId);
                        } catch (Exception e) {
                            LOGGER.error("Caught exception when trying to clear editor on case with id {}: {}", caseId, e.getMessage(), e);
                        }
                        batchJobMonitor.progressed(EDITOR_RESET_JOB);
                    }
                } finally {
                    batchJobMonitor.finished(EDITOR_RESET_JOB);
                    updateLock.unlock();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Caught exception in scheduled job 'updateCaseAssignedEditor()': {}", e.getMessage());
        }
    }

    public List<Integer> getCaseIdsWithInactiveEditor() {
        return entityManager.createNamedQuery(PromatCase.GET_CASE_IDS_WITH_INACTIVE_EDITOR_NAME, Integer.class)
                .getResultList();
    }
}
