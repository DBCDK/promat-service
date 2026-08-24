package dk.dbc.promat.service.connectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpPost;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Client for the fbi-api GraphQL service.
 */
public class FbiApiConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(FbiApiConnector.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final long TOKEN_EXPIRY_MARGIN_SECONDS = 15;

    private final FailSafeHttpClient failSafeHttpClient;
    private final String baseUrl;
    private final String loginUrl;
    private final String clientId;
    private final String clientSecret;
    private final String username;
    private final String password;

    private volatile String accessToken;
    private volatile Instant accessTokenExpiresAt = Instant.MIN;

    public FbiApiConnector(FailSafeHttpClient failSafeHttpClient, String baseUrl, String loginUrl,
                            String clientId, String clientSecret, String username, String password) {
        this.failSafeHttpClient = Objects.requireNonNull(failSafeHttpClient, "failSafeHttpClient must not be null");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        this.loginUrl = Objects.requireNonNull(loginUrl, "loginUrl must not be null");
        this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret must not be null");
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.password = Objects.requireNonNull(password, "password must not be null");
    }

    // For test
    public void setAccessToken(String accessToken, Instant accessTokenExpiresAt) {
        this.accessToken = accessToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }

    public void close() {
        failSafeHttpClient.getClient().close();
    }

    /**
     * Executes a GraphQL query or mutation against fbi-api.
     *
     * @param query     GraphQL query/mutation document
     * @param variables GraphQL variables, may be empty
     * @return the "data" part of the GraphQL response
     * @throws FbiApiConnectorException on transport failure, GraphQL errors, or missing data
     */
    public JsonNode execute(String query, Map<String, Object> variables) throws FbiApiConnectorException {
        LOGGER.info("Calling fbi-api at {}", baseUrl);

        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements("promat", "graphql")
                .withHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .withJsonData(new GraphQLRequest(query, variables));

        try (Response response = httpPost.execute()) {
            assertResponseStatus(response, "fbi-api");
            final GraphQLResponse graphQLResponse = readResponseEntity(response, GraphQLResponse.class, "fbi-api");
            if (graphQLResponse.errors() != null && !graphQLResponse.errors().isEmpty()) {
                throw new FbiApiConnectorException("fbi-api returned GraphQL errors: " + graphQLResponse.errors());
            }
            if (graphQLResponse.data() == null) {
                throw new FbiApiConnectorException("fbi-api returned no data");
            }
            return graphQLResponse.data();
        }
    }

    /**
     * Executes a GraphQL query or mutation against fbi-api and maps the "data" part to the given type.
     */
    public <T> T execute(String query, Map<String, Object> variables, Class<T> dataType) throws FbiApiConnectorException {
        try {
            return OBJECT_MAPPER.treeToValue(execute(query, variables), dataType);
        } catch (JsonProcessingException e) {
            throw new FbiApiConnectorException("Unable to map fbi-api response to " + dataType.getName(), e);
        }
    }

    private String getAccessToken() throws FbiApiConnectorException {
        if (accessToken == null || Instant.now().isAfter(accessTokenExpiresAt)) {
            synchronized (this) {
                if (accessToken == null || Instant.now().isAfter(accessTokenExpiresAt)) {
                    fetchAccessToken();
                }
            }
        }
        return accessToken;
    }

    private void fetchAccessToken() throws FbiApiConnectorException {
        LOGGER.info("Fetching new fbi-api access token from {}", loginUrl);

        final String credentials = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        final String body = String.format("grant_type=password&username=%s&password=%s",
                urlEncode(username), urlEncode(password));

        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(loginUrl)
                .withPathElements("oauth", "token")
                .withHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                .withData(body, MediaType.APPLICATION_FORM_URLENCODED);

        try (Response response = httpPost.execute()) {
            assertResponseStatus(response, "fbi-api login");
            final TokenResponse tokenResponse = readResponseEntity(response, TokenResponse.class, "fbi-api login");
            accessToken = tokenResponse.accessToken();
            accessTokenExpiresAt = Instant.now()
                    .plusSeconds(tokenResponse.expiresIn())
                    .minusSeconds(TOKEN_EXPIRY_MARGIN_SECONDS);
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void assertResponseStatus(Response response, String context) throws FbiApiConnectorException {
        final Response.Status actualStatus = Response.Status.fromStatusCode(response.getStatus());
        if (!Response.Status.OK.equals(actualStatus)) {
            throw new FbiApiConnectorException(String.format(
                    "%s returned with unexpected status code: %s", context, response.getStatus()));
        }
    }

    private <T> T readResponseEntity(Response response, Class<T> type, String context) throws FbiApiConnectorException {
        final T entity = response.readEntity(type);
        if (entity == null) {
            throw new FbiApiConnectorException(String.format(
                    "%s returned with null-valued %s entity", context, type.getName()));
        }
        return entity;
    }

    private record GraphQLRequest(String query, Map<String, Object> variables) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GraphQLResponse(JsonNode data, List<GraphQLError> errors) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GraphQLError(String message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn) {}

    /**
     * Bibliographic data for a single manifestation, fetched from fbi-api. Field-for-field
     * equivalent of {@link dk.dbc.promat.service.connectors.OpenFormatConnector.PromatElements},
     * to ease migrating existing consumers once OpenFormat is retired.
     */
    public record PromatElements(
            List<String> faust,
            List<String> creator,
            List<String> dk5,
            List<String> isbn,
            TypeList materialtypes,
            TypeList materialtypesDetail,
            List<String> extent,
            List<String> publisher,
            List<String> edition,
            List<String> series,
            CodeList catalogcodes,
            List<String> title,
            List<String> targetgroup,
            List<String> metakompassubject) {

        public record TypeList(List<String> type) {}

        public record CodeList(List<String> code) {}
    }
}
