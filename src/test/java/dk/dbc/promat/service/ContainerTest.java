/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.promat.service.rest.JsonMapperProvider;
import dk.dbc.promat.service.rest.SubjectsIT;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

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
import java.time.Duration;

public abstract class ContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerTest.class);
    private static boolean setupDone;
    protected static final ObjectMapper mapper = new JsonMapperProvider().getObjectMapper();

    static final EmbeddedPostgres pg = pgStart();

    static {
        Testcontainers.exposeHostPorts(pg.getPort());
    }

    protected static final GenericContainer promatServiceContainer;
    protected static final String promatServiceBaseUrl;
    protected static final HttpClient httpClient;

    static {
        promatServiceContainer = new GenericContainer("docker-io.dbc.dk/promat-service:devel")
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withEnv("JAVA_MAX_HEAP_SIZE", "2G")
                .withEnv("LOG_FORMAT", "text")
                .withEnv("PROMAT_DB_URL", String.format("postgres:@host.testcontainers.internal:%s/postgres",
                        pg.getPort()))
                .withEnv("OPENSEARCH_SERVICE_URL", "")
                .withEnv("WORK_PRESENTATION_SERVICE_URL", "")
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp("/openapi"))
                .withStartupTimeout(Duration.ofMinutes(2));
        promatServiceContainer.start();
        promatServiceBaseUrl = "http://" + promatServiceContainer.getContainerIpAddress() +
                ":" + promatServiceContainer.getMappedPort(8080);
        httpClient = HttpClient.create(HttpClient.newClient(
                new ClientConfig().register(new JacksonFeature())));
    }

    @BeforeAll
    public static void setUp() throws SQLException, IOException, URISyntaxException {
        if (!setupDone) {
            LOGGER.info("Populating database for test");
            Connection connection = connectToPromatDB();
            executeScript(connection, SubjectsIT.class.getResource("/dk/dbc/promat/service/db/subjects/subjectsdump.sql"));
            executeScript(connection, SubjectsIT.class.getResource("/dk/dbc/promat/service/db/subjects/promatusers.sql"));
            setupDone = true;
        } else {
            LOGGER.info("Database populate already done.");
        }
    }

    private static EmbeddedPostgres pgStart() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

    public <T> T get(String path, Class<T> tClass) {
        return new HttpGet(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements(path)
                .execute()
                .readEntity(tClass);
    }

    public Response getResponse(String path) {
        return new HttpGet(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements(path)
                .execute();
    }
}
