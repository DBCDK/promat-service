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
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the weekcode-precedence (BKM/BKX/FFK) and date-math logic that decides whether
 * and when CaseInformationUpdater moves a case into PENDING_EXPORT.
 */
public class CaseInformationUpdaterExportTimingIT extends CaseInformationUpdaterTestBase {

    @Test
    public void testUpdateCaseWithPendingExportForPrehistoricWeekcode() throws JsonProcessingException, OpennumberRollConnectorException, OpenFormatConnectorException {

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
                .withReviewer(1).withTasks(Collections.singletonList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(List.of("24699773"))
                ));

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        ScheduledCaseInformationUpdater upd = configure();
        upd.caseInformationUpdater.openFormatHandler = mockOpenFormat(new BibliographicInformation()
                .withCatalogcodes(Arrays.asList("BKM202001", "ACC202001")));
        upd.caseInformationUpdater.repository = mockRepositoryAssigningRecordId("123456789");

        created.setStatus(CaseStatus.APPROVED);

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));
        created.getTasks().forEach(t -> assertThat("recordId", t.getRecordId(), is("123456789")));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseWithPendingExportForCurrentWeekcode() throws OpenFormatConnectorException, JsonProcessingException, OpennumberRollConnectorException {

        // Create a case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("24699773")
                .withTitle("Title for 24699773")
                .withWeekCode("BMK202002")
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
        LocalDate date = LocalDate.now().plusWeeks(1);
        upd.caseInformationUpdater.openFormatHandler = mockOpenFormat(new BibliographicInformation()
                .withCatalogcodes(Arrays.asList(weekcode("BKM", date), "ACC202001")));
        upd.caseInformationUpdater.repository = mockRepositoryAssigningRecordId("123456789");

        created.setStatus(CaseStatus.APPROVED);

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));
        created.getTasks().forEach(t -> assertThat("recordId", t.getRecordId(), is("123456789")));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseWithPendingExportForNextWeeksWeekcode() throws OpenFormatConnectorException, OpennumberRollConnectorException {

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
                .withReviewer(1).withTasks(Collections.singletonList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(List.of("24699773"))
                ));

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        ScheduledCaseInformationUpdater upd = configure();

        // Get weekcode 2 weeks into the future - and check for end-of-year rollaround which can cause
        // trouble for the 'ww' weekcode formatter when used with a fixed 'yyyy' year
        LocalDate date = LocalDate.now().plusWeeks(2);
        upd.caseInformationUpdater.openFormatHandler = mockOpenFormat(new BibliographicInformation()
                .withCatalogcodes(Arrays.asList(weekcode("BKM", date), "ACC202001")));
        upd.caseInformationUpdater.repository = mockRepositoryAssigningRecordId("123456789");

        created.setStatus(CaseStatus.APPROVED);

        // BKM is next-next week so PENDING_EXPORT should change back to APPROVED since it must wait another week

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.APPROVED));
        created.getTasks().forEach(t -> assertThat("recordId", t.getRecordId(), is(nullValue())));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseWithNoWeekcode() throws OpenFormatConnectorException {

        // Create a case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("24699773")
                .withTitle("Title for 24699773")
                .withWeekCode("")
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
                .withCatalogcodes(List.of("ACC202001")));
        upd.caseInformationUpdater.repository = mock(Repository.class);

        created.setStatus(CaseStatus.APPROVED);

        Counter mockedCounter = mock(Counter.class);
        doAnswer(answer -> mockedCounter).when(metricRegistry).counter(any(Metadata.class));

        AtomicInteger errors = new AtomicInteger(0);
        doAnswer(answer -> {
            errors.getAndIncrement();
            return null;
        }).when(mockedCounter).inc();

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));

        assertThat("no errors", errors.get(), is(0));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseWithApprovedForBKMWeekcode() throws OpenFormatConnectorException, OpennumberRollConnectorException {

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
        LocalDate date = LocalDate.now().minusWeeks(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyww", dkLocale);
        upd.caseInformationUpdater.openFormatHandler = mockOpenFormat(new BibliographicInformation()
                .withCatalogcodes(Arrays.asList("BKM" + date.format(formatter), "BKX299999", "FFK299999", "ACC202001")));
        upd.caseInformationUpdater.repository = mockRepositoryAssigningRecordId("123456789");

        created.setStatus(CaseStatus.APPROVED);

        // BKM is previous week, but BKX and FFK is in the future, and since BKX or FFK takes precedence, status should not change

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.APPROVED));
        created.getTasks().forEach(t -> assertThat("recordId", t.getRecordId(), is(nullValue())));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseWithApprovedForBKMAndBkxWeekcode() throws OpenFormatConnectorException, OpennumberRollConnectorException {

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
        LocalDate date = LocalDate.now().minusWeeks(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyww", dkLocale);
        upd.caseInformationUpdater.openFormatHandler = mockOpenFormat(new BibliographicInformation()
                .withCatalogcodes(Arrays.asList("BKM" + date.format(formatter), "BKX" + date.format(formatter), "FFK299999", "ACC202001")));
        upd.caseInformationUpdater.repository = mockRepositoryAssigningRecordId("123456789");

        created.setStatus(CaseStatus.APPROVED);

        // BKM and BKX is previous week, but FFK is in the future, and since FFK takes precedence, status should not change

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.APPROVED));
        created.getTasks().forEach(t -> assertThat("recordId", t.getRecordId(), is(nullValue())));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseWithPendingExportForBKXWeekcode() throws OpenFormatConnectorException, OpennumberRollConnectorException {

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
        LocalDate date = LocalDate.now().minusWeeks(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyww", dkLocale);
        upd.caseInformationUpdater.openFormatHandler = mockOpenFormat(new BibliographicInformation()
                .withCatalogcodes(Arrays.asList("BKX" + date.format(formatter), "BKM299999", "ACC202001")));
        upd.caseInformationUpdater.repository = mockRepositoryAssigningRecordId("123456789");

        created.setStatus(CaseStatus.APPROVED);

        // BKX is previous week, but BKM is in the future, since BKX takes precedence, status should change

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));
        created.getTasks().forEach(t -> assertThat("recordId", t.getRecordId(), is("123456789")));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseWithPendingExportForFFKWeekcode() throws OpenFormatConnectorException, OpennumberRollConnectorException {

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
        LocalDate date = LocalDate.now().minusWeeks(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyww", dkLocale);
        upd.caseInformationUpdater.openFormatHandler = mockOpenFormat(new BibliographicInformation()
                .withCatalogcodes(Arrays.asList("FFK" + date.format(formatter), "BKM299999", "BKX299999", "ACC202001")));
        upd.caseInformationUpdater.repository = mockRepositoryAssigningRecordId("123456789");

        created.setStatus(CaseStatus.APPROVED);

        // FFK is previous week, but BKM and BKX is in the future, since FFK takes precedence, status should change

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));
        created.getTasks().forEach(t -> assertThat("recordId", t.getRecordId(), is("123456789")));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseWithWeekcodeMovingToLater() throws OpenFormatConnectorException, OpennumberRollConnectorException {

        // Create a case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("52000202")
                .withTitle("Title for 52000202")
                .withWeekCode("BKM202002")
                .withDetails("Details for 52000202")
                .withMaterialType(MaterialType.BOOK)
                .withDeadline("2021-09-15")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1)
                .withTasks(Collections.singletonList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(List.of("52000202"))
                ));

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        ScheduledCaseInformationUpdater upd = configure();
        LocalDate date = LocalDate.now().plusWeeks(2);
        upd.caseInformationUpdater.openFormatHandler = mockOpenFormat(new BibliographicInformation()
                .withCatalogcodes(Arrays.asList(weekcode("BKM", date), "ACC202001")));
        upd.caseInformationUpdater.repository = mockRepositoryAssigningRecordId("123456789");

        created.setStatus(CaseStatus.PENDING_EXPORT);

        // BKM is next-next week so PENDING_EXPORT should change back to APPROVED since it must wait another week

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.APPROVED));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseWithWeekcodeNextWeek() throws OpenFormatConnectorException, OpennumberRollConnectorException {

        // Create a case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("53000202")
                .withTitle("Title for 53000202")
                .withWeekCode("BKM202002")
                .withDetails("Details for 53000202")
                .withMaterialType(MaterialType.BOOK)
                .withDeadline("2021-09-15")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1)
                .withTasks(Collections.singletonList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(List.of("53000202"))
                ));

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        ScheduledCaseInformationUpdater upd = configure();
        LocalDate date = LocalDate.now().plusWeeks(1);
        upd.caseInformationUpdater.openFormatHandler = mockOpenFormat(new BibliographicInformation()
                .withCatalogcodes(Arrays.asList(weekcode("BKM", date), "ACC202001")));
        upd.caseInformationUpdater.repository = mockRepositoryAssigningRecordId("123456789");

        created.setStatus(CaseStatus.PENDING_EXPORT);

        // BKM is next week so case must remain in status PENDING_EXPORT
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseWithWeekcodeLastWeekOfLastYear() throws OpenFormatConnectorException, JsonProcessingException, OpennumberRollConnectorException {

        // Create a case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("53000303")
                .withTitle("Title for 53000303")
                .withWeekCode("BKM202203") // weekcode in the future
                .withDetails("Details for 53000303")
                .withMaterialType(MaterialType.BOOK)
                .withDeadline("2021-12-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1)
                .withTasks(Collections.singletonList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(List.of("53000303"))
                ));

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        ScheduledCaseInformationUpdater upd = configure();
        upd.caseInformationUpdater.openFormatHandler = mockOpenFormat(new BibliographicInformation()
                .withCatalogcodes(Arrays.asList("BKM202152", "ACC202203")));
        upd.caseInformationUpdater.repository = mockRepositoryAssigningRecordId("123456789");
        when(upd.caseInformationUpdater.dates.getCurrentDate()).thenReturn(LocalDate.parse("2021-12-26"));

        created.setStatus(CaseStatus.APPROVED);

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseWithWeekcodeFirstWeekOfNewYear() throws OpenFormatConnectorException, OpennumberRollConnectorException {

        // Create a case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("53000304")
                .withTitle("Title for 53000304")
                .withWeekCode("BKM202203")
                .withDetails("Details for 53000304")
                .withMaterialType(MaterialType.BOOK)
                .withDeadline("2021-12-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1)
                .withTasks(Collections.singletonList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(List.of("53000304"))
                ));

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        ScheduledCaseInformationUpdater upd = configure();
        upd.caseInformationUpdater.openFormatHandler = mockOpenFormat(new BibliographicInformation()
                .withCatalogcodes(Arrays.asList("BKM202201", "ACC202203")));
        upd.caseInformationUpdater.repository = mockRepositoryAssigningRecordId("123456789");
        when(upd.caseInformationUpdater.dates.getCurrentDate()).thenReturn(LocalDate.parse("2022-01-02"));

        created.setStatus(CaseStatus.APPROVED);

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseWithWeekcodeSecondWeekOfNewYear() throws OpenFormatConnectorException, OpennumberRollConnectorException {

        // Create a case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("53000305")
                .withTitle("Title for 53000305")
                .withWeekCode("BKM202203")
                .withDetails("Details for 53000305")
                .withMaterialType(MaterialType.BOOK)
                .withDeadline("2021-12-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1)
                .withTasks(Collections.singletonList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(List.of("53000305"))
                ));

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        ScheduledCaseInformationUpdater upd = configure();
        upd.caseInformationUpdater.openFormatHandler = mockOpenFormat(new BibliographicInformation()
                .withCatalogcodes(Arrays.asList("BKM202202", "ACC202203")));
        upd.caseInformationUpdater.repository = mockRepositoryAssigningRecordId("123456789");
        when(upd.caseInformationUpdater.dates.getCurrentDate()).thenReturn(LocalDate.parse("2022-01-03"));

        created.setStatus(CaseStatus.APPROVED);

        // BKM is next shiftday (friday this week) so case must be moved to status PENDING_EXPORT
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseWithWeekcodeLastWeekOf2026() throws OpenFormatConnectorException, OpennumberRollConnectorException {

        // Create a case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("53000306")
                .withTitle("Title for 53000306")
                .withWeekCode("BKM202703")
                .withDetails("Details for 53000306")
                .withMaterialType(MaterialType.BOOK)
                .withDeadline("2021-12-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1)
                .withTasks(Collections.singletonList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(List.of("53000306"))
                ));

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        ScheduledCaseInformationUpdater upd = configure();
        upd.caseInformationUpdater.openFormatHandler = mockOpenFormat(new BibliographicInformation()
                .withCatalogcodes(Arrays.asList("BKM202653", "ACC202703")));
        upd.caseInformationUpdater.repository = mockRepositoryAssigningRecordId("123456789");
        when(upd.caseInformationUpdater.dates.getCurrentDate()).thenReturn(LocalDate.parse("2026-12-27"));

        created.setStatus(CaseStatus.APPROVED);

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseWithWeekcodeFirstWeekOf2027() throws OpenFormatConnectorException, OpennumberRollConnectorException {

        // Create a case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("53000307")
                .withTitle("Title for 53000307")
                .withWeekCode("BKM202703")
                .withDetails("Details for 53000307")
                .withMaterialType(MaterialType.BOOK)
                .withDeadline("2021-12-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1)
                .withTasks(Collections.singletonList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(List.of("53000307"))
                ));

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        ScheduledCaseInformationUpdater upd = configure();
        upd.caseInformationUpdater.openFormatHandler = mockOpenFormat(new BibliographicInformation()
                .withCatalogcodes(Arrays.asList("BKM202701", "ACC202203")));
        upd.caseInformationUpdater.repository = mockRepositoryAssigningRecordId("123456789");
        when(upd.caseInformationUpdater.dates.getCurrentDate()).thenReturn(LocalDate.parse("2027-01-02"));

        created.setStatus(CaseStatus.APPROVED);

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testUpdateCaseWithWeekcodeSecondWeekOf2027() throws OpenFormatConnectorException, OpennumberRollConnectorException {

        // Create a case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("53000308")
                .withTitle("Title for 53000308")
                .withWeekCode("BKM202703")
                .withDetails("Details for 53000308")
                .withMaterialType(MaterialType.BOOK)
                .withDeadline("2021-12-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1)
                .withTasks(Collections.singletonList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(List.of("53000308"))
                ));

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        ScheduledCaseInformationUpdater upd = configure();
        upd.caseInformationUpdater.openFormatHandler = mockOpenFormat(new BibliographicInformation()
                .withCatalogcodes(Arrays.asList("BKM202702", "ACC202203")));
        upd.caseInformationUpdater.repository = mockRepositoryAssigningRecordId("123456789");
        when(upd.caseInformationUpdater.dates.getCurrentDate()).thenReturn(LocalDate.parse("2027-01-04"));

        created.setStatus(CaseStatus.APPROVED);

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));

        // Delete the case so that we dont mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }
}
