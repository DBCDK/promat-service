package dk.dbc.promat.service.batch;

import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.ReviewerRequest;
import dk.dbc.promat.service.persistence.Address;
import dk.dbc.promat.service.persistence.Notification;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ReviewerCoreDataChangedIT extends ContainerTest {

    @Test
    public void testThatMailsAreOnlySentOnRealChangesAndWhenNotifyQueryParmIsTrue() {

        // Reset "inactiveInterval". Minus 1 means do perform creation of notifications now.
        clear();

        // Add private address, And check that a mail is added to the mailqueue with the changes.
        ReviewerRequest reviewerRequest =
                new ReviewerRequest()
                        .withPrivateAddress(new Address().withAddress1("Hellig Helges Vej"));

        List<Notification> notifications = performUpdateAndGetNotificationList(reviewerRequest, "Hellig Helges Vej", true);

        assertThat("There is only one mail containing info with this address change",
                notifications.size(), is(1));
        assertThat("This is a mail to the LU mailaddress", notifications.get(0).getToAddress(), is("lumailaddress-test@dbc.dk"));

        // Change the private address. Now with no notify parm. Expect nothing further in mail queue.
        reviewerRequest.getPrivateAddress().setAddress1("Thors Torden gade 11");
        notifications = performUpdateAndGetNotificationList(reviewerRequest, "Thors Torden gade 11", false);
        assertThat("There are no mails to this change", notifications.size(), is(0));

        // Now submit the same address, but now with notify. Expect no mail.
        notifications = performUpdateAndGetNotificationList(reviewerRequest, "Thors Torden", true);
        assertThat("There are no mails to this change", notifications.size(), is(0));

        // Change various stuff: Phone, private address, and "selected" on addresses.
        reviewerRequest = new ReviewerRequest()
                .withPhone("123456112233445566")
                .withPrivatePhone("987654321112233445566")
                .withPrivateAddress(new Address().withAddress1("Classensgade 12").withSelected(true))
                .withAddress(new Address().withSelected(false));

        notifications = performUpdateAndGetNotificationList(reviewerRequest, "123456112233445566", true);
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

    @Test
    public void testThatNumerousUpdatesWithinSafeLimitsOfUserEditTimeoutsResultsInOnlyOneNotification() throws InterruptedException {
        clear();

        try {
            // Lower "inactiveInterval" to 3 seconds to simulate user interactions.
            Response response = postResponse("v1/api/batch/job/userupdater/config/3", null);

            ReviewerRequest reviewerRequest = new ReviewerRequest();
            assertThat("response status", response.getStatus(), is(200));
            reviewerRequest.withPrivateAddress(new Address().withAddress1("Hedne Hedwigs gade"));
            assertThat("response status", response.getStatus(), is(200));
            List<Notification> notifications = performUpdateAndGetNotificationList(reviewerRequest, "Hedne Hedwig", true);
            assertThat("There are no mail matching this change. Yet!.", notifications.size(), is(0));

            // Do a lot of changes to vacation for user 7
            reviewerRequest = new ReviewerRequest().withHiatusBegin(LocalDate.now().plusDays(1));
            notifications = performUpdateAndGetNotificationList(reviewerRequest, "Hedne Hedwig", true);
            assertThat("There are no mail matching this change. Yet!.", notifications.size(), is(0));

            reviewerRequest = new ReviewerRequest().withHiatusEnd(LocalDate.now().plusDays(5));
            notifications = performUpdateAndGetNotificationList(reviewerRequest, "Hedne Hedwig", true);
            assertThat("There are no mail matching this change. Yet!.", notifications.size(), is(0));

            reviewerRequest = new ReviewerRequest().withHiatusEnd(LocalDate.now().plusDays(3)).withHiatusBegin(LocalDate.now().plusDays(2));
            notifications = performUpdateAndGetNotificationList(reviewerRequest, "Hedne Hedwig", true);
            assertThat("There are no mail matching this change. Yet!.", notifications.size(), is(0));

            Thread.sleep(4000);

            // Cronjob should have fired by now. Resulting in only one mail.
            postResponse("v1/api/batch/job/userupdater", null);
            notifications = getNotifications(null, "Hedne Hedwig");
            assertThat("There is one mail matching this change.", notifications.size(), is(1));

            clear();
        } finally {

            // Reset update interval to a more reasonable half an hour
            Response response = postResponse("v1/api/batch/job/userupdater/config/1800", null);
            assertThat("response status", response.getStatus(), is(200));
        }
    }

    private List<Notification> performUpdateAndGetNotificationList(ReviewerRequest reviewerRequest, String bodyTextWildcard, boolean notify) {
        return performUpdateAndGetNotificationList(reviewerRequest, bodyTextWildcard, null, notify);
    }

    private List<Notification> performUpdateAndGetNotificationList(ReviewerRequest reviewerRequest, String bodyTextWildcard, String subjectWildCard, boolean notify) {
        Response response = putResponse("v1/api/reviewers/7", reviewerRequest, notify ? Map.of("notify", true) : null, "2-3-4-5-6");
        assertThat("response status", response.getStatus(), is(200));
        response = postResponse("v1/api/batch/job/userupdater",null);
        assertThat("response status", response.getStatus(), is(200));
        return getNotifications(null, bodyTextWildcard);
    }


    // Flush the rest of all pending userupdates
    private void clear() {
        Response response = postResponse("v1/api/batch/job/userupdater/config/-1", null);
        assertThat("response status", response.getStatus(), is(200));
        response = postResponse("v1/api/batch/job/userupdater",null);
        assertThat("response status", response.getStatus(), is(200));
    }

}
