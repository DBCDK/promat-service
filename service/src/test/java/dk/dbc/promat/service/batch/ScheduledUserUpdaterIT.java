package dk.dbc.promat.service.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import dk.dbc.commons.persistence.TransactionScopedPersistenceContext;
import dk.dbc.connector.openformat.OpenFormatConnectorException;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.cluster.ServerRole;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.Reviewer;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScheduledUserUpdaterIT extends ContainerTest {
    private TransactionScopedPersistenceContext persistenceContext;
    private final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final Counter counter = mock(Counter.class);
    private static WireMockServer wireMockServer;

    @BeforeAll
    public static void startWiremock() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    public static void stopWiremock() {
        wireMockServer.stop();
    }

    @BeforeEach
    public void setup() {
        persistenceContext = new TransactionScopedPersistenceContext(entityManager);
        when(metricRegistry.counter(any(Metadata.class))).thenReturn(counter);
    }

    @Test
    public void testUserUpdatesDeactivatedUser() throws JsonProcessingException, OpenFormatConnectorException {

        ScheduledUserUpdater upd = new ScheduledUserUpdater();
        upd.userUpdater = new UserUpdater();
        upd.userUpdater.metricRegistry = metricRegistry;
        upd.entityManager = entityManager;
        upd.serverRole = ServerRole.PRIMARY;

        // Fetch all users as they where before the test..
        final List<Editor> allEditorsBeforeUpdate = getAllEditors();
        final List<Reviewer> allReviewersBeforeUpdate = getAllReviewers();

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
                .findFirst().orElseThrow()
                .setActiveChanged(Date.from(inactive));

        // Verify user state before running the update
        assertThat("number of editors", allEditorsBeforeUpdate.size(), greaterThanOrEqualTo(5));
        assertThat("number of reviewers", allReviewersBeforeUpdate.size(), greaterThanOrEqualTo(10));

        assertThat("no deactivated editors", allEditorsBeforeUpdate.stream()
                .filter(e -> e.getDeactivated() != null).count(), is(0L));
        assertThat("no deactivated reviewers", allReviewersBeforeUpdate.stream()
                .filter(r -> r.getDeactivated() != null).count(), is(0L));

        assertThat("0 editors to deactivate", allEditorsBeforeUpdate.stream()
                .filter(e -> !e.isActive() && e.getActiveChanged().toInstant().isBefore(ZonedDateTime.now().minusYears(5).toInstant()))
                .count(), is(0L));

        assertThat("1 reviewer to deactivate", allReviewersBeforeUpdate.stream()
                .filter(r -> !r.isActive() && r.getActiveChanged().toInstant().isBefore(ZonedDateTime.now().minusYears(5).toInstant()))
                .count(), is(1L));

        // Check that the getInactiveXxx() functions returns the correct number of users
        // Note: getInactiveReviewers will return 1+1 since user 15 is currently inactive, but will
        //       be updated before running the test, to be inactive for slightly less than 5 years
        assertThat("0 editors to deactivate", upd.getInactiveEditors().size(), is(0));
        assertThat("2 reviewers to deactivate", upd.getInactiveReviewers().size(), is(2));

        persistenceContext.run(() -> {

            Reviewer reviewer = entityManager.find(Reviewer.class, 15);
            reviewer.setActiveChanged(Date.from(inactive));
            entityManager.persist(reviewer);
            entityManager.flush();

            upd.updateUsers();

            entityManager.flush();
        });

        // Verify that all editors is untouched
        final List<Editor> allEditorsAfterUpdate = getAllEditors();
        assertThat("no editors has changed", allEditorsAfterUpdate, is(allEditorsBeforeUpdate));

        // Verify that all but one reviewer is untouched
        final List<Reviewer> allReviewersAfterUpdate = getAllReviewers();
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
                .findFirst().orElseThrow();
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
        assertThat("deactivated is now", updated.getDeactivated().after(
                Date.from(ZonedDateTime.now().minusMinutes(1).toInstant())), is(true));
    }

    public List<Reviewer> getAllReviewers() {
        return entityManager
                .createQuery("SELECT r FROM Reviewer r", Reviewer.class)
                .getResultList();
    }

    public List<Editor> getAllEditors() {
        return entityManager
                .createQuery("SELECT e FROM Editor e", Editor.class)
                .getResultList();
    }

}
