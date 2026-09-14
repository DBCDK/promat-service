package dk.dbc.promat.service.batch;

import dk.dbc.promat.service.api.BibliographicInformation;
import dk.dbc.promat.service.connectors.FbiApiConnectorException;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.dto.TaskDto;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.persistence.TaskType;
import org.junit.jupiter.api.Test;

import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the fulltext-link lookup side effect CaseInformationUpdater applies alongside the
 * main title/weekcode sync. (The Metakompas subject-data tracking side effect that used to live
 * here was removed - promat-service now stores the reviewer's actual Metakompas selection
 * directly on the task itself, via tasks/{taskId}/metakompas, rather than inferring completion
 * indirectly from fbi-api's bibliographic metadata.)
 */
public class CaseInformationUpdaterSideEffectsIT extends CaseInformationUpdaterTestBase {

    @Test
    public void testThatFulltextLinksAreUpdated() throws FbiApiConnectorException {
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

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        PromatCase promatCase = getCaseWithId(created.getId());
        ScheduledCaseInformationUpdater upd = configure();
        upd.caseInformationUpdater.fbiApiHandler = mockFbiApiHandler(new BibliographicInformation()
                .withCatalogcodes(new ArrayList<>()));

        ContentLookUp contentLookUpMock = mock(ContentLookUp.class);
        upd.caseInformationUpdater.contentLookUp = contentLookUpMock;
        when(contentLookUpMock.lookUpContent("48959940")).thenReturn(Optional.of(DOWNLOAD_LINK));

        //
        // Now do an update, and confirm that the corrct link is present.
        //
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(promatCase));
        assertThat("Download link is now present", promatCase.getFulltextLink(), is(DOWNLOAD_LINK));

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    private PromatCase getCaseWithId(Integer id) {
        TypedQuery<PromatCase> query = entityManager.createQuery(
                "SELECT c FROM PromatCase c " +
                        "WHERE c.id = :id", PromatCase.class);
        query.setParameter("id", id);
        return query.getSingleResult();
    }
}
