package dk.dbc.promat.service.api;

import dk.dbc.promat.service.persistence.JsonMapperProvider;
import dk.dbc.promat.service.persistence.SubjectNote;
import org.glassfish.jersey.jackson.JacksonFeature;

import jakarta.annotation.security.DeclareRoles;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.microprofileext.openapi.swaggerui.OpenApiUiService;

import java.util.Set;

// GOTCHA: this project doesn't use JAX-RS's classpath auto-scanning - a resource class with
// correct @GET/@Path annotations is still unreachable over HTTP unless it's listed in
// getClasses() below. Unrelated to CDI/EJB registration (e.g. the batch/ package's scheduled
// jobs need no such list) - this only governs HTTP routing. TaxonomyService and
// BuggiOptionsService were added here as part of the taxonomy/Kafka work.
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
