/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.batch;

import dk.dbc.promat.service.cluster.ServerRole;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Startup
@Singleton
public class ScheduledCaseInformationUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledCaseInformationUpdater.class);

    @Inject
    ServerRole serverRole;

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @EJB
    CaseInformationUpdater caseInformationUpdater;

    private static Lock updateLock = new ReentrantLock();

    // Since every update traverses all active cases, we should not run too often.
    // Run once every 10 minutes on digit 0 to match dataio which is running every
    // 10 minutes on digit 5.
    // Only run during working days and normal working hours
    @Schedule(second = "0", minute = "*/10", hour = "6-18", dayOfWeek = "Mon-Fri", persistent = false)
    public void updateCaseInformation() {

        try {
            if(serverRole == ServerRole.PRIMARY) {

                // Prevent running multiple updates at once - since the update runs only every hour,
                // we should never encounter a lock - so if we do, something is frightfully wrong!
                if(!updateLock.tryLock()) {
                    LOGGER.error("Aborting update since update is already running. Check that the service is not locked or frozen!");
                    return;
                }

                try {
                    List<PromatCase> casesForUpdate = getCasesForUpdate();
                    if(casesForUpdate != null && casesForUpdate.size() > 0) {
                        for(PromatCase promatCase : casesForUpdate) {
                            LOGGER.info("Updating case with id {}", promatCase.getId());
                            caseInformationUpdater.updateCaseInformation(promatCase);
                        }
                    }

                    entityManager.flush();
                } finally {
                    updateLock.unlock();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Caught exception in scheduled job 'updateCaseInformation()': {}", e.getMessage());
        }
    }

    public List<PromatCase> getCasesForUpdate() {
        TypedQuery<PromatCase> query = entityManager.createNamedQuery(PromatCase.GET_CASES_FOR_UPDATE_NAME, PromatCase.class);
        return query.getResultList();
    }

    @Schedule(second = "0", minute = "15", hour = "01", dayOfWeek = "Mon-Fri", persistent = false)
    public void updateCaseAssignedEditor() {

        try {
            if(serverRole == ServerRole.PRIMARY) {

                // Prevent running multiple updates at once
                if(!updateLock.tryLock()) {
                    LOGGER.error("Aborting update since update is already running. Check that the service is not locked or frozen!");
                    return;
                }

                try {
                    List<PromatCase> casesForUpdate = getCasesWithInactiveEditor();
                    if(casesForUpdate != null && casesForUpdate.size() > 0) {
                        for(PromatCase promatCase : casesForUpdate) {
                            LOGGER.info("Clearing editor on case with id {}", promatCase.getId());
                            caseInformationUpdater.clearEditor(promatCase);
                        }
                    }

                    entityManager.flush();
                } finally {
                    updateLock.unlock();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Caught exception in scheduled job 'updateCaseAssignedEditor()': {}", e.getMessage());
        }
    }

    public List<PromatCase> getCasesWithInactiveEditor() {
        LOGGER.info("getCasesWithInactiveEditor");
        TypedQuery<PromatCase> query = entityManager.createNamedQuery(PromatCase.GET_CASES_WITH_INACTIVE_EDITOR_NAME, PromatCase.class);
        var x = query.getResultList();
        LOGGER.info("resultset {}, {}", x, x.size());
        return x;
        //return query.getResultList();
    }
}
