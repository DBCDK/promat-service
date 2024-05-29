package dk.dbc.promat.service.api;

import dk.dbc.promat.service.persistence.SubjectNote;
import org.glassfish.jersey.jackson.JacksonFeature;

import jakarta.annotation.security.DeclareRoles;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.Set;

@ApplicationPath("/v1/api")
@DeclareRoles("authenticated-user")
public class PromatApplication extends Application {
    private static final Set<Class<?>> classes = Set.of(
            Cases.class, Editors.class, JacksonFeature.class, Records.class, Reviewers.class, Subjects.class,
            Users.class, Tasks.class, PersistenceExceptionMapper.class, Messages.class, Payments.class, SubjectNote.class,
            LocalDateConverterProvider.class, JsonMapperProvider.class);

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }
}
