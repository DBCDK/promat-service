package dk.dbc.promat.service.connectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpPost;
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

    private final FailSafeHttpClient failSafeHttpClient;
    private final String baseUrl;
    private final String schemaName;
    private final String netpunktGroup;
    private final String netpunktUser;
    private final String netpunktPassword;

    public CatalogingUpdateConnector(FailSafeHttpClient failSafeHttpClient, String baseUrl, String schemaName,
                                     String netpunktGroup, String netpunktUser, String netpunktPassword) {
        this.failSafeHttpClient = Objects.requireNonNull(failSafeHttpClient, "failSafeHttpClient must not be null");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        this.schemaName = Objects.requireNonNull(schemaName, "schemaName must not be null");
        this.netpunktGroup = Objects.requireNonNull(netpunktGroup, "netpunktGroup must not be null");
        this.netpunktUser = Objects.requireNonNull(netpunktUser, "netpunktUser must not be null");
        this.netpunktPassword = Objects.requireNonNull(netpunktPassword, "netpunktPassword must not be null");
    }

    public void updateRecord(String pid, String marcRecord) throws CatalogingUpdateConnectorException {
        String trackingId = "DBC_PROMAT_" + pid + "_" + Instant.now();
        LOGGER.info("Calling update-service REST endpoint for pid {}", pid);
        try {
            UpdateRecordResponseDTO response = post(createUpdateRequest(marcRecord, trackingId));
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

    private UpdateServiceRequestDTO createUpdateRequest(String marcRecord, String trackingId) {
        // Authentication/schema/trackingId mirror the fields sent by metakompasset,
        // but are passed through update-service's typed REST DTOs here.
        AuthenticationDTO authentication = new AuthenticationDTO();
        authentication.setGroupId(netpunktGroup);
        authentication.setUserId(netpunktUser);
        authentication.setPassword(netpunktPassword);

        RecordDataDTO recordData = new RecordDataDTO();
        recordData.setContent(List.of(marcRecord));

        BibliographicRecordDTO bibliographicRecord = new BibliographicRecordDTO();
        bibliographicRecord.setRecordSchema(MARCXCHANGE_SCHEMA);
        bibliographicRecord.setRecordPacking("xml");
        bibliographicRecord.setRecordDataDTO(recordData);

        UpdateServiceRequestDTO request = new UpdateServiceRequestDTO();
        request.setAuthenticationDTO(authentication);
        request.setSchemaName(schemaName);
        request.setBibliographicRecordDTO(bibliographicRecord);
        request.setTrackingId(trackingId);
        return request;
    }
}
