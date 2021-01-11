/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.batch;

import dk.dbc.promat.service.cluster.ServerRole;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.NotificationStatus;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Startup
@Singleton
public class ScheduledNotificationSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledNotificationSender.class);

    @EJB
    NotificationSender notificationSender;

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @Inject
    ServerRole serverRole;

    @Schedule(second = "0", minute = "*/5", hour = "*")
    public void processNotifications() throws InterruptedException {
        if (serverRole == ServerRole.PRIMARY) {
            LOGGER.info("Checking for notifications");
            prepareErrorsForRetry();
            Notification notification = pop();
            while (notification != null) {
                LOGGER.info("Notifying: '{}' on subject '{}'", notification.getToAddress(), notification.getSubject());
                notificationSender.notifyMailRecipient(notification);
                notification = pop();
            }
        }
    }

    private Notification pop() {
        TypedQuery<Notification> query = entityManager
                .createQuery(Notification.SELECT_FROM_NOTIFCATION_QUEUE_QUERY, Notification.class);
        query.setParameter("status", NotificationStatus.PENDING);
        query.setMaxResults(1);
        List<Notification> notifications = query.getResultList();
        return (notifications.isEmpty()) ? null : notifications.get(0);
    }

    private void prepareErrorsForRetry() {
        TypedQuery<Notification> query = entityManager
                .createQuery(Notification.SELECT_FROM_NOTIFCATION_QUEUE_QUERY, Notification.class);
        query.setParameter("status", NotificationStatus.ERROR);
        for (Notification notification : query.getResultList()) {
            notification.setStatus(NotificationStatus.PENDING);
        }
        notificationSender.resetMailFailuresGauge();
    }
}
