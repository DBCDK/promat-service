/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import dk.dbc.connector.openformat.OpenFormatConnector;
import dk.dbc.connector.openformat.OpenFormatConnectorException;
import dk.dbc.connector.openformat.model.formats.Promat.PromatElements;
import dk.dbc.connector.openformat.model.formats.Promat.PromatEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.stream.Collectors;

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
        PromatEntity entity = connector.format(faust, agency, PromatEntity.class);

        if (entity.getFormatResponse().getError().size() > 0) {
            LOGGER.error("Error when trying to obtain bibliographic information for faust {} in agency {}: {}",
                    faust, agency, entity.getFormatResponse().getError());

            return new BibliographicInformation().withError(
                    entity.getFormatResponse().getError().stream()
                    .map(error -> error.getMessage().getValue())
                    .collect(Collectors.joining(",")));
        }

        // We only expect a single result since only 1 faustnumber is being looked up
        if (entity.getFormatResponse().getPromat().size() == 0) {
            LOGGER.error("No bibliographic information returned for faust {} in agency {}", faust, agency);
            return new BibliographicInformation().withError("No results");
        }

        // Map the relatively complex openformat result to simple tring values (or lists of strings)
        PromatElements elements = entity.getFormatResponse().getPromat().get(0).getElements();
        return new BibliographicInformation()
        .withFaust(elements.getFaust().getValue())
        .withCreator(elements.getCreator() == null ? null : elements.getCreator().getValue())
        .withDk5(elements.getDk5().getValue())
        .withIsbn(elements.getIsbn() == null ? null : elements.getIsbn().stream()
                .map(isbn -> isbn.getValue())
                .collect(Collectors.toList()))
        .withMaterialtypes(elements.getMaterialtypes().getType().stream()
                .map(materialtype -> materialtype.getValue())
                .collect(Collectors.toList()))
        .withExtent(elements.getExtent() == null ? null : elements.getExtent().getValue())
        .withPublisher(elements.getPublisher().getValue())
        .withCatalogcodes(elements.getCatalogcodes().getCode().stream()
                .map(code -> code.getValue())
                .collect(Collectors.toList()))
        .withTitle(elements.getTitle().getValue())
        .withTargetgroup(elements.getTargetgroup().getValue());
    }
}
