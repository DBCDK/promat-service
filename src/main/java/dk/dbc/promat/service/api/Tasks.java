/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.dto.TaskDto;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.Repository;
import dk.dbc.promat.service.persistence.PromatTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
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

    @EJB Repository repository;

    @POST
    @Path("tasks/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTask(@PathParam("id") final Integer id, TaskDto dto) {
        LOGGER.info("tasks/{} (POST) body: {}", id, dto);

        try {

            // Fetch the existing task with the given id
            PromatTask existing = entityManager.find(PromatTask.class, id);
            if( existing == null ) {
                LOGGER.info("No such task {}", id);
                return ServiceErrorDto.NotFound("No such task", String.format("Task with id {} does not exist", id));
            }

            // Lock tables so that we can ensure that target faustnumbers only exist on a single case and task
            repository.getExclusiveAccessToTable(PromatCase.TABLE_NAME);
            repository.getExclusiveAccessToTable(PromatTask.TABLE_NAME);

            // Check that target faustnumbers has unique usage across a case
            if(dto.getTargetFausts() != null) {
                // Todo: check that all targetfausts exists as related fausts on the case
                // Todo: check that all targetfausts do not exist on other tasks on the case
            }

            // Update fields
            if(dto.getData() != null) {
                existing.setData(dto.getData()); // It is allowed to update with an empty value
            }
            if(dto.getTargetFausts() != null) {
                existing.setTargetFausts(dto.getTargetFausts());
            }

            return Response.ok(existing).build();
        }
        catch(Exception exception) {
            LOGGER.error("Caught exception: {}", exception.getMessage());
            return ServiceErrorDto.Failed(exception.getMessage());
        }
    }
}
