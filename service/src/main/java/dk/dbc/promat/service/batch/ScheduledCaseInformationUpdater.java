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
    // Run once every hour during working days and normal working hours
    @Schedule(second = "0", minute = "42", hour = "6-16", dayOfWeek = "Mon-Fri")
    public void updateCaseInformation() {
        try {
            LOGGER.info("Executing scheduled job 'updateCaseInformation()'");

            if(serverRole == ServerRole.PRIMARY) {
                LOGGER.info("Starting periodic update of case information");

                // Prevent running multiple updates at once - since the update runs only every hour,
                // we should never encounter a lock - so if we do, something is frightfully wrong!
                if(!updateLock.tryLock()) {
                    LOGGER.error("Aborting update since update is already running. Check that the service is not locked or frozen!");
                    return;
                }

                try {
                    caseInformationUpdater.resetUpdateCaseFailuresGauge();
                    List<PromatCase> casesForUpdate = getCasesForUpdate();
                    if(casesForUpdate != null && casesForUpdate.size() > 0) {
                        for(PromatCase promatCase : casesForUpdate) {
                            caseInformationUpdater.updateCaseInformation(promatCase);
                        }
                    }

                    entityManager.flush();
                } finally {
                    updateLock.unlock();
                    LOGGER.info("Ending periodic update of case information");
                }
            } else {
                LOGGER.info("Not ServerRole.PRIMARY, aborting");
            }
        } catch (Exception e) {
            LOGGER.error("Caught exception in scheduled job 'updateCaseInformation()': {}", e.getMessage());
        }
    }

    public List<PromatCase> getCasesForUpdate() {
        TypedQuery<PromatCase> query = entityManager.createNamedQuery(PromatCase.GET_CASES_FOR_UPDATE_NAME, PromatCase.class);
        return query.getResultList();
    }
}
