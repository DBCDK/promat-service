/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.batch;

import dk.dbc.promat.service.cluster.ServerRole;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.NotificationStatus;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import java.util.ArrayList;
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

    @Schedule(second = "0", minute = "*/10", hour = "*", persistent = false)
    public void processNotifications() {
        try {
            if(serverRole == ServerRole.PRIMARY) {
                notificationSender.resetMailFailuresGauge();
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
