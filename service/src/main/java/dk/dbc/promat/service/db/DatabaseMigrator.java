package dk.dbc.promat.service.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import dk.dbc.promat.service.batch.RuntimeGuards;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBException;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import javax.sql.DataSource;
import java.sql.SQLException;

@Startup
@Singleton
public class DatabaseMigrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMigrator.class);

    @Resource(lookup = "jdbc/promat")
    DataSource dataSource;

    public DatabaseMigrator() {}

    public DatabaseMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // @PostConstruct means: run this once, automatically, right after the
    // container finishes constructing/injecting this singleton bean - i.e.
    // once per application startup. That's what makes this "run migrations
    // on boot" rather than something anyone has to remember to call.
    @PostConstruct
    public void migrate() {
        // Local/dev escape hatch: if PROMAT_SKIP_MIGRATIONS=true, don't touch
        // the schema at all. Useful when pointing this service at a shared
        // staging DB you don't want Flyway silently changing.
        if (RuntimeGuards.skipMigrations()) {
            LOGGER.warn("database migrations disabled via {}", RuntimeGuards.SKIP_MIGRATIONS_ENV);
            return;
        }
        if (isDatabaseAccessReadOnly()) {
            LOGGER.info("database access is read-only, no migration attempted");
            return;
        }
        final var flyway = Flyway.configure()
                .table("schema_version")
                .dataSource(dataSource)
                .locations("classpath:dk/dbc/promat/service/db/migration")
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
