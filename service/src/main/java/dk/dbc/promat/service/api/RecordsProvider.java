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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This is the class the REST layer (Records.java) actually calls. It sits
// between the JAX-RS resource and FbiApiHandler/RecordServiceConnector,
// and is responsible for the *promat-specific* decisions: when to fall
// back to rawrepo, which record counts as "primary", and how to translate
// fbi-api's shape into the RecordDto/RecordsListDto shape that goes out
// over the wire as JSON.
@ApplicationScoped
public class RecordsProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordsProvider.class);
    // Same "common/national data" agency id used by FbiApiHandler - see
    // that class's comment on AGENCY_ID. rawrepo-record-service needs it
    // as an int rather than a String, hence the separate constant here.
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


    // `id` can be a faust number, an ISBN, or a barcode - faustResolver
    // turns whichever one was given into the actual set of faust numbers it
    // refers to (usually one, but e.g. an ISBN shared by a paperback and
    // ebook edition can resolve to several).
    public RecordsListDto getRecords(String id) throws FbiApiConnectorException, FaustResolverException, RecordServiceConnectorException {
        Set<String> manifestations = faustResolver.resolve(id);
        List<RecordDto> recordList = fbiApiHandler.recordInfo(manifestations)
                    .stream()
                    // The one manifestation whose faust matches the id the
                    // caller searched for is marked "primary" - useful when
                    // several manifestations come back and the UI needs to
                    // highlight which one was actually asked for.
                    .map(info -> toRecordDto(info, id.equals(info.faust())))
                    .toList();
        if (recordList.size() < manifestations.size()) {
            LOGGER.warn("Partial result for id {}: expected {} manifestations, got {}", id, manifestations.size(), recordList.size());
        }
        // Fallthrough: fbi-api doesn't know this faust at all yet (e.g. a
        // record catalogued minutes ago hasn't been indexed into fbi-api
        // yet, but already exists in rawrepo, the underlying source of
        // truth). Rather than fail outright, check whether rawrepo at least
        // knows the record exists, and if so return a minimal faust+title
        // result so the caller (e.g. "create case") can still proceed.
        // Only a rawrepo-record-service lookup of faust.
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

    // Shared by both getRecords() and search() so the field mapping only
    // has to be written (and kept correct) once. "primary" is the only
    // thing that differs between the two call sites, so it's passed in as
    // a parameter instead of being decided in here.
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
                .withTypes(List.of(mapMaterialType(info.materialTypeGeneralCode(), info.materialTypeSpecificDisplay())));
    }

    // "245" is the MARC field number for a record's title (a library
    // cataloguing standard - every bibliographic record is built from
    // numbered fields like this, regardless of which system stores it).
    // Subfield 'a' is the main title text within that field.
    private static final String TITLE_FIELD = "245";

    // Best-effort title lookup for records rawrepo knows about but fbi-api has no data for.
    // Failures here should not fail the overall lookup - a record without a title is still
    // more useful to the caller than no record at all.
    private String resolveTitle(String id) {
        try {
            return recordServiceConnector.getRecordContentCollection(DBC_AGENCY, id).stream()
                    .map(marcBinding -> marcBinding.getSubFieldValue(TITLE_FIELD, 'a'))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        } catch (RecordServiceConnectorException e) {
            LOGGER.warn("Unable to resolve title for id {} via rawrepo-record-service", id, e);
            return null;
        }
    }

    private RecordMaterialTypeDto mapMaterialType(String code, String display) {
        return new RecordMaterialTypeDto()
                .withMaterialType(mapGeneralMaterialType(code))
                .withSpecificType(display);
    }

    // fbi-api's material type codes (e.g. "BOOKS", "EBOOKS", "FILMS") are
    // much more granular than promat's own MaterialType enum (just
    // BOOK/MOVIE/MULTIMEDIA/UNKNOWN, since that's all promat's business
    // logic - assigning reviewers, task types - actually cares about). This
    // switch expression is where fbi-api's world gets translated down into
    // promat's simpler one; the original fbi-api code is kept too (as
    // "specificType" in RecordMaterialTypeDto) for display purposes.
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
