package dk.dbc.promat.service.batch;

import dk.dbc.mail.Headers;
import dk.dbc.mail.MailManager;
import dk.dbc.mail.MailSender;
import dk.dbc.mail.Sender;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Startup
@Singleton
public class ScheduledNotificationSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledNotificationSender.class);

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @Inject
    MailManager mailManager;

    @Schedule(second = "0", minute = "*", hour = "*")
    public void processNotifications() {
        LOGGER.info("Checkin for notifications");
        TypedQuery<Notification> namedQuery = entityManager
                .createNamedQuery(Notification.POP_FROM_NOTIFCATION_QUEUE_NAME, Notification.class);
        for (Notification notification : namedQuery.getResultList()) {
            try {
                notify(notification);
                entityManager.remove(notification);
            } catch (MessagingException e) {
                setError(notification);
            }
        }
    }

    private void setError(Notification notification) {
        entityManager.createNamedQuery(Notification.SET_STATUS_ERROR_NAME)
                .setParameter(1, notification.getId())
                .executeUpdate();
    }

    private void notify(Notification notification) throws MessagingException {
        mailManager.newMail()
                .withRecipients(notification.getToAddress())
                .withSubject(notification.getSubject())
                .withBodyText(notification.getBodyText())
                .withHeaders(
                        new Headers()
                                .withHeader("Content-type", "text/HTML; charset=UTF-8").build()
                )
                .build().send();
    }

}
