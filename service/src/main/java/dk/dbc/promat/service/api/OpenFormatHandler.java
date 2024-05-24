package dk.dbc.promat.service.api;

import dk.dbc.connector.openformat.OpenFormatConnector;
import dk.dbc.connector.openformat.OpenFormatConnectorException;
import dk.dbc.connector.openformat.model.OpenFormatResponse;
import dk.dbc.connector.openformat.model.formats.Promat.PromatElements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;

@ApplicationScoped
public class OpenFormatHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenFormatHandler.class);
    private static final String OPENFORMAT_AGENCY = "870970";

    @Inject
    OpenFormatConnector connector;

    public OpenFormatHandler withConnector(OpenFormatConnector connector) {
        this.connector = connector;
        return this;
    }

    public BibliographicInformation format(String faust) throws OpenFormatConnectorException {
        return format(faust, OPENFORMAT_AGENCY);
    }

    public BibliographicInformation format(String faust, String agency) throws OpenFormatConnectorException {
        OpenFormatResponse<PromatElements> response = connector.format(faust, agency, PromatElements.class);

        if (response.hasError()) {
            LOGGER.error("Error when trying to obtain bibliographic information for faust {} in agency {}: {}",
                    faust, agency, String.join(",", response.getErrors()));

            return new BibliographicInformation().withError(
                    String.join(",", response.getErrors()));
        }

        // We only expect a single result since only 1 faustnumber is being looked up
        if (response.getElements() == null) {
            LOGGER.error("No bibliographic information returned for faust {} in agency {}", faust, agency);
            return new BibliographicInformation().withError("No results");
        }

        // Map the relatively complex openformat result to simple string values (or lists of strings)
        PromatElements elements = response.getElements();
        return new BibliographicInformation()
                .withFaust(elements.getFaust().stream().findFirst().orElse(""))
                .withCreator(elements.getCreator() != null && elements.getCreator().size() > 0
                        ? String.join(", ", elements.getCreator())
                        : "")
                .withDk5(elements.getDk5() != null && elements.getDk5().size() > 0
                        ? elements.getDk5()
                        : new ArrayList<>())
                .withIsbn(elements.getIsbn() != null && elements.getIsbn().size() > 0
                        ? elements.getIsbn()
                        : new ArrayList<>())
                .withMaterialtypes(elements.getMaterialTypes() != null && elements.getMaterialTypes().getType() != null
                        ? elements.getMaterialTypes().getType()
                        : new ArrayList<>())
                .withExtent(elements.getExtent().stream().findFirst().orElse(""))
                .withPublisher(String.join(", ", elements.getPublisher()))
                .withCatalogcodes(elements.getCatalogCodes() != null && elements.getCatalogCodes().getCode() != null
                        ? elements.getCatalogCodes().getCode()
                        : new ArrayList<>())
                .withTitle(elements.getTitle().stream().findFirst().orElse(""))
                .withTargetgroup(elements.getTargetGroup() != null
                        ? elements.getTargetGroup()
                        : new ArrayList<>())
                .withMetakompassubject(elements.getMetakompasSubject().stream().findFirst().orElse(""));
    }
}
