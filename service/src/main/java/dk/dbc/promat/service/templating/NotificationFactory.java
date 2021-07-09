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
import dk.dbc.promat.service.templating.model.ChangedValue;
import dk.dbc.promat.service.templating.model.DeadlinePassedMail;
import dk.dbc.promat.service.templating.model.EarlyReminderMail;
import dk.dbc.promat.service.templating.model.MailToReviewerOnNewMessage;
import dk.dbc.promat.service.templating.model.ReviewerDataChanged;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private static final String subjectTemplate;
    private static final String subjectTemplateReviewerChanged;
    private static final String subjectTemplateNewMessageFromEditor;
    private static final String subjectReminderCloseToDeadline;
    private static final String subjectDeadlinePassed;

    static {
        try {
            subjectTemplate = Files.readString(
                    Path.of(Notification.class.getResource("/mail/subject.template").getPath()));
            subjectTemplateReviewerChanged = Files.readString(
                    Path.of(Notification.class.getResource("/mail/subject.reviewer.change.template").getPath()));
            subjectTemplateNewMessageFromEditor = Files.readString(
                    Path.of(Notification.class.getResource("/mail/subject.reviewer.new.message.template").getPath()));
            subjectReminderCloseToDeadline = Files.readString(
                    Path.of(Notification.class.getResource("/mail/subject.reminder.closetodeadline.template").getPath()));
            subjectDeadlinePassed = Files.readString(
                    Path.of(Notification.class.getResource( "/mail/subject.reminder.deadlinepassed.template").getPath()));

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
        List<String> fausts = collectFausts(promatCase);
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
            Map<String, ChangedValue> diffMap = reviewerDiffer.getChangedValueMap(reviewer, reviewerRequest);
            if (diffMap.isEmpty()) {
                LOGGER.info("Diff: No diff detected.");
                return null;
            }
            model.withDiff(diffMap);
            LOGGER.info("Diff: {}", model);
            return notification
                    .withToAddress(LU_MAILADDRESS)
                    .withSubject(subject)
                    .withBodyText(renderer.render("reviewer_data_changed.jte", model))
                    .withStatus(NotificationStatus.PENDING);

        } catch (IllegalAccessException e) {
            throw new ValidateException(e);
        }
    }

    public Notification notificationOf(MailToReviewerOnNewMessage model) throws ValidateException {
        Notification notification = new Notification();

        Reviewer reviewer = model.getPromatCase().getReviewer();
        String subject = String.format(subjectTemplateNewMessageFromEditor, model.getPromatCase().getTitle());
        String mailAddress = reviewer.getAddress().getSelected() ? reviewer.getEmail() : reviewer.getPrivateEmail();
        return notification
                .withToAddress(mailAddress)
                .withSubject(subject)
                .withBodyText(renderer.render("new_message_to_reviewer_mail.jte", model))
                .withStatus(NotificationStatus.PENDING);
    }

    public Notification notificationOf(EarlyReminderMail model) throws OpenFormatConnectorException {
        Notification notification = new Notification();
        List<String> fausts = collectFausts(model.getPromatCase());
        Reviewer reviewer = model.getPromatCase().getReviewer();
        String mailAddress = reviewer.getAddress().getSelected() ? reviewer.getEmail() : reviewer.getPrivateEmail();
        return notification
                .withToAddress(mailAddress)
                .withSubject(subjectReminderCloseToDeadline)
                .withBodyText(renderer.render("promatcase_near_deadline.jte",
                        model.withTitleSections(getTitleSections(fausts))))
                .withStatus(NotificationStatus.PENDING);
    }

    public Notification notificationOf(DeadlinePassedMail model) throws OpenFormatConnectorException {
        Notification notification = new Notification();
        List<String> fausts = collectFausts(model.getPromatCase());
        Reviewer reviewer = model.getPromatCase().getReviewer();
        String mailAddress = reviewer.getAddress().getSelected() ? reviewer.getEmail() : reviewer.getPrivateEmail();
        return notification
                .withToAddress(mailAddress)
                .withSubject(subjectDeadlinePassed)
                .withBodyText(renderer.render("promatcase_passed_deadline.jte",
                        model.withTitleSections(getTitleSections(fausts))))
                .withStatus(NotificationStatus.PENDING);
    }

    private List<BibliographicInformation> getTitleSections(List<String> fausts) throws OpenFormatConnectorException {
        List<BibliographicInformation> titleSections = new ArrayList<>();
        for (String faust : fausts) {
            titleSections.add(openFormatHandler.format(faust));
        }
        return titleSections;
    }

    private List<String> collectFausts(PromatCase promatCase) {
        List<String> fausts = new ArrayList<>(List.of(promatCase.getPrimaryFaust()));
        if (promatCase.getRelatedFausts() != null) {
            fausts.addAll(promatCase.getRelatedFausts()
                    .stream()
                    .filter(f -> !f.equals(promatCase.getPrimaryFaust())).collect(Collectors.toList()));
        }
        return fausts;
    }
}
