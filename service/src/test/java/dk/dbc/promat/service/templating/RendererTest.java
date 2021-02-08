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
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.templating.model.AssignReviewerNotification;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
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
    public void simpleTest() throws NotificationFactory.ValidateException, OpenFormatConnectorException, IOException {
        PromatCase aCase = new PromatCase()
                .withPrimaryFaust("48742238")
                .withRelatedFausts(List.of("47672201", "38582801", "51785347"))
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
                        .withEmail("kreste@krestense.dk")
                );
        Notification notification = notificationFactory.of(new AssignReviewerNotification().withPromatCase(aCase));
        Files.writeString(Paths.get("/tmp", "test.html"), notification.getBodyText());
        assertThat("Mailtext", notification.getBodyText(), is(
                "\n\n\n\n<p><b>Kære Hans Hansen</b></p>\n" +
                "<p>\n" +
                "Du er blevet bedt om at lave anmeldelse af følgende materiale:\n" +
                "</p>\n" +
                "<p><i>\"TvekampenAsterix og briterne\"</i></p>\n\n" +
                "<p>Anmeldelsen bedes udarbejdet senest: 16/1 2021\n" +
                "<br/>\n\n" +
                "Materialet er på vej til dig i posten.</p>\n" +
                "<br/>\n" +
                "<p>Med venlig hilsen,<br/>\n" +
                "ProMat redaktionen\n" +
                "</p>\n\n\n\n"));

        assertThat("Subject", notification.getSubject(), is("Ny promat anmeldelse"));
        assertThat("Mail address", notification.getToAddress(), is("hans@hansen.dk"));
    }

    }
