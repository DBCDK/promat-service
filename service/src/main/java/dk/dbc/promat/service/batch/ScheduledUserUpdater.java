/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.batch;

import dk.dbc.promat.service.cluster.ServerRole;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.PromatUser;
import dk.dbc.promat.service.persistence.Reviewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Startup
@Singleton
public class ScheduledUserUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledUserUpdater.class);

    @Inject
    ServerRole serverRole;

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @EJB
    UserUpdater userUpdater;

    private static Lock updateLock = new ReentrantLock();

    // Users must be deactivated after 5 years having active=f, so no need to run
    // this more than one time each day
    @Schedule(minute = "15", hour = "01", persistent = false)
    public void updateUsers() {

        try {
            if(serverRole == ServerRole.PRIMARY) {

                // Prevent running multiple updates at once - since the update runs only once a day,
                // we should never encounter a lock - so if we do, something is frightfully wrong!
                if(!updateLock.tryLock()) {
                    LOGGER.error("Aborting userupdate since update is already running. Check that the service is not locked or frozen!");
                    return;
                }

                try {
                    userUpdater.resetUpdateUserFailuresGauge();

                    List<Editor> allEditors = getAllEditors();
                    if(allEditors == null || allEditors.size() == 0) {
                        LOGGER.error("No editors found when trying to update all editors - this is unexpected!");
                    }

                    List<Reviewer> allReviewers = getAllReviewers();
                    if(allReviewers == null || allReviewers.size() == 0) {
                        LOGGER.error("No reviewers found when trying to update all reviewers - this is unexpected!");
                    }

                    for(Editor editor : allEditors) {
                        LOGGER.info("Updating editor with id {}", editor.getId());
                        userUpdater.updateEditor(editor);
                    }

                    for(Reviewer reviewer: allReviewers) {
                        LOGGER.info("Updating reviewer with id {}", reviewer.getId());
                        userUpdater.updateReviewer(reviewer);
                    }

                    entityManager.flush();
                } finally {
                    updateLock.unlock();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Caught exception in scheduled job 'updateUser()': {}", e.getMessage());
        }
    }

    public List<Reviewer> getAllReviewers() {
        return entityManager
                .createQuery("SELECT r FROM Reviewer r", Reviewer.class)
                .getResultList();
    }

    public List<Editor> getAllEditors() {
        return entityManager
                .createQuery("SELECT e FROM Editor e", Editor.class)
                .getResultList();
    }
}
