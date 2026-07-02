package dk.dbc.promat.service.connectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpPost;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class OpenFormatConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenFormatConnector.class);
    private static final String FORMAT_NAME = "promat";
    private static final String FORMAT_MEDIA_TYPE = "application/json";

    private final String baseUrl;
    private final FailSafeHttpClient failSafeHttpClient;

    public OpenFormatConnector(FailSafeHttpClient failSafeHttpClient, String baseUrl) {
        if (failSafeHttpClient == null || baseUrl == null) {
            throw new NullPointerException(String.format(
                    "No parameters is allowed to be null in call to OpenFormatConnector(%s, %s)",
                    failSafeHttpClient == null ? "null" : failSafeHttpClient.toString(),
                    baseUrl == null ? "null" : baseUrl));
        }
        this.failSafeHttpClient = failSafeHttpClient;
        this.baseUrl = baseUrl;
    }

    public void close() {
        failSafeHttpClient.getClient().close();
    }

    public PromatElements format(String faust, String agency) throws OpenFormatConnectorException {
        String repositoryId = agency + "-basis:" + faust;
        String requestBody = String.format(
                "{\"formats\":[{\"name\":\"%s\",\"mediaType\":\"%s\"}],\"objects\":[{\"repositoryId\":\"%s\"}]}",
                FORMAT_NAME, FORMAT_MEDIA_TYPE, repositoryId);

        LOGGER.info("Calling openformat at {}/api/v2/format for repository id {}", baseUrl, repositoryId);

        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements("api", "v2", "format")
                .withJsonData(requestBody);

        try (Response response = httpPost.execute()) {
            assertResponseStatus(response, repositoryId);
            return extractElements(readResponseEntity(response, FormatResponse.class));
        }
    }

    private void assertResponseStatus(Response response, String repositoryId) throws OpenFormatConnectorException {
        final Response.Status actualStatus = Response.Status.fromStatusCode(response.getStatus());
        if (!Response.Status.OK.equals(actualStatus)) {
            throw new OpenFormatConnectorException(
                    "OpenFormat service returned with unexpected status code: " + response.getStatus() + " for " + repositoryId);
        }
    }

    private <T> T readResponseEntity(Response response, Class<T> type) throws OpenFormatConnectorException {
        final T entity = response.readEntity(type);
        if (entity == null) {
            throw new OpenFormatConnectorException(
                    "OpenFormat returned with null-valued " + type.getName() + " entity");
        }
        return entity;
    }

    private PromatElements extractElements(FormatResponse response) {
        if (response.objects() == null || response.objects().isEmpty()) {
            return null;
        }
        FormatResponseObject obj = response.objects().getFirst();
        if (obj.promat() == null || obj.promat().isEmpty()) {
            return null;
        }
        FormatEntry entry = obj.promat().getFirst();
        if (entry.formatted() == null || entry.formatted().records() == null || entry.formatted().records().isEmpty()) {
            return null;
        }
        return entry.formatted().records().getFirst().elements();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FormatResponse(
            List<FormatResponseObject> objects,
            String trackingId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FormatResponseObject(
            @JsonProperty("promat") List<FormatEntry> promat) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FormatEntry(
            Formatted formatted,
            String mediaType) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Formatted(
            String format,
            List<FormatRecord> records) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FormatRecord(
            PromatElements elements) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
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
            List<String> metakompasSubject) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TypeList(List<String> type) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CodeList(List<String> code) {}
}
