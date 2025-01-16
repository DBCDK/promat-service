package dk.dbc.promat.service.api;

import dk.dbc.promat.service.batch.ScheduledNotificationSender;
import dk.dbc.promat.service.batch.UserEditConfig;
import dk.dbc.promat.service.batch.UserUpdater;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
@Path("")
public class Batch {
    private static final Logger LOGGER = LoggerFactory.getLogger(Batch.class);
    /**
     * Endpoints primarily used in integration testing, but also
     * useful for forcing a build of the reviewer data changed notifications.
     */

    @EJB
    UserUpdater userUpdater;

    @POST
    @Path("batch/job/userupdater/")
    public Response triggerUserUpdater() {
        userUpdater.processUserDataChanges();
        return Response.ok().build();
    }

    @POST
    @Path("batch/job/userupdater/config/{userEditTimeOut}")
    public Response triggerUserUpdater(@PathParam("userEditTimeOut") int timeOut) {
        LOGGER.info("Triggering user updater for userEditTimeOut {}", timeOut);
        UserEditConfig.setUserEditTimeOut(timeOut);
        return Response.ok().build();
    }


}
