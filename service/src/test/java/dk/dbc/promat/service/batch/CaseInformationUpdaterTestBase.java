package dk.dbc.promat.service.batch;

import com.github.tomakehurst.wiremock.WireMockServer;
import dk.dbc.commons.persistence.TransactionScopedPersistenceContext;
import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.opennumberroll.OpennumberRollConnectorException;
import dk.dbc.promat.service.AuthMocks;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.Dates;
import dk.dbc.promat.service.FbiApiMocks;
import dk.dbc.promat.service.Repository;
import dk.dbc.promat.service.api.BibliographicInformation;
import dk.dbc.promat.service.api.FbiApiHandler;
import dk.dbc.promat.service.cluster.ServerRole;
import dk.dbc.promat.service.connectors.FbiApiConnectorException;
import dk.dbc.promat.service.connectors.FbiApiConnectorProducer;
import dk.dbc.promat.service.persistence.PromatCase;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Shared wiremock/mock plumbing for the CaseInformationUpdater/ScheduledCaseInformationUpdater
 * IT suite, split across several *IT classes by behavior. Not itself a test class (no "IT"
 * suffix) so Surefire/Failsafe won't try to run it.
 */
public abstract class CaseInformationUpdaterTestBase extends ContainerTest {
    protected TransactionScopedPersistenceContext persistenceContext;
    protected static final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final Timer timer = mock(Timer.class);
    private final Counter gauge = mock(Counter.class);
    protected static WireMockServer wireMockServer;
    protected static Locale dkLocale = new Locale("da", "DK");
    private static String wiremockHost;

    @BeforeAll
    static void startWiremock() {
        wireMockServer = new WireMockServer(options().dynamicPort()
                .extensions(FbiApiMocks.bodyTransformer()));
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
        wiremockHost = wireMockServer.baseUrl();

        AuthMocks.mockFBILoginAuth(wireMockServer, "123456789", "abcdef");
        FbiApiMocks.mockFbiApiResponses(wireMockServer);
    }

    @AfterAll
    static void stopWiremock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setup() {
        persistenceContext = new TransactionScopedPersistenceContext(entityManager);
        when(metricRegistry.timer(any(Metadata.class))).thenReturn(timer);
        when(metricRegistry.counter(any(Metadata.class))).thenReturn(gauge);
    }

    protected void deleteTestCase(Integer id) {
        try (Response response = deleteResponse("v1/api/cases/" + id)) {
            assertThat("status code", response.getStatus(), is(200));
        }
    }

    protected String weekcode(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyww", dkLocale);
        String weekcodeDate = date.format(formatter);

        int weekcodeYear = Integer.parseInt(weekcodeDate.substring(0, 4));
        int weekcodeWeek = Integer.parseInt(weekcodeDate.substring(4, 6));
        String currentWeekcodeDate = LocalDate.now().format(formatter);
        int currentWeekcodeYear = Integer.parseInt(currentWeekcodeDate.substring(0, 4));
        int currentWeekcodeWeek = Integer.parseInt(currentWeekcodeDate.substring(4, 6));
        if (weekcodeWeek < currentWeekcodeWeek && currentWeekcodeYear == weekcodeYear) {
            // We are end-of-year, so '2024-12-30' should become '202501', but instead is
            // formatted as '202401' which is no good
            weekcodeDate = String.format("%04d%02d", weekcodeYear + 1, weekcodeWeek);
        }

        return weekcodeDate;
    }

    protected String weekcode(String catalogCode, LocalDate date) {
        return catalogCode + weekcode(date);
    }

    // entityManager/serverRole are always wired so both call styles work: tests driving
    // CaseInformationUpdater directly, and tests exercising the @Schedule entry points on
    // ScheduledCaseInformationUpdater (which silently no-op when serverRole isn't PRIMARY).
    protected ScheduledCaseInformationUpdater configure() {
        ScheduledCaseInformationUpdater upd = new ScheduledCaseInformationUpdater();
        upd.caseInformationUpdater = new CaseInformationUpdater();
        upd.caseInformationUpdater.metricRegistry = metricRegistry;
        upd.entityManager = entityManager;
        upd.serverRole = ServerRole.PRIMARY;
        upd.caseInformationUpdater.fbiApiHandler = new FbiApiHandler()
                .withConnector(FbiApiConnectorProducer.produce(wiremockHost, wiremockHost, "123456789",
                        "abcdef", new UserAgent("PROMAT_IT")));
        ContentLookUp contentLookUpMock = mock(ContentLookUp.class);
        upd.caseInformationUpdater.contentLookUp = contentLookUpMock;
        when(contentLookUpMock.lookUpContent(anyString())).thenReturn(Optional.empty());

        Dates mockedDates = mock(Dates.class);
        upd.caseInformationUpdater.dates = mockedDates;
        when(mockedDates.getCurrentDate()).thenReturn(LocalDate.now());
        return upd;
    }

    protected FbiApiHandler mockFbiApiHandler(BibliographicInformation info) throws FbiApiConnectorException {
        FbiApiHandler handler = mock(FbiApiHandler.class);
        when(handler.format(anyString())).thenReturn(info);
        return handler;
    }

    protected Repository mockRepositoryAssigningRecordId(String recordId) throws OpennumberRollConnectorException {
        Repository repository = mock(Repository.class);
        doAnswer(invocation -> {
            PromatCase existing = invocation.getArgument(0);
            existing.getTasks().forEach(t -> t.setRecordId(recordId));
            return null;
        }).when(repository).assignFaustnumber(any(PromatCase.class));
        return repository;
    }
}
