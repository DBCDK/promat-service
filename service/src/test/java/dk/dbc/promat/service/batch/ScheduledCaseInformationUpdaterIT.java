package dk.dbc.promat.service.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import dk.dbc.commons.persistence.TransactionScopedPersistenceContext;
import dk.dbc.connector.openformat.OpenFormatConnectorException;
import dk.dbc.connector.openformat.OpenFormatConnectorFactory;
import dk.dbc.opennumberroll.OpennumberRollConnectorException;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.Dates;
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
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScheduledCaseInformationUpdaterIT extends ContainerTest {
    private TransactionScopedPersistenceContext persistenceContext;
    private final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final SimpleTimer timer = mock(SimpleTimer.class);
    private final ConcurrentGauge gauge = mock(ConcurrentGauge.class);
    private static WireMockServer wireMockServer;
    private static String wiremockHost;
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledCaseInformationUpdaterIT.class);

    @BeforeAll
    public static void startWiremock() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
        wiremockHost = wireMockServer.baseUrl();
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

        ContentLookUp contentLookUpMock = mock(ContentLookUp.class);
        upd.caseInformationUpdater.contentLookUp = contentLookUpMock;
        when(contentLookUpMock.lookUpContent(anyString())).thenReturn(Optional.empty());

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

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

        ContentLookUp contentLookUpMock = mock(ContentLookUp.class);
        upd.caseInformationUpdater.contentLookUp = contentLookUpMock;
        when(contentLookUpMock.lookUpContent(anyString())).thenReturn(Optional.empty());

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

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

        ContentLookUp contentLookUpMock = mock(ContentLookUp.class);
        upd.caseInformationUpdater.contentLookUp = contentLookUpMock;
        when(contentLookUpMock.lookUpContent(anyString())).thenReturn(Optional.empty());

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

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

        ContentLookUp contentLookUpMock = mock(ContentLookUp.class);
        upd.caseInformationUpdater.contentLookUp = contentLookUpMock;
        when(contentLookUpMock.lookUpContent(anyString())).thenReturn(Optional.empty());

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

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

        ContentLookUp contentLookUpMock = mock(ContentLookUp.class);
        upd.caseInformationUpdater.contentLookUp = contentLookUpMock;
        when(contentLookUpMock.lookUpContent(anyString())).thenReturn(Optional.empty());

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

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

        ContentLookUp contentLookUpMock = mock(ContentLookUp.class);
        upd.caseInformationUpdater.contentLookUp = contentLookUpMock;
        when(contentLookUpMock.lookUpContent(anyString())).thenReturn(Optional.empty());

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

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

        ContentLookUp contentLookUpMock = mock(ContentLookUp.class);
        upd.caseInformationUpdater.contentLookUp = contentLookUpMock;
        when(contentLookUpMock.lookUpContent(anyString())).thenReturn(Optional.empty());

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

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

        ContentLookUp contentLookUpMock = mock(ContentLookUp.class);
        upd.caseInformationUpdater.contentLookUp = contentLookUpMock;
        when(contentLookUpMock.lookUpContent(anyString())).thenReturn(Optional.empty());

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

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

        ContentLookUp contentLookUpMock = mock(ContentLookUp.class);
        upd.caseInformationUpdater.contentLookUp = contentLookUpMock;
        when(contentLookUpMock.lookUpContent(anyString())).thenReturn(Optional.empty());

        created.setStatus(CaseStatus.APPROVED);
        when(mockedHandler.format(anyString()))
                .thenReturn(new BibliographicInformation()
                        .withCatalogcodes(Arrays.asList("BKM202001", "ACC202001")));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

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

        ContentLookUp contentLookUpMock = mock(ContentLookUp.class);
        upd.caseInformationUpdater.contentLookUp = contentLookUpMock;
        when(contentLookUpMock.lookUpContent(anyString())).thenReturn(Optional.empty());

        LocalDate date = LocalDate.now().plusWeeks(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyww", new Locale("da", "DK"));

        created.setStatus(CaseStatus.APPROVED);
        when(mockedHandler.format(anyString()))
                .thenReturn(new BibliographicInformation()
                        .withCatalogcodes(Arrays.asList("BKM" + date.format(formatter), "ACC202001")));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

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

        ContentLookUp contentLookUpMock = mock(ContentLookUp.class);
        upd.caseInformationUpdater.contentLookUp = contentLookUpMock;
        when(contentLookUpMock.lookUpContent(anyString())).thenReturn(Optional.empty());

        LocalDate date = LocalDate.now().plusWeeks(2);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyww", new Locale("da", "DK"));

        created.setStatus(CaseStatus.APPROVED);
        when(mockedHandler.format(anyString()))
                .thenReturn(new BibliographicInformation()
                        .withCatalogcodes(Arrays.asList("BKM" + date.format(formatter), "ACC202001")));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

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

        ContentLookUp contentLookUpMock = mock(ContentLookUp.class);
        upd.caseInformationUpdater.contentLookUp = contentLookUpMock;
        when(contentLookUpMock.lookUpContent(anyString())).thenReturn(Optional.empty());

        created.setStatus(CaseStatus.APPROVED);

        when(mockedHandler.format(anyString()))
                .thenReturn(new BibliographicInformation()
                        .withCatalogcodes(Arrays.asList("ACC202001")));

        ConcurrentGauge mockedGauge = mock(ConcurrentGauge.class);
        doAnswer(answer -> {
            return mockedGauge;
        }).when(metricRegistry).concurrentGauge(any(Metadata.class));

        AtomicInteger errors = new AtomicInteger(0);
        doAnswer(answer -> {
            errors.getAndIncrement();
            return null;
        }).when(mockedGauge).inc();

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));

        assertThat("no errors", errors.get(), is(0));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testUpdateCaseWithApprovedForBKMWeekcode() throws OpenFormatConnectorException, JsonProcessingException, OpennumberRollConnectorException {

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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyww", new Locale("da", "DK"));

        created.setStatus(CaseStatus.APPROVED);
        when(mockedHandler.format(anyString()))
                .thenReturn(new BibliographicInformation()
                        .withCatalogcodes(Arrays.asList("BKM" + date.format(formatter), "BKX299999", "FFK299999", "ACC202001")));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

        // BKM is previous week, but BKX and FFK is in the future, and since BKX or FFK takes precedence, status should not change

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.APPROVED));
        created.getTasks().stream().forEach(t -> assertThat("recordId", t.getRecordId(), is(nullValue())));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testUpdateCaseWithApprovedForBKMAndBkxWeekcode() throws OpenFormatConnectorException, JsonProcessingException, OpennumberRollConnectorException {

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

        ContentLookUp contentLookUpMock = mock(ContentLookUp.class);
        upd.caseInformationUpdater.contentLookUp = contentLookUpMock;
        when(contentLookUpMock.lookUpContent(anyString())).thenReturn(Optional.empty());

        LocalDate date = LocalDate.now().minusWeeks(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyww", new Locale("da", "DK"));

        created.setStatus(CaseStatus.APPROVED);
        when(mockedHandler.format(anyString()))
                .thenReturn(new BibliographicInformation()
                        .withCatalogcodes(Arrays.asList("BKM" + date.format(formatter),
                                "BKX" + date.format(formatter),
                                "FFK299999",
                                "ACC202001")));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

        // BKM and BKX is previous week, but FFK is in the future, and since FFK takes precedence, status should not change

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
                .withWeekCode("BKM202002")
                .withDetails("Details for 24699773")
                .withMaterialType(MaterialType.BOOK)
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

        ContentLookUp contentLookUpMock = mock(ContentLookUp.class);
        upd.caseInformationUpdater.contentLookUp = contentLookUpMock;
        when(contentLookUpMock.lookUpContent(anyString())).thenReturn(Optional.empty());

        LocalDate date = LocalDate.now().minusWeeks(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyww", new Locale("da", "DK"));

        created.setStatus(CaseStatus.APPROVED);
        when(mockedHandler.format(anyString()))
                .thenReturn(new BibliographicInformation()
                        .withCatalogcodes(Arrays.asList("BKX" + date.format(formatter), "BKM299999", "ACC202001")));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

        // BKX is previous week, but BKM is in the future, since BKX takes precedence, status should change

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));
        created.getTasks().stream().forEach(t -> assertThat("recordId", t.getRecordId(), is("123456789")));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testUpdateCaseWithPendingExportForFFKWeekcode() throws OpenFormatConnectorException, JsonProcessingException, OpennumberRollConnectorException {

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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyww", new Locale("da", "DK"));

        created.setStatus(CaseStatus.APPROVED);
        when(mockedHandler.format(anyString()))
                .thenReturn(new BibliographicInformation()
                        .withCatalogcodes(Arrays.asList("FFK" + date.format(formatter), "BKM299999", "BKX299999", "ACC202001")));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

        // FFK is previous week, but BKM and BKX is in the future, since FFK takes precedence, status should change

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));
        created.getTasks().stream().forEach(t -> assertThat("recordId", t.getRecordId(), is("123456789")));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testThatFulltextLinksAreUpdated() throws JsonProcessingException, OpenFormatConnectorException {
        final String DOWNLOAD_LINK = "http://host.testcontainers.internal:" + wireMockServer.port() +
                "?faust=48959940";

        // Create a case. No download is present for main faust.
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("48959940")
                .withTitle("Title for 48959940")
                .withDetails("Details for 48959940")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(
                        List.of(
                                new TaskDto()
                                        .withTaskType(TaskType.GROUP_2_100_UPTO_199_PAGES)
                                        .withTaskFieldType(TaskFieldType.BRIEF)
                                        .withTargetFausts(List.of("48959940"))))
                .withDeadline("2024-08-07")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1);

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        PromatCase promatCase = getCaseWithId(created.getId());
        ScheduledCaseInformationUpdater upd = new ScheduledCaseInformationUpdater();
        upd.caseInformationUpdater = new CaseInformationUpdater();
        upd.caseInformationUpdater.metricRegistry = metricRegistry;
        upd.caseInformationUpdater.openFormatHandler = mock(OpenFormatHandler.class);
        when(upd.caseInformationUpdater.openFormatHandler.format(anyString()))
                .thenReturn(new BibliographicInformation()
                        .withCatalogcodes(new ArrayList<>()));

        ContentLookUp contentLookUpMock = mock(ContentLookUp.class);
        upd.caseInformationUpdater.contentLookUp = contentLookUpMock;
        when(contentLookUpMock.lookUpContent("48959940")).thenReturn(Optional.of(DOWNLOAD_LINK));

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

        //
        // Now do an update, and confirm that the corrct link is present.
        //
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(promatCase));
        assertThat("Download link is now present", promatCase.getFulltextLink(), is(DOWNLOAD_LINK));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
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

        when(mockedHandler.format(anyString()))
                .thenReturn(new BibliographicInformation()
                        .withCatalogcodes(null));
        doNothing().when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        LOGGER.info("codes: {}", created.getCodes());
        assertThat("codes exists", created.getCodes(), is(nullValue()));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
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

        when(mockedHandler.format(anyString()))
                .thenReturn(new BibliographicInformation()
                        .withCatalogcodes(Arrays.asList("FFK20210603", "BKM20210603", "bkx20210602", "ACC20210601")));
        doNothing().when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("codes exists", created.getCodes(), is(notNullValue()));
        assertThat("codes contains", created.getCodes().stream()
                .sorted().collect(Collectors.toList()),
                is(Arrays.asList("ACC20210601", "BKM20210603", "BKX20210602", "FFK20210603")));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testClearInactiveEditors() throws JsonProcessingException, OpenFormatConnectorException {

        // Create first case and move it to PENDING_APPROVAL
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("51000101")
                .withTitle("Title for 51000101")
                .withWeekCode("BKM202137")
                .withDetails("Details for 51000101")
                .withMaterialType(MaterialType.BOOK)
                .withDeadline("2024-09-14")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1);

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        dto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        response = postResponse("v1/api/cases/" + created.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));

        Integer activeId = created.getId();

        // Create second case and move it to PENDING_APPROVAL
        dto = new CaseRequest()
                .withPrimaryFaust("51000202")
                .withTitle("Title for 51000202")
                .withWeekCode("BKM202137")
                .withDetails("Details for 51000202")
                .withMaterialType(MaterialType.BOOK)
                .withDeadline("2024-09-14")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1);

        response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        dto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        response = postResponse("v1/api/cases/" + created.getId(), dto);
        assertThat("status code", response.getStatus(), is(200));

        Integer inactiveId = created.getId();

        // Send the active case back to the reviewer, then again to approval - to set the keepEditor flag
        dto = new CaseRequest().withStatus(CaseStatus.PENDING_ISSUES).withEditor(10);
        response = postResponse("v1/api/cases/" + activeId, dto);
        assertThat("status code", response.getStatus(), is(200));
        dto.setStatus(CaseStatus.PENDING_APPROVAL);
        response = postResponse("v1/api/cases/" + activeId, dto);
        assertThat("status code", response.getStatus(), is(200));

        // Run nightly update to clear the editor on the 'inactive case, but retain the editor on the 'active' case
        ScheduledCaseInformationUpdater upd = new ScheduledCaseInformationUpdater();
        upd.caseInformationUpdater = new CaseInformationUpdater();
        upd.caseInformationUpdater.metricRegistry = metricRegistry;
        upd.entityManager = entityManager;
        upd.serverRole = ServerRole.PRIMARY;

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

        persistenceContext.run(() -> {
            upd.updateCaseAssignedEditor();
            entityManager.flush();
        });


        PromatCase updated = entityManager.find(PromatCase.class, activeId);
        assertThat("editor is retained", updated.getEditor(), is(notNullValue()));
        assertThat("editor is same editor", updated.getEditor().getId(), is(10));

        updated = entityManager.find(PromatCase.class, inactiveId);
        assertThat("editor is cleared", updated.getEditor(), is(nullValue()));

        // Delete the cases so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + activeId);
        assertThat("status code", response.getStatus(), is(200));
        response = deleteResponse("v1/api/cases/" + inactiveId);
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testUpdateCaseWithWeekcodeMovingToLater() throws OpenFormatConnectorException, JsonProcessingException, OpennumberRollConnectorException {

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
                .withTasks(Arrays.asList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(Arrays.asList("52000202"))
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyww", new Locale("da", "DK"));

        created.setStatus(CaseStatus.PENDING_EXPORT);
        when(mockedHandler.format(anyString()))
                .thenReturn(new BibliographicInformation()
                        .withCatalogcodes(Arrays.asList("BKM" + date.format(formatter), "ACC202001")));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

        // BKM is next-next week so PENDING_EXPORT should change back to APPROVED since it must wait another week

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.APPROVED));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testUpdateCaseWithWeekcodeNextWeek() throws OpenFormatConnectorException, JsonProcessingException, OpennumberRollConnectorException {

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
                .withTasks(Arrays.asList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(Arrays.asList("53000202"))
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyww", new Locale("da", "DK"));

        created.setStatus(CaseStatus.PENDING_EXPORT);
        when(mockedHandler.format(anyString()))
                .thenReturn(new BibliographicInformation()
                        .withCatalogcodes(Arrays.asList("BKM" + date.format(formatter), "ACC202001")));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());

        // BKM is next week so case must remain in status PENDING_EXPORT

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
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
                .withTasks(Arrays.asList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(Arrays.asList("53000303"))
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
                        .withCatalogcodes(Arrays.asList("BKM202152" , "ACC202203")));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.parse("2021-12-26"));

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testUpdateCaseWithWeekcodeFirstWeekOfNewYear() throws OpenFormatConnectorException, JsonProcessingException, OpennumberRollConnectorException {

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
                .withTasks(Arrays.asList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(Arrays.asList("53000304"))
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
                        .withCatalogcodes(Arrays.asList("BKM202201" , "ACC202203")));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.parse("2022-01-02"));

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testUpdateCaseWithWeekcodeSecondWeekOfNewYear() throws OpenFormatConnectorException, JsonProcessingException, OpennumberRollConnectorException {

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
                .withTasks(Arrays.asList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(Arrays.asList("53000305"))
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
                        .withCatalogcodes(Arrays.asList("BKM202202" , "ACC202203")));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.parse("2022-01-03"));

        // BKM is next shiftday (friday this week) so case must be moved to status PENDING_EXPORT

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testUpdateCaseWithWeekcodeLastWeekOf2026() throws OpenFormatConnectorException, JsonProcessingException, OpennumberRollConnectorException {

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
                .withTasks(Arrays.asList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(Arrays.asList("53000306"))
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
                        .withCatalogcodes(Arrays.asList("BKM202653" , "ACC202703")));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.parse("2026-12-27"));

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testUpdateCaseWithWeekcodeFirstWeekOf2027() throws OpenFormatConnectorException, JsonProcessingException, OpennumberRollConnectorException {

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
                .withTasks(Arrays.asList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(Arrays.asList("53000307"))
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
                        .withCatalogcodes(Arrays.asList("BKM202701" , "ACC202203")));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.parse("2027-01-02"));

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

    @Test
    public void testUpdateCaseWithWeekcodeSecondWeekOf2027() throws OpenFormatConnectorException, JsonProcessingException, OpennumberRollConnectorException {

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
                .withTasks(Arrays.asList(new TaskDto()
                        .withTaskType(TaskType.GROUP_1_LESS_THAN_100_PAGES)
                        .withTaskFieldType(TaskFieldType.BRIEF)
                        .withTargetFausts(Arrays.asList("53000308"))
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
                        .withCatalogcodes(Arrays.asList("BKM202702" , "ACC202203")));
        doAnswer(answer -> {
            PromatCase existing = ((PromatCase) answer.getArgument(0));
            existing.getTasks().stream().forEach(t -> t.setRecordId("123456789"));
            return null;
        }).when(mockedRepository).assignFaustnumber(any(PromatCase.class));

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.parse("2027-01-04"));

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(created));
        assertThat("status", created.getStatus(), is(CaseStatus.PENDING_EXPORT));

        // Delete the case so that we dont mess up payments and dataio-export tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }

}
