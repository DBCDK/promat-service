/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.templating;

import dk.dbc.connector.openformat.OpenFormatConnectorException;
import dk.dbc.promat.service.api.BibliographicInformation;
import dk.dbc.promat.service.api.OpenFormatHandler;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.NotificationStatus;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.templating.model.AssignReviewerNotification;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ejb.Stateless;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class NotificationFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationFactory.class);
    public class ValidateException extends Exception {
        ValidateException(String reason) {
            super(reason);
        }
    }

    @Inject
    OpenFormatHandler openFormatHandler;

    private final Renderer renderer = new Renderer();
    private final String AGENCY_ID = "870970";

    public Notification of(AssignReviewerNotification model) throws ValidateException, OpenFormatConnectorException {
        Notification notification = new Notification();
        PromatCase promatCase = model.getPromatCase();

        // Lookup main faust and related fausts titles
        List<String> fausts = new ArrayList<>(List.of(promatCase.getPrimaryFaust()));
        fausts.addAll(promatCase.getRelatedFausts());


        return notification
                .withToAddress(promatCase.getReviewer().getEmail())
                .withSubject("Ny promat anmeldelse")
                .withBodyText(renderer.render("reviewer_assign_to_case.jte", model.withTitleSections(getTitleSections(fausts))))
                .withStatus(NotificationStatus.PENDING);
    }


    private List<BibliographicInformation> getTitleSections(List<String> fausts) throws OpenFormatConnectorException {
        List<BibliographicInformation> titleSections = new ArrayList<>();
        for (String faust : fausts) {
            titleSections.add(openFormatHandler.format(faust));
        }
        return titleSections;
    }
}
