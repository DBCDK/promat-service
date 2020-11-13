package dk.dbc.promat.service.batch;

import dk.dbc.mail.Headers;
import dk.dbc.mail.MailManager;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.NotificationStatus;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class NotificationSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationSender.class);


    @Inject
    MailManager mailManager;


    @Inject
    @PromatEntityManager
    EntityManager entityManager;


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void notifyMailRecipient(Notification notification) throws InterruptedException {
        try {
            mailManager.newMail()
                    .withRecipients(notification.getToAddress())
                    .withSubject(notification.getSubject())
                    .withBodyText(notification.getBodyText())
                    .withHeaders(
                            new Headers()
                                    .withHeader("Content-type", "text/HTML; charset=UTF-8").build()
                    )
                    .build().send();
            notification.setStatus(NotificationStatus.DONE);
            entityManager.merge(notification);
        } catch (MessagingException e) {
            LOGGER.error("Unable to send mail. Notification:{}",notification.toString());
            Thread.sleep(1000);
            notification.setStatus(NotificationStatus.ERROR);
            entityManager.merge(notification);

        }
    }
}
