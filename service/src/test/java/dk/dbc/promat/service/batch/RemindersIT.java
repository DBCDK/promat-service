package dk.dbc.promat.service.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.dto.TaskDto;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.persistence.TaskType;
import dk.dbc.promat.service.templating.Formatting;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class RemindersIT extends ContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemindersIT.class);

    @Test
    public void testThatRemindersAreSentThreeDaysPriorToDealineAndEverydayPostDeadline() throws JsonProcessingException {


    // Create a case with deadline three days from now.
    CaseRequest dto = new CaseRequest()
            .withTitle("Title for 80011115")
            .withDetails("Details for 80011115")
            .withPrimaryFaust("80011115")
            .withEditor(10)
            .withReviewer(7)
            .withSubjects(Arrays.asList(3, 4))
            .withDeadline(LocalDate.now().plusDays(3L).toString())
            .withMaterialType(MaterialType.MULTIMEDIA)
            .withTasks(Arrays.asList(
                    new TaskDto()
                            .withTaskType(TaskType.MULTIMEDIA_FEE)
                            .withTaskFieldType(TaskFieldType.BRIEF)
                            .withTargetFausts(List.of("80011115")),
                    new TaskDto()
                            .withTaskType(TaskType.MULTIMEDIA_FEE)
                            .withTaskFieldType(TaskFieldType.BKM)
                            .withTargetFausts(List.of("80011115"))
            ));

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase promatCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Run an update ONLY on this case. And right now. And make sure that a reminder is produced.
        response = postResponse("v1/api/cases/"+promatCase.getId()+"/processreminder", null);
        assertThat("status code", response.getStatus(), is(200));
        promatCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("reminder sent today", promatCase.getReminderSent(), is(LocalDate.now()));

        // Make sure that there is just one notification, on this reminder.
        List<Notification> notifications =
                getNotifications("Afleveringsfrist for anmeldelse nærmer sig",
                        String.format("Afleveringsfristen er %s. Du har derfor mindre end 3 dage til at",
                                Formatting.format(promatCase.getDeadline())))
                        .stream().filter(notification ->
                        notification.getToAddress().contains("@holg.dk")).collect(Collectors.toList());
        assertThat("only one close-to-deadline reminder on this case", notifications.size(), is(1));


        // Ok. Now lets pretend that in a previous run a deadline was passed, and thus reminders were sent a month ago.
        // But then the deadline was moved, to within three days from now (in effect: no change in deadline
        // from previous test).
        dto = new CaseRequest().withReminderSent(LocalDate.now().minusMonths(1L).toString());
        response = postResponse("v1/api/cases/"+promatCase.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        promatCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Run an update ONLY on this case. And right now. And make sure that a reminder is produced.
        response = postResponse("v1/api/cases/"+promatCase.getId()+"/processreminder", null);
        assertThat("status code", response.getStatus(), is(200));
        promatCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("reminder sent today", promatCase.getReminderSent(), is(LocalDate.now()));

        // Make sure that there are now TWO of the same type of notifications, on this reminder.
         notifications =
                getNotifications("Afleveringsfrist for anmeldelse nærmer sig",
                        String.format("Afleveringsfristen er %s. Du har derfor mindre end 3 dage til at",
                                Formatting.format(promatCase.getDeadline())))
                        .stream().filter(notification ->
                        notification.getToAddress().contains("@holg.dk")).collect(Collectors.toList());
        assertThat("now two close-to-deadline reminders on this case", notifications.size(), is(2));

        // Now move deadline to yesterday to provoke a "you're-late" reminder.
        dto = new CaseRequest()
                .withDeadline(LocalDate.now().minusDays(1L).toString())
                .withReminderSent(LocalDate.now().minusDays(4).toString());
        response = postResponse("v1/api/cases/"+promatCase.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        promatCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);


        // Run an update ONLY on this case. And right now. And make sure that a reminder is produced.
        response = postResponse("v1/api/cases/"+promatCase.getId()+"/processreminder", null);
        assertThat("status code", response.getStatus(), is(200));
        promatCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("reminder sent today", promatCase.getReminderSent(), is(LocalDate.now()));


        // Make sure that there are only one 'you're-late' reminder.
        notifications =
                getNotifications("Afleveringsfrist for anmeldelse",
                        String.format("Afleveringsfristen var %s, " +
                                        "men vi har endnu ikke modtaget udtalelsen fra dig.",
                                Formatting.format(promatCase.getDeadline())))
                        .stream().filter(notification ->
                        notification.getToAddress().contains("@holg.dk")).collect(Collectors.toList());
        assertThat("A 'you're-late' reminder was sent today", notifications.size(), is(1));

        // Delete the case
        response = deleteResponse("v1/api/cases/" + promatCase.getId());
        assertThat("status code", response.getStatus(), is(200));

    }
}
