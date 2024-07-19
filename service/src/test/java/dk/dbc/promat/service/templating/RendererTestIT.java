package dk.dbc.promat.service.templating;

import com.github.tomakehurst.wiremock.WireMockServer;
import dk.dbc.connector.openformat.OpenFormatConnectorException;
import dk.dbc.connector.openformat.OpenFormatConnectorFactory;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.api.OpenFormatHandler;
import dk.dbc.promat.service.dto.ReviewerRequest;
import dk.dbc.promat.service.persistence.Address;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.templating.model.AssignReviewer;
import dk.dbc.promat.service.templating.model.ChangedValue;
import dk.dbc.promat.service.templating.model.ReviewerDataChanged;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class RendererTestIT extends ContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RendererTestIT.class);
    private static final NotificationFactory notificationFactory = new NotificationFactory();
    private static WireMockServer wireMockServer;
    private final PromatCase aCase = new PromatCase()
            .withPrimaryFaust("48742238")
            .withDeadline(LocalDate.of(2021, 1, 16))
            .withTitle("Tvekampen. Asterix og briterne")
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
    public static void startWiremock() throws OpenFormatConnectorException {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();

        notificationFactory.openFormatHandler = new OpenFormatHandler().withConnector(
                OpenFormatConnectorFactory.create(ContainerTest.getOpenFormatBaseUrl(wireMockServer.baseUrl() + "/api/v2")));
        notificationFactory.reviewerDiffer = new ReviewerDiffer();
        notificationFactory.LU_MAILADDRESS = "TEST@dbc.dk";
        notificationFactory.CC_MAILADDRESS = "cc_test@dbc.dk";
    }

    @AfterAll
    public static void stopWiremock() {
        wireMockServer.stop();
    }

    @Test
    public void testReviewCollection() throws NotificationFactory.ValidateException, OpenFormatConnectorException, IOException {
        String NOTE = "Du bedes udarbejde en samlet anmeldelse af materialerne.<br/> " +
                "Bøgerne er kandidater til inddatering i Metabuggi. " +
                "Du bedes afgøre om de er relevante for Buggi og i positiv fald tildele dem metadata.";

        Notification notification = notificationFactory.notificationOf(new AssignReviewer()
                .withPromatCase(aCase.withRelatedFausts(List.of("47672201", "38582801", "51785347"))
                                .withTasks(List.of(new PromatTask()
                                .withTaskFieldType(TaskFieldType.METAKOMPAS)
                                        .withTargetFausts(List.of("47672201", "38582801", "51785347")))))
                .withNote(NOTE));
        String expected = stripTrailingAndLeading(
                Files.readString(
                        Path.of(RendererTestIT.class.getResource("/mailBodys/collectionReview.html").getPath())));
        String actual = stripTrailingAndLeading(notification.getBodyText());

        assertThat("Subject", notification.getSubject(), is("Ny ProMat anmeldelse:  Frist: 16/1 2021. - Tvekampen. Asterix og briterne"));
        assertThat("Mailtext", actual, is(expected));

        assertThat("Mail address", notification.getToAddress(),
                is(String.join(",","hans@hansen.dk", notificationFactory.CC_MAILADDRESS)));
        assertThat( "Note", notification.getBodyText().contains(NOTE));
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
                        Path.of(RendererTestIT.class.getResource("/mailBodys/printfileReview.html").getPath())));
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
                        Path.of(RendererTestIT.class.getResource("/mailBodys/ebookReview.html").getPath())));
        String actual = stripTrailingAndLeading(notification.getBodyText());

        assertThat("Mailtext", actual, is(expected));

        assertThat("Subject", notification.getSubject(), is("Ny ProMat anmeldelse: EKSPRES! Frist: 16/1 2021. - Tvekampen. Asterix og briterne"));
        assertThat("Mail address", notification.getToAddress(), is(String.join(",",
                "hans@hansen.dk", notificationFactory.CC_MAILADDRESS)));
    }

    @Test
    public void reviewerChangedCoreData() throws IllegalAccessException, NoSuchFieldException {
        Reviewer reviewer = new Reviewer()
                .withFirstName("hans")
                .withLastName("Hansen")
                .withPrivateAddress(new Address().withAddress2("Snerrebunden 44").withZip("7777"))
                .withPrivateEmail("hans@hansen.dk");
        LocalDate somedaysahead = LocalDate.now().plusDays(3);
        LocalDate alittlelater = LocalDate.now().plusDays(5);
        ReviewerRequest reviewerRequest = new ReviewerRequest()
                .withFirstName("Hans")
                .withLastName("Hansen")
                .withHiatusBegin(somedaysahead)
                .withHiatusEnd(alittlelater)
                .withPaycode(123456)
                .withActive(true)
                .withAddress(new Address().withAddress1("Snevej 1").withSelected(true))
                .withPrivateAddress(new Address().withAddress2("Snurretoppen 88"))
                .withPrivateEmail("hans@ny.hansen.dk");
        Map<String, ChangedValue> actual =
                new ReviewerDiffer().getChangedValueMap(reviewer, reviewerRequest);
        Map<String, ChangedValue> expected = Map.of(
                "firstName",
                new ChangedValue()
                        .withFromValue("hans")
                        .withToValue("Hans"),
                "hiatusBegin",
                new ChangedValue()
                        .withFromValue(null)
                        .withToValue(Formatting.format(somedaysahead)),
                "hiatusEnd",
                new ChangedValue()
                        .withFromValue(null)
                        .withToValue(Formatting.format(alittlelater)),
                "paycode",
                new ChangedValue()
                        .withFromValue(null)
                        .withToValue("123456"),
                "active",
                new ChangedValue()
                        .withFromValue("false")
                        .withToValue("true"),
                "address1",
                new ChangedValue()
                        .withFromValue(null)
                        .withToValue("Snevej 1"),
                "privateAddress2",
                new ChangedValue()
                        .withFromValue("Snerrebunden 44")
                        .withToValue("Snurretoppen 88"),
                "selected",
                new ChangedValue()
                        .withFromValue(null)
                        .withToValue(true+""),
                "privateEmail",
                new ChangedValue()
                        .withFromValue("hans@hansen.dk")
                        .withToValue("hans@ny.hansen.dk"));
        assertThat("change detected", actual.size(), is(expected.size()));
        for(String changed : expected.keySet()) {
            assertThat(changed, actual.containsKey(changed));
            assertThat("value", actual.get(changed), is(expected.get(changed)));
        }
    }

    @Test
    public void reviewerDataChangedMail() throws NotificationFactory.ValidateException, IOException {
        Reviewer reviewer = new Reviewer()
                .withId(1001)
                .withPaycode(1001)
                .withEmail("m@olsen.dk")
                .withFirstName("Mikeller").withLastName("Olsen")
                .withAddress(new Address()
                        .withAddress1("Fælledvej 393, 8.th")
                        .withZip("2200")
                        .withCity("København N"))
                .withPrivateAddress(
                        new Address()
                                .withAddress1("Rådhuspladsen 1")
                                .withZip("1550"))
                .withPrivateEmail("m-privat@olsen.dk");
        ReviewerRequest reviewerRequest = new ReviewerRequest()
                .withAddress(new Address()
                        .withAddress1("Frederiksgårds Allé").withAddress2("166 B, 19.mf")
                        .withZip("2720")
                        .withCity("Vanløse"))
                .withEmail("mikeller@olsen.dk")
                .withPrivateEmail("mulle-privat@olsen.dk")
                .withPrivateAddress(
                        new Address()
                                .withAddress1("Rungsted Kystvej 199")
                                .withAddress2("Postbox 40")
                                .withCity("Rungsted")
                                .withZip("2930"));
        Notification notification = notificationFactory.notificationOf(
                new ReviewerDataChanged()
                .withReviewer(reviewer)
                .withReviewerRequest(reviewerRequest));
        String actual = stripTrailingAndLeading(notification.getBodyText());
        String expected = stripTrailingAndLeading(
                Files.readString(Path.of(RendererTestIT.class.getResource("/mailBodys/reviewerDataChanged.html").getPath()))
        );
        assertThat("Bodytext", actual, is(expected));
        assertThat("Subject", notification.getSubject(), is("ProMat anmelderprofil '1001' er ændret"));
    }

    private String stripTrailingAndLeading(String text) {
        return text.lines().map(String::strip).collect(Collectors.joining("\n"));
    }



}
