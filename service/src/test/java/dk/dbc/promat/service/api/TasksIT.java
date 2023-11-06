package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.dto.TaskDto;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PayCategory;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.persistence.TaskType;
import dk.dbc.promat.service.util.PromatTaskUtils;
import org.junit.jupiter.api.Test;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

public class TasksIT extends ContainerTest {

    Logger LOGGER = LoggerFactory.getLogger(TasksIT.class);

    @Test
    public void testEditTask() throws JsonProcessingException {

        // Update of unknown task - should return 404 NOT FOUND
        TaskDto dto = new TaskDto();
        Response response = putResponse("v1/api/tasks/9876", dto);
        assertThat("status code", response.getStatus(), is(404));

        // Create a new case
        CaseRequest caseDto = new CaseRequest()
                .withTitle("Title for 31001111")
                .withDetails("Details for 31001111")
                .withPrimaryFaust("31001111")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.DESCRIPTION)
                                .withTargetFausts(Arrays.asList("31001111", "31002222")),
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Arrays.asList("31003333", "31004444"))
                ));
        response = postResponse("v1/api/cases", caseDto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase createdCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("related fausts", createdCase.getTasks()
                        .stream()
                        .flatMap(t -> t.getTargetFausts()
                                .stream())
                        .sorted()
                        .collect(Collectors.toList()),
                is(Arrays.asList("31001111", "31002222", "31003333", "31004444")));

        // Find task ids
        PromatTask taskWithPrimaryTargetFaust = createdCase.getTasks().stream()
                .filter(task -> task.getTargetFausts() != null && task.getTargetFausts().size() != 0)
                .filter(task -> task.getTargetFausts().contains("31001111"))
                .findFirst().orElseThrow();
        PromatTask taskWithRelatedTargetFaust = createdCase.getTasks().stream()
                .filter(task -> task.getTargetFausts() != null && task.getTargetFausts().size() != 0)
                .filter(task -> task.getTargetFausts().contains("31003333"))
                .findFirst().orElseThrow();
        assertThat("has task with primary targetFaust", taskWithPrimaryTargetFaust, is(notNullValue()));
        assertThat("has task with related targetFaust", taskWithRelatedTargetFaust, is(notNullValue()));

        // Update first task - should return 200 OK
        dto = new TaskDto().withData("Here is data for task without targetFaust")
                .withTaskType(TaskType.GROUP_2_100_UPTO_199_PAGES);
        response = putResponse("v1/api/tasks/" + taskWithPrimaryTargetFaust.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        PromatTask updated = mapper.readValue(response.readEntity(String.class), PromatTask.class);

        assertThat("data value is correct", updated.getData().equals("Here is data for task without targetFaust"), is(true));
        assertThat("tasktype value is correct", updated.getTaskType(), is(TaskType.GROUP_2_100_UPTO_199_PAGES));
        assertThat("paycategory value is correct", updated.getPayCategory(), is(PayCategory.GROUP_2_100_UPTO_199_PAGES));

        // Update second task - should return 200 OK
        dto = new TaskDto().withData("Here is data for task with targetFaust")
                .withTaskType(TaskType.GROUP_2_100_UPTO_199_PAGES);
        response = putResponse("v1/api/tasks/" + taskWithRelatedTargetFaust.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), PromatTask.class);
        assertThat("data value is correct", updated.getData().equals("Here is data for task with targetFaust"), is(true));
        assertThat("tasktype value is correct", updated.getTaskType(), is(TaskType.GROUP_2_100_UPTO_199_PAGES));
        assertThat("paycategory value is correct", updated.getPayCategory(), is(PayCategory.BRIEF));

        // Check that we can update the data field with an empty string, but not null - but neither request should fail
        dto = new TaskDto().withData(null);
        response = putResponse("v1/api/tasks/" + taskWithPrimaryTargetFaust.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), PromatTask.class);
        assertThat("data value is correct", updated.getData().equals("Here is data for task without targetFaust"), is(true));

        dto = new TaskDto().withData("");
        response = putResponse("v1/api/tasks/" + taskWithPrimaryTargetFaust.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), PromatTask.class);
        assertThat("data value is correct", updated.getData().equals(""), is(true));

        // Add a new faustnumber to one task, this should succeed
        dto = new TaskDto().withTargetFausts(updated.getTargetFausts());
        dto.getTargetFausts().add("31005555");
        LOGGER.info("targetFaust is {}", dto.getTargetFausts());

        response = putResponse("v1/api/tasks/" + taskWithPrimaryTargetFaust.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), PromatTask.class);
        assertThat("targetfaust is not null", updated.getTargetFausts(), is(notNullValue()));
        assertThat("targetfaust contains", updated.getTargetFausts().stream().sorted().collect(Collectors.toList()),
                is(Arrays.asList("31001111", "31002222", "31005555")));

        // When adding a targetfaust, the number should either not belong to any active case, or belong (as primary
        // or related faust) to the case to which the task belongs
        // - 003333 is related faust on case id 1
        // - 004444 is primary faust on case id 2
        dto = new TaskDto().withTargetFausts(Arrays.asList("003333"));
        response = putResponse("v1/api/tasks/" + taskWithPrimaryTargetFaust.getId(), dto);
        assertThat("status code", response.getStatus(), is(400));
        dto = new TaskDto().withTargetFausts(Arrays.asList("004444"));
        response = putResponse("v1/api/tasks/" + taskWithPrimaryTargetFaust.getId(), dto);
        assertThat("status code", response.getStatus(), is(400));

        // When updating faustnumbers they must be unique and ordered by entry
        // the system will remove all duplicate faustnumbers
        dto = new TaskDto().withTargetFausts(updated.getTargetFausts());

        // The faustnumbers set earlier in the test
        assertThat("targetfaust contains", updated.getTargetFausts().stream().sorted().collect(Collectors.toList()),
                is(Arrays.asList("31001111", "31002222", "31005555")));
        response = putResponse("v1/api/tasks/" + taskWithPrimaryTargetFaust.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));

        // We can easily add duplicate faustnumbers to the list
        dto.getTargetFausts().add("31001111");
        dto.getTargetFausts().add("31007777");
        dto.getTargetFausts().add("31006666");

        // The list now contains duplicate faustnumbers; 31001111 has two entries
        assertThat("targetfaust contains", new ArrayList<>(updated.getTargetFausts()),
                is(Arrays.asList("31001111", "31002222", "31005555", "31001111", "31007777", "31006666")));
        response = putResponse("v1/api/tasks/" + taskWithPrimaryTargetFaust.getId(), dto);

        // our API call succeeds
        assertThat("status code", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), PromatTask.class);

        // Duplicate faustnumbers were removed by the system without notifying the user and the order maintained
        assertThat("targetfaust contains no duplicates", new ArrayList<>(updated.getTargetFausts()),
                is(Arrays.asList("31001111", "31002222", "31005555", "31007777", "31006666")));
    }

    @Test
    public void testDeleteTask() throws JsonProcessingException {

        // Try to delete task on non-existing case
        assertThat("status code", deleteResponse("v1/api/tasks/99999").getStatus(), is(404));

        // Check that the testcase is as expected
        Response response = getResponse("v1/api/cases/1");
        assertThat("status code", response.getStatus(), is(200));
        PromatCase existing = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("number of tasks", existing.getTasks().size(), is(5));

        // Delete tasks
        assertThat("status code", deleteResponse("v1/api/tasks/1").getStatus(), is(401));
        assertThat("status code", deleteResponse("v1/api/tasks/2").getStatus(), is(401));
        assertThat("status code", deleteResponse("v1/api/tasks/3").getStatus(), is(401));
        assertThat("status code", deleteResponse("v1/api/tasks/4").getStatus(), is(200));
        assertThat("status code", deleteResponse("v1/api/tasks/5").getStatus(), is(200));

        // Testcase should now contain only two tasks
        response = getResponse("v1/api/cases/1");
        assertThat("status code", response.getStatus(), is(200));
        existing = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("number of tasks", existing.getTasks().size(), is(3));
        assertThat("expected tasks", existing.getTasks().stream().map(task -> task.getId()).sorted().collect(Collectors.toList()),
                is(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testPromatTaskUtilsGetFormattedDataForLinksMarkup() {
        assertThat("no markup",
                PromatTaskUtils.getFormattedDataForLinksMarkup("Here is some data without markup")
                        .equals("Here is some data without markup"));
        assertThat("markup, current style",
                PromatTaskUtils.getFormattedDataForLinksMarkup("this link <t>1234<t> is marked")
                        .equals("this link <span style=\"font-style: italic;\">1234</span> is marked"));
        assertThat("markup of multiple links, current style",
                PromatTaskUtils.getFormattedDataForLinksMarkup("this link <t>1234<t> is marked, and also <t>this<t> link is marked")
                        .equals("this link <span style=\"font-style: italic;\">1234</span> is marked, and also <span style=\"font-style: italic;\">this</span> link is marked"));
        assertThat("markup, leading tag",
                PromatTaskUtils.getFormattedDataForLinksMarkup("<t>1234<t> is marked")
                        .equals("<span style=\"font-style: italic;\">1234</span> is marked"));
        assertThat("markup, trailing tag",
                PromatTaskUtils.getFormattedDataForLinksMarkup("this link should be marked <t>1234<t>")
                        .equals("this link should be marked <span style=\"font-style: italic;\">1234</span>"));
        assertThat("markup, leading and trailing tags",
                PromatTaskUtils.getFormattedDataForLinksMarkup("<t>this link should be marked 1234<t>")
                        .equals("<span style=\"font-style: italic;\">this link should be marked 1234</span>"));
    }
}
