/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

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
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

public class TasksIT extends ContainerTest {

    @Test
    public void testEditTask() throws JsonProcessingException {

        // Update of unknown task - should return 404 NOT FOUND
        TaskDto dto = new TaskDto();
        Response response = putResponse("v1/api/tasks/9876", dto);
        assertThat("status code", response.getStatus(), is(404));

        // Create a new case
        CaseRequest caseDto = new CaseRequest()
                .withTitle("Title for 11001111")
                .withDetails("Details for 11001111")
                .withPrimaryFaust("11001111")
                .withRelatedFausts(Arrays.asList("11002222"))
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.DESCRIPTION)
                                .withTargetFausts(Arrays.asList("11001111")),
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withTargetFausts(Arrays.asList(new String[] {"11003333", "11004444"}))
                ));
        response = postResponse("v1/api/cases", caseDto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase createdCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("related fausts", createdCase.getRelatedFausts()
                        .stream().sorted().collect(Collectors.toList()),
                is(Arrays.asList("11002222", "11003333", "11004444")));

        // Find task ids
        PromatTask taskWithPrimaryTargetFaust = createdCase.getTasks().stream()
                .filter(task -> task.getTargetFausts() != null && task.getTargetFausts().size() != 0)
                .filter(task -> task.getTargetFausts().contains("11001111"))
                .findFirst().get();
        PromatTask taskWithRelatedTargetFaust = createdCase.getTasks().stream()
                .filter(task -> task.getTargetFausts() != null && task.getTargetFausts().size() != 0)
                .filter(task -> task.getTargetFausts().contains("11003333"))
                .findFirst().get();
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

        // Add an existing related faustnumber to one task, this should succeed
        dto = new TaskDto().withTargetFausts(Arrays.asList("11002222"));
        response = putResponse("v1/api/tasks/" + taskWithRelatedTargetFaust.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), PromatTask.class);
        assertThat("targetfaust is not null", updated.getTargetFausts(), is(notNullValue()));
        assertThat("targetfaust contains", updated.getTargetFausts().stream().findFirst().get().equals("11002222"), is(true));

        // Related fausts should remain unchanged
        response = getResponse("v1/api/cases/" + createdCase.getId());
        assertThat("status code", response.getStatus(), is(200));
        PromatCase updatedCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("related fausts", updatedCase.getRelatedFausts()
                        .stream().sorted().collect(Collectors.toList()),
                is(Arrays.asList("11002222", "11003333", "11004444")));

        // Add a new faustnumber to one task, this should succeed
        dto = new TaskDto().withTargetFausts(updated.getTargetFausts());
        dto.getTargetFausts().add("11005555");
        response = putResponse("v1/api/tasks/" + taskWithPrimaryTargetFaust.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), PromatTask.class);
        assertThat("targetfaust is not null", updated.getTargetFausts(), is(notNullValue()));
        assertThat("targetfaust contains", updated.getTargetFausts().stream().sorted().collect(Collectors.toList()),
                is(Arrays.asList("11002222", "11005555")));

        // Related fausts should now also contain 11005555
        response = getResponse("v1/api/cases/" + createdCase.getId());
        assertThat("status code", response.getStatus(), is(200));
        updatedCase = mapper.readValue(response.readEntity(String.class), PromatCase.class);
        assertThat("related fausts", updatedCase.getRelatedFausts()
                        .stream().sorted().collect(Collectors.toList()),
                is(Arrays.asList("11002222", "11003333", "11004444", "11005555")));

        // We do not prevent adding the same targetfaust to more tasks, even though it may be
        // a bit useless if it is added to two different tasks with the same TaskFieldType.
        // Such duality may exist for a short period when the user is moving a targetfaust from one task
        // to another and they would wonder why the got an error.
        dto = new TaskDto().withTargetFausts(taskWithRelatedTargetFaust.getTargetFausts());
        dto.getTargetFausts().add("11002222");
        response = putResponse("v1/api/tasks/" + taskWithRelatedTargetFaust.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), PromatTask.class);
        assertThat("targetfaust is not null", updated.getTargetFausts(), is(notNullValue()));
        assertThat("targetfaust contains", updated.getTargetFausts().stream().sorted().collect(Collectors.toList()),
                is(Arrays.asList("11002222", "11003333", "11004444")));

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
}
