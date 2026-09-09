package dk.dbc.promat.service.api;

import dk.dbc.promat.service.connectors.FaustResolver;
import dk.dbc.promat.service.connectors.FaustResolverException;
import dk.dbc.promat.service.connectors.FbiApiConnectorException;
import dk.dbc.promat.service.dto.RecordDto;
import dk.dbc.promat.service.dto.RecordMaterialTypeDto;
import dk.dbc.promat.service.dto.RecordsListDto;
import dk.dbc.promat.service.persistence.MaterialType;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
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
        List<RecordDto> recordList = fbiApiHandler.recordInfo(manifestations)
                    .stream()
                    .map(info -> toRecordDto(info, id.equals(info.faust())))
                    .toList();
        if (recordList.size() < manifestations.size()) {
            LOGGER.warn("Partial result for id {}: expected {} manifestations, got {}", id, manifestations.size(), recordList.size());
        }
        // Fallthrough: Only rawrepo-record-service lookup of faust; now also resolves a title.
        if (recordList.isEmpty() && recordServiceConnector.recordExists(DBC_AGENCY, id)) {
           return new RecordsListDto().withRecords(List.of(new RecordDto()
                           .withFaust(id)
                           .withPrimary(true)
                           .withTitle(resolveTitle(id))))
                   .withNumFound(1);
        }
        return new RecordsListDto().withRecords(recordList).withNumFound(recordList.size());
    }

    /**
     * Looks up a single, known faust directly - bypassing {@link FaustResolver}'s
     * ISBN/barcode resolution entirely, since the caller already has a faust, not an
     * id that might resolve to several manifestations. Always returns at most one
     * record (or {@code null} if none was found), never a list, unlike
     * {@link #getRecords(String)}, which has to stay list-shaped to also serve
     * ISBN/barcode lookups that can genuinely match more than one manifestation.
     */
    public RecordDto getRecordByFaust(String faust) throws FbiApiConnectorException, RecordServiceConnectorException {
        List<FbiApiHandler.RecordInfo> records = fbiApiHandler.recordInfo(Set.of(faust));
        if (!records.isEmpty()) {
            return toRecordDto(records.getFirst(), true);
        }
        // Fallthrough: fbi-api doesn't know this faust yet, but rawrepo (the
        // underlying source of truth) already does - return a minimal
        // faust+title result so the caller can still proceed.
        if (recordServiceConnector.recordExists(DBC_AGENCY, faust)) {
            return new RecordDto().withFaust(faust).withPrimary(true).withTitle(resolveTitle(faust));
        }
        return null;
    }

    /**
     * Free-text search by title and/or creator, backed by fbi-api's complexSearch.
     * Unlike {@link #getRecords(String)}, hits are not tied to a requested id, so none
     * of them are ever marked primary.
     */
    public RecordsListDto search(String title, String creator, Integer limit) throws FbiApiConnectorException {
        List<RecordDto> recordList = fbiApiHandler.search(title, creator, limit)
                .stream()
                .map(info -> toRecordDto(info, false))
                .toList();
        return new RecordsListDto().withRecords(recordList).withNumFound(recordList.size());
    }

    // Shared by getRecords() and search(); "primary" differs between the two call sites.
    private RecordDto toRecordDto(FbiApiHandler.RecordInfo info, boolean primary) {
        return new RecordDto()
                .withFaust(info.faust())
                .withPrimary(primary)
                .withTitle(info.title())
                .withCreator(info.creator())
                .withPublisher(info.publisher())
                .withExtent(info.extent())
                .withEdition(info.edition())
                .withIsbn(info.isbn())
                .withDk5(info.dk5())
                .withSeries(info.series())
                .withTargetGroup(info.targetgroup())
                .withCatalogCodes(info.catalogcodes())
                .withTypes(info.materialTypes().stream()
                        .map(pair -> mapMaterialType(pair.generalCode(), pair.specificDisplay()))
                        .toList());
    }

    // "245" is the MARC field for a record's title; subfield 'a' is the main title text.
    private static final String TITLE_FIELD = "245";

    // MarcBinding only carries Jackson (JSON) bindings, no JAXB/XML support at all. The
    // record-service /content endpoint's *default* response format differs by backend:
    // DM3 (datawell-test) defaults to JSON there, so it happens to work with no params -
    // but DM2 (cisterne, fbstest) defaults to marcxchange XML, which MarcBinding cannot be
    // deserialized from, so the call throws MessageBodyProviderNotFoundException (a
    // RuntimeException, NOT a RecordServiceConnectorException, so it isn't caught below
    // either - it escapes as an unhandled error rather than degrading gracefully).
    // Requesting MARC_JSON explicitly makes both backends respond identically, verified
    // against DM3 (datawell-test) and DM2 (cisterne, fbstest) with the same faust.
    private static final RecordServiceConnector.Params CONTENT_PARAMS = new RecordServiceConnector.Params()
            .withOutputFormat(RecordServiceConnector.Params.OutputFormat.MARC_JSON);

    // Best-effort title lookup for records rawrepo knows about but fbi-api has no data for.
    // Failures here should not fail the overall lookup - a record without a title is still
    // more useful to the caller than no record at all. Also catches ProcessingException so
    // that if a backend ever responds in a format CONTENT_PARAMS didn't anticipate (see its
    // comment above), we degrade to a missing title instead of an unhandled error.
    private String resolveTitle(String id) {
        try {
            return recordServiceConnector.getRecordContentCollection(DBC_AGENCY, id, CONTENT_PARAMS).stream()
                    .map(marcBinding -> marcBinding.getSubFieldValue(TITLE_FIELD, 'a'))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        } catch (RecordServiceConnectorException | ProcessingException e) {
            LOGGER.warn("Unable to resolve title for id {} via rawrepo-record-service", id, e);
            return null;
        }
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
