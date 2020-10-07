/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.rest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Set;

@ApplicationPath("/")
public class PromatApplication extends Application {
    private static final Set<Class<?>> classes = Set.of();

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }
}
