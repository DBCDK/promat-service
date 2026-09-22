package dk.dbc.promat.service.connectors;

import dk.dbc.updateservice.UpdateServiceUpdateConnector;
import dk.dbc.updateservice.dto.AuthenticationDTO;
import dk.dbc.updateservice.dto.BibliographicRecordDTO;
import dk.dbc.updateservice.dto.RecordDataDTO;
import dk.dbc.updateservice.dto.UpdateRecordResponseDTO;
import dk.dbc.updateservice.dto.UpdateServiceRequestDTO;
import dk.dbc.updateservice.dto.UpdateStatusEnumDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class CatalogingUpdateConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogingUpdateConnector.class);
    // update-service expects MarcXchange XML inside the REST DTO, so we use the
    // shared Java REST connector instead of constructing SOAP by hand.
    private static final String MARCXCHANGE_SCHEMA = "info:lc/xmlns/marcxchange-v1";

    private final UpdateServiceUpdateConnector updateServiceUpdateConnector;
    private final String schemaName;
    private final String netpunktGroup;
    private final String netpunktUser;
    private final String netpunktPassword;

    public CatalogingUpdateConnector(UpdateServiceUpdateConnector updateServiceUpdateConnector, String schemaName,
                                     String netpunktGroup, String netpunktUser, String netpunktPassword) {
        this.updateServiceUpdateConnector = Objects.requireNonNull(updateServiceUpdateConnector, "updateServiceUpdateConnector must not be null");
        this.schemaName = Objects.requireNonNull(schemaName, "schemaName must not be null");
        this.netpunktGroup = Objects.requireNonNull(netpunktGroup, "netpunktGroup must not be null");
        this.netpunktUser = Objects.requireNonNull(netpunktUser, "netpunktUser must not be null");
        this.netpunktPassword = Objects.requireNonNull(netpunktPassword, "netpunktPassword must not be null");
    }

    public void updateRecord(String pid, String marcRecord) throws CatalogingUpdateConnectorException {
        String trackingId = "DBC_PROMAT_" + pid + "_" + Instant.now();
        LOGGER.info("Calling update-service REST endpoint for pid {}", pid);
        try {
            UpdateRecordResponseDTO response = updateServiceUpdateConnector.updateRecord(
                    createUpdateRequest(marcRecord, trackingId));
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
        updateServiceUpdateConnector.close();
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
