/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Set;

@ApplicationPath("/v1/api")
public class PromatApplication extends Application {
    private static final Set<Class<?>> classes = Set.of(
            Cases.class, Editors.class, JacksonFeature.class, Records.class, Reviewers.class, Subjects.class,
            Users.class, Tasks.class, PersistenceExceptionMapper.class, Messages.class, Payments.class);

    private static final Set<Object> singletons = Set.of(new JsonMapperProvider(), new LocalDateConverterProvider());

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
