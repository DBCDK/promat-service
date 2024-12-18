package dk.dbc.promat.service.batch;

import dk.dbc.commons.persistence.TransactionScopedPersistenceContext;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.cluster.ServerRole;
import dk.dbc.promat.service.dto.ReviewerRequest;
import dk.dbc.promat.service.persistence.Address;
import dk.dbc.promat.service.persistence.Notification;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.mock_javamail.Mailbox;

import java.util.List;
import java.util.Map;

import static dk.dbc.promat.service.batch.ScheduledNotificationSenderIT.mailManager;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class ReviewerCoreDataChangedIT extends ContainerTest {
    TransactionScopedPersistenceContext persistenceContext;
    private final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final ScheduledNotificationSender scheduledNotificationSender  = new ScheduledNotificationSender();;

    @BeforeEach
    public void setupMailStuff() {
        persistenceContext = new TransactionScopedPersistenceContext(entityManager);
        Mailbox.clearAll();
        scheduledNotificationSender.entityManager = entityManager;
        scheduledNotificationSender.serverRole = ServerRole.PRIMARY;
        scheduledNotificationSender.notificationSender = new NotificationSender();
        scheduledNotificationSender.notificationSender.mailManager = mailManager;
        scheduledNotificationSender.notificationSender.metricRegistry = metricRegistry;

    }
    @Test
    public void testThatMailsAreOnlySentOnRealChangesAndWhenNotifyQueryParmIsTrue() throws InterruptedException {

        // Add private address, And check that a mail is added to the mailqueue with the changes.
        ReviewerRequest reviewerRequest =
                new ReviewerRequest()
                        .withPrivateAddress(new Address().withAddress1("Hellig Helges Vej"));
        Response response = putResponse("v1/api/reviewers/7", reviewerRequest, Map.of("notify", true), "1-2-3-4-5");
        persistenceContext.run(scheduledNotificationSender::processNotifications);
        assertThat("response status", response.getStatus(), is(200));

        List<Notification> notifications = getNotifications(null, "Hellig Helges Vej");

        assertThat("There is only one mail containing info with this address change",
                notifications.size(), is(1));
        assertThat("This is a mail to the LU mailaddress", notifications.get(0).getToAddress(), is("TEST@dbc.dk"));


        // Change the private address. Now with no notify parm. Expect nothing further in mail queue.
        reviewerRequest.getPrivateAddress().setAddress1("Thors Torden gade 11");
        response = putResponse("v1/api/reviewers/7", reviewerRequest, "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        notifications = getNotifications(null, "Thors Torden");
        assertThat("There are no mails to this change", notifications.size(), is(0));

        // Now submit the same address, but now with notify. Expect no mail.
        response = putResponse("v1/api/reviewers/7", reviewerRequest, Map.of("notify", true), "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        notifications = getNotifications(null, "Thors Torden");
        assertThat("There are no mails to this change", notifications.size(), is(0));

        // Change various stuff: Phone, private address, and "selected" on addresses.
        reviewerRequest = new ReviewerRequest()
                .withPhone("123456112233445566")
                .withPrivatePhone("987654321112233445566")
                .withPrivateAddress(new Address().withAddress1("Classensgade 12").withSelected(true))
                .withAddress(new Address().withSelected(false));
        response = putResponse("v1/api/reviewers/7", reviewerRequest, Map.of("notify", true), "1-2-3-4-5");
        assertThat("response status", response.getStatus(), is(200));
        notifications = getNotifications(null, "123456112233445566");
        assertThat("There are one mail matching this change", notifications.size(), is(1));
        Notification notification = notifications.get(0);

        assertThat("Address change is present", notification.getBodyText()
                .contains("Classensgade 12"));

        assertThat("Private phone change is present", notification.getBodyText()
                .contains("987654321112233445566"));

        assertThat("Phone change is present", notification.getBodyText()
                .contains("123456112233445566"));

        assertThat("Private address is selected", notification.getBodyText()
                .contains("<tr>\n" +
                        "                <td>[privateSelected]</td>\n" +
                        "            </tr>\n" +
                        "            \n" +
                        "                <tr>\n" +
                        "                    <td><table>\n" +
                        "                            <tr>\n" +
                        "                                <td>\n" +
                        "                                    Fra:\n" +
                        "                                </td>\n" +
                        "                                <td>\n" +
                        "                                    false\n" +
                        "                                </td>\n" +
                        "                            </tr>\n" +
                        "                            <tr>\n" +
                        "                                <td>\n" +
                        "                                    Til:\n" +
                        "                                </td>\n" +
                        "                                <td>\n" +
                        "                                    true\n" +
                        "                                </td>\n" +
                        "                            </tr>\n" +
                        "                        </table>"));


    }
}
