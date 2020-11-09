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
    protected MailSender mailSender;
    protected Sender sender;

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @Inject
    MailManager mailManager;

    @Schedule(second = "0", minute = "0", hour = "1")
    public void processNotifications() {
        entityManager.getTransaction().begin();
        Notification notification = pop();
        while (notification != null) {
            try {
                LOGGER.info("Popped:{}", notification);
                notify(notification);
                LOGGER.info("Notified:{}", notification);
                entityManager.remove(notification);
            } catch (MessagingException e) {
                LOGGER.info("Setting error");
                setError(notification);
                LOGGER.info("Error sat");
            }
            notification = pop();
        }
        entityManager.getTransaction().commit();
    }

    private void setError(Notification notification) {
        entityManager.createNamedQuery(Notification.SET_STATUS_ERROR_NAME)
                .setParameter(1, notification.getId())
                .executeUpdate();
    }

    private void notify(Notification notification) throws MessagingException {
        mailManager.newMail()
                .withRecipients(notification.getToAddress())
                .withBodyText(notification.getBodyText())
                .withHeaders(
                        new Headers()
                                .withHeader("Content-type", "text/HTML; charset=UTF-8").build()
                )
                .build().send();
    }

    private Notification pop() {
        TypedQuery<Notification> namedQuery = entityManager
                .createNamedQuery(Notification.POP_FROM_NOTIFCATION_QUEUE_NAME, Notification.class);

        return namedQuery.getResultList().size()==0?null:namedQuery.getResultList().get(0);

    }

}
