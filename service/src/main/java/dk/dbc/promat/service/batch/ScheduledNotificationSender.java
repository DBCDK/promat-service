package dk.dbc.promat.service.batch;

import dk.dbc.promat.service.cluster.ServerRole;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.NotificationStatus;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import java.util.List;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Startup
@Singleton
public class ScheduledNotificationSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledNotificationSender.class);

    @EJB
    NotificationSender notificationSender;

    @EJB
    UserUpdater userUpdater;

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @Inject
    ServerRole serverRole;


    @Schedule(second = "0", minute = "*/10", hour = "*", persistent = false)
    public void processNotifications() {
        try {
            // Local-dev guard (see RuntimeGuards): with
            // PROMAT_DISABLE_SCHEDULED_JOBS=true, this (and every other
            // @Schedule-based job in this package) becomes a no-op. Handy
            // when running against a shared/staging database locally, where
            // you don't want your local instance racing the real deployment
            // to process the same notifications/reminders/etc.
            if (RuntimeGuards.disableScheduledJobs()) {
                LOGGER.warn("Skipping scheduled notifications because {}=true", RuntimeGuards.DISABLE_SCHEDULED_JOBS_ENV);
                return;
            }
            if (serverRole == ServerRole.PRIMARY) {

                userUpdater.processUserDataChanges();

                Notification notification = pop(-1);
                while(notification != null) {
                    LOGGER.info("Notifying: '{}' on subject '{}'", notification.getToAddress(), notification.getSubject());
                    notificationSender.notifyMailRecipient(notification);
                    notification = pop(notification.getId());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Caught exception in scheduled job 'processNotifications()'",e);
        }
    }

    private Notification pop(Integer id) {
        TypedQuery<Notification> query = entityManager
                .createQuery(Notification.SELECT_FROM_NOTIFCATION_QUEUE_QUERY, Notification.class);
        query.setParameter("status", List.of(NotificationStatus.PENDING, NotificationStatus.ERROR));
        query.setParameter("lastid", id);
        query.setMaxResults(1);
        List<Notification> notifications = query.getResultList();
        return (notifications.isEmpty()) ? null : notifications.get(0);
    }
}
