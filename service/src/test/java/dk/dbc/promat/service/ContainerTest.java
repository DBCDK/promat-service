/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import dk.dbc.httpclient.HttpDelete;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.httpclient.HttpPut;
import dk.dbc.promat.service.api.JsonMapperProvider;
import dk.dbc.promat.service.connector.PromatServiceConnector;
import dk.dbc.promat.service.connector.PromatServiceConnectorFactory;
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
import static com.github.tomakehurst.wiremock.client.WireMock.requestMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;


public abstract class ContainerTest extends IntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerTest.class);
    protected static final ObjectMapper mapper = new JsonMapperProvider().getObjectMapper();

    private static WireMockServer wireMockServer;

    static {
        wireMockServer = new WireMockServer(options().dynamicPort());

        // Add a "catch-all" for openformat requests.
        // All openformat requests will return the same json.
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.queryParameter("action").isPresent() &&
                                request.queryParameter("action").containsValue("formatObject") &&
                                request.queryParameter("outputFormat").isPresent() &&
                                request.queryParameter("outputFormat").containsValue("promat") &&
                                request.queryParameter("pid").isPresent() &&
                                request.queryParameter("pid").firstValue().contains("870970-basis:")
                ))

                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(genericOpenFormatResult)));
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
        Testcontainers.exposeHostPorts(wireMockServer.port());
        Testcontainers.exposeHostPorts(pg.getPort());
        LOGGER.info("Wiremock server at port:{}", wireMockServer.port());
    }

    protected static final GenericContainer promatServiceContainer;
    protected static final String promatServiceBaseUrl;
    protected static final PromatServiceConnector promatServiceConnector;

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
                .withEnv("PROMAT_AGENCY_ID", "190976")
                .withEnv("OPENSEARCH_SERVICE_URL", "http://host.testcontainers.internal:" + wireMockServer.port() + "/b3.5_5.2/")
                .withEnv("OPENSEARCH_PROFILE", "dbckat")
                .withEnv("OPENSEARCH_AGENCY", "010100")
                .withEnv("OPENSEARCH_REPOSITORY", "rawrepo_basis")
                .withEnv("WORK_PRESENTATION_SERVICE_URL", "http://host.testcontainers.internal:" + wireMockServer.port() + "/api/")
                .withEnv("PROMAT_CLUSTER_NAME", "")
                .withEnv("MAIL_HOST", "mailhost")
                .withEnv("MAIL_USER", "mail.user")
                .withEnv("MAIL_FROM", "some@address.dk")
                .withEnv("OPENFORMAT_SERVICE_URL", "http://host.testcontainers.internal:" + wireMockServer.port()+"/")
                .withEnv("LU_MAILADDRESS", "TEST@dbc.dk")
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp("/openapi"))
                .withStartupTimeout(Duration.ofMinutes(2));
        promatServiceContainer.start();
        promatServiceBaseUrl = "http://" + promatServiceContainer.getContainerIpAddress() +
                ":" + promatServiceContainer.getMappedPort(8080);
        promatServiceConnector = PromatServiceConnectorFactory.create(promatServiceBaseUrl + "/v1/api");
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

    public <T> Response deleteResponse(String path) {
        HttpDelete httpDelete = new HttpDelete(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements(path);
        return httpClient.execute(httpDelete);
    }

    public static String getWiremockUrl(String path) {
        return "http://host.testcontainers.internal:" + wireMockServer.port() + path;
    }
}