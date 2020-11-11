package dk.dbc.promat.service.batch;

import dk.dbc.commons.persistence.TransactionScopedPersistenceContext;
import dk.dbc.mail.MailManager;
import dk.dbc.promat.service.IntegrationTest;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.NotificationStatus;
import java.util.List;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.mock_javamail.Mailbox;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;


public class ScheduledNotificationSenderIT extends IntegrationTest {
    static MailManager mailManager;
    static {
        Properties props = System.getProperties();

        props.put("mail.smtp.host", "a.mailhost.somewhere");
        Session session = Session.getInstance(props, null);
        mailManager = new MailManager(session);
    }
    TransactionScopedPersistenceContext persistenceContext;

    @BeforeEach
    public void setupMailStuff() {
         persistenceContext = new TransactionScopedPersistenceContext(entityManager);
        Mailbox.clearAll();
    }

    @Test
    public void test_that_notifications_are_popped_and_sent() throws MessagingException {

        ScheduledNotificationSender scheduledNotificationSender = new ScheduledNotificationSender();
        scheduledNotificationSender.entityManager = entityManager;
        NotificationSender notificationSender = new NotificationSender();
        scheduledNotificationSender.notificationSender = notificationSender;
        scheduledNotificationSender.notificationSender.mailManager = mailManager;
        scheduledNotificationSender.notificationSender.entityManager = entityManager;
        persistenceContext.run(()->scheduledNotificationSender.processNotifications());

        List<Message> inbox1 = Mailbox.get("test1@test.dk");
        assertThat("Test1 recieved a mail", inbox1.size(), is(1));
        List<Message> inbox2 = Mailbox.get("test2@test.dk");
        assertThat("Test2 recieved no mail", inbox2.size(), is(0));
        List<Message> inbox3 = Mailbox.get("test3@test.dk");
        assertThat("Test3 recieved a mail", inbox3.size(), is(1));

        TypedQuery<Notification> query = entityManager.createQuery(Notification.SELECT_FROM_NOTIFCATION_QUEUE_QUERY, Notification.class);
        query.setParameter("status", NotificationStatus.DONE);
        assertThat("All notifications in db are now DONE", query.getResultList().size(), is(3));
    }

}