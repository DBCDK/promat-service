/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.templating;

import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.NotificationType;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.Reviewer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;


public class RendererTest {

    @Test
    public void simpleTest() throws NotificationFactory.ValidateException {
        PromatCase aCase = new PromatCase()
                .withDeadline(LocalDate.of(2021, 1, 16))
                .withTitle("En ny spændende bog der skal anmeldes")
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

        Notification notification = NotificationFactory.getInstance().of(NotificationType.CASE_ASSIGNED, aCase);
        assertThat("Mailtext", notification.getBodyText(), is(
                "<p><b>Kære Hans Hansen</b></p>\n" +
                "\n" +
                "<p>\n" +
                "Du er blevet bedt om at lave anmeldelse af følgende materiale:\n" +
                "</p>\n" +
                "<p><i>\"En ny spændende bog der skal anmeldes\"</i></p>\n" +
                "<p>Anmeldelsen bedes udarbejdet senest: 16/1 2021\n" +
                "<br/>\n" +
                "Materialet er på vej til dig i posten.</p>\n" +
                "<br/>\n" +
                "<p>Med venlig hilsen,<br/>\n" +
                "ProMat redaktionen\n" +
                "</p>"));

        assertThat("Subject", notification.getSubject(), is("Ny promat anmeldelse"));
        assertThat("Mail address", notification.getToAddress(), is("hans@hansen.dk"));
    }

}
