package dk.dbc.promat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import dk.dbc.httpclient.HttpDelete;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.httpclient.HttpPut;
import dk.dbc.promat.service.api.Users;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.persistence.JsonMapperProvider;
import dk.dbc.promat.service.connector.PromatServiceConnector;
import dk.dbc.promat.service.connector.PromatServiceConnectorFactory;
import jakarta.json.bind.Jsonb;
import org.apache.hc.client5.http.impl.Wire;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import jakarta.ws.rs.core.Response;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.requestMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static dk.dbc.promat.service.api.Users.IDP_EDITOR_RIGHT_NAME;
import static dk.dbc.promat.service.api.Users.IDP_PRODUCT_NAME;
import static dk.dbc.promat.service.api.Users.IDP_REVIEWER_RIGHT_NAME;

public abstract class ContainerTest extends IntegrationTestIT {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ContainerTest.class);
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
        return postAndAssert(path, body, null, String.class, expectedStatus);
    }

    public <T, R> R postAndAssert(String path, T body, Class<R> responseClass, Response.Status expectedStatus) {
        return postAndAssert(path, body, null, responseClass, expectedStatus);
    }

    public <T, R> R postAndAssert(String path, T requestBody, String authToken, Class<R> responseClass, Response.Status expectedStatus) {
        try (Response response = postResponse(path, requestBody, authToken)) {

            Response.StatusType status = response.getStatusInfo().toEnum();
            String responseBody = response.readEntity(String.class);

            Assertions.assertEquals(expectedStatus, status, "Response to call " + path
                    + " was expected to be: " + expectedStatus + " but was: " + status
                    + "\nResponse body was: " + responseBody);

            if (responseBody.getClass() == responseClass) {
                // Typical use is: responseClass == String.class, then prevent "uncheck cast from type xxx" warnings by use of convertValue
                return mapper.convertValue(responseBody, responseClass);
            } else {
                return mapper.readValue(responseBody, responseClass);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
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

    public <T> Response postResponse(String path, T body, Map<String, Object> queryParameter, String authToken) {
        HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(promatServiceBaseUrl)
                .withPathElements(path)
                .withData(body, "application/json");
        if (queryParameter != null) {
            httpPost.getQueryParameters().putAll(queryParameter);
        }
        if (authToken != null) {
            httpPost.getHeaders().put("Authorization", "Bearer " + authToken);
        }
        return httpClient.execute(httpPost);
    }

    public <T> Response putResponse(String path, T body, Map<String, Object> queryParameter, String authToken) {
        HttpPut httpPut = new HttpPut(httpClient).withBaseUrl(promatServiceBaseUrl).withPathElements(path);
        if (body != null) {
            httpPut.withData(body, "application/json");
        }
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

    private static boolean isContainerDebugging() {
        return !getDebuggingHost().isEmpty();
    }

    private static String getDebuggingHost() {
        String debuggingHost = getSysVar("REMOTE_DEBUGGING_HOST", System::getenv, System::getProperty);
        return debuggingHost == null ? "" : debuggingHost;
    }

    @SafeVarargs
    @SuppressWarnings("SameParameterValue")
    private static String getSysVar(String name, Function<String, String>... functions) {
        for (Function<String, String> function : functions) {
            String value = function.apply(name);
            if(value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static WireMockServer makeWireMockServer() {
        WireMockServer wireMockServer = new WireMockServer(options().dynamicPort());

        mockAuthenticationResponses(wireMockServer);

        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
        Testcontainers.exposeHostPorts(wireMockServer.port());
        LOGGER.info("Wiremock server at port:{}", wireMockServer.port());
        return wireMockServer;
    }

    private static GenericContainer<?> makePromatServiceContainer() {
        @SuppressWarnings("resource")
        GenericContainer<?> container = new GenericContainer<>("docker-metascrum.artifacts.dbccloud.dk/promat-service:devel")
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withEnv("TZ", "Europe/Copenhagen")
                .withEnv("JAVA_MAX_HEAP_SIZE", "2G")
                .withEnv("LOG_FORMAT", "text")
                .withEnv("PROMAT_DB_URL", promatDBContainer.getPayaraDockerJdbcUrl())
                .withEnv("CULR_SERVICE_URL", "http://host.testcontainers.internal:" + wireMockServer.port() + "/1.4/CulrWebService")
                .withEnv("CULR_SERVICE_USER_ID", "connector")
                .withEnv("CULR_SERVICE_PASSWORD", "connector-pass")
                .withEnv("PROMAT_AGENCY_ID", "190976")
                .withEnv("OPENSEARCH_SERVICE_URL", "http://host.testcontainers.internal:" + wireMockServer.port() + "/b3.5_5.2/")
                .withEnv("OPENSEARCH_PROFILE", "dbckat")
                .withEnv("OPENSEARCH_AGENCY", "010100")
                .withEnv("OPENSEARCH_REPOSITORY", "rawrepo_basis")
                .withEnv("PROMAT_CLUSTER_NAME", "")
                .withEnv("MAIL_HOST", "mailhost")
                .withEnv("MAIL_USER", "mail.user")
                .withEnv("MAIL_FROM", "some@address.dk")
                .withEnv("OPENFORMAT_SERVICE_URL", getOpenFormatBaseUrl("http://host.testcontainers.internal:" + wireMockServer.port() + "/api/v2"))
                .withEnv("LU_MAILADDRESS", "lumailaddress-test@dbc.dk")
                .withEnv("OPENNUMBERROLL_SERVICE_URL", "http://host.testcontainers.internal:" + wireMockServer.port() + "/1.1")
                .withEnv("EMATERIAL_CONTENT_REPO", "http://host.testcontainers.internal:" + wireMockServer.port() +
                        "?faust=%s")
                .withEnv("OPENNUMBERROLL_NUMBERROLLNAME", "faust")
                .withEnv("ENABLE_REMINDERS", String.valueOf(true))
                .withEnv("CC_MAILADDRESS", "ccmailaddress-test@dbc.dk")
                .withEnv("OAUTH2_CLIENT_ID", "123456789")
                .withEnv("OAUTH2_CLIENT_SECRET", "abcdef")
                .withEnv("OAUTH2_INTROSPECTION_URL", "http://host.testcontainers.internal:" + wireMockServer.port() + "/oauth/introspection")
                .withEnv("OAUTH2_USERINFO_URL", "http://host.testcontainers.internal:" + wireMockServer.port() + "/userinfo")
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp("/health"))
                .withStartupTimeout(Duration.ofMinutes(2));
        if(isContainerDebugging()) {
            container.withEnv("REMOTE_DEBUGGING_HOST", getDebuggingHost())
                    .withCopyFileToContainer(MountableFile.forClasspathResource("start-payara.sh", Integer.valueOf("777", 8)), "/opt/payara5/scripts/start-payara.sh");
        }
        container.start();
        return container;
    }

    /**
     * Helper method to set wiremock host for open-format, since we use open-format
     * quite a few places, and it is a pain in the ****e to go between a real server,
     * a local wiremock(recorder) and the in-test wiremock host when making changes that
     * involves the openformat connector (or mocks thereof)
     *
     * @param server The servername under normal circumstances
     * @return Either the given open-format baseurl, or if modified, a static address
     */
    public static String getOpenFormatBaseUrl(String server) {

        // Use fixed address for a real open-format broker
        // NEVER COMMIT THIS AS ACTIVE !
        //return "http://open-format-broker.cisterne.svc.cloud.dbc.dk/api/v2";

        // Use local wiremock recorder. Use '--proxy-all http://open-format-broker.cisterne.svc.cloud.dbc.dk'
        // NEVER COMMIT THIS AS ACTIVE !
        //return "http://172.17.33.64:8080/api/v2";

        // Use default server value as given by the various tests.
        return server;
    }


    /**
     * Helper method to mock responses to various authentication requests with
     * 'AdgangsPlatformen'
     *
     * @param wireMockServer The wiremock server instance
     *
     * authtoken         status          usertype      promatId     culr-id          login-type
     * ----------------------------------------------------------------------------------------
     * 1-2-3-4-5         active=true     EDITOR        12           53               bibliotek
     * 2-3-4-5-6         active=true     EDITOR        13           klnp             netpunkt
     * 6-7-8-9-0         active=false
     */
    public static void mockAuthenticationResponses(WireMockServer wireMockServer) {

        // Mock logged-in editor with id=12, using 'biblioteks login' (cpr number)
        mockAuthenticationResponseForEditor12(wireMockServer);

        // Mock logged-in editor with id=13, using 'netpunkt login' (userid+agency)
        mockAuthenticationResponseForEditor13(wireMockServer);

        // Mock logged-in reviewer with id=2, using 'netpunkt login' (userid+agency)
        mockAuthenticationResponseForReviewer2(wireMockServer);

        // Mock logged-in user not found in promat, using 'netpunkt login' (userid+agency)
        mockAuthenticationResponseForUnknownReviewerUserId(wireMockServer);

        // Mock logged-in user not found in promat, using 'netpunkt login' (userid+agency)
        mockAuthenticationResponseForUnknownReviewerAgency(wireMockServer);

        // Mock logged-in reviewer with id=2 having no promat rights, using 'netpunkt login' (userid+agency)
        mockAuthenticationResponseForReviewer2WithRevokedRights(wireMockServer);

        // Mock logged-in editor with id=13 with incorrect right as reviewer, using 'netpunkt login' (userid+agency)
        mockAuthenticationResponseForEditor13WithReviewerRights(wireMockServer);

        // Mock logged-in reviewer with id=2 with incorrect right as editor, using 'netpunkt login' (userid+agency)
        mockAuthenticationResponseForReviewer2WithEditorRights(wireMockServer);

        // Mock logged-out user
        mockAuthenticationResponseForLoggedOutUser(wireMockServer);
    }

    public static void mockAuthenticationResponseForEditor12(WireMockServer wireMockServer) {
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/oauth/introspection") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("1-2-3-4-5")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"active\": true," +
                                "  \"clientId\": \"123-456-789-12345\"," +
                                "  \"expires\": \"2029-01-13T12:58:53.967Z\"," +
                                "  \"agency\": \"790900\"," +
                                "  \"uniqueId\": \"53\"," +
                                "  \"search\": {" +
                                "    \"profile\": \"opac\"," +
                                "    \"agency\": \"790900\"" +
                                "  }," +
                                "  \"type\": \"authorized\"," +
                                "  \"name\": \"promat prod\"," +
                                "  \"contact\": {" +
                                "    \"owner\": {" +
                                "      \"name\": \"Henrik Witt Hansen\"," +
                                "      \"email\": \"hwha@dbc.dk\"," +
                                "      \"phone\": \"\"" +
                                "    }" +
                                "  }" +
                                "}")));

        // Editor authorized using 'biblioteks login', so no IDP field
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/userinfo") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("1-2-3-4-5")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"attributes\": {" +
                                "    \"serviceStatus\": {" +
                                "      \"borchk\": \"ok\"," +
                                "      \"culr\": \"ok\"" +
                                "    }," +
                                "    \"uniqueId\": \"456-789-012-12345\"," +
                                "    \"municipality\": \"101\"," +
                                "    \"municipalityAgencyId\": \"790900\"" +
                                "  }" +
                                "}")));
    }

    public static void mockAuthenticationResponseForEditor13(WireMockServer wireMockServer) {
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/oauth/introspection") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("2-3-4-5-6")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"active\": true," +
                                "  \"clientId\": \"123-456-789-23456\"," +
                                "  \"expires\": \"2029-01-13T12:58:53.967Z\"," +
                                "  \"agency\": \"790900\"," +
                                "  \"uniqueId\": null," +
                                "  \"search\": {" +
                                "    \"profile\": \"opac\"," +
                                "    \"agency\": \"790900\"" +
                                "  }," +
                                "  \"type\": \"authorized\"," +
                                "  \"name\": \"Promat prod\"," +
                                "  \"contact\": {" +
                                "    \"owner\": {" +
                                "      \"name\": \"Henrik Witt Hansen\"," +
                                "      \"email\": \"hwha@dbc.dk\"," +
                                "      \"phone\": \"\"" +
                                "    }" +
                                "  }" +
                                "}")));

        // Editor authorized using 'netpunkts login', so we have the IDP field
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/userinfo") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("2-3-4-5-6")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"attributes\": {" +
                                "    \"serviceStatus\": {" +
                                "      \"borchk\": \"ok\"," +
                                "      \"culr\": \"ok\"" +
                                "    }," +
                                "    \"userId\": \"klnp\"," +
                                "    \"municipality\": \"909\"," +
                                "    \"netpunktAgency\": \"790900\"," +
                                "    \"municipalityAgencyId\": \"790900\"," +
                                "    \"dbcidp\": [" +
                                "      {" +
                                "        \"agencyId\": \"790900\"," +
                                "        \"rights\": [" +
                                "          {" +
                                "            \"productName\": \"" + IDP_PRODUCT_NAME + "\"," +
                                "            \"name\": \"" + IDP_EDITOR_RIGHT_NAME + "\"," +
                                "            \"description\": \"adgang til Promat som DBC redaktør\"" +
                                "          }" +
                                "        ]" +
                                "      }" +
                                "    ]" +
                                "  }" +
                                "}")));
    }

    public static void mockAuthenticationResponseForReviewer2(WireMockServer wireMockServer) {
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/oauth/introspection") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("3-4-5-6-7")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"active\": true," +
                                "  \"clientId\": \"123-456-789-34567\"," +
                                "  \"expires\": \"2029-01-13T12:58:53.967Z\"," +
                                "  \"agency\": \"790900\"," +
                                "  \"uniqueId\": null," +
                                "  \"search\": {" +
                                "    \"profile\": \"opac\"," +
                                "    \"agency\": \"790900\"" +
                                "  }," +
                                "  \"type\": \"authorized\"," +
                                "  \"name\": \"Promat prod\"," +
                                "  \"contact\": {" +
                                "    \"owner\": {" +
                                "      \"name\": \"Henrik Witt Hansen\"," +
                                "      \"email\": \"hwha@dbc.dk\"," +
                                "      \"phone\": \"\"" +
                                "    }" +
                                "  }" +
                                "}")));

        // Editor authorized using 'netpunkts login', so we have the IDP field
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/userinfo") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("3-4-5-6-7")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"attributes\": {" +
                                "    \"serviceStatus\": {" +
                                "      \"borchk\": \"ok\"," +
                                "      \"culr\": \"ok\"" +
                                "    }," +
                                "    \"userId\": \"axel52\"," +
                                "    \"municipality\": \"909\"," +
                                "    \"netpunktAgency\": \"820010\"," +
                                "    \"municipalityAgencyId\": \"820020\"," +
                                "    \"dbcidp\": [" +
                                "      {" +
                                "        \"agencyId\": \"820010\"," +
                                "        \"rights\": [" +
                                "          {" +
                                "            \"productName\": \"" + IDP_PRODUCT_NAME + "\"," +
                                "            \"name\": \"" + IDP_REVIEWER_RIGHT_NAME + "\"," +
                                "            \"description\": \"adgang til Promat som ekstern anmelder\"" +
                                "          }" +
                                "        ]" +
                                "      }" +
                                "    ]" +
                                "  }" +
                                "}")));
    }

    public static void mockAuthenticationResponseForUnknownReviewerUserId(WireMockServer wireMockServer) {
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/oauth/introspection") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("4-5-6-7-8")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"active\": true," +
                                "  \"clientId\": \"123-456-789-34567\"," +
                                "  \"expires\": \"2029-01-13T12:58:53.967Z\"," +
                                "  \"agency\": \"790900\"," +
                                "  \"uniqueId\": null," +
                                "  \"search\": {" +
                                "    \"profile\": \"opac\"," +
                                "    \"agency\": \"790900\"" +
                                "  }," +
                                "  \"type\": \"authorized\"," +
                                "  \"name\": \"Promat prod\"," +
                                "  \"contact\": {" +
                                "    \"owner\": {" +
                                "      \"name\": \"Henrik Witt Hansen\"," +
                                "      \"email\": \"hwha@dbc.dk\"," +
                                "      \"phone\": \"\"" +
                                "    }" +
                                "  }" +
                                "}")));

        // Editor authorized using 'netpunkts login', so we have the IDP field
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/userinfo") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("4-5-6-7-8")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"attributes\": {" +
                                "    \"serviceStatus\": {" +
                                "      \"borchk\": \"ok\"," +
                                "      \"culr\": \"ok\"" +
                                "    }," +
                                "    \"userId\": \"axel53\"," +
                                "    \"municipality\": \"909\"," +
                                "    \"netpunktAgency\": \"820010\"," +
                                "    \"municipalityAgencyId\": \"820020\"," +
                                "    \"dbcidp\": [" +
                                "      {" +
                                "        \"agencyId\": \"820010\"," +
                                "        \"rights\": [" +
                                "          {" +
                                "            \"productName\": \"" + IDP_PRODUCT_NAME + "\"," +
                                "            \"name\": \"" + IDP_REVIEWER_RIGHT_NAME + "\"," +
                                "            \"description\": \"adgang til Promat som ekstern anmelder\"" +
                                "          }" +
                                "        ]" +
                                "      }" +
                                "    ]" +
                                "  }" +
                                "}")));
    }

    public static void mockAuthenticationResponseForUnknownReviewerAgency(WireMockServer wireMockServer) {
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/oauth/introspection") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("5-6-7-8-9")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"active\": true," +
                                "  \"clientId\": \"123-456-789-34567\"," +
                                "  \"expires\": \"2029-01-13T12:58:53.967Z\"," +
                                "  \"agency\": \"790900\"," +
                                "  \"uniqueId\": null," +
                                "  \"search\": {" +
                                "    \"profile\": \"opac\"," +
                                "    \"agency\": \"790900\"" +
                                "  }," +
                                "  \"type\": \"authorized\"," +
                                "  \"name\": \"Promat prod\"," +
                                "  \"contact\": {" +
                                "    \"owner\": {" +
                                "      \"name\": \"Henrik Witt Hansen\"," +
                                "      \"email\": \"hwha@dbc.dk\"," +
                                "      \"phone\": \"\"" +
                                "    }" +
                                "  }" +
                                "}")));

        // Editor authorized using 'netpunkts login', so we have the IDP field
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/userinfo") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("5-6-7-8-9")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"attributes\": {" +
                                "    \"serviceStatus\": {" +
                                "      \"borchk\": \"ok\"," +
                                "      \"culr\": \"ok\"" +
                                "    }," +
                                "    \"userId\": \"axel52\"," +
                                "    \"municipality\": \"909\"," +
                                "    \"netpunktAgency\": \"820030\"," +
                                "    \"municipalityAgencyId\": \"820020\"," +
                                "    \"dbcidp\": [" +
                                "      {" +
                                "        \"agencyId\": \"820010\"," +
                                "        \"rights\": [" +
                                "          {" +
                                "            \"productName\": \"" + IDP_PRODUCT_NAME + "\"," +
                                "            \"name\": \"" + IDP_REVIEWER_RIGHT_NAME + "\"," +
                                "            \"description\": \"adgang til Promat som ekstern anmelder\"" +
                                "          }" +
                                "        ]" +
                                "      }" +
                                "    ]" +
                                "  }" +
                                "}")));
    }

    public static void mockAuthenticationResponseForReviewer2WithRevokedRights(WireMockServer wireMockServer) {
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/oauth/introspection") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("5-4-3-2-1")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"active\": true," +
                                "  \"clientId\": \"123-456-789-34567\"," +
                                "  \"expires\": \"2029-01-13T12:58:53.967Z\"," +
                                "  \"agency\": \"790900\"," +
                                "  \"uniqueId\": null," +
                                "  \"search\": {" +
                                "    \"profile\": \"opac\"," +
                                "    \"agency\": \"790900\"" +
                                "  }," +
                                "  \"type\": \"authorized\"," +
                                "  \"name\": \"Promat prod\"," +
                                "  \"contact\": {" +
                                "    \"owner\": {" +
                                "      \"name\": \"Henrik Witt Hansen\"," +
                                "      \"email\": \"hwha@dbc.dk\"," +
                                "      \"phone\": \"\"" +
                                "    }" +
                                "  }" +
                                "}")));

        // Editor authorized using 'netpunkts login', so we have the IDP field
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/userinfo") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("5-4-3-2-1")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"attributes\": {" +
                                "    \"serviceStatus\": {" +
                                "      \"borchk\": \"ok\"," +
                                "      \"culr\": \"ok\"" +
                                "    }," +
                                "    \"userId\": \"axel52\"," +
                                "    \"municipality\": \"909\"," +
                                "    \"netpunktAgency\": \"820010\"," +
                                "    \"municipalityAgencyId\": \"820020\"," +
                                "    \"dbcidp\": [" +
                                "      {" +
                                "        \"agencyId\": \"820010\"," +
                                "        \"rights\": [" +
                                "          {" +
                                "            \"productName\": \"DMAT\"," +
                                "            \"name\": \"READ\"," +
                                "            \"description\": \"adgang til dmat\"" +
                                //           No Promat rights
                                "          }" +
                                "        ]" +
                                "      }" +
                                "    ]" +
                                "  }" +
                                "}")));
    }

    public static void mockAuthenticationResponseForEditor13WithReviewerRights(WireMockServer wireMockServer) {
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/oauth/introspection") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("4-3-2-1-0")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"active\": true," +
                                "  \"clientId\": \"123-456-789-23456\"," +
                                "  \"expires\": \"2029-01-13T12:58:53.967Z\"," +
                                "  \"agency\": \"790900\"," +
                                "  \"uniqueId\": null," +
                                "  \"search\": {" +
                                "    \"profile\": \"opac\"," +
                                "    \"agency\": \"790900\"" +
                                "  }," +
                                "  \"type\": \"authorized\"," +
                                "  \"name\": \"Promat prod\"," +
                                "  \"contact\": {" +
                                "    \"owner\": {" +
                                "      \"name\": \"Henrik Witt Hansen\"," +
                                "      \"email\": \"hwha@dbc.dk\"," +
                                "      \"phone\": \"\"" +
                                "    }" +
                                "  }" +
                                "}")));

        // Editor authorized using 'netpunkts login', so we have the IDP field
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/userinfo") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("4-3-2-1-0")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"attributes\": {" +
                                "    \"serviceStatus\": {" +
                                "      \"borchk\": \"ok\"," +
                                "      \"culr\": \"ok\"" +
                                "    }," +
                                "    \"userId\": \"klnp\"," +
                                "    \"municipality\": \"909\"," +
                                "    \"netpunktAgency\": \"790900\"," +
                                "    \"municipalityAgencyId\": \"790900\"," +
                                "    \"dbcidp\": [" +
                                "      {" +
                                "        \"agencyId\": \"790900\"," +
                                "        \"rights\": [" +
                                "          {" +
                                "            \"productName\": \"" + IDP_PRODUCT_NAME + "\"," +
                                "            \"name\": \"" + IDP_REVIEWER_RIGHT_NAME + "\"," +
                                "            \"description\": \"adgang til Promat som ekstern anmelder\"" +
                                "          }" +
                                "        ]" +
                                "      }" +
                                "    ]" +
                                "  }" +
                                "}")));
    }

    public static void mockAuthenticationResponseForReviewer2WithEditorRights(WireMockServer wireMockServer) {
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/oauth/introspection") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("3-2-1-0-9")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"active\": true," +
                                "  \"clientId\": \"123-456-789-34567\"," +
                                "  \"expires\": \"2029-01-13T12:58:53.967Z\"," +
                                "  \"agency\": \"790900\"," +
                                "  \"uniqueId\": null," +
                                "  \"search\": {" +
                                "    \"profile\": \"opac\"," +
                                "    \"agency\": \"790900\"" +
                                "  }," +
                                "  \"type\": \"authorized\"," +
                                "  \"name\": \"Promat prod\"," +
                                "  \"contact\": {" +
                                "    \"owner\": {" +
                                "      \"name\": \"Henrik Witt Hansen\"," +
                                "      \"email\": \"hwha@dbc.dk\"," +
                                "      \"phone\": \"\"" +
                                "    }" +
                                "  }" +
                                "}")));

        // Editor authorized using 'netpunkts login', so we have the IDP field
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/userinfo") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("3-2-1-0-9")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"attributes\": {" +
                                "    \"serviceStatus\": {" +
                                "      \"borchk\": \"ok\"," +
                                "      \"culr\": \"ok\"" +
                                "    }," +
                                "    \"userId\": \"axel52\"," +
                                "    \"municipality\": \"909\"," +
                                "    \"netpunktAgency\": \"820010\"," +
                                "    \"municipalityAgencyId\": \"820020\"," +
                                "    \"dbcidp\": [" +
                                "      {" +
                                "        \"agencyId\": \"820010\"," +
                                "        \"rights\": [" +
                                "          {" +
                                "            \"productName\": \"" + IDP_PRODUCT_NAME + "\"," +
                                "            \"name\": \"" + IDP_EDITOR_RIGHT_NAME + "\"," +
                                "            \"description\": \"adgang til Promat som DBC redaktør\"" +
                                "          }" +
                                "        ]" +
                                "      }" +
                                "    ]" +
                                "  }" +
                                "}")));
    }

    private static void mockAuthenticationResponseForLoggedOutUser(WireMockServer wireMockServer) {

        // Mock /introspection endpoint
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("6-7-8-9-0")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{\"active\":false}")));

        // Mock /userinfo endpoint
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/userinfo") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("6-7-8-9-0")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(401)
                        .withBody("{" +
                                "  \"error\": \"invalid_token\"," +
                                "  \"error_description\": \"Invalid token: access token is invalid\"" +
                                "}")));
    }
}
