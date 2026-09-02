package dk.dbc.promat.service.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

// This class is the "business logic" layer on top of the raw FbiApiConnector:
// it knows what a manifestation is, builds the right GraphQL queries, and
// reshapes fbi-api's response into the domain types the rest of promat-service
// actually works with (BibliographicInformation, RecordInfo). Everything in
// this class is @ApplicationScoped, meaning CDI creates a single shared
// instance for the whole application, reused across all requests.
@ApplicationScoped
public class FbiApiHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FbiApiHandler.class);
    // fbi-api identifies a record by "pid" (e.g. "870970-basis:24699773"),
    // combining an agency id and a faust number. 870970 is the "common"
    // agency (DBC's shared/national bibliographic data), which is what all
    // of promat's lookups use.
    private static final String AGENCY_ID = "870970";
    private static final String METAKOMPASDATA_PRESENT = "true";
    // Query text is loaded once from a .graphql file on the classpath (see
    // loadResource below) and reused for every call - GraphQL query
    // documents are just strings sent in the POST body, so there's no need
    // to rebuild this text on every request. GraphQL lets a client ask for
    // exactly the fields it needs - see manifestationByPid.graphql - and
    // the "$pid" variable in that file gets its actual value at request
    // time (from fetchManifestation below), not hardcoded into the query
    // text. NOTE: the query's field list must line up exactly with the
    // `Manifestation` record (further down this file) that Jackson
    // deserializes the response into - AND several test fixtures under
    // service/src/test/resources/mappings/ match this exact query text
    // byte-for-byte, so editing the .graphql file itself (not just this
    // constant) requires updating those fixtures too.
    private static final String QUERY = loadQuery();

    private FbiApiConnector connector;

    // CDI beans need a no-arg constructor (the container uses it to create
    // a dynamic proxy around the bean, e.g. for @ApplicationScoped's
    // lazy-init behavior) *in addition to* the constructor CDI actually
    // calls to build the real instance (the one below, annotated @Inject).
    public FbiApiHandler() {}

    @Inject
    public FbiApiHandler(FbiApiConnector connector) {
        this.connector = connector;
    }

    // A "builder-style" setter used only by tests, to swap in a manually
    // constructed connector (e.g. pointed at a WireMock stub) instead of
    // waiting for CDI/@Inject to wire one up.
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

    // A single fetch, but two different possible "shapes" to return it as -
    // rather than duplicate the fetch-and-null-check logic twice, this takes
    // a Class<T> "token" and picks which mapping to apply based on it. This
    // is a slightly unusual pattern (most methods would just have two
    // separate names), kept here because both callers need the exact same
    // not-found handling.
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
     * Full bibliographic data for a single manifestation, plus the general/specific material
     * type pair resolved from that same manifestation fetch (so the two stay correctly paired,
     * unlike independently derived lists elsewhere in this class).
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
            String materialTypeGeneralCode,
            String materialTypeSpecificDisplay) {}

    // Used by RecordsProvider (GET /v1/api/records/{id}) to fetch full
    // bibliographic data for one or more known fausts, e.g. a case's target
    // fausts. One fbi-api round trip per faust - see toRecordInfo below for
    // where the actual field mapping happens.
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
        // No search terms given -> nothing to search for. Returning early
        // here (rather than sending an empty query to fbi-api) avoids an
        // unnecessary network call and a confusing error from the far end.
        if (cql == null) {
            return List.of();
        }
        final int effectiveLimit = limit == null ? MAX_SEARCH_LIMIT : Math.min(limit, MAX_SEARCH_LIMIT);
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

        // complexSearch groups hits by "work" (e.g. a novel), and each work
        // can have several "manifestations" (e.g. the paperback, the ebook,
        // the audiobook). fbi-api picks one representative manifestation per
        // work for us ("bestRepresentations") - we don't need our own logic
        // to choose which edition to show.
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

    // CQL (Contextual Query Language) is the query syntax fbi-api's search
    // expects, e.g. term.title='some title' AND term.creator='some author'.
    // We only add a clause for each field the caller actually supplied.
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

    // Search terms are user input dropped straight into a CQL string
    // literal. If a title contains a single quote (e.g. "Don't Look Now"),
    // an unescaped quote would end the string literal early and corrupt the
    // query. Doubling any embedded quote (' -> '') is the standard way to
    // escape a quote *inside* a quoted string - the same trick SQL uses.
    private static String cqlQuote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    // Manifestation.pid is the full pid, e.g. "870970-basis:24699773" - the faust
    // is the bibliographic record id portion after the last colon.
    private static String faustFromPid(String pid) {
        if (pid == null) {
            return null;
        }
        final int lastColon = pid.lastIndexOf(':');
        return lastColon >= 0 ? pid.substring(lastColon + 1) : pid;
    }

    // Builds a RecordInfo from one manifestation. Kept as a single shared
    // method (used by both recordInfo() and search() above) so the general
    // and specific material type always come from the *same* manifestation
    // fetch - see firstMaterialType's comment for why that matters.
    private RecordInfo toRecordInfo(String faust, Manifestation manifestation) {
        final MaterialTypeCode general = firstMaterialType(manifestation, false);
        final MaterialTypeCode specific = firstMaterialType(manifestation, true);
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
                general != null ? general.code() : null,
                specific != null ? specific.display() : null);
    }

    // A manifestation can list several materialTypes entries (e.g. a
    // combined print+ebook record), each with its own general/specific
    // pair. We only ever show one type in the UI, so we simplify by always
    // taking the first entry - `getFirst()` is a List method (Java 21+)
    // that's just a clearer way of writing `.get(0)`.
    private MaterialTypeCode firstMaterialType(Manifestation m, boolean specific) {
        if (m.materialTypes() == null || m.materialTypes().isEmpty()) {
            return null;
        }
        if (specific) {
            return m.materialTypes().getFirst().materialTypeSpecific();
        }
        return m.materialTypes().getFirst().materialTypeGeneral();
    }


    private FbiApiConnector.PromatElements fetchElements(String faust) throws FbiApiConnectorException {
        final Manifestation manifestation = fetchManifestation(faust);
        if (manifestation == null) {
            return null;
        }
        return toPromatElements(faust, manifestation);
    }

    // Every fbi-api call in this class (except search) goes through this one
    // method: build the pid from the faust, run the shared QUERY, and pull
    // the "manifestation" field out of the response. `response.manifestation()`
    // is null when fbi-api has no data for this faust yet - callers check
    // for that and fall back to whatever makes sense for them (e.g.
    // RecordsProvider falls back to rawrepo-record-service).
    private Manifestation fetchManifestation(String faust) throws FbiApiConnectorException {
        final String pid = AGENCY_ID + "-basis:" + faust;
        final ManifestationResponse response = connector.execute(QUERY, Map.of("pid", pid), ManifestationResponse.class);
        return response.manifestation();
    }

    // Maps fbi-api's PromatElements shape onto the BibliographicInformation
    // domain object that CaseInformationUpdater copies onto a PromatCase.
    // Note the `.stream().findFirst().orElse("")` pattern repeated below:
    // several fbi-api fields come back as a List<String> even though we only
    // ever want the first value, so this is a compact, null-safe way to say
    // "give me the first element, or an empty string if the list is missing
    // or empty" without an explicit null check + if/else for each field.
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

    // Repackages one manifestation into the older PromatElements shape (see
    // its javadoc in FbiApiConnector for why that shape exists at all).
    // Everything below this point (creators, dk5, isbn, ...) is a small
    // private helper that pulls one specific field out of the raw
    // GraphQL-shaped `Manifestation` and flattens it into a simple
    // List<String>, null-checking along the way since any nested field in
    // a GraphQL response can legitimately be missing.
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

    // A record can have several classifications (DK5 is the Danish library
    // classification system - like a subject/genre code). We only want the
    // primary one, marked "MAIN_ENTRY" by fbi-api, not every classification
    // ever attached to the record.
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

    // A book series has a title and, usually, this book's number within it
    // (e.g. "Harry Potter, 3"). We display both together when we have a
    // number, otherwise just the series title.
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
                .toList();
    }

    // This isn't a real fbi-api field - it's shoehorned into the same
    // List<String> shape as everything else in PromatElements purely so it
    // fits the "field-for-field" old-system-compatible record (see
    // PromatElements' javadoc). All it really carries is a yes/no: does
    // this record have subjects DBC has manually verified for Metakompas
    // (the taxonomy system)? CaseInformationUpdater checks for the literal
    // string "true" in this list elsewhere in the codebase.
    private List<String> metakompassubject(Manifestation m) {
        final boolean hasDbcVerifiedSubjects = m.subjects() != null
                && m.subjects().dbcVerified() != null
                && !m.subjects().dbcVerified().isEmpty();
        return hasDbcVerifiedSubjects ? List.of(METAKOMPASDATA_PRESENT) : List.of();
    }

    private static String loadQuery() {
        return loadResource("/graphql/manifestationByPid.graphql");
    }

    // GraphQL query documents are just text files here (see
    // service/src/main/resources/graphql/*.graphql) rather than being
    // embedded as Java string literals, so they're easier to read/edit and
    // get syntax highlighting in an editor. getResourceAsStream loads a file
    // bundled inside the deployed application (on the classpath) rather
    // than from the filesystem, which is why this works the same whether
    // running from an IDE, a test, or the packaged WAR.
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

    // Everything from here down is just plain data: these records mirror
    // the shape of fbi-api's GraphQL schema closely enough for Jackson to
    // deserialize a response straight into them. Compare each record's
    // fields against the corresponding .graphql query file in
    // service/src/main/resources/graphql/ to see where each one comes from.
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ManifestationResponse(Manifestation manifestation) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ComplexSearchResponse(ComplexSearch complexSearch) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ComplexSearch(Integer hitcount, String errorMessage, List<Work> works) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Work(WorkManifestations manifestations) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WorkManifestations(List<Manifestation> bestRepresentations) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Creator(String display) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Classification(String dk5Heading, String entryType) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Edition(String edition) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Identifier(String type, String value) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MaterialType(MaterialTypeCode materialTypeGeneral, MaterialTypeCode materialTypeSpecific) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MaterialTypeCode(String code, String display) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PhysicalDescription(String summaryFull) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CatalogueCodes(List<String> nationalBibliography, List<String> otherCatalogues) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Titles(List<String> main) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MaterialSelection(List<SelectionGroup> selectionGroup) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SelectionGroup(String display) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Subjects(List<DbcVerifiedSubject> dbcVerified) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DbcVerifiedSubject(String type, String display, String local) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Series(String title, String numberInSeries) {}
}
