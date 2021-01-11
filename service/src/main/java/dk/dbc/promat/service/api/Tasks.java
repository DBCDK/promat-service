/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import dk.dbc.promat.service.Repository;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.dto.TaskDto;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.PromatTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@Path("")
public class Tasks {
    private static final Logger LOGGER = LoggerFactory.getLogger(Tasks.class);

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @EJB
    Repository repository;

    @PUT
    @Path("tasks/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTask(@PathParam("id") final Integer id, TaskDto dto) {
        LOGGER.info("tasks/{} (PUT) body: {}", id, dto);

        try {

            // Fetch the existing task with the given id
            PromatTask existing = entityManager.find(PromatTask.class, id);
            if( existing == null ) {
                LOGGER.info("No such task {}", id);
                return ServiceErrorDto.NotFound("No such task", String.format("Task with id {} does not exist", id));
            }

            // Find the case to which the stated task belongs
            PromatCase caseOfTask = getCaseOfTask(id);

            // Lock tables so that we can ensure that target faustnumbers only exist on a single case and task
            repository.getExclusiveAccessToTable(PromatCase.TABLE_NAME);
            repository.getExclusiveAccessToTable(PromatTask.TABLE_NAME);

            // Check that target faustnumbers has unique usage across a case
            if(dto.getTargetFausts() != null) {
                if(!Faustnumbers.checkNoOpenCaseWithFaust(entityManager, caseOfTask.getId(), dto.getTargetFausts().toArray(String[]::new))) {
                    LOGGER.info("Attempt to add one or more targetfausts {} which is in use on another active case", dto.getTargetFausts());
                    return ServiceErrorDto.InvalidRequest("Target faustnumber is in use", "One or more target faustnumbers is in use on another active case");
                }
            }

            // Update fields
            if(dto.getData() != null) {
                existing.setData(dto.getData()); // It is allowed to update with an empty value
            }
            if(dto.getTargetFausts() != null) {
                existing.setTargetFausts(dto.getTargetFausts());
                updateRelatedFausts(caseOfTask, existing);
            }

            return Response.ok(existing).build();
        } catch(ServiceErrorException serviceErrorException) {
            LOGGER.info("Received serviceErrorException while updating task: {}", serviceErrorException.getMessage());
            return Response.status(serviceErrorException.getHttpStatus()).entity(serviceErrorException.getServiceErrorDto()).build();
        } catch(Exception exception) {
            LOGGER.error("Caught exception: {}", exception.getMessage());
            return ServiceErrorDto.Failed(exception.getMessage());
        }
    }

    @DELETE
    @Path("tasks/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteTask(@PathParam("id") final Integer id) {
        LOGGER.info("tasks/{} (DELETE)", id);

        try {

            /* Locking is not needed since removing a task does not modify the list of related faustnumbers */

            // Find the case to which the stated task belongs
            PromatCase caseOfTask = getCaseOfTask(id);
            LOGGER.info("Task with id {} exist on case {}", id, caseOfTask.getId());

            // Find the task to be removed
            PromatTask task = caseOfTask.getTasks().stream()
                    .filter(t -> t.getId() == id)
                    .findFirst()
                    .get();
            if( task == null ) {
                // This is highly unexpected!. If we got a case, we must be able to find the task in the list of tasks
                LOGGER.error("No such task {} allthough we did resolve the case of this task", id);
                return Response.serverError().entity("Task not found on case which did resolve to this task id").build();
            }

            // Make sure the task can be deleted
            if( task.getApproved() != null || task.getPayed() != null || (task.getData() != null && !task.getData().isEmpty())) {
                LOGGER.info("Task is approved, payed or has data. Deletion rejected");
                return ServiceErrorDto.Forbidden("Not possible", "Task is approved, payed or has data. Deletion is not possible");
            }

            // Delete the task
            LOGGER.info("Deleting task with id {} from case with id {}", task.getId(), caseOfTask.getId());
            caseOfTask.getTasks().remove(task);
            entityManager.remove(task);

            return Response.ok().build();
        } catch(ServiceErrorException serviceErrorException) {
            LOGGER.info("Received serviceErrorException while updating task: {}", serviceErrorException.getMessage());
            return Response.status(serviceErrorException.getHttpStatus()).entity(serviceErrorException.getServiceErrorDto()).build();
        } catch(Exception exception) {
            LOGGER.error("Caught exception: {}", exception.getMessage());
            return ServiceErrorDto.Failed(exception.getMessage());
        }
    }

    private PromatCase getCaseOfTask(int taskId) throws ServiceErrorException {
        final TypedQuery<PromatCase> query = entityManager.createNamedQuery(
                PromatCase.GET_CASE_NAME, PromatCase.class);
        query.setParameter("taskid", taskId);

        PromatCase caseOfTask = query.getResultList().stream().findFirst().orElse(null);

        if( caseOfTask == null) {
            LOGGER.info(String.format("Unable to load  case of task with id %d", taskId));
            throw new ServiceErrorException("Case not found")
                    .withCause("Case not found")
                    .withDetails(String.format("Task with id %d do not belong to any task", taskId))
                    .withCode(ServiceErrorCode.NOT_FOUND)
                    .withHttpStatus(404);
        }

        return entityManager.merge(caseOfTask);
    }

    private void updateRelatedFausts(PromatCase caseOfTask, PromatTask task) {
        for(String faust : task.getTargetFausts()) {
            if(!caseOfTask.getRelatedFausts().contains(faust)) {
                caseOfTask.getRelatedFausts().add(faust);
            }
        }
    }
}
