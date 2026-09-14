package dk.dbc.promat.service.db;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBException;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Startup
@Singleton
public class DatabaseMigrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMigrator.class);

    @Resource(lookup = "jdbc/promat")
    DataSource dataSource;

    // Only ever set for ephemeral feature-branch/local databases - seeds a fixed set of
    // sample data via a repeatable migration. Never enabled for a real staging/prod database.
    @Inject
    @ConfigProperty(name = "PROMAT_SEED_DATABASE", defaultValue = "false")
    boolean seedDatabase;

    public DatabaseMigrator() {}

    public DatabaseMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void migrate() {
        if (isDatabaseAccessReadOnly()) {
            LOGGER.info("database access is read-only, no migration attempted");
            return;
        }
        List<String> locations = new ArrayList<>();
        locations.add("classpath:dk/dbc/promat/service/db/migration");
        if (seedDatabase) {
            LOGGER.info("PROMAT_SEED_DATABASE is true - including seed data migration");
            locations.add("classpath:dk/dbc/promat/service/db/seed");
        }
        final var flyway = Flyway.configure()
                .table("schema_version")
                .dataSource(dataSource)
                .locations(locations.toArray(new String[0]))
                .baselineOnMigrate(true)
                .load();
        for (MigrationInfo info : flyway.info().all()) {
            LOGGER.info("database migration {} : {} from file '{}'",
                    info.getVersion(), info.getDescription(), info.getScript());
        }
        flyway.migrate();
    }
    
    private boolean isDatabaseAccessReadOnly() {
        try (var connection = dataSource.getConnection()) {
            return connection.isReadOnly();
        } catch (SQLException e) {
            throw new EJBException("Unable to acquire read-only property", e);
        }
    }
}
