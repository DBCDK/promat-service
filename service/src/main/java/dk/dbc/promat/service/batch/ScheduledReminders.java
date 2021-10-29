package dk.dbc.promat.service.batch;

import dk.dbc.promat.service.cluster.ServerRole;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Startup
@Singleton
public class ScheduledReminders {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledReminders.class);

    @Inject
    ServerRole serverRole;

    private static final Lock lock = new ReentrantLock();

    @Inject
    @ConfigProperty(name = "ENABLE_REMINDERS", defaultValue = "no")
    String ENABLE_REMINDERS;

    @EJB
    Reminders reminders;

    @Schedule(second = "30", minute = "50", hour = "6-16", dayOfWeek = "Mon-Fri", persistent = false)
    public void processReminders() {
        if (serverRole == ServerRole.PRIMARY) {
            if (!lock.tryLock()) {
                LOGGER.error("Aborting, since processReminders is already running! " +
                        "Check that the service is not locked or frozen.");
                return;
            }

            // Optional: Disable reminders entirely.
            //  In the intermediary period, where old and new versions of promat are running
            //  side by side, we might risk mails being sent from here on "stale" cases,
            //  already handled in old promat.
            try {
                if ("true".equals(ENABLE_REMINDERS.toLowerCase())) {
                    reminders.processReminders();
                } else {
                    LOGGER.info("Reminders batch is currently switched off '{}'. To reenable set env var ENABLE_REMINDERS to true.", ENABLE_REMINDERS);
                }
            }
            catch(Exception e) {
                LOGGER.error("Caught exception {}:{} when trying to process reminders", e.getCause(), e.getMessage());
                LOGGER.info("Exception stacktrace: {}", e.getStackTrace());
            }
            finally {
                lock.unlock();
            }
        }
    }
}
