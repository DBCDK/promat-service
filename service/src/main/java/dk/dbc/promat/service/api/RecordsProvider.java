package dk.dbc.promat.service.api;

import dk.dbc.promat.service.connectors.FaustResolver;
import dk.dbc.promat.service.connectors.FaustResolverException;
import dk.dbc.promat.service.connectors.FbiApiConnectorException;
import dk.dbc.promat.service.dto.RecordDto;
import dk.dbc.promat.service.dto.RecordMaterialTypeDto;
import dk.dbc.promat.service.dto.RecordsListDto;
import dk.dbc.promat.service.persistence.MaterialType;
import java.util.List;
import java.util.Set;

import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RecordsProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordsProvider.class);
    public static final int DBC_AGENCY = 870970;

    FaustResolver faustResolver;
    FbiApiHandler fbiApiHandler;
    RecordServiceConnector recordServiceConnector;

    // For CDI
    RecordsProvider() {}

    @Inject
    public RecordsProvider(FaustResolver faustResolver, FbiApiHandler fbiApiHandler, RecordServiceConnector recordServiceConnector) {
        this.faustResolver = faustResolver;
        this.fbiApiHandler = fbiApiHandler;
        this.recordServiceConnector = recordServiceConnector;
    }


    public RecordsListDto getRecords(String id) throws FbiApiConnectorException, FaustResolverException, RecordServiceConnectorException {
        Set<String> manifestations = faustResolver.resolve(id);
        List<RecordDto> recordList = fbiApiHandler.materialTypeInfo(manifestations)
                    .stream()
                    .map(info -> new RecordDto()
                            .withFaust(info.faust())
                            .withPrimary(id.equals(info.faust()))
                            .withTypes(List.of(mapMaterialType(info.code(), info.display()))))
                    .toList();
        if (recordList.size() < manifestations.size()) {
            LOGGER.warn("Partial result for id {}: expected {} manifestations, got {}", id, manifestations.size(), recordList.size());
        }
        // Fallthrough: Only rawrepo-record-service lookup of faust.
        if (recordList.isEmpty() && recordServiceConnector.recordExists(DBC_AGENCY, id)) {
           return new RecordsListDto().withRecords(List.of(new RecordDto().withFaust(id).withPrimary(true)))
                   .withNumFound(1);
        }
        return new RecordsListDto().withRecords(recordList).withNumFound(recordList.size());
    }

    private RecordMaterialTypeDto mapMaterialType(String code, String display) {
        return new RecordMaterialTypeDto()
                .withMaterialType(mapGeneralMaterialType(code))
                .withSpecificType(display);
    }

    private static MaterialType mapGeneralMaterialType(String generalMaterialTypeCode) {
        if (generalMaterialTypeCode == null) {
            return MaterialType.UNKNOWN;
        }
        return switch (generalMaterialTypeCode) {
            case "AUDIO_BOOKS", "BOOKS", "COMICS", "PODCASTS", "EBOOKS" -> MaterialType.BOOK;
            case "COMPUTER_GAMES", "BOARD_GAMES" -> MaterialType.MULTIMEDIA;
            case "FILMS", "TV_SERIES" -> MaterialType.MOVIE;
            default -> MaterialType.UNKNOWN;
        };
    }
}
