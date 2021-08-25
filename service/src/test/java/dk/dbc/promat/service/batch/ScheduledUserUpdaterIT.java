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
import dk.dbc.promat.service.persistence.Address;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.PromatUser;
import dk.dbc.promat.service.persistence.Reviewer;
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

import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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

public class ScheduledUserUpdaterIT extends ContainerTest {
    private TransactionScopedPersistenceContext persistenceContext;
    private final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final ConcurrentGauge gauge = mock(ConcurrentGauge.class);
    private static WireMockServer wireMockServer;
    private static String wiremockHost;
    private static PromatTaskUtils promatTaskUtils;
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledUserUpdaterIT.class);

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
        when(metricRegistry.concurrentGauge(any(Metadata.class))).thenReturn(gauge);
    }

    @Test
    public void testUserUpdatesDeactivatedUser() throws JsonProcessingException, OpenFormatConnectorException {

        ScheduledUserUpdater upd = new ScheduledUserUpdater();
        upd.userUpdater = new UserUpdater();
        upd.userUpdater.metricRegistry = metricRegistry;
        upd.entityManager = entityManager;
        upd.serverRole = ServerRole.PRIMARY;

        // Fetch all users as they where before the test..
        final List<Editor> allEditorsBeforeUpdate = upd.getAllEditors();
        final List<Reviewer> allReviewersBeforeUpdate = upd.getAllReviewers();

        // Detach entities to prevent them from updating during the update
        for( Editor editor : allEditorsBeforeUpdate ) {
            entityManager.detach(editor);
        }
        for( Reviewer reviewer : allReviewersBeforeUpdate ) {
            entityManager.detach(reviewer);
        }

        // Manually update activeChanged for reviewer 15, set it just before 5 years
        Instant inactive = ZonedDateTime.now().minusYears(5).plusDays(1).toInstant();
        allReviewersBeforeUpdate.stream()
                .filter(r -> r.getId() == 15)
                .findFirst().get()
                .setActiveChanged(Date.from(inactive));

        // Verify user state before running the update
        assertThat("number of editors", allEditorsBeforeUpdate.size(), is(5));
        assertThat("number of reviewers", allReviewersBeforeUpdate.size(), is(10));

        assertThat("no deactivated editors", allEditorsBeforeUpdate.stream()
                .filter(e -> e.getDeactivated() != null).count(), is(0L));
        assertThat("no deactivated reviewers", allReviewersBeforeUpdate.stream()
                .filter(r -> r.getDeactivated() != null).count(), is(0L));

        assertThat("0 editors to deactivate", allEditorsBeforeUpdate.stream()
                .filter(e -> e.getActiveChanged().toInstant().isBefore(ZonedDateTime.now().minusYears(5).toInstant()))
                .count(), is(0L));

        assertThat("1 reviewer to deactivate", allReviewersBeforeUpdate.stream()
                .filter(r -> !r.isActive() && r.getActiveChanged().toInstant().isBefore(ZonedDateTime.now().minusYears(5).toInstant()))
                .count(), is(1L));

        persistenceContext.run(() -> {

            Reviewer reviewer = entityManager.find(Reviewer.class, 15);
            reviewer.setActiveChanged(Date.from(inactive));
            entityManager.persist(reviewer);
            entityManager.flush();

            upd.updateUsers();

            entityManager.flush();
        });

        // Verify that all editors is untouched
        final List<Editor> allEditorsAfterUpdate = upd.getAllEditors();
        assertThat("no editors has changed", allEditorsAfterUpdate, is(allEditorsBeforeUpdate));

        // Verify that all but one reviewer is untouched
        final List<Reviewer> allReviewersAfterUpdate = upd.getAllReviewers();
        assertThat("most reviewers has not changed",
                allReviewersAfterUpdate.stream()
                        .filter(r -> r.getId() != 9)
                        .collect(Collectors.toList()),
                is(allReviewersBeforeUpdate.stream()
                        .filter(r -> r.getId() != 9)
                        .collect(Collectors.toList())));

        // reviewer with id 9 has changed
        // (compare address fields directly since 'selected' will always become true for the work address, if
        // both work address and private address has 'selected' set to false)
        Reviewer updated = allReviewersAfterUpdate.stream()
                .filter(r -> r.getId() == 9)
                .findFirst().get();
        assertThat("email cleared", updated.getEmail().equals(""));
        assertThat("private email cleared", updated.getPrivateEmail().equals(""));
        assertThat("phone cleared", updated.getPhone().equals(""));
        assertThat("private phone cleared", updated.getPrivatePhone().equals(""));
        assertThat("address1 cleared", updated.getAddress().getAddress1(), is(nullValue()));
        assertThat("address2 cleared", updated.getAddress().getAddress2(), is(nullValue()));
        assertThat("city cleared", updated.getAddress().getCity(), is(nullValue()));
        assertThat("zip cleared", updated.getAddress().getZip(), is(nullValue()));
        assertThat("selected", updated.getAddress().getSelected(), is(true));
        assertThat("private address1 cleared", updated.getPrivateAddress().getAddress1(), is(nullValue()));
        assertThat("private address2 cleared", updated.getPrivateAddress().getAddress2(), is(nullValue()));
        assertThat("private city cleared", updated.getPrivateAddress().getCity(), is(nullValue()));
        assertThat("private zip cleared", updated.getPrivateAddress().getZip(), is(nullValue()));
        assertThat("private selected", updated.getPrivateAddress().getSelected(), is(false));
        assertThat("deactivated has been set", updated.getDeactivated(), is(notNullValue()));
        assertThat("deactivated has been set", updated.getDeactivated().after(
                Date.from(ZonedDateTime.now().minusMinutes(1).toInstant())), is(true));
    }
}
