/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.cluster;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ServerRoleFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerRoleFactory.class);
    private ServerRole serverRole;

    @Inject
    @ConfigProperty(name = "HOSTNAME")
    private String hostname;

    @PostConstruct
    public void setServerRole() {
        if (hostname!=null && hostname.endsWith("-0")) {
            serverRole = ServerRole.PRIMARY;
        }
        else {
            serverRole = ServerRole.SECONDARY;
        }
        LOGGER.info("Hostname:{}", hostname);
        LOGGER.info("ServerRole:{}", serverRole);
    }

    @Produces
    public ServerRole getInstance() {
        return serverRole;
    }
}
