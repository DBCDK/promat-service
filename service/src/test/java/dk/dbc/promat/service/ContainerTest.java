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
import org.junit.jupiter.api.Assertions;
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
    protected static WireMockServer wireMockServer = makeWireMockServer();
    protected static final GenericContainer<?> promatServiceContainer = makePromatServiceContainer();
    protected static final String promatServiceBaseUrl = "http://" + promatServiceContainer.getContainerIpAddress() +
            ":" + promatServiceContainer.getMappedPort(8080);
    protected static final PromatServiceConnector promatServiceConnector  = PromatServiceConnectorFactory.create(promatServiceBaseUrl + "/v1/api");

    public <T> T get(String path, Class<T> tClass) {
        try (Response response = getResponse(path)) {
            return response.readEntity(tClass);
        }
    }

    public Response getResponse(String path) {
        return new HttpGet(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements(path)
                .execute();
    }

    public Response getResponse(String path, String authToken) {
        return new HttpGet(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements(path)
                .withHeader("Authorization", "Bearer " + authToken)
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
        return postResponse(path, body, null);
    }


    @SuppressWarnings("UnusedReturnValue")
    public <T> String postAndAssert(String path, T body, Response.Status expectedStatus) {
        return postAndAssert(path, body, String.class, expectedStatus);
    }

    public <T, R> R postAndAssert(String path, T body, Class<R> responseClass, Response.Status expectedStatus) {
        try (Response response = postResponse(path, body)) {
            Assertions.assertEquals(expectedStatus, response.getStatusInfo().toEnum(), "Response to call " + path
                    + " was expected to be: " + expectedStatus);
            return response.readEntity(responseClass);
        }
    }

    public <T> Response postResponse(String path, T body, String authToken) {
        HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements(path)
                .withData(body, "application/json");
        if (authToken != null) {
            httpPost.getHeaders().put("Authorization", "Bearer " + authToken);
        }
        return httpClient.execute(httpPost);
    }

    public <T> Response putResponse(String path, T body, Map<String, Object> queryParameter, String authToken) {
        HttpPut httpPut = new HttpPut(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements(path)
                .withData(body, "application/json");
        if (queryParameter != null) {
            httpPut.getQueryParameters().putAll(queryParameter);
        }
        if (authToken != null) {
            httpPut.getHeaders().put("Authorization", "Bearer " + authToken);
        }
        return httpClient.execute(httpPut);
    }

    public <T> Response putResponse(String path, T body, String authToken) {
        return putResponse(path, body, null, authToken);
    }

    public <T> Response putResponse(String path, T body) {
        return putResponse(path, body, null, null);
    }

    @SuppressWarnings("unused")
    public <T> Response deleteResponse(String path) {
        HttpDelete httpDelete = new HttpDelete(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements(path);
        return httpClient.execute(httpDelete);
    }

    public static String getWiremockUrl(String path) {
        return "http://host.testcontainers.internal:" + wireMockServer.port() + path;
    }

    private static WireMockServer makeWireMockServer() {
        WireMockServer wireMockServer = new WireMockServer(options().dynamicPort());

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

        // Add two catch-all responses for AdgangsPlatformen's introspect endpoint.
        // ...
        // It is not possible to record the request/response using an external wiremock
        // unless ssl certificate validation is disabled for the service application.
        // Instead use regular http for the introspection endpoint and mock the response here
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("1-2-3-4-5")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{\"active\":true}")));
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("6-7-8-9-0")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{\"active\":false}")));

        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
        Testcontainers.exposeHostPorts(wireMockServer.port());
        LOGGER.info("Wiremock server at port:{}", wireMockServer.port());
        return wireMockServer;
    }

    private static GenericContainer<?> makePromatServiceContainer() {
        String javaHome = System.getProperty("java.home");
        @SuppressWarnings("resource")
        GenericContainer<?> container = new GenericContainer<>("docker-metascrum.artifacts.dbccloud.dk/promat-service:devel")
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withEnv("JAVA_MAX_HEAP_SIZE", "2G")
                .withEnv("LOG_FORMAT", "text")
                .withEnv("PROMAT_DB_URL", String.format("postgres:@host.testcontainers.internal:%s/postgres",
                        promatDBContainer.getHostPort()))
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
                .withEnv("OPENFORMAT_SERVICE_URL", "http://host.testcontainers.internal:" + wireMockServer.port() + "/")
                .withEnv("LU_MAILADDRESS", "TEST@dbc.dk")
                .withEnv("OPENNUMBERROLL_SERVICE_URL", "http://host.testcontainers.internal:" + wireMockServer.port() + "/")
                .withEnv("EMATERIAL_CONTENT_REPO", "http://host.testcontainers.internal:" + wireMockServer.port() +
                        "?faust=%s")
                .withEnv("OPENNUMBERROLL_NUMBERROLLNAME", "faust")
                .withEnv("ENABLE_REMINDERS", String.valueOf(true))
                .withEnv("CC_MAILADDRESS", "cc_test@dbc.dk")
                .withEnv("OAUTH2_CLIENT_ID", "123456789")
                .withEnv("OAUTH2_CLIENT_SECRET", "abcdef")
                .withEnv("OAUTH2_INTROSPECTION_URL", "http://host.testcontainers.internal:" + wireMockServer.port())
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp("/openapi"))
                .withStartupTimeout(Duration.ofMinutes(2));
        container.start();
        return container;
    }
}
