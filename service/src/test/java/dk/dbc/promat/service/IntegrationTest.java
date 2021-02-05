/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.promat.service.db.DatabaseMigrator;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;

public class IntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerTest.class);
    static final EmbeddedPostgres pg = pgStart();
    protected static final HttpClient httpClient;
    protected static EntityManager entityManager;
    private static boolean setupDone;
    protected static String genericWorkPresentationResult;

    static {
        httpClient = HttpClient.create(HttpClient.newClient());
        LOGGER.info("Postres url is:{}", String.format("postgres:@host.testcontainers.internal:%s/postgres",
                pg.getPort()));
    }

    private static EmbeddedPostgres pgStart() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static PGSimpleDataSource getDataSource() {
        final PGSimpleDataSource datasource = new PGSimpleDataSource();
        datasource.setURL( pg.getJdbcUrl("postgres", "postgres"));
        datasource.setUser("postgres");
        datasource.setPassword("");
        return datasource;
    }

    protected static Connection connectToPromatDB() {
        try {
            Class.forName("org.postgresql.Driver");
            final String dbUrl = String.format("jdbc:postgresql://localhost:%s/postgres", pg.getPort());
            final Connection connection = DriverManager.getConnection(dbUrl, "postgres", "");
            connection.setAutoCommit(true);
            return connection;
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void executeScript(Connection connection, URL script) throws IOException, SQLException, URISyntaxException {
        JDBCUtil.executeScript(connection, new File(script.toURI()), StandardCharsets.UTF_8.name());
    }

    public String get(String uri) {
        final Response response = new HttpGet(httpClient)
                .withBaseUrl(uri)
                .execute();
        return response.readEntity(String.class);
    }

    @BeforeAll
    public static void setUp() throws SQLException, IOException, URISyntaxException {
        if (!setupDone) {
            LOGGER.info("Doing various setup stuff");
            LOGGER.info("..Populating database for test");
            DataSource dataSource = getDataSource();
            migrate(dataSource);
            Connection connection = connectToPromatDB();
            executeScript(connection, IntegrationTest.class.getResource("/dk/dbc/promat/service/db/subjectsdump.sql"));
            executeScript(connection, IntegrationTest.class.getResource("/dk/dbc/promat/service/db/promatusers.sql"));
            executeScript(connection, IntegrationTest.class.getResource("/dk/dbc/promat/service/db/promatcases.sql"));
            executeScript(connection, IntegrationTest.class.getResource("/dk/dbc/promat/service/db/notification.sql"));
            executeScript(connection, IntegrationTest.class.getResource("/dk/dbc/promat/service/db/payments.sql"));
            entityManager = createEntityManager(getDataSource(),
                    "promatITPU");
            setupDone = true;
            LOGGER.info("..Populating database tables done");
            LOGGER.info("..Getting data for generic work presentation response");
            genericWorkPresentationResult = Files.readString(
                    Path.of(IntegrationTest.class.getResource("/__files/body-api-work-presentation-generic.json")
                            .getPath()));
            LOGGER.info("..Done");
            LOGGER.info("Setup done!");
        } else {
            LOGGER.info("No setup stuff to do. Already done.");
        }
    }

    private static EntityManager createEntityManager(
            PGSimpleDataSource dataSource, String persistenceUnitName) {
        Map<String, String> entityManagerProperties = new HashMap<>();
        entityManagerProperties.put(JDBC_USER, dataSource.getUser());
        entityManagerProperties.put(JDBC_PASSWORD, dataSource.getPassword());
        entityManagerProperties.put(JDBC_URL, dataSource.getUrl());
        entityManagerProperties.put(JDBC_DRIVER, "org.postgresql.Driver");
        entityManagerProperties.put("eclipselink.logging.level", "FINE");
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(persistenceUnitName,
                entityManagerProperties);
        return factory.createEntityManager(entityManagerProperties);
    }

    public static void migrate(DataSource dataSource) {
        DatabaseMigrator databaseMigrator = new DatabaseMigrator(dataSource);
        databaseMigrator.migrate();
    }
}
