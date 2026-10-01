package dk.dbc.promat.service.batch;

import dk.dbc.promat.service.cluster.ServerRole;
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
import jakarta.persistence.TemporalType;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// Bean-managed concurrency: updateLock already prevents overlapping runs (see ADR 0007)
@Startup
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class ScheduledUserUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledUserUpdater.class);

    static final String USER_UPDATE_JOB = "user-update";

    @Inject
    ServerRole serverRole;

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @EJB
    UserUpdater userUpdater;

    @Inject
    BatchJobMonitor batchJobMonitor;

    private static Lock updateLock = new ReentrantLock();

    // Users must be deactivated after 5 years having active=f, so no need to run
    // this more than one time each day.
    // Keep this apart from ScheduledCaseInformationUpdater.updateCaseAssignedEditor (01:45),
    // since concurrent reads/writes of the same editors and reviewers can deadlock the shared JPA cache.
    // No outer transaction: each user is updated and committed in its own transaction (see ADR 0007)
    @Schedule(minute = "15", hour = "01", persistent = false)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void updateUsers() {

        try {
            if(serverRole == ServerRole.PRIMARY) {

                // Prevent running multiple updates at once - since the update runs only once a day,
                // we should never encounter a lock - so if we do, something is frightfully wrong!
                if(!updateLock.tryLock()) {
                    LOGGER.error("Aborting userupdate since update is already running. Check that the service is not locked or frozen!");
                    return;
                }

                batchJobMonitor.started(USER_UPDATE_JOB);
                try {
                    for (Integer editorId : getInactiveEditorIds()) {
                        LOGGER.info("Updating inactive editor with id {}", editorId);
                        try {
                            userUpdater.deactivateEditor(editorId);
                        } catch (Exception e) {
                            LOGGER.error("Caught exception when trying to deactivate editor with id {}: {}", editorId, e.getMessage(), e);
                        }
                        batchJobMonitor.progressed(USER_UPDATE_JOB);
                    }

                    for (Integer reviewerId : getInactiveReviewerIds()) {
                        LOGGER.info("Updating inactive reviewer with id {}", reviewerId);
                        try {
                            userUpdater.deactivateReviewer(reviewerId);
                        } catch (Exception e) {
                            LOGGER.error("Caught exception when trying to deactivate reviewer with id {}: {}", reviewerId, e.getMessage(), e);
                        }
                        batchJobMonitor.progressed(USER_UPDATE_JOB);
                    }
                } catch(Exception e) {
                    LOGGER.error("Caught exception {}:{} when trying to update users", e.getCause(), e.getMessage());
                    LOGGER.info("Exception: {}", e);
                } finally {
                    batchJobMonitor.finished(USER_UPDATE_JOB);
                    updateLock.unlock();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Caught exception in scheduled job 'updateUser()': {}", e.getMessage());
        }
    }

    // Users inactive for more than 5 years that have not been deactivated yet.
    // Already deactivated users are skipped, so they are not rewritten every night.
    public List<Integer> getInactiveReviewerIds() {
        return entityManager
                .createQuery("SELECT r.id FROM Reviewer r WHERE r.active = false AND r.activeChanged < :cutoff AND r.deactivated IS NULL ORDER BY r.id", Integer.class)
                .setParameter("cutoff", fiveYearsAgo(), TemporalType.TIMESTAMP)
                .getResultList();
    }

    public List<Integer> getInactiveEditorIds() {
        return entityManager
                .createQuery("SELECT e.id FROM Editor e WHERE e.active = false AND e.activeChanged < :cutoff AND e.deactivated IS NULL ORDER BY e.id", Integer.class)
                .setParameter("cutoff", fiveYearsAgo(), TemporalType.TIMESTAMP)
                .getResultList();
    }

    private static Date fiveYearsAgo() {
        return Date.from(ZonedDateTime.now().minusYears(5).toInstant());
    }
}
