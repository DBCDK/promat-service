package dk.dbc.promat.service.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.commons.persistence.TransactionScopedPersistenceContext;
import dk.dbc.connector.openformat.OpenFormatConnectorException;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.cluster.ServerRole;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.dto.TaskDto;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.persistence.TaskType;
import dk.dbc.promat.service.templating.Formatting;
import dk.dbc.promat.service.templating.NotificationFactoryIT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScheduleRemindersIT extends ContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleRemindersIT.class);

    private TransactionScopedPersistenceContext persistenceContext;
    private static String wiremockHost;

    @BeforeAll
    public static void startWiremock() {
        wiremockHost = wireMockServer.baseUrl();
    }

    @BeforeEach
    public void setup() {
        persistenceContext = new TransactionScopedPersistenceContext(entityManager);
    }

    @Test
    public void testThatMailsAreSentEverydayAfterDeadline() throws JsonProcessingException, OpenFormatConnectorException {

        // Create a case with deadline yesterday
        CaseRequest dto = new CaseRequest()
                .withTitle("Title for 80011116")
                .withDetails("Details for 80011116")
                .withPrimaryFaust("80011116")
                .withEditor(10)
                .withReviewer(6)
                .withSubjects(Arrays.asList(3, 4))
                .withDeadline(LocalDate.now().minusDays(1L).toString())
                .withMaterialType(MaterialType.MULTIMEDIA)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.MULTIMEDIA_FEE)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(List.of("80011116")),
                        new TaskDto()
                                .withTaskType(TaskType.MULTIMEDIA_FEE)
                                .withTaskFieldType(TaskFieldType.BKM)
                                .withTargetFausts(List.of("80011116"))));

        // Rig up a test-local Scheduler.
        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase promatCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        ScheduledReminders scheduledReminders = new ScheduledReminders();
        scheduledReminders.reminders = new Reminders();
        scheduledReminders.reminders.entityManager = entityManager;
        scheduledReminders.reminders.notificationFactory = NotificationFactoryIT.getNotificationFactory(wiremockHost);
        scheduledReminders.ENABLE_REMINDERS = "true";
        scheduledReminders.serverRole = ServerRole.PRIMARY;
        persistenceContext.run(scheduledReminders::processReminders);

        // Make sure that there is only one 'you're-late' reminder.
        List<Notification> notifications =
                getNotifications("Afleveringsfrist for anmeldelse",
                        String.format("Afleveringsfristen var %s, " +
                                        "men vi har endnu ikke modtaget udtalelsen fra dig.",
                                Formatting.format(promatCase.getDeadline())))
                        .stream().filter(notification ->
                        notification.getToAddress().contains("@mich.dk")).collect(Collectors.toList());
        assertThat("A 'you're-late' reminder was sent today", notifications.size(), is(1));

        // Test that reminders keep being sent everyday into eternity, as long as deadline never changes.
        LocalDate tomorrow = LocalDate.now().plusDays(1L);
        LocalDate today = LocalDate.now();
        scheduledReminders.reminders.todayProvider = mock(Reminders.Today.class);
        when(scheduledReminders.reminders.todayProvider.toDay()).thenReturn(tomorrow);
        persistenceContext.run(scheduledReminders::processReminders);
        for(Notification  n : getNotifications()) {
            LOGGER.info("To:{} subject:{} created:{}", n.getToAddress(), n.getSubject(), n.getCreated());
        }
        
        // Make sure that there are now two 'you're-late' reminders.
        notifications =
                getNotifications("Afleveringsfrist for anmeldelse",
                        String.format("Afleveringsfristen var %s, " +
                                        "men vi har endnu ikke modtaget udtalelsen fra dig.",
                                Formatting.format(promatCase.getDeadline())))
                        .stream().filter(notification ->
                        notification.getToAddress().contains("@mich.dk")).collect(Collectors.toList());
        assertThat("Two 'you're-late' reminders were sent. One today, one tomorrow (timetravel!)", notifications.size(), is(2));

        // Make sure, that even if reminders are processed several times a day, a reminder is only sent once.
        persistenceContext.run(scheduledReminders::processReminders);
        notifications =
                getNotifications("Afleveringsfrist for anmeldelse",
                        String.format("Afleveringsfristen var %s, " +
                                        "men vi har endnu ikke modtaget udtalelsen fra dig.",
                                Formatting.format(promatCase.getDeadline())))
                        .stream().filter(notification ->
                        notification.getToAddress().contains("@mich.dk")).collect(Collectors.toList());
        assertThat("Still only two 'you're-late' reminders was sent. " +
                "Even after two invocations of processReminders", notifications.size(), is(2));


        // Delete the case
        response = deleteResponse("v1/api/cases/" + promatCase.getId());
        assertThat("status code", response.getStatus(), is(200));
    }
}
