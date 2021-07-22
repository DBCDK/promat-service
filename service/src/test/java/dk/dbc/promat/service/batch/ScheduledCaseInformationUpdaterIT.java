/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import dk.dbc.commons.persistence.TransactionScopedPersistenceContext;
import dk.dbc.connector.openformat.OpenFormatConnectorException;
import dk.dbc.connector.openformat.OpenFormatConnectorFactory;
import dk.dbc.opennumberroll.OpennumberRollConnectorException;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.Repository;
import dk.dbc.promat.service.api.BibliographicInformation;
import dk.dbc.promat.service.api.OpenFormatHandler;
import dk.dbc.promat.service.cluster.ServerRole;

import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.dto.TaskDto;

import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.persistence.TaskType;
import dk.dbc.promat.service.util.PromatTaskUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScheduledCaseInformationUpdaterIT extends ContainerTest {
    private TransactionScopedPersistenceContext persistenceContext;
    private final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final SimpleTimer timer = mock(SimpleTimer.class);
    private final ConcurrentGauge gauge = mock(ConcurrentGauge.class);
    private static WireMockServer wireMockServer;
    private static String wiremockHost;
    private static PromatTaskUtils promatTaskUtils;
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledCaseInformationUpdaterIT.class);

    @BeforeAll
    public static void startWiremock() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
        wiremockHost = wireMockServer.baseUrl();
        promatTaskUtils = new PromatTaskUtils();
    }

    @AfterAll
    public static void stopWiremock() {
        wireMockServer.stop();
    }

    @BeforeEach
    public void setup() {
        persistenceContext = new TransactionScopedPersistenceContext(entityManager);
        when(metricRegistry.simpleTimer(any(Metadata.class))).thenReturn(timer);
        when(metricRegistry.concurrentGauge(any(Metadata.class))).thenReturn(gauge);

    }

    @Test
    public void testUpdateCaseWithWeekcode() throws OpenFormatConnectorException, JsonProcessingException {

        // Create a case with incorrect title and weekcode
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("24699773")
                .withTitle("Title for 24699773")
                .withWeekCode("DPF202002")
                .withDetails("Details for 24699773")
                .withMaterialType(MaterialType.BOOK)
                .withAssigned("2021-01-28")
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1);

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        ScheduledCaseInformationUpdater upd = new ScheduledCaseInformationUpdater();
        upd.caseInformationUpdater = new CaseInformationUpdater();
        upd.caseInformationUpdater.metricRegistry = metricRegistry;
        upd.caseInformationUpdater.openFormatHandler = new OpenFormatHandler()
                .withConnector(OpenFormatConnectorFactory.create(wiremockHost));
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));

        assertThat("title is correct", created.getTitle(), is("Den lukkede bog"));
        assertThat("weekcode is correct", created.getWeekCode(), is("BKM201105"));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testUpdateCaseWithNullWeekcode() throws JsonProcessingException, OpenFormatConnectorException {

        // Create a case with no weekcode
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("24699773")
                .withTitle("Title for 24699773")
                .withDetails("Details for 24699773")
                .withMaterialType(MaterialType.BOOK)
                .withAssigned("2021-01-28")
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1);

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        ScheduledCaseInformationUpdater upd = new ScheduledCaseInformationUpdater();
        upd.caseInformationUpdater = new CaseInformationUpdater();
        upd.caseInformationUpdater.metricRegistry = metricRegistry;
        upd.caseInformationUpdater.openFormatHandler = new OpenFormatHandler()
                .withConnector(OpenFormatConnectorFactory.create(wiremockHost));
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));

        assertThat("weekcode is correct", created.getWeekCode(), is("BKM201105"));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
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
                .withAssigned("2021-01-28")
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1);

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        ScheduledCaseInformationUpdater upd = new ScheduledCaseInformationUpdater();
        upd.caseInformationUpdater = new CaseInformationUpdater();
        upd.caseInformationUpdater.metricRegistry = metricRegistry;
        upd.caseInformationUpdater.openFormatHandler = new OpenFormatHandler()
                .withConnector(OpenFormatConnectorFactory.create(wiremockHost));
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));

        assertThat("weekcode is correct", created.getWeekCode(), is("BKM201105"));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
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
                .withAssigned("2021-01-28")
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1);

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        ScheduledCaseInformationUpdater upd = new ScheduledCaseInformationUpdater();
        upd.caseInformationUpdater = new CaseInformationUpdater();
        upd.caseInformationUpdater.metricRegistry = metricRegistry;
        upd.caseInformationUpdater.openFormatHandler = new OpenFormatHandler()
                .withConnector(OpenFormatConnectorFactory.create(wiremockHost));
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));

        assertThat("weekcode is correct", created.getWeekCode(), is(""));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testCaseUpdates() throws JsonProcessingException, OpenFormatConnectorException {

        // Create a case with no weekcode
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("22677780")
                .withTitle("Title for 22677780")
                .withWeekCode("DPF202002")
                .withDetails("Details for 22677780")
                .withMaterialType(MaterialType.BOOK)
                .withAssigned("2021-01-28")
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1);

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        ScheduledCaseInformationUpdater upd = new ScheduledCaseInformationUpdater();
        upd.caseInformationUpdater = new CaseInformationUpdater();
        upd.caseInformationUpdater.metricRegistry = metricRegistry;
        upd.entityManager = entityManager;
        upd.serverRole = ServerRole.PRIMARY;
        OpenFormatHandler mockedHandler = mock(OpenFormatHandler.class);
        upd.caseInformationUpdater.openFormatHandler = mockedHandler;
        when(mockedHandler.format(created.getPrimaryFaust()))
                .thenReturn(new BibliographicInformation()
                        .withTitle("UPDATED_TITLE")
                        .withCatalogcodes(Arrays.asList("BKM999999"))
                        .withCreator("UPDATED_AUTHOR"));
        when(mockedHandler.format(not(eq(created.getPrimaryFaust()))))
                .thenReturn(new BibliographicInformation()
                        .withError("not real handler")); // Causing update of case to be skipped

        persistenceContext.run(() -> {
            upd.updateCaseInformation();

            entityManager.flush();

            TypedQuery<PromatCase> query = entityManager.createQuery(
                    "SELECT c FROM PromatCase c " +
                            "WHERE c.id = :id", PromatCase.class);
            query.setParameter("id", created.getId());
            PromatCase updated = query.getSingleResult();
            assertThat("title is updated", updated.getTitle().equals("UPDATED_TITLE"));
            assertThat("weekcode is updated", updated.getWeekCode().equals("BKM999999"));
            assertThat("author is updated", updated.getAuthor().equals("UPDATED_AUTHOR"));
        });

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
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
                .withAssigned("2021-01-28")
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1);

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        ScheduledCaseInformationUpdater upd = new ScheduledCaseInformationUpdater();
        upd.caseInformationUpdater = new CaseInformationUpdater();
        upd.caseInformationUpdater.metricRegistry = metricRegistry;
        upd.caseInformationUpdater.openFormatHandler = new OpenFormatHandler()
                .withConnector(OpenFormatConnectorFactory.create(wiremockHost));
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));

        assertThat("weekcode is removed", created.getWeekCode(), is(""));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
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
                .withAssigned("2021-01-28")
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1);

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        ScheduledCaseInformationUpdater upd = new ScheduledCaseInformationUpdater();
        upd.caseInformationUpdater = new CaseInformationUpdater();
        upd.caseInformationUpdater.metricRegistry = metricRegistry;
        upd.caseInformationUpdater.openFormatHandler = new OpenFormatHandler()
                .withConnector(OpenFormatConnectorFactory.create(wiremockHost));
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));

        assertThat("title is updated", created.getTitle().equals("Deadpool"));
        assertThat("weekcode is updated", created.getWeekCode().equals("BKM999999"));
        assertThat("author is updated", created.getAuthor().equals(""));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testWaitForMetakompasData() throws Exception {

        // Create a case three fausts to check metakompas for.
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("48959939")
                .withTitle("Title for 48959939")
                .withDetails("Details for 48959939")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(
                        List.of(
                                new TaskDto()
                                        .withTaskType(TaskType.GROUP_2_100_UPTO_199_PAGES)
                                        .withTaskFieldType(TaskFieldType.METAKOMPAS)
                                        .withTargetFausts(List.of("48959939")),
                                new TaskDto()
                                        .withTaskType(TaskType.GROUP_2_100_UPTO_199_PAGES)
                                        .withTaskFieldType(TaskFieldType.METAKOMPAS)
                                        .withTargetFausts(List.of( "48959955", "48959912"))
                        )
                )
                .withAssigned("2021-07-21")
                .withDeadline("2024-08-07")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1);

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        Map<String, BibliographicInformation> openFormatResponse =
                Map.of(
                        "48959939", getOpenformatResponseFromResource("48959939").withMetakompassubject(null),
                        "48959912", getOpenformatResponseFromResource("48959912").withMetakompassubject("false"),
                        "48959955", getOpenformatResponseFromResource("48959955").withMetakompassubject(null)
                );


        PromatCase promatCase = getCaseWithId(created.getId());
        ScheduledCaseInformationUpdater upd = new ScheduledCaseInformationUpdater();
        upd.caseInformationUpdater = new CaseInformationUpdater();
        upd.caseInformationUpdater.metricRegistry = metricRegistry;
        OpenFormatHandler openFormatHandler = mock(OpenFormatHandler.class);
        upd.caseInformationUpdater.openFormatHandler = openFormatHandler;
        when(openFormatHandler.format(anyString()))
                .thenAnswer(invocationOnMock -> openFormatResponse.get(invocationOnMock.getArgument(0)));

        //
        // First round: Lets say that none are ready yet.
        //
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(promatCase));

        for (PromatTask task : PromatTaskUtils.getTasksOfType(promatCase, TaskFieldType.METAKOMPAS)) {
            assertThat("metakompasdata task",
                    task.getData(),
                    anyOf(is(nullValue()), is("false")));
        }

        //
        // Second round: lets say metakompasdata for primary faust now has been done.
        //
        openFormatResponse.get("48959939").setMetakompassubject(CaseInformationUpdater.METAKOMPASDATA_PRESENT);
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(promatCase));
        created = getCaseWithId(promatCase.getId());
        List<PromatTask> tasks = getTasksWhereMetakompasIsPresent(created);
        assertThat("There is only one finished.", tasks.size(), is(1));
        assertThat("And it is the primaryfaust", tasks.get(0).getTargetFausts().contains("48959939"));

        //
        // Third round: Metadata for one of the related faust has been done. There is still only one in
        // the list of done Metakompas tasks.
        //
        openFormatResponse.get("48959955").setMetakompassubject(CaseInformationUpdater.METAKOMPASDATA_PRESENT);
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(promatCase));
        created = getCaseWithId(promatCase.getId());
        tasks = getTasksWhereMetakompasIsPresent(created);
        assertThat("There is only one finished.", tasks.size(), is(1));
        assertThat("And it is the task with the primaryfaust", tasks.get(0).getTargetFausts().contains("48959939"));



        //
        // Fourth round: Metadata for both of the related faust has been done.
        // AND lets say we updated the case to PENDING_EXTERNAL.
        //
        promatCase.setStatus(CaseStatus.PENDING_EXTERNAL);
        entityManager.persist(promatCase);
        openFormatResponse.get("48959912").setMetakompassubject(CaseInformationUpdater.METAKOMPASDATA_PRESENT);
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(promatCase));
        created = getCaseWithId(promatCase.getId());
        LOGGER.info("task1 fausts:{}, task2 fausts:{}", created.getTasks().get(0).getData(), created.getTasks().get(1).getData());

        tasks = getTasksWhereMetakompasIsPresent(created);
        assertThat("They all are finished.", tasks.size(), is(2));

        created = getCaseWithId(promatCase.getId());
        assertThat("case closed", created.getStatus(), is(CaseStatus.APPROVED));


        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    private List<PromatTask> getTasksWhereMetakompasIsPresent(PromatCase promatCase) {
        return PromatTaskUtils.getTasksOfType(promatCase, TaskFieldType.METAKOMPAS)
                .stream().filter(promatTask -> promatTask.getData() != null &&
                        promatTask.getData().equals(CaseInformationUpdater.METAKOMPASDATA_PRESENT))
                .collect(Collectors.toList());
    }
    private PromatCase getCaseWithId(Integer id) {
        TypedQuery<PromatCase> query = entityManager.createQuery(
                "SELECT c FROM PromatCase c " +
                        "WHERE c.id = :id", PromatCase.class);
        query.setParameter("id", id);
        return query.getSingleResult();

    }
    private int updateCaseWithId(Integer id, CaseStatus caseStatus) {
        TypedQuery<PromatCase> query = entityManager.createQuery(
                "UPDATE PromatCase c SET c.status = :status " +
                        "WHERE c.id = :id", PromatCase.class);
        query.setParameter("id", id);
        query.setParameter("status", caseStatus);
        return query.executeUpdate();
    }

    private BibliographicInformation getOpenformatResponseFromResource(String faust) throws IOException {
        return mapper.readValue(
                Files.readString(
                        Path.of(ScheduledCaseInformationUpdaterIT.class
                                .getResource(String.format("/openformat/%s.json", faust))
                .getPath())), BibliographicInformation.class);
    }

    @Test
    public void testUpdateCaseWithPendingExportForPrehistoricWeekcode() throws OpenFormatConnectorException, JsonProcessingException, OpennumberRollConnectorException {

        // Create a case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("24699773")
                .withTitle("Title for 24699773")
                .withWeekCode("BKM202002")
                .withDetails("Details for 24699773")
                .withMaterialType(MaterialType.BOOK)
                .withAssigned("2021-01-28")
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1).withTasks(Arrays.asList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(Arrays.asList("24699773"))
                ));

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        ScheduledCaseInformationUpdater upd = new ScheduledCaseInformationUpdater();
        upd.caseInformationUpdater = new CaseInformationUpdater();
        upd.caseInformationUpdater.metricRegistry = metricRegistry;
        upd.entityManager = entityManager;
        upd.serverRole = ServerRole.PRIMARY;
        OpenFormatHandler mockedHandler = mock(OpenFormatHandler.class);
        upd.caseInformationUpdater.openFormatHandler = mockedHandler;
        Repository mockedRepository = mock(Repository.class);
        upd.caseInformationUpdater.repository = mockedRepository;

        created.setStatus(CaseStatus.APPROVED);
        when(mockedHandler.format(anyString()))
                .thenReturn(new BibliographicInformation()
                        .withCatalogcodes(Arrays.asList("BKM202001")));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));
        created.getTasks().stream().forEach(t -> assertThat("recordId", t.getRecordId(), is("123456789")));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
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
                .withAssigned("2021-01-28")
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1)
                .withTasks(Arrays.asList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(Arrays.asList("24699773"))
                ));

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        ScheduledCaseInformationUpdater upd = new ScheduledCaseInformationUpdater();
        upd.caseInformationUpdater = new CaseInformationUpdater();
        upd.caseInformationUpdater.metricRegistry = metricRegistry;
        upd.entityManager = entityManager;
        upd.serverRole = ServerRole.PRIMARY;
        OpenFormatHandler mockedHandler = mock(OpenFormatHandler.class);
        upd.caseInformationUpdater.openFormatHandler = mockedHandler;
        Repository mockedRepository = mock(Repository.class);
        upd.caseInformationUpdater.repository = mockedRepository;

        LocalDate date = LocalDate.now().plusWeeks(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyw", new Locale("da", "DK"));

        created.setStatus(CaseStatus.APPROVED);
        when(mockedHandler.format(anyString()))
                .thenReturn(new BibliographicInformation()
                        .withCatalogcodes(Arrays.asList("BKM" + date.format(formatter))));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));
        created.getTasks().stream().forEach(t -> assertThat("recordId", t.getRecordId(), is("123456789")));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testUpdateCaseWithPendingExportForNextWeeksWeekcode() throws OpenFormatConnectorException, JsonProcessingException, OpennumberRollConnectorException {

        // Create a case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("24699773")
                .withTitle("Title for 24699773")
                .withWeekCode("BKM202002")
                .withDetails("Details for 24699773")
                .withMaterialType(MaterialType.BOOK)
                .withAssigned("2021-01-28")
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1).withTasks(Arrays.asList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(Arrays.asList("24699773"))
                ));

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        ScheduledCaseInformationUpdater upd = new ScheduledCaseInformationUpdater();
        upd.caseInformationUpdater = new CaseInformationUpdater();
        upd.caseInformationUpdater.metricRegistry = metricRegistry;
        upd.entityManager = entityManager;
        upd.serverRole = ServerRole.PRIMARY;
        OpenFormatHandler mockedHandler = mock(OpenFormatHandler.class);
        upd.caseInformationUpdater.openFormatHandler = mockedHandler;
        Repository mockedRepository = mock(Repository.class);
        upd.caseInformationUpdater.repository = mockedRepository;

        LocalDate date = LocalDate.now().plusWeeks(2);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyw", new Locale("da", "DK"));

        created.setStatus(CaseStatus.APPROVED);
        when(mockedHandler.format(anyString()))
                .thenReturn(new BibliographicInformation()
                        .withCatalogcodes(Arrays.asList("BKM" + date.format(formatter))));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.APPROVED));
        created.getTasks().stream().forEach(t -> assertThat("recordId", t.getRecordId(), is(nullValue())));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testUpdateCaseWithNoWeekcode() throws OpenFormatConnectorException, JsonProcessingException, OpennumberRollConnectorException {

        // Create a case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("24699773")
                .withTitle("Title for 24699773")
                .withWeekCode("")
                .withDetails("Details for 24699773")
                .withMaterialType(MaterialType.BOOK)
                .withAssigned("2021-01-28")
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1)
                .withTasks(Arrays.asList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(Arrays.asList("24699773"))
                ));

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        ScheduledCaseInformationUpdater upd = new ScheduledCaseInformationUpdater();
        upd.caseInformationUpdater = new CaseInformationUpdater();
        upd.caseInformationUpdater.metricRegistry = metricRegistry;
        upd.entityManager = entityManager;
        upd.serverRole = ServerRole.PRIMARY;
        OpenFormatHandler mockedHandler = mock(OpenFormatHandler.class);
        upd.caseInformationUpdater.openFormatHandler = mockedHandler;
        Repository mockedRepository = mock(Repository.class);
        upd.caseInformationUpdater.repository = mockedRepository;

        created.setStatus(CaseStatus.APPROVED);

        when(mockedHandler.format(anyString()))
                .thenReturn(new BibliographicInformation()
                        .withCatalogcodes(new ArrayList<>()));

        ConcurrentGauge mockedGauge = mock(ConcurrentGauge.class);
        doAnswer(answer -> {
            return mockedGauge;
        }).when(metricRegistry).concurrentGauge(any(Metadata.class));

        AtomicInteger errors = new AtomicInteger(0);
        doAnswer(answer -> {
            errors.getAndIncrement();
            return null;
        }).when(mockedGauge).inc();

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));

        assertThat("no errors", errors.get(), is(0));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testUpdateCaseWithPendingExportForBKMWeekcode() throws OpenFormatConnectorException, JsonProcessingException, OpennumberRollConnectorException {

        // Create a case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("24699773")
                .withTitle("Title for 24699773")
                .withWeekCode("BMK202002")
                .withDetails("Details for 24699773")
                .withMaterialType(MaterialType.BOOK)
                .withAssigned("2021-01-28")
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1)
                .withTasks(Arrays.asList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(Arrays.asList("24699773"))
                ));

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        ScheduledCaseInformationUpdater upd = new ScheduledCaseInformationUpdater();
        upd.caseInformationUpdater = new CaseInformationUpdater();
        upd.caseInformationUpdater.metricRegistry = metricRegistry;
        upd.entityManager = entityManager;
        upd.serverRole = ServerRole.PRIMARY;
        OpenFormatHandler mockedHandler = mock(OpenFormatHandler.class);
        upd.caseInformationUpdater.openFormatHandler = mockedHandler;
        Repository mockedRepository = mock(Repository.class);
        upd.caseInformationUpdater.repository = mockedRepository;

        LocalDate date = LocalDate.now().minusWeeks(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyw", new Locale("da", "DK"));

        created.setStatus(CaseStatus.APPROVED);
        when(mockedHandler.format(anyString()))
                .thenReturn(new BibliographicInformation()
                        .withCatalogcodes(Arrays.asList("BKM" + date.format(formatter), "BKX299999")));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        // BKM is previous week, but BKX is in the future, and since BKX takes precedence, status should not change

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.APPROVED));
        created.getTasks().stream().forEach(t -> assertThat("recordId", t.getRecordId(), is(nullValue())));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testUpdateCaseWithPendingExportForBKXWeekcode() throws OpenFormatConnectorException, JsonProcessingException, OpennumberRollConnectorException {

        // Create a case
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("24699773")
                .withTitle("Title for 24699773")
                .withWeekCode("BMK202002")
                .withDetails("Details for 24699773")
                .withMaterialType(MaterialType.BOOK)
                .withAssigned("2021-01-28")
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1)
                .withTasks(Arrays.asList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(Arrays.asList("24699773"))
                ));

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        ScheduledCaseInformationUpdater upd = new ScheduledCaseInformationUpdater();
        upd.caseInformationUpdater = new CaseInformationUpdater();
        upd.caseInformationUpdater.metricRegistry = metricRegistry;
        upd.entityManager = entityManager;
        upd.serverRole = ServerRole.PRIMARY;
        OpenFormatHandler mockedHandler = mock(OpenFormatHandler.class);
        upd.caseInformationUpdater.openFormatHandler = mockedHandler;
        Repository mockedRepository = mock(Repository.class);
        upd.caseInformationUpdater.repository = mockedRepository;

        LocalDate date = LocalDate.now().minusWeeks(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyw", new Locale("da", "DK"));

        created.setStatus(CaseStatus.APPROVED);
        when(mockedHandler.format(anyString()))
                .thenReturn(new BibliographicInformation()
                        .withCatalogcodes(Arrays.asList("BKX" + date.format(formatter), "BKM299999")));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        // BKX is previous week, but BKM is in the future, since BKX takes precedence, status should change

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));
        created.getTasks().stream().forEach(t -> assertThat("recordId", t.getRecordId(), is("123456789")));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

}
