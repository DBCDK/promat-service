package dk.dbc.promat.service.batch;

import dk.dbc.commons.persistence.TransactionScopedPersistenceContext;
import dk.dbc.mail.MailManager;
import dk.dbc.promat.service.IntegrationTestIT;
import dk.dbc.promat.service.cluster.ServerRole;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.NotificationStatus;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.mock_javamail.Mailbox;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.persistence.TypedQuery;
import org.mockito.Mock;

import java.util.List;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScheduledNotificationSenderIT extends IntegrationTestIT {
    static MailManager mailManager;
    static {
        Properties props = System.getProperties();

        props.put("mail.smtp.host", "a.mailhost.somewhere");
        Session session = Session.getInstance(props, null);
        mailManager = new MailManager(session);
    }
    TransactionScopedPersistenceContext persistenceContext;
    private final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final Counter counter = mock(Counter.class);

    @BeforeEach
    public void setupMailStuff() {
        when(metricRegistry.counter(any(Metadata.class))).thenReturn(counter);
        persistenceContext = new TransactionScopedPersistenceContext(entityManager);
        Mailbox.clearAll();
    }

    @Test
    public void test_that_notifications_are_popped_and_sent() throws MessagingException, InterruptedException {
        ScheduledNotificationSender scheduledNotificationSender = new ScheduledNotificationSender();
        scheduledNotificationSender.entityManager = entityManager;
        scheduledNotificationSender.serverRole = ServerRole.PRIMARY;
        scheduledNotificationSender.notificationSender = new NotificationSender();
        scheduledNotificationSender.notificationSender.mailManager = mailManager;
        scheduledNotificationSender.notificationSender.metricRegistry = metricRegistry;
        UserUpdater userUpdater = mock(UserUpdater.class);
        scheduledNotificationSender.userUpdater = userUpdater;
        persistenceContext.run(scheduledNotificationSender::processNotifications);

        List<Message> inbox1 = Mailbox.get("test1@test.dk");
        assertThat("Test1 recieved a mail", inbox1.size(), is(1));
        List<Message> inbox2 = Mailbox.get("test2@test.dk");
        assertThat("Test2 recieved no mail", inbox2.size(), is(0));
        List<Message> inbox3 = Mailbox.get("test3@test.dk");
        assertThat("Test3 recieved a mail", inbox3.size(), is(1));
        List<Message> inbox4 = Mailbox.get("test4@test.dk");
        assertThat("Test4 recieved a mail", inbox4.size(), is(1));

        TypedQuery<Notification> query = entityManager.createQuery(
                "SELECT notification FROM Notification notification " +
                        "WHERE notification.toAddress IN :addresses AND notification.status = :status", Notification.class);
        query.setParameter("addresses", List.of("test1@test.dk", "test2@test.dk", "test3@test.dk", "test4@test.dk"));
        query.setParameter("status", NotificationStatus.DONE);
        assertThat("All notifications in db are now DONE", query.getResultList().size(), is(4));
    }

}
