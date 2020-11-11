package dk.dbc.promat.service.templating;

import dk.dbc.promat.service.persistence.Case;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.Reviewer;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;


public class RenderTest {

    @Test
    public void simpleTest() {
        Render render = new Render();
        Case aCase = new Case()
                .withDeadline(LocalDate.of(2021, 1, 16))
                .withTitle("En ny spændende bog der skal anmeldes")
                .withReviewer(
                        new Reviewer()
                                .withFirstName("Hans")
                                .withLastName("Hansen")
                );

        Notification notification = render.mail("reviewer_assign_to_case.jte", aCase);
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
                "<p>Med velig hilsen,<br/>\n" +
                "ProMat redaktionen\n" +
                "</p>"));
    }

}
