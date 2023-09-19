package dk.dbc.promat.service.templating;

import dk.dbc.promat.service.api.ServiceErrorException;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PayCategory;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.persistence.TaskType;
import dk.dbc.promat.service.templating.model.XmlCaseview.XmlCaseview;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class XmlCaseViewTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(XmlCaseViewTest.class);

    @Test
    public void testXmlCaseView() throws ServiceErrorException, IOException {
        final PromatCase promatCase = new PromatCase()
                .withPrimaryFaust("123456789")
                .withDeadline(LocalDate.now())
                .withTitle("title")
                .withAssigned(LocalDate.now())
                .withDetails("details")
                .withId(123)
                .withAuthor("author")
                .withCodes(List.of("BKM202338", "BKX202337"))
                .withCreated(LocalDate.now())
                .withFulltextLink("https:://full.text.link")
                .withMaterialType(MaterialType.BOOK)
                .withNote("note")
                .withPublisher("Publisher")
                .withStatus(CaseStatus.APPROVED)
                .withReviewer(
                        new Reviewer()
                                .withFirstName("Re")
                                .withLastName("Viewer")
                                .withEmail("re@wiever.dk"))
                .withEditor(
                        new Editor()
                                .withFirstName("An")
                                .withLastName("Melder")
                                .withEmail("and@melder.dk"))
                .withTasks(List.of(new PromatTask()
                        .withTargetFausts(List.of("123456789", "456789123"))
                        .withRecordId("789123456")
                        .withData("data")
                        .withApproved(LocalDate.now())
                        .withCreated(LocalDate.now().minusWeeks(1))
                        .withId(456)
                        .withTaskType(TaskType.GROUP_2_100_UPTO_199_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withPayCategory(PayCategory.BRIEF)));

        CaseviewXmlTransformer transformer = new CaseviewXmlTransformer(false);

        // Check exact generated xml structure
        XmlCaseview caseview = transformer.toCaseView("123456789", promatCase);
        caseview.getCaseviewResponse().setDatetime(LocalDateTime.parse("2023-09-19T10:53:42.831525"));
        String actual = new String(transformer.toXml(caseview), StandardCharsets.ISO_8859_1).trim();
        String expected = new String(Files.readAllBytes(Path.of("src/test/resources/__files/case-view-for-id-123-123456789.xml"))).trim();
        assertThat("Xml is correct", actual, is(expected));

        // Check that tests using only textual content will still be valid
        String actualText = Jsoup.parse(actual).text();
        String expectedText = Jsoup.parse(expected).text();
        assertThat("Xml text is correct", actualText, is(expectedText));
    }
}
