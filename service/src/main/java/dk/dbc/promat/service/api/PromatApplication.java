package dk.dbc.promat.service.api;

import dk.dbc.promat.service.persistence.JsonMapperProvider;
import dk.dbc.promat.service.persistence.SubjectNote;
import org.glassfish.jersey.jackson.JacksonFeature;

import jakarta.annotation.security.DeclareRoles;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.microprofileext.openapi.swaggerui.OpenApiUiService;

import java.util.Set;

// GOTCHA: no JAX-RS classpath auto-scanning - a resource class is unreachable over HTTP
// unless listed in getClasses() below, regardless of its @GET/@Path annotations.
@ApplicationPath("v1/api")
@DeclareRoles("authenticated-user")
public class PromatApplication extends Application {
    private static final Set<Class<?>> classes = Set.of(
            Cases.class, Editors.class, JacksonFeature.class, Records.class, Reviewers.class, Subjects.class,
            Users.class, Tasks.class, PersistenceExceptionMapper.class, Messages.class, Payments.class, SubjectNote.class,
            LocalDateConverterProvider.class, JsonMapperProvider.class, Batch.class, OpenApiUiService.class, TaxonomyService.class,
            BuggiOptionsService.class);

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }
}
