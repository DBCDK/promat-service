/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.templating;

import dk.dbc.opensearch.workpresentation.WorkPresentationConnector;
import dk.dbc.opensearch.workpresentation.WorkPresentationConnectorException;
import dk.dbc.opensearch.workpresentation.WorkPresentationQuery;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.NotificationStatus;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.templating.model.AssignReviewerNotification;
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
    WorkPresentationConnector workPresentationConnector;

    private final Renderer renderer = new Renderer();
    private final String AGENCY_ID = "870970";

    public Notification of(AssignReviewerNotification model) throws ValidateException {
        Notification notification = new Notification();
        PromatCase promatCase = model.getPromatCase();

        // Lookup related fausts titles
        List<String> relatedFausts = promatCase.getRelatedFausts();
        if (relatedFausts != null && !relatedFausts.isEmpty()) {
            Map titles = relatedFausts.stream()
                    .collect(Collectors.toMap(
                            faust -> faust,
                            this::getTitle));
            model.setRelatedFaustsTitles(titles);
        }
        if (promatCase.getEditor() == null || promatCase.getDeadline() == null) {
            throw new ValidateException("Editor or deadline is null");
        }

        return notification
                .withToAddress(promatCase.getReviewer().getEmail())
                .withSubject("Ny promat anmeldelse")
                .withBodyText(renderer.render("reviewer_assign_to_case.jte", model))
                .withStatus(NotificationStatus.PENDING);
    }

    private String getTitle(String faust) {
        try {
            LOGGER.info("About to do wpc");
            String title =  workPresentationConnector.presentWorks(
                new WorkPresentationQuery().withAgencyId(AGENCY_ID).withManifestation(faust)).getTitle();
            LOGGER.info("wpc done: {}", title);
            return title;
        } catch (WorkPresentationConnectorException e) {
            LOGGER.error("Related faust: '{}' not found.", faust);
            return "NO TITLE";
        }
    }
}
