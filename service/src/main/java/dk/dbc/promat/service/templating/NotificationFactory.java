/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.templating;

import dk.dbc.connector.openformat.OpenFormatConnectorException;
import dk.dbc.promat.service.api.BibliographicInformation;
import dk.dbc.promat.service.api.OpenFormatHandler;
import dk.dbc.promat.service.dto.ReviewerRequest;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.NotificationStatus;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.templating.model.AssignReviewer;
import dk.dbc.promat.service.templating.model.ReviewerDataChanged;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.ejb.Stateless;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class NotificationFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationFactory.class);

    public class ValidateException extends Exception {
        ValidateException(String reason) {
            super(reason);
        }
        ValidateException(Exception e) {
            super(e);
        }
    }

    @Inject
    OpenFormatHandler openFormatHandler;

    @Inject
    ReviewerDiffer reviewerDiffer;

    @Inject
    @ConfigProperty(name = "LU_MAILADDRESS")
    String LU_MAILADDRESS;

    private final Renderer renderer = new Renderer();
    private static String subjectTemplate;
    private static String subjectTemplateReviewerChanged;
    static {
        try {
            subjectTemplate = Files.readString(
                    Path.of(Notification.class.getResource("/mail/subject.template").getPath()));
            subjectTemplateReviewerChanged = Files.readString(
                    Path.of(Notification.class.getResource("/mail/subject.reviewer.change.template").getPath()));
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    public Notification notificationOf(AssignReviewer model) throws ValidateException, OpenFormatConnectorException {
        Notification notification = new Notification();
        PromatCase promatCase = model.getPromatCase();

        // Lookup main faust and related fausts titles
        // For some reason frontend posts a related-faust equal to the primary
        // one.
        List<String> fausts = new ArrayList<>(List.of(promatCase.getPrimaryFaust()));
        if (promatCase.getRelatedFausts() != null) {
            fausts.addAll(promatCase.getRelatedFausts()
                    .stream()
                    .filter(f -> !f.equals(promatCase.getPrimaryFaust())).collect(Collectors.toList()));
        }
        String subject = String.format(subjectTemplate,
                (promatCase.getTasks().stream().anyMatch(c -> c.getTaskFieldType() == TaskFieldType.EXPRESS)
                        ? "EKSPRES!" : ""),
                Formatting.format(promatCase.getDeadline()),
                promatCase.getTitle());
        return notification
                .withToAddress(promatCase.getReviewer().getEmail())
                .withSubject(subject)
                .withBodyText(renderer.render("reviewer_assign_to_case.jte",
                        model.withTitleSections(getTitleSections(fausts))))
                .withStatus(NotificationStatus.PENDING);
    }

    public Notification notificationOf(ReviewerDataChanged model) throws ValidateException {
        Notification notification = new Notification();

        Reviewer reviewer = model.getReviewer();
        ReviewerRequest reviewerRequest = model.getReviewerRequest();
        String subject = String.format(subjectTemplateReviewerChanged, reviewer.getId());
        try {
            model.withDiff(reviewerDiffer.getChangedValueMap(reviewer, reviewerRequest));
            LOGGER.info("Diff: {}", model);
            return notification
                    .withToAddress(LU_MAILADDRESS)
                    .withSubject(subject)
                    .withBodyText(renderer.render("reviewer_data_changed.jte", model));

        } catch (IllegalAccessException e) {
            throw new ValidateException(e);
        }
    }


    private List<BibliographicInformation> getTitleSections(List<String> fausts) throws OpenFormatConnectorException {
        List<BibliographicInformation> titleSections = new ArrayList<>();
        for (String faust : fausts) {
            titleSections.add(openFormatHandler.format(faust));
        }
        return titleSections;
    }
}
