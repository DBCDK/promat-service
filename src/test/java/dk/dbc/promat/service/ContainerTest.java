/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.promat.service.rest.JsonMapperProvider;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import java.time.Duration;
import java.util.Map;


public abstract class ContainerTest extends IntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerTest.class);
    protected static final ObjectMapper mapper = new JsonMapperProvider().getObjectMapper();

    static {
        Testcontainers.exposeHostPorts(pg.getPort());
    }

    protected static final GenericContainer promatServiceContainer;
    protected static final String promatServiceBaseUrl;

    static {
        promatServiceContainer = new GenericContainer("docker-io.dbc.dk/promat-service:devel")
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withEnv("JAVA_MAX_HEAP_SIZE", "2G")
                .withEnv("LOG_FORMAT", "text")
                .withEnv("PROMAT_DB_URL", String.format("postgres:@host.testcontainers.internal:%s/postgres",
                        pg.getPort()))
                .withEnv("OPENSEARCH_SERVICE_URL", "")
                .withEnv("WORK_PRESENTATION_SERVICE_URL", "")
                .withEnv("PROMAT_CLUSTER_NAME", "")
                .withEnv("MAIL_HOST", "mailhost")
                .withEnv("MAIL_USER", "mail.user")
                .withEnv("MAIL_FROM", "some@address.dk")
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp("/openapi"))
                .withStartupTimeout(Duration.ofMinutes(2));
        promatServiceContainer.start();
        promatServiceBaseUrl = "http://" + promatServiceContainer.getContainerIpAddress() +
                ":" + promatServiceContainer.getMappedPort(8080);
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

    public Response getResponse(String path, Map<String, Object> queryParameters) {
        HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements(path);

        httpGet.getQueryParameters().putAll(queryParameters);

        return httpGet.execute();
    }
}
