package dk.dbc.promat.service.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.api.BibliographicInformation;
import dk.dbc.promat.service.api.FbiApiHandler;
import dk.dbc.promat.service.connectors.FbiApiConnectorException;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PromatCase;
import org.junit.jupiter.api.Test;

import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.core.Response;
import java.util.List;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for ScheduledCaseInformationUpdater's own @Schedule entry points (batch-scanning +
 * PRIMARY-server-role gating). Tests for the per-case update logic in CaseInformationUpdater
 * live in the other CaseInformationUpdater*IT classes in this package.
 */
public class ScheduledCaseInformationUpdaterIT extends CaseInformationUpdaterTestBase {

    @Test
    public void testCaseUpdates() throws JsonProcessingException, FbiApiConnectorException {

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
        FbiApiHandler mockedHandler = mock(FbiApiHandler.class);
        upd.caseInformationUpdater.fbiApiHandler = mockedHandler;
        when(mockedHandler.format(created.getPrimaryFaust()))
                .thenReturn(new BibliographicInformation()
                        .withTitle("UPDATED_TITLE")
                        .withCatalogcodes(List.of("BKM999999"))
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

        // Delete the case so that we don't mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testClearInactiveEditors() throws JsonProcessingException, FbiApiConnectorException {

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

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        dto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        postAndAssert("v1/api/cases/" + created.getId(), dto, Response.Status.OK);

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

        created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        dto = new CaseRequest().withStatus(CaseStatus.PENDING_APPROVAL);
        postAndAssert("v1/api/cases/" + created.getId(), dto, Response.Status.OK);

        Integer inactiveId = created.getId();

        // Send the active case back to the reviewer, then again to approval - to set the keepEditor flag
        dto = new CaseRequest().withStatus(CaseStatus.PENDING_ISSUES).withEditor(10);
        postAndAssert("v1/api/cases/" + activeId, dto, Response.Status.OK);
        dto.setStatus(CaseStatus.PENDING_APPROVAL);
        postAndAssert("v1/api/cases/" + activeId, dto, Response.Status.OK);

        // Run nightly update to clear the editor on the 'inactive case, but retain the editor on the 'active' case
        ScheduledCaseInformationUpdater upd = configure();

        persistenceContext.run(() -> {
            upd.updateCaseAssignedEditor();
            entityManager.flush();
        });

        PromatCase updated = entityManager.find(PromatCase.class, activeId);
        assertThat("editor is retained", updated.getEditor(), is(notNullValue()));
        assertThat("editor is same editor", updated.getEditor().getId(), is(10));

        updated = entityManager.find(PromatCase.class, inactiveId);
        assertThat("editor is cleared", updated.getEditor(), is(nullValue()));

        // Delete the cases so that we don't mess up payments and dataio-export tests
        deleteTestCase(activeId);
        deleteTestCase(inactiveId);
    }
}
