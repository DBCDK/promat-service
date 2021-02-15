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
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.templating.model.AssignReviewer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class NotificationFactory {

    public class ValidateException extends Exception {
        ValidateException(String reason) {
            super(reason);
        }
    }

    @Inject
    OpenFormatHandler openFormatHandler;

    private final Renderer renderer = new Renderer();
    private static String subjectTemplate;
    static {
        try {
            subjectTemplate = Files.readString(
                    Path.of(Notification.class.getResource("/mail/subject.template").getPath()));
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    public Notification notificationOf(AssignReviewer model) throws ValidateException, OpenFormatConnectorException {
        Notification notification = new Notification();
        PromatCase promatCase = model.getPromatCase();

        // Lookup main faust and related fausts titles
        List<String> fausts = new ArrayList<>(List.of(promatCase.getPrimaryFaust()));
        if (promatCase.getRelatedFausts() != null) {
            fausts.addAll(promatCase.getRelatedFausts());
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


    private List<BibliographicInformation> getTitleSections(List<String> fausts) throws OpenFormatConnectorException {
        List<BibliographicInformation> titleSections = new ArrayList<>();
        for (String faust : fausts) {
            titleSections.add(openFormatHandler.format(faust));
        }
        return titleSections;
    }
}
