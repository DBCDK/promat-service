package dk.dbc.promat.service.api;

import dk.dbc.promat.service.persistence.JsonMapperProvider;
import dk.dbc.promat.service.persistence.SubjectNote;
import org.glassfish.jersey.jackson.JacksonFeature;

import jakarta.annotation.security.DeclareRoles;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.microprofileext.openapi.swaggerui.OpenApiUiService;

import java.util.Set;

// javax.ws.rs.core.Application (here: its jakarta.* successor) is JAX-RS's own registration
// mechanism, separate from and unrelated to CDI/EJB dependency injection. Only classes
// returned from getClasses() (or getSingletons(), not used in this project) are wired into
// JAX-RS's HTTP routing table - a class can have every @GET/@Path annotation correctly in
// place and still be completely unreachable over HTTP if it isn't listed here. This is a
// DELIBERATE choice this project makes (JAX-RS also supports automatic classpath scanning for
// @Path-annotated classes as an alternative, not used here) - the tradeoff is an explicit,
// single-file manifest of the whole REST API surface, at the cost of every new resource class
// needing to be added here by hand (TaxonomyService.class and BuggiOptionsService.class,
// added as part of this project's Kafka/database taxonomy work, are both new entries).
//
// This is UNRELATED to whether a class is a working CDI/EJB bean - @Singleton/@Stateless
// beans (like the batch/ package's scheduled jobs) are discovered and managed automatically
// by the container with no equivalent registration list; this file specifically governs "is
// this class also reachable as an HTTP endpoint", nothing more.
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
