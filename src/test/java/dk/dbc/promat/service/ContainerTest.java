/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import dk.dbc.httpclient.HttpDelete;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.httpclient.HttpPut;
import dk.dbc.promat.service.rest.JsonMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import javax.ws.rs.core.Response;
import java.time.Duration;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;


public abstract class ContainerTest extends IntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerTest.class);
    protected static final ObjectMapper mapper = new JsonMapperProvider().getObjectMapper();

    private static WireMockServer wireMockServer;

    static {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
        Testcontainers.exposeHostPorts(wireMockServer.port());
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
                .withEnv("CULR_SERVICE_URL", "http://host.testcontainers.internal:" + wireMockServer.port() + "/1.4/CulrWebService")
                .withEnv("CULR_SERVICE_USER_ID", "connector")
                .withEnv("CULR_SERVICE_PASSWORD", "connector-pass")
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

    public Response getResponse(String path, Map<String, Object> queryParameter) {
        HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements(path);

        httpGet.getQueryParameters().putAll(queryParameter);

        return httpGet.execute();
    }

    public <T> Response postResponse(String path, T body) {
        HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements(path)
                .withData(body, "application/json");
        return httpClient.execute(httpPost);
    }

    public <T> Response putResponse(String path, T body) {
        HttpPut httpPut = new HttpPut(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements(path)
                .withData(body, "application/json");
        return httpClient.execute(httpPut);
    }

    public Response deleteResponse(String path) {
        return new HttpDelete(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements(path)
                .execute();
    }

}
