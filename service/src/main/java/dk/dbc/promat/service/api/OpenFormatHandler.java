package dk.dbc.promat.service.api;

import dk.dbc.promat.service.connectors.OpenFormatConnector;
import dk.dbc.promat.service.connectors.OpenFormatConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class OpenFormatHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenFormatHandler.class);
    private static final String OPENFORMAT_AGENCY = "870970";

    private OpenFormatConnector connector;

    public OpenFormatHandler() {}

    @Inject
    public OpenFormatHandler(OpenFormatConnector connector) {
        this.connector = connector;
    }

    public OpenFormatHandler withConnector(OpenFormatConnector connector) {
        this.connector = connector;
        return this;
    }

    public BibliographicInformation format(String faust) throws OpenFormatConnectorException {
        return format(faust, OPENFORMAT_AGENCY, BibliographicInformation.class);
    }

    public List<OpenFormatConnector.PromatElements> format(Set<String> fausts) throws OpenFormatConnectorException {
        List<OpenFormatConnector.PromatElements> result = new ArrayList<>();
        for (String faust : fausts) {
            OpenFormatConnector.PromatElements elements = format(faust, OPENFORMAT_AGENCY, OpenFormatConnector.PromatElements.class);
            if (elements != null) {
                result.add(elements);
            }
        }
        return result;
    }

    public <T> T format(String faust, Class<T> clazz) throws OpenFormatConnectorException {
        return format(faust, OPENFORMAT_AGENCY, clazz);
    }

    public <T> T format(String faust, String agency, Class<T> clazz) throws OpenFormatConnectorException {
        OpenFormatConnector.PromatElements elements = connector.format(faust, agency);

        if (elements == null) {
            LOGGER.error("No bibliographic information returned for faust {} in agency {}", faust, agency);
            if (clazz == BibliographicInformation.class) {
                return clazz.cast(new BibliographicInformation().withError("No results"));
            }
            return null;
        }

        if (clazz == OpenFormatConnector.PromatElements.class) {
            return clazz.cast(elements);
        }

        if (clazz == BibliographicInformation.class) {
            BibliographicInformation bibliographicInformation = toBibliographicInformation(elements);
            LOGGER.info("Returning bibliographic information: {}", bibliographicInformation);
            return clazz.cast(bibliographicInformation);
        }

        throw new IllegalArgumentException("Unsupported return type: " + clazz.getName());
    }

    private BibliographicInformation toBibliographicInformation(OpenFormatConnector.PromatElements e) {
        return new BibliographicInformation()
                .withFaust(e.faust() != null ? e.faust().stream().findFirst().orElse("") : "")
                .withCreator(e.creator() != null && !e.creator().isEmpty()
                        ? String.join(", ", e.creator())
                        : "")
                .withDk5(e.dk5() != null ? e.dk5() : new ArrayList<>())
                .withIsbn(e.isbn() != null ? e.isbn() : new ArrayList<>())
                .withMaterialtypes(e.materialtypes() != null && e.materialtypes().type() != null
                        ? e.materialtypes().type()
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
}
