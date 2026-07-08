package dk.dbc.promat.service.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.opennumberroll.OpennumberRollConnectorException;
import dk.dbc.promat.service.Repository;
import dk.dbc.promat.service.api.BibliographicInformation;
import dk.dbc.promat.service.connectors.OpenFormatConnectorException;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.dto.TaskDto;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.persistence.TaskType;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

/**
 * Tests that CaseInformationUpdater correctly syncs title/author/weekcode/catalog codes
 * from OpenFormat bibliographic data onto a case.
 */
public class CaseInformationUpdaterBibliographicSyncIT extends CaseInformationUpdaterTestBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseInformationUpdaterBibliographicSyncIT.class);

    @Test
    public void testUpdateCaseWithWeekcode() throws JsonProcessingException {

        // Create a case with incorrect title and weekcode
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("24699773")
                .withTitle("Title for 24699773")
                .withWeekCode("DPF202002")
                .withDetails("Details for 24699773")
                .withMaterialType(MaterialType.BOOK)
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1);

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        ScheduledCaseInformationUpdater upd = configure();
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));

        assertThat("title is correct", created.getTitle(), is("Den lukkede bog"));
        assertThat("weekcode is correct", created.getWeekCode(), is("BKM201105"));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseWithNullWeekcode() throws JsonProcessingException, OpenFormatConnectorException {

        // Create a case with no weekcode
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("24699773")
                .withTitle("Title for 24699773")
                .withDetails("Details for 24699773")
                .withMaterialType(MaterialType.BOOK)
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1);

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        ScheduledCaseInformationUpdater upd = configure();
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));

        assertThat("weekcode is correct", created.getWeekCode(), is("BKM201105"));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseWithEmptyWeekcode() throws JsonProcessingException, OpenFormatConnectorException {

        // Create a case with no weekcode
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("24699773")
                .withTitle("Title for 24699773")
                .withWeekCode("")
                .withDetails("Details for 24699773")
                .withMaterialType(MaterialType.BOOK)
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1);

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        ScheduledCaseInformationUpdater upd = configure();
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));

        assertThat("weekcode is correct", created.getWeekCode(), is("BKM201105"));

        // Delete the case so that we dont mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseRemoveWeekcode() throws JsonProcessingException, OpenFormatConnectorException {

        // Create a case with no weekcode
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("22677780")
                .withTitle("Title for 22677780")
                .withWeekCode("DPF202002")
                .withDetails("Details for 22677780")
                .withMaterialType(MaterialType.BOOK)
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1);

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        ScheduledCaseInformationUpdater upd = configure();
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));

        assertThat("weekcode is correct", created.getWeekCode(), is("BKM201102"));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateWithNoWeekcode() throws JsonProcessingException, OpenFormatConnectorException {

        // Create a case with no weekcode
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("23319322")
                .withTitle("Title for 23319322")
                .withWeekCode("NOP000000")
                .withDetails("Details for 23319322")
                .withMaterialType(MaterialType.BOOK)
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1);

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        ScheduledCaseInformationUpdater upd = configure();
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));

        assertThat("weekcode is removed", created.getWeekCode(), is(""));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateWithPublisherAsArray() throws JsonProcessingException, OpenFormatConnectorException {

        // Create a case with no weekcode
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("38600052")
                .withTitle("Title for 38600052")
                .withWeekCode("NOP000000")
                .withAuthor("Author for 38600052")
                .withDetails("Details for 38600052")
                .withMaterialType(MaterialType.BOOK)
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1);

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        ScheduledCaseInformationUpdater upd = configure();
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));

        assertThat("title is updated", created.getTitle().equals("Præsidentjagt"));
        assertThat("weekcode is updated", created.getWeekCode().equals("BKM202111"));
        assertThat("author is updated", created.getAuthor().equals("Duggan, Gerry"));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseWithNoCatalogCodes() throws OpenFormatConnectorException, JsonProcessingException, OpennumberRollConnectorException {

        // Create a case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("24699773")
                .withTitle("Title for 24699773")
                .withWeekCode("BKM202002")
                .withDetails("Details for 24699773")
                .withMaterialType(MaterialType.BOOK)
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1)
                .withTasks(Collections.singletonList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(List.of("24699773"))
                ));

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        ScheduledCaseInformationUpdater upd = configure();
        upd.caseInformationUpdater.openFormatHandler = mockOpenFormat(new BibliographicInformation()
                .withCatalogcodes(null));
        Repository mockedRepository = mock(Repository.class);
        upd.caseInformationUpdater.repository = mockedRepository;
        doNothing().when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        LOGGER.info("codes: {}", created.getCodes());
        assertThat("codes exists", created.getCodes(), is(nullValue()));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseWithManyCatalogCodes() throws OpenFormatConnectorException, JsonProcessingException, OpennumberRollConnectorException {

        // Create a case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("24699773")
                .withTitle("Title for 24699773")
                .withWeekCode("BKM202002")
                .withDetails("Details for 24699773")
                .withMaterialType(MaterialType.BOOK)
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1)
                .withTasks(Collections.singletonList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(List.of("24699773"))
                ));

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        ScheduledCaseInformationUpdater upd = configure();
        upd.caseInformationUpdater.openFormatHandler = mockOpenFormat(new BibliographicInformation()
                .withCatalogcodes(Arrays.asList("FFK20210603", "BKM20210603", "bkx20210602", "ACC20210601")));
        Repository mockedRepository = mock(Repository.class);
        upd.caseInformationUpdater.repository = mockedRepository;
        doNothing().when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("codes exists", created.getCodes(), is(notNullValue()));
        assertThat("codes contains", created.getCodes().stream()
                        .sorted().collect(Collectors.toList()),
                is(Arrays.asList("ACC20210601", "BKM20210603", "BKX20210602", "FFK20210603")));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }
}
