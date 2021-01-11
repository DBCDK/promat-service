/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import dk.dbc.promat.service.Repository;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.PromatTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Stateless
@Path("payments")
public class Payments {
    private static final Logger LOGGER = LoggerFactory.getLogger(Payments.class);

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @EJB
    Repository repository;

    @GET
    @Path("preview")
    @Produces("application/csv") // Todo: Do we need a more gui-friendly format here ?
    public Response preview() {
        LOGGER.info("payments/preview (GET)");

        // Lock all relevant tables
        repository.getExclusiveAccessToTable(PromatCase.TABLE_NAME);
        repository.getExclusiveAccessToTable(PromatTask.TABLE_NAME);

        // Todo: Most likely, we'll end up with a more advanced object to hold payments data, but this works for now
        String csv = "Lønnr;Lønart;Antal;;;Tekst\n";

        // Fetch all non-payed tasks with status 'EXPORTED'
        // Todo: Perhaps more status should be exported ?
        return Response.status(200)
                .header("Content-Type", "application/csv")
                .header("Content-Disposition", "attachment; filename=payments_preview.csv")  // Todo: Append calendar period
                .header("Pragma", "no-cache")
                .entity(csv)
                .build();
    }
}
