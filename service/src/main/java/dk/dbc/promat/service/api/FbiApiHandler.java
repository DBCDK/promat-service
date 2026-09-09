package dk.dbc.promat.service.api;

import dk.dbc.promat.service.connectors.FbiApiConnector;
import dk.dbc.promat.service.connectors.FbiApiConnectorException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ApplicationScoped
public class FbiApiHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FbiApiHandler.class);
    private static final String AGENCY_ID = "870970";
    private static final String METAKOMPASDATA_PRESENT = "true";
    // Field-list alignment with `Manifestation` is enforced by FbiApiHandlerQueryFieldsTest.
    // Editing this file also requires updating the WireMock fixtures under
    // service/src/test/resources/mappings/, which match on this exact query text.
    private static final String QUERY = loadQuery();

    private FbiApiConnector connector;

    public FbiApiHandler() {}

    @Inject
    public FbiApiHandler(FbiApiConnector connector) {
        this.connector = connector;
    }

    public FbiApiHandler withConnector(FbiApiConnector connector) {
        this.connector = connector;
        return this;
    }

    public BibliographicInformation format(String faust) throws FbiApiConnectorException {
        return format(faust, BibliographicInformation.class);
    }

    public List<FbiApiConnector.PromatElements> format(Set<String> fausts) throws FbiApiConnectorException {
        ArrayList<FbiApiConnector.PromatElements> elements = new ArrayList<>();
        for (String faust : fausts) {
            FbiApiConnector.PromatElements e = format(faust, FbiApiConnector.PromatElements.class);
            if (e != null) {
                elements.add(e);
            }
        }
        return elements;
    }

    public <T> T format(String faust, Class<T> clazz) throws FbiApiConnectorException {
        final FbiApiConnector.PromatElements elements = fetchElements(faust);

        if (elements == null) {
            LOGGER.error("No bibliographic information returned for faust {}", faust);
            if (clazz == BibliographicInformation.class) {
                return clazz.cast(new BibliographicInformation().withError("No results"));
            }
            return null;
        }

        if (clazz == FbiApiConnector.PromatElements.class) {
            return clazz.cast(elements);
        }

        if (clazz == BibliographicInformation.class) {
            final BibliographicInformation bibliographicInformation = toBibliographicInformation(elements);
            LOGGER.info("Returning bibliographic information: {}", bibliographicInformation);
            return clazz.cast(bibliographicInformation);
        }

        throw new IllegalArgumentException("Unsupported return type: " + clazz.getName());
    }

    /**
     * One manifestation's general/specific material type pair - a manifestation can list
     * several of these (e.g. a combined print+ebook record), each kept together so the two
     * stay correctly matched instead of being derived independently.
     */
    public record MaterialTypePair(String generalCode, String specificDisplay) {}

    /**
     * Full bibliographic data for a single manifestation, plus every general/specific material
     * type pair it lists (usually just one).
     */
    public record RecordInfo(
            String faust,
            String title,
            String creator,
            String publisher,
            String extent,
            String edition,
            List<String> isbn,
            List<String> dk5,
            List<String> series,
            List<String> targetgroup,
            List<String> catalogcodes,
            List<MaterialTypePair> materialTypes) {}

    // One fbi-api round trip per faust - see toRecordInfo below for the field mapping.
    public List<RecordInfo> recordInfo(Set<String> fausts) throws FbiApiConnectorException {
        final List<RecordInfo> result = new ArrayList<>();
        for (String faust : fausts) {
            final Manifestation manifestation = fetchManifestation(faust);
            if (manifestation == null) {
                continue;
            }
            result.add(toRecordInfo(faust, manifestation));
        }
        return result;
    }

    private static final String SEARCH_QUERY = loadResource("/graphql/complexSearchByCql.graphql");
    // fbi-api caps how many results a single complexSearch call can return;
    // we also don't want an editor's search accidentally asking for
    // thousands of rows, so this is enforced on our side too (see
    // Math.min below), not just left to fbi-api to reject.
    private static final int MAX_SEARCH_LIMIT = 100;

    /**
     * Free-text search by title and/or creator, via fbi-api's complexSearch.
     * Returns at most one hit per work (fbi-api's "best representation" pick).
     */
    public List<RecordInfo> search(String title, String creator, Integer limit) throws FbiApiConnectorException {
        final String cql = buildCql(title, creator);
        // No search terms given -> nothing to search for.
        if (cql == null) {
            return List.of();
        }
        final int effectiveLimit = limit == null ? MAX_SEARCH_LIMIT : Math.min(limit, MAX_SEARCH_LIMIT);
        // TODO: offset is always 0 - add real pagination (offset param + surfacing
        // ComplexSearch.hitcount()) instead of silently truncating at MAX_SEARCH_LIMIT.
        final Map<String, Object> variables = Map.of(
                "cql", cql,
                "offset", 0,
                "limit", effectiveLimit,
                "filters", Map.of(),
                "sort", List.of());

        final ComplexSearchResponse response = connector.execute(SEARCH_QUERY, variables, ComplexSearchResponse.class);
        if (response.complexSearch() == null || response.complexSearch().works() == null) {
            return List.of();
        }

        // complexSearch groups hits by "work"; fbi-api already picks one
        // representative manifestation per work for us ("bestRepresentations").
        final List<RecordInfo> result = new ArrayList<>();
        for (Work work : response.complexSearch().works()) {
            if (work.manifestations() == null || work.manifestations().bestRepresentations() == null) {
                continue;
            }
            for (Manifestation m : work.manifestations().bestRepresentations()) {
                result.add(toRecordInfo(faustFromPid(m.pid()), m));
            }
        }
        return result;
    }

    // CQL is the query syntax fbi-api's search expects, e.g.
    // term.title='some title' AND term.creator='some author'.
    private static String buildCql(String title, String creator) {
        final List<String> clauses = new ArrayList<>();
        if (title != null && !title.isBlank()) {
            clauses.add("term.title=" + cqlQuote(title));
        }
        if (creator != null && !creator.isBlank()) {
            clauses.add("term.creator=" + cqlQuote(creator));
        }
        return clauses.isEmpty() ? null : String.join(" AND ", clauses);
    }

    // Search terms are dropped straight into a CQL string literal, so an
    // embedded quote (e.g. "Don't Look Now") needs escaping to avoid ending
    // the literal early and corrupting the query.
    private static String cqlQuote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private static String faustFromPid(String pid) {
        if (pid == null) {
            return null;
        }
        final int lastColon = pid.lastIndexOf(':');
        return lastColon >= 0 ? pid.substring(lastColon + 1) : pid;
    }

    // Shared by recordInfo() and search() so the general/specific material
    // type always comes from the same manifestation fetch.
    private RecordInfo toRecordInfo(String faust, Manifestation manifestation) {
        final List<String> creators = creators(manifestation);
        final List<String> publishers = publisher(manifestation);
        return new RecordInfo(
                faust,
                title(manifestation).stream().findFirst().orElse(null),
                creators.isEmpty() ? null : String.join(", ", creators),
                publishers.isEmpty() ? null : String.join(", ", publishers),
                extent(manifestation).stream().findFirst().orElse(null),
                edition(manifestation).stream().findFirst().orElse(null),
                isbn(manifestation),
                dk5(manifestation),
                series(manifestation),
                targetgroup(manifestation),
                catalogcodes(manifestation),
                materialTypePairs(manifestation));
    }

    private List<MaterialTypePair> materialTypePairs(Manifestation m) {
        if (m.materialTypes() == null) {
            return List.of();
        }
        return m.materialTypes().stream()
                .map(mt -> new MaterialTypePair(
                        mt.materialTypeGeneral() != null ? mt.materialTypeGeneral().code() : null,
                        mt.materialTypeSpecific() != null ? mt.materialTypeSpecific().display() : null))
                .toList();
    }


    private FbiApiConnector.PromatElements fetchElements(String faust) throws FbiApiConnectorException {
        final Manifestation manifestation = fetchManifestation(faust);
        if (manifestation == null) {
            return null;
        }
        return toPromatElements(faust, manifestation);
    }

    private Manifestation fetchManifestation(String faust) throws FbiApiConnectorException {
        final String pid = AGENCY_ID + "-basis:" + faust;
        final ManifestationResponse response = connector.execute(QUERY, Map.of("pid", pid), ManifestationResponse.class);
        return response.manifestation();
    }

    private BibliographicInformation toBibliographicInformation(FbiApiConnector.PromatElements e) {
        return new BibliographicInformation()
                .withFaust(e.faust() != null ? e.faust().stream().findFirst().orElse("") : "")
                .withCreator(e.creator() != null && !e.creator().isEmpty()
                        ? String.join(", ", e.creator())
                        : "")
                .withDk5(e.dk5() != null ? e.dk5() : new ArrayList<>())
                .withIsbn(e.isbn() != null ? e.isbn() : new ArrayList<>())
                .withMaterialtypes(e.materialtypesDetail() != null && e.materialtypesDetail().type() != null
                        ? e.materialtypesDetail().type()
                        : new ArrayList<>())
                .withExtent(e.extent() != null ? e.extent().stream().findFirst().orElse("") : "")
                .withPublisher(e.publisher() != null ? String.join(", ", e.publisher()) : "")
                .withCatalogcodes(e.catalogcodes() != null && e.catalogcodes().code() != null
                        ? e.catalogcodes().code()
                        : new ArrayList<>())
                .withTitle(e.title() != null ? e.title().stream().findFirst().orElse("") : "")
                .withSeries(e.series() != null ? e.series() : new ArrayList<>())
                .withTargetgroup(e.targetgroup() != null ? e.targetgroup() : new ArrayList<>())
                .withMetakompassubject(e.metakompassubject() != null
                        ? e.metakompassubject().stream().findFirst().orElse("")
                        : "");
    }

    private FbiApiConnector.PromatElements toPromatElements(String faust, Manifestation m) {
        return new FbiApiConnector.PromatElements(
                List.of(faust),
                creators(m),
                dk5(m),
                isbn(m),
                new FbiApiConnector.PromatElements.TypeList(materialtypes(m)),
                new FbiApiConnector.PromatElements.TypeList(materialtypesDetail(m)),
                extent(m),
                publisher(m),
                edition(m),
                series(m),
                new FbiApiConnector.PromatElements.CodeList(catalogcodes(m)),
                title(m),
                targetgroup(m),
                metakompassubject(m));
    }

    private List<String> creators(Manifestation m) {
        if (m.creators() == null) {
            return List.of();
        }
        return m.creators().stream()
                .map(Creator::display)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> dk5(Manifestation m) {
        if (m.classifications() == null) {
            return List.of();
        }
        return m.classifications().stream()
                .filter(c -> "MAIN_ENTRY".equals(c.entryType()))
                .map(Classification::dk5Heading)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> isbn(Manifestation m) {
        if (m.identifiers() == null) {
            return List.of();
        }
        return m.identifiers().stream()
                .filter(i -> "ISBN".equals(i.type()))
                .map(Identifier::value)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> materialtypes(Manifestation m) {
        if (m.materialTypes() == null) {
            return List.of();
        }
        return m.materialTypes().stream()
                .map(mt -> mt.materialTypeGeneral() != null ? mt.materialTypeGeneral().code() : null)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> materialtypesDetail(Manifestation m) {
        if (m.materialTypes() == null) {
            return List.of();
        }
        return m.materialTypes().stream()
                .map(mt -> mt.materialTypeSpecific() != null ? mt.materialTypeSpecific().display() : null)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> extent(Manifestation m) {
        if (m.physicalDescription() == null || m.physicalDescription().summaryFull() == null) {
            return List.of();
        }
        return List.of(m.physicalDescription().summaryFull());
    }

    private List<String> publisher(Manifestation m) {
        return m.publisher() != null ? m.publisher() : List.of();
    }

    private List<String> edition(Manifestation m) {
        if (m.edition() == null || m.edition().edition() == null) {
            return List.of();
        }
        return List.of(m.edition().edition());
    }

    // Includes the number within the series when present, e.g. "Harry Potter, 3".
    private List<String> series(Manifestation m) {
        if (m.series() == null) {
            return List.of();
        }
        return m.series().stream()
                .map(s -> s.numberInSeries() != null
                        ? String.format("%s, %s", s.title(), s.numberInSeries())
                        : s.title())
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> catalogcodes(Manifestation m) {
        if (m.catalogueCodes() == null) {
            return List.of();
        }
        final List<String> codes = new ArrayList<>();
        if (m.catalogueCodes().nationalBibliography() != null) {
            codes.addAll(m.catalogueCodes().nationalBibliography());
        }
        if (m.catalogueCodes().otherCatalogues() != null) {
            codes.addAll(m.catalogueCodes().otherCatalogues());
        }
        return codes;
    }

    private List<String> title(Manifestation m) {
        if (m.titles() == null || m.titles().main() == null) {
            return List.of();
        }
        return m.titles().main();
    }

    private List<String> targetgroup(Manifestation m) {
        if (m.materialSelection() == null || m.materialSelection().selectionGroup() == null) {
            return List.of();
        }
        return m.materialSelection().selectionGroup().stream()
                .map(SelectionGroup::display)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<String> metakompassubject(Manifestation m) {
        final boolean hasDbcVerifiedSubjects = m.subjects() != null
                && m.subjects().dbcVerified() != null
                && !m.subjects().dbcVerified().isEmpty();
        return hasDbcVerifiedSubjects ? List.of(METAKOMPASDATA_PRESENT) : List.of();
    }

    private static String loadQuery() {
        return loadResource("/graphql/manifestationByPid.graphql");
    }

    private static String loadResource(String path) {
        try (InputStream is = FbiApiHandler.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("Unable to find " + path + " on classpath");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record ManifestationResponse(Manifestation manifestation) {}

    private record ComplexSearchResponse(ComplexSearch complexSearch) {}

    private record ComplexSearch(Integer hitcount, String errorMessage, List<Work> works) {}

    private record Work(WorkManifestations manifestations) {}

    private record WorkManifestations(List<Manifestation> bestRepresentations) {}

    private record Manifestation(
            String pid,
            List<Creator> creators,
            List<Classification> classifications,
            Edition edition,
            List<Identifier> identifiers,
            List<MaterialType> materialTypes,
            PhysicalDescription physicalDescription,
            List<String> publisher,
            CatalogueCodes catalogueCodes,
            Titles titles,
            MaterialSelection materialSelection,
            Subjects subjects,
            List<Series> series) {}

    private record Creator(String display) {}

    private record Classification(String dk5Heading, String entryType) {}

    private record Edition(String edition) {}

    private record Identifier(String type, String value) {}

    private record MaterialType(MaterialTypeCode materialTypeGeneral, MaterialTypeCode materialTypeSpecific) {}

    private record MaterialTypeCode(String code, String display) {}

    private record PhysicalDescription(String summaryFull) {}

    private record CatalogueCodes(List<String> nationalBibliography, List<String> otherCatalogues) {}

    private record Titles(List<String> main) {}

    private record MaterialSelection(List<SelectionGroup> selectionGroup) {}

    private record SelectionGroup(String display) {}

    private record Subjects(List<DbcVerifiedSubject> dbcVerified) {}

    private record DbcVerifiedSubject(String type, String display, String local) {}

    private record Series(String title, String numberInSeries) {}
}
