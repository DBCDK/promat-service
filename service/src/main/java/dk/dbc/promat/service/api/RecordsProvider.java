package dk.dbc.promat.service.api;

import dk.dbc.promat.service.connectors.FaustResolver;
import dk.dbc.promat.service.connectors.FaustResolverException;
import dk.dbc.promat.service.connectors.OpenFormatConnectorException;
import dk.dbc.promat.service.dto.RecordDto;
import dk.dbc.promat.service.dto.RecordMaterialTypeDto;
import dk.dbc.promat.service.dto.RecordsListDto;
import dk.dbc.promat.service.persistence.MaterialType;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RecordsProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordsProvider.class);

    FaustResolver faustResolver;
    OpenFormatHandler openFormatHandler;

    // For CDI
    RecordsProvider() {}

    @Inject
    public RecordsProvider(FaustResolver faustResolver, OpenFormatHandler openFormatHandler) {
        this.faustResolver = faustResolver;
        this.openFormatHandler = openFormatHandler;
    }


    public RecordsListDto getRecords(String id) throws OpenFormatConnectorException, FaustResolverException {
        Set<String> manifestations = faustResolver.resolve(id);
        List<RecordDto> recordList = openFormatHandler.format(manifestations)
                    .stream()
                    .map(promatElements -> {
                        String faust = promatElements.faust().getFirst();
                        var detail = promatElements.materialtypesDetail();
                        String[] sf = detail != null && detail.type() != null && !detail.type().isEmpty()
                                ? detail.type().getFirst().split(" ")
                                : null;
                        return new RecordDto()
                                .withFaust(faust)
                                .withPrimary(id.equals(faust))
                                .withTypes(List.of(mapRrType(sf)));

                    }).toList();
        if (recordList.size() < manifestations.size()) {
            LOGGER.warn("Partial result for id {}: expected {} manifestations, got {}", id, manifestations.size(), recordList.size());
        }
        return new RecordsListDto().withRecords(recordList).withNumFound(recordList.size());
    }

    private RecordMaterialTypeDto mapRrType(String... sf) {
        if (sf == null || sf.length != 2) {
            return new RecordMaterialTypeDto().withMaterialType(MaterialType.UNKNOWN);
        }

        String specificType = sf[0] + " " + sf[1];

        return switch (sf[0]) {
            case "a", "r" ->
                    new RecordMaterialTypeDto().withMaterialType(MaterialType.BOOK).withSpecificType(specificType);
            case "m" -> new RecordMaterialTypeDto().withMaterialType(MaterialType.MOVIE).withSpecificType(specificType);
            case "t" ->
                    new RecordMaterialTypeDto().withMaterialType(MaterialType.MULTIMEDIA).withSpecificType(specificType);
            default ->
                    new RecordMaterialTypeDto().withMaterialType(MaterialType.UNKNOWN).withSpecificType(specificType);
        };
    }
}
