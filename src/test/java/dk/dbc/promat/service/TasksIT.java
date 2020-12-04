/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.dto.CaseRequestDto;
import dk.dbc.promat.service.dto.CaseSummaryList;
import dk.dbc.promat.service.dto.TaskDto;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.Subject;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.persistence.TaskType;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

public class TasksIT extends ContainerTest {

    @Test
    public void testEditTask() throws JsonProcessingException {

        // Update of unknown task - should return 404 NOT FOUND
        TaskDto dto = new TaskDto();
        Response response = postResponse("v1/api/tasks/9876", dto);
        assertThat("status code", response.getStatus(), is(404));

        // Create a new case
        CaseRequestDto caseDto = new CaseRequestDto()
                .withTitle("Title for 11001111")
                .withDetails("Details for 11001111")
                .withPrimaryFaust("11001111")
                .withRelatedFausts(Arrays.asList("11002222"))
                .withMaterialType(MaterialType.BOOK)
                .withTasks(Arrays.asList(
                        new TaskDto()
                                .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                                .withTaskFieldType(TaskFieldType.BRIEF),
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
        PromatTask taskNoTargetFaust = createdCase.getTasks().stream().filter(task -> task.getTargetFausts() == null).findFirst().get();
        PromatTask taskWithTargetFaust = createdCase.getTasks().stream().filter(task -> task.getTargetFausts() != null && task.getTargetFausts().size() != 0).findFirst().get();
        assertThat("has task without targetFaust", taskNoTargetFaust, is(notNullValue()));
        assertThat("has task with targetFaust", taskNoTargetFaust, is(notNullValue()));

        // Update first task - should return 200 OK
        dto = new TaskDto().withData("Here is data for task without targetFaust");
        response = postResponse("v1/api/tasks/" + taskNoTargetFaust.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        PromatTask updated = mapper.readValue(response.readEntity(String.class), PromatTask.class);
        assertThat("data value is correct", updated.getData().equals("Here is data for task without targetFaust"), is(true));

        // Update second task - should return 200 OK
        dto = new TaskDto().withData("Here is data for task with targetFaust");
        response = postResponse("v1/api/tasks/" + taskWithTargetFaust.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), PromatTask.class);
        assertThat("data value is correct", updated.getData().equals("Here is data for task with targetFaust"), is(true));

        // Check that we can update the data field with an empty string, but not null - but neither request should fail
        dto = new TaskDto().withData(null);
        response = postResponse("v1/api/tasks/" + taskNoTargetFaust.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), PromatTask.class);
        assertThat("data value is correct", updated.getData().equals("Here is data for task without targetFaust"), is(true));

        dto = new TaskDto().withData("");
        response = postResponse("v1/api/tasks/" + taskNoTargetFaust.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));
        updated = mapper.readValue(response.readEntity(String.class), PromatTask.class);
        assertThat("data value is correct", updated.getData().equals(""), is(true));

        // Add an existing related faustnumber to one task, this should succeed
        dto = new TaskDto().withTargetFausts(Arrays.asList("11002222"));
        response = postResponse("v1/api/tasks/" + taskNoTargetFaust.getId(), dto);
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
        response = postResponse("v1/api/tasks/" + taskNoTargetFaust.getId(), dto);
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
    }
}
