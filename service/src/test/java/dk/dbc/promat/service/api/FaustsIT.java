package dk.dbc.promat.service.api;

import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.dto.TaskDto;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.persistence.TaskType;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

class FaustsIT extends ContainerTest {
    CaseRequest dto = new CaseRequest()
            .withTitle("Title for 18001113")
            .withDetails("Details for 18001113")
            .withPrimaryFaust("18001113")
            .withEditor(10)
            .withSubjects(Arrays.asList(3, 4))
            .withDeadline("2020-12-18")
            .withMaterialType(MaterialType.BOOK)
            .withTasks(Arrays.asList(
                    new TaskDto()
                            .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                            .withTaskFieldType(TaskFieldType.BRIEF)
                            .withTargetFausts(Arrays.asList("18002223", null, "18004446")),
                    new TaskDto()
                            .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                            .withTaskFieldType(TaskFieldType.BKM)
                            .withTargetFausts(Arrays.asList("18001112"))
            ));

    private TaskDto getTask(TaskFieldType taskFieldType, boolean withNull) {
        TaskDto taskWithNull = dto.getTasks().stream()
                .filter(taskDto -> taskDto.getTaskFieldType() == taskFieldType)
                .findFirst().orElse(null);
        Assertions.assertNotNull(taskWithNull);
        taskWithNull.setTargetFausts(Arrays.asList("18002223", withNull ? null : "180011115", "18004446"));
        return taskWithNull;
    }

    @Test
    void testNullFaustInCreateCase() {

        // Try Creating a case with a task that has one null target faust
        dto.withTasks(Arrays.asList(
                getTask(TaskFieldType.BRIEF, true),
                getTask(TaskFieldType.BKM, false)));

        ServiceErrorDto e = postResponse("v1/api/cases", dto, ServiceErrorDto.class, Response.Status.BAD_REQUEST);
        assertThat(e.getDetails(), is("One or more target faustnumbers are null or empty. Task: " +
                "TaskDto{taskType=GROUP_1_LESS_THAN_100_PAGES, taskFieldType=BRIEF, " +
                "targetFausts=[18002223, null, 18004446], data='null'}"));
    }

    @Test
    void thatChangingFaustsOnExistingToNullFaustWillFail() {

        // Create a valid case first
        dto.withTasks(Arrays.asList(
                getTask(TaskFieldType.BRIEF, false),
                getTask(TaskFieldType.BKM, false)
        ));
        PromatCase createdCase = postResponse("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);


        // Now try to change one of the task target fausts to null on the existing case.
        // Get the brief task.
        PromatTask t = createdCase.getTasks().stream()
                .filter(task -> task.getTaskFieldType() == TaskFieldType.BRIEF)
                .findFirst().orElse(null);
        Assertions.assertNotNull(t);
        t.setTargetFausts(Arrays.asList("18002223", null));

        ServiceErrorDto e = putResponse("v1/api/tasks/" + t.getId(), t, ServiceErrorDto.class, Response.Status.BAD_REQUEST);
        assertThat(e.getDetails(), is("One or more target faustnumbers are null or empty. " +
                "Task: TaskDto{taskType=GROUP_1_LESS_THAN_100_PAGES, " +
                "taskFieldType=BRIEF, targetFausts=[18002223, null], data='null'}"));

        deleteResponse("v1/api/cases/" + createdCase.getId());
    }
}
