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
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.PromatUser;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    @Inject
    @ConfigProperty(name = "CC_MAILADDRESS", defaultValue = "-")
    String CC_MAILADDRESS;

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
                .withToAddress(compileMailAddressesForReviewerMails(promatCase.getReviewer()))
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

        String subject = String.format(subjectTemplateNewMessageFromEditor, model.getPromatCase().getTitle());
        return notification
                .withToAddress(compileMailAddressesForReviewerMails(model.getPromatCase().getReviewer()))
                .withSubject(subject)
                .withBodyText(renderer.render("new_message_to_reviewer_mail.jte", model))
                .withStatus(NotificationStatus.PENDING);
    }

    public Notification notificationOf(EarlyReminderMail model) throws OpenFormatConnectorException, ValidateException {
        Notification notification = new Notification();
        List<String> fausts = collectFausts(model.getPromatCase());

        return notification
                .withToAddress(compileMailAddressesForReviewerMails(model.getPromatCase().getReviewer()))
                .withSubject(subjectReminderCloseToDeadline)
                .withBodyText(renderer.render("promatcase_near_deadline.jte",
                        model.withTitleSections(getTitleSections(fausts))))
                .withStatus(NotificationStatus.PENDING);
    }

    public Notification notificationOf(DeadlinePassedMail model) throws OpenFormatConnectorException, ValidateException {
        Notification notification = new Notification();
        List<String> fausts = collectFausts(model.getPromatCase());

        return notification
                .withToAddress(compileMailAddressesForReviewerMails(model.getPromatCase().getReviewer()))
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
        String faust = promatCase.getPrimaryFaust();
        if(promatCase.getTasks() != null) {
            return Stream.concat(Stream.of(faust),
                    promatCase.getTasks().stream()
                            .map(PromatTask::getTargetFausts)
                            .filter(Objects::nonNull)
                            .flatMap(Collection::stream)
            ).distinct().sorted().collect(Collectors.toList());
        }
        return List.of(faust);
    }

    private String compileMailAddressesForReviewerMails(Reviewer reviewer) throws ValidateException {
        Predicate<String> isValid = s -> Objects.nonNull(s) && !s.isBlank() && !s.equals("-");
        List<String> addresses = Stream.of(reviewer.getEmail(), reviewer.getPrivateEmail()).filter(isValid).collect(Collectors.toCollection(ArrayList::new));
        if (addresses.isEmpty()) {
            throw new ValidateException("Email address for reviewer " + reviewer.getId() + ", and private email addresses cannot both be unassigned.");
        }
        if (isValid.test(CC_MAILADDRESS)) {
            addresses.add(CC_MAILADDRESS);
        }
        return String.join(",", addresses);
    }
}
