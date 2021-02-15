/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.templating;

import com.github.tomakehurst.wiremock.WireMockServer;
import dk.dbc.connector.openformat.OpenFormatConnectorException;
import dk.dbc.connector.openformat.OpenFormatConnectorFactory;
import dk.dbc.promat.service.api.OpenFormatHandler;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.templating.model.AssignReviewer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;


public class RendererTest {
    private static final NotificationFactory notificationFactory = new NotificationFactory();
    private static WireMockServer wireMockServer;
    private final PromatCase aCase = new PromatCase()
            .withPrimaryFaust("48742238")
            .withDeadline(LocalDate.of(2021, 1, 16))
            .withTitle("TvekampenAsterix og briterne")
            .withReviewer(
                    new Reviewer()
                            .withFirstName("Hans")
                            .withLastName("Hansen")
                            .withEmail("hans@hansen.dk"))
            .withEditor(
                    new Editor()
                            .withFirstName("Kresten")
                            .withLastName("Krestensen")
                            .withEmail("kreste@krestense.dk"));

    @BeforeAll
    private static void startWiremock() throws OpenFormatConnectorException {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();

        notificationFactory.openFormatHandler = new OpenFormatHandler().withConnector(
                OpenFormatConnectorFactory.create(wireMockServer.baseUrl()));
    }

    @AfterAll
    private static void stopWiremock() {
        wireMockServer.stop();
    }

    @Test
    public void testReviewCollection() throws NotificationFactory.ValidateException, OpenFormatConnectorException, IOException {

        Notification notification = notificationFactory.notificationOf(new AssignReviewer()
                .withPromatCase(aCase.withRelatedFausts(List.of("47672201", "38582801", "51785347"))
                                .withTasks(List.of(new PromatTask()
                                .withTaskFieldType(TaskFieldType.METAKOMPAS))))
                .withNote("Du bedes udarbejde en samlet anmeldelse af materialerne. " +
                        "Bøgerne er kandidater til inddatering i Metabuggi. " +
                        "Du bedes afgøre om de er relevante for Buggi og i positiv fald tildele dem metadata."));
        String expected = stripTrailingAndLeading(
                Files.readString(
                        Path.of(RendererTest.class.getResource("/mailBodys/collectionReview.html").getPath())));
        String actual = stripTrailingAndLeading(notification.getBodyText());

        assertThat("Subject", notification.getSubject(), is("Ny ProMat anmeldelse:  Frist: 16/1 2021. - TvekampenAsterix og briterne"));
        assertThat("Mailtext", actual, is(expected));


        assertThat("Mail address", notification.getToAddress(), is("hans@hansen.dk"));
    }

    @Test
    public void testMailWithMaterialThatShouldBeDownloaded() throws OpenFormatConnectorException, NotificationFactory.ValidateException, IOException {
        Notification notification = notificationFactory.notificationOf(new AssignReviewer()
                .withPromatCase(aCase
                        .withFulltextLink("Alink")
                        .withTasks(List.of(new PromatTask()
                            .withTaskFieldType(TaskFieldType.METAKOMPAS)))));
        String expected = stripTrailingAndLeading(
                Files.readString(
                        Path.of(RendererTest.class.getResource("/mailBodys/printfileReview.html").getPath())));
        String actual = stripTrailingAndLeading(notification.getBodyText());

        assertThat("Mailtext", actual, is(expected));
    }

    @Test
    public void testMailWithMaterialEbookAndExpress() throws OpenFormatConnectorException, NotificationFactory.ValidateException, IOException {
        Notification notification = notificationFactory.notificationOf(new AssignReviewer()
                .withPromatCase(aCase
                        .withPrimaryFaust("48951147")
                        .withTasks(List.of(
                                new PromatTask()
                                .withTaskFieldType(TaskFieldType.METAKOMPAS),
                                new PromatTask()
                                .withTaskFieldType(TaskFieldType.EXPRESS)))));
        String expected = stripTrailingAndLeading(
                Files.readString(
                        Path.of(RendererTest.class.getResource("/mailBodys/ebookReview.html").getPath())));
        String actual = stripTrailingAndLeading(notification.getBodyText());

        assertThat("Mailtext", actual, is(expected));

        assertThat("Subject", notification.getSubject(), is("Ny ProMat anmeldelse: EKSPRES! Frist: 16/1 2021. - TvekampenAsterix og briterne"));
        assertThat("Mail address", notification.getToAddress(), is("hans@hansen.dk"));
    }

    private String stripTrailingAndLeading(String text) {
        return text.lines().map(String::strip).collect(Collectors.joining("\n"));
    }

}
