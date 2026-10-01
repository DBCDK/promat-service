package dk.dbc.promat.service.connectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.updateservice.dto.AuthenticationDTO;
import dk.dbc.updateservice.dto.BibliographicRecordDTO;
import dk.dbc.updateservice.dto.RecordDataDTO;
import dk.dbc.updateservice.dto.UpdateRecordResponseDTO;
import dk.dbc.updateservice.dto.UpdateServiceRequestDTO;
import dk.dbc.updateservice.dto.UpdateStatusEnumDTO;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class CatalogingUpdateConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogingUpdateConnector.class);
    // Plain ObjectMapper, like the JSON binding updateservice-rest-connector uses for the same DTOs
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    // update-service expects MarcXchange XML inside the REST DTO
    private static final String MARCXCHANGE_SCHEMA = "info:lc/xmlns/marcxchange-v1";
    // Makes update-service merge the 665/664 fields into the existing record, instead of treating the
    // sent record as the whole record. The same schema for Metakompas and Buggi, as in Metakompasset
    private static final String SCHEMA_NAME = "metakompas";

    // Metakompasset registers Metakompas and Buggi with a netpunkt login of their own each
    public record NetpunktCredentials(String group, String user, String password) {
        public NetpunktCredentials {
            Objects.requireNonNull(group, "group must not be null");
            Objects.requireNonNull(user, "user must not be null");
            Objects.requireNonNull(password, "password must not be null");
        }
    }

    private final FailSafeHttpClient failSafeHttpClient;
    private final String baseUrl;
    private final NetpunktCredentials metakompasCredentials;
    private final NetpunktCredentials buggiCredentials;

    public CatalogingUpdateConnector(FailSafeHttpClient failSafeHttpClient, String baseUrl,
                                     NetpunktCredentials metakompasCredentials, NetpunktCredentials buggiCredentials) {
        this.failSafeHttpClient = Objects.requireNonNull(failSafeHttpClient, "failSafeHttpClient must not be null");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        this.metakompasCredentials = Objects.requireNonNull(metakompasCredentials, "metakompasCredentials must not be null");
        this.buggiCredentials = Objects.requireNonNull(buggiCredentials, "buggiCredentials must not be null");
    }

    public void updateRecord(TaskFieldType taskFieldType, String pid, String marcRecord) throws CatalogingUpdateConnectorException {
        NetpunktCredentials credentials = switch(taskFieldType) {
            case READING_EXPERIENCE_ADULT -> metakompasCredentials;
            case READING_EXPERIENCE_CHILD -> buggiCredentials;
            default -> throw new IllegalArgumentException("No update-service registration for task type " + taskFieldType);
        };
        String trackingId = "DBC_PROMAT_" + pid + "_" + Instant.now();
        LOGGER.info("Calling update-service REST endpoint for {} registration of pid {}", taskFieldType, pid);
        try {
            UpdateRecordResponseDTO response = post(createUpdateRequest(marcRecord, trackingId, credentials));
            if(response.getUpdateStatusEnumDTO() != UpdateStatusEnumDTO.OK) {
                throw new CatalogingUpdateConnectorException("update-service returned non-ok response: " + response);
            }
        } catch(CatalogingUpdateConnectorException e) {
            throw e;
        } catch(Exception e) {
            throw new CatalogingUpdateConnectorException("Unable to call update-service", e);
        }
    }

    public void close() {
        failSafeHttpClient.getClient().close();
    }

    // Same call as updateservice-rest-connector's UpdateServiceUpdateConnector.updateRecord
    private UpdateRecordResponseDTO post(UpdateServiceRequestDTO request) throws Exception {
        HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements("api", "v1", "updateservice")
                .withHeader("Accept", MediaType.APPLICATION_JSON)
                .withData(OBJECT_MAPPER.writeValueAsString(request), MediaType.APPLICATION_JSON);
        try(Response response = httpPost.execute()) {
            if(response.getStatus() != Response.Status.OK.getStatusCode()) {
                throw new CatalogingUpdateConnectorException("update-service returned with unexpected status code: " + response.getStatus());
            }
            return OBJECT_MAPPER.readValue(response.readEntity(String.class), UpdateRecordResponseDTO.class);
        }
    }

    private UpdateServiceRequestDTO createUpdateRequest(String marcRecord, String trackingId, NetpunktCredentials credentials) {
        // Authentication/schema/trackingId mirror the fields sent by metakompasset,
        // but are passed through update-service's typed REST DTOs here.
        AuthenticationDTO authentication = new AuthenticationDTO();
        authentication.setGroupId(credentials.group());
        authentication.setUserId(credentials.user());
        authentication.setPassword(credentials.password());

        RecordDataDTO recordData = new RecordDataDTO();
        recordData.setContent(List.of(marcRecord));

        BibliographicRecordDTO bibliographicRecord = new BibliographicRecordDTO();
        bibliographicRecord.setRecordSchema(MARCXCHANGE_SCHEMA);
        bibliographicRecord.setRecordPacking("xml");
        bibliographicRecord.setRecordDataDTO(recordData);

        UpdateServiceRequestDTO request = new UpdateServiceRequestDTO();
        request.setAuthenticationDTO(authentication);
        request.setSchemaName(SCHEMA_NAME);
        request.setBibliographicRecordDTO(bibliographicRecord);
        request.setTrackingId(trackingId);
        return request;
    }
}
