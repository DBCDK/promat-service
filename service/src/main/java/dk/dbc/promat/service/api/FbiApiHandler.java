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

@ApplicationScoped
public class FbiApiHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FbiApiHandler.class);
    private static final String AGENCY_ID = "870970";
    private static final String METAKOMPASDATA_PRESENT = "true";
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

    public record MaterialTypeInfo(String faust, String code, String display) {}

    public List<MaterialTypeInfo> materialTypeInfo(Set<String> fausts) throws FbiApiConnectorException {
        final List<MaterialTypeInfo> result = new ArrayList<>();
        for (String faust : fausts) {
            final Manifestation manifestation = fetchManifestation(faust);
            if (manifestation == null) {
                continue;
            }
            final MaterialTypeCode general = firstMaterialType(manifestation, false);
            final MaterialTypeCode specific = firstMaterialType(manifestation, true);
            result.add(new MaterialTypeInfo(
                    faust,
                    general != null ? general.code() : null,
                    specific != null ? specific.display() : null));
        }
        return result;
    }

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
                List.of(),
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

    private List<String> metakompassubject(Manifestation m) {
        final boolean hasDbcVerifiedSubjects = m.subjects() != null
                && m.subjects().dbcVerified() != null
                && !m.subjects().dbcVerified().isEmpty();
        return hasDbcVerifiedSubjects ? List.of(METAKOMPASDATA_PRESENT) : List.of();
    }

    private static String loadQuery() {
        try (InputStream is = FbiApiHandler.class.getResourceAsStream("/graphql/manifestationByPid.graphql")) {
            if (is == null) {
                throw new IllegalStateException("Unable to find /graphql/manifestationByPid.graphql on classpath");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ManifestationResponse(Manifestation manifestation) {}

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
            Subjects subjects) {}

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
}
