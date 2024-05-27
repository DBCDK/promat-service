package dk.dbc.promat.service;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.promat.service.db.DatabaseMigrator;
import dk.dbc.promat.service.persistence.Notification;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;
import javax.sql.DataSource;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;

@SuppressWarnings("SameParameterValue")
public class IntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTest.class);
    static final DBCPostgreSQLContainer promatDBContainer = makeDBContainer();

    protected static final HttpClient httpClient;
    protected static EntityManager entityManager;
    private static boolean setupDone;

    static {
        httpClient = HttpClient.create(HttpClient.newClient());
        LOGGER.info("Postres url is:{}", String.format("postgres:@host.testcontainers.internal:%s/postgres",
                promatDBContainer.getHostPort()));
    }

    protected static void executeScript(Connection connection, URL script) throws IOException, SQLException, URISyntaxException {
        JDBCUtil.executeScript(connection, new File(script.toURI()), StandardCharsets.UTF_8.name());
    }

    public String get(String uri) {
        try(Response response = new HttpGet(httpClient).withBaseUrl(uri).execute()) {
            return response.readEntity(String.class);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @BeforeAll
    public static void setUp() throws SQLException, IOException, URISyntaxException {
        if (!setupDone) {
            LOGGER.info("Doing various setup stuff");
            LOGGER.info("..Populating database for test");
            migrate(promatDBContainer.datasource());
            Connection connection = promatDBContainer.createConnection();
            executeScript(connection, IntegrationTest.class.getResource("/dk/dbc/promat/service/db/subjectsdump.sql"));
            executeScript(connection, IntegrationTest.class.getResource("/dk/dbc/promat/service/db/promatusers.sql"));
            executeScript(connection, IntegrationTest.class.getResource("/dk/dbc/promat/service/db/promatcases.sql"));
            executeScript(connection, IntegrationTest.class.getResource("/dk/dbc/promat/service/db/notification.sql"));
            executeScript(connection, IntegrationTest.class.getResource("/dk/dbc/promat/service/db/payments.sql"));
            entityManager = createEntityManager(promatDBContainer, "promatITPU");
            LOGGER.info("..Populating database tables done");
            LOGGER.info("Setup done!");
            setupDone = true;
        } else {
            LOGGER.info("No setup stuff to do. Already done.");
        }
    }

    private static EntityManager createEntityManager(DBCPostgreSQLContainer dbContainer, String persistenceUnitName) {
        Map<String, String> entityManagerProperties = Map.of(
                JDBC_USER, dbContainer.getUsername(),
                JDBC_PASSWORD, dbContainer.getPassword(),
                JDBC_URL, dbContainer.getJdbcUrl(),
                JDBC_DRIVER, "org.postgresql.Driver",
                "eclipselink.logging.level", "FINE");
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(persistenceUnitName, entityManagerProperties);
        return factory.createEntityManager(entityManagerProperties);
    }

    public static void migrate(DataSource dataSource) {
        DatabaseMigrator databaseMigrator = new DatabaseMigrator(dataSource);
        databaseMigrator.migrate();
    }

    public List<Notification> getNotifications(String subjectWildcard, String bodyTextWildcard) {
        TypedQuery<Notification> query = entityManager
                .createQuery("SELECT notification " +
                        "FROM Notification notification ORDER BY notification.id", Notification.class);
        List<Notification> allNotifications = query.getResultList();
        List<Notification> notifications;
        if (subjectWildcard != null) {
            notifications = allNotifications.stream().filter(notification ->
                    notification.getSubject().contains(subjectWildcard)).collect(Collectors.toList());
        } else {
            notifications = new ArrayList<>(allNotifications);
        }
        if (bodyTextWildcard != null) {
            notifications = notifications.stream().filter(notification ->
                    notification.getBodyText().contains(bodyTextWildcard)).collect(Collectors.toList());
        }
        return notifications;
    }

    public List<Notification> getNotifications() {
        return getNotifications(null, null);
    }

    private static DBCPostgreSQLContainer makeDBContainer() {
        DBCPostgreSQLContainer container = new DBCPostgreSQLContainer().withReuse(false);
        container.start();
        container.exposeHostPort();
        LOGGER.info("Postgres url is:{}", container.getDockerJdbcUrl());
        return container;
    }

}
