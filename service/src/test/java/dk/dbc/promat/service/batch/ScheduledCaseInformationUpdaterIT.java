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
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.api.BibliographicInformation;
import dk.dbc.promat.service.api.OpenFormatHandler;
import dk.dbc.promat.service.cluster.ServerRole;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PromatCase;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;

import java.util.Arrays;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScheduledCaseInformationUpdaterIT extends ContainerTest {
    private TransactionScopedPersistenceContext persistenceContext;
    private final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final SimpleTimer timer = mock(SimpleTimer.class);
    private final ConcurrentGauge gauge = mock(ConcurrentGauge.class);
    private static WireMockServer wireMockServer;
    private static String wiremockHost;

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
    public void testUpdateCaseWithEmptyWeekcode() throws JsonProcessingException, OpenFormatConnectorException {

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
                        .withCatalogcodes(Arrays.asList("BKM999999")));
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
}
