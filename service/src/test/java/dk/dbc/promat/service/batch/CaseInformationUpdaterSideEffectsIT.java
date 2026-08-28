package dk.dbc.promat.service.batch;

import dk.dbc.promat.service.api.BibliographicInformation;
import dk.dbc.promat.service.api.FbiApiHandler;
import dk.dbc.promat.service.connectors.FbiApiConnectorException;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.dto.TaskDto;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.persistence.TaskType;
import dk.dbc.promat.service.util.PromatTaskUtils;
import org.junit.jupiter.api.Test;

import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the two side effects CaseInformationUpdater applies alongside the main
 * title/weekcode sync: Metakompas subject-data tracking and fulltext-link lookup.
 */
public class CaseInformationUpdaterSideEffectsIT extends CaseInformationUpdaterTestBase {

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

        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);

        Map<String, BibliographicInformation> fbiApiHandlerResponse =
                Map.of(
                        "48959939", getFbiApiResponseFromResource("48959939").withMetakompassubject(null),
                        "48959912", getFbiApiResponseFromResource("48959912").withMetakompassubject("false"),
                        "48959955", getFbiApiResponseFromResource("48959955").withMetakompassubject(null)
                );

        PromatCase promatCase = getCaseWithId(created.getId());
        ScheduledCaseInformationUpdater upd = configure();
        FbiApiHandler fbiApiHandler = mock(FbiApiHandler.class);
        upd.caseInformationUpdater.fbiApiHandler = fbiApiHandler;
        when(fbiApiHandler.format(anyString()))
                .thenAnswer(invocationOnMock -> fbiApiHandlerResponse.get(invocationOnMock.getArgument(0)));

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
        fbiApiHandlerResponse.get("48959939").setMetakompassubject(CaseInformationUpdater.METAKOMPASDATA_PRESENT);
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(promatCase));
        created = getCaseWithId(promatCase.getId());
        List<PromatTask> tasks = getTasksWhereMetakompasIsPresent(created);
        assertThat("There is only one finished.", tasks.size(), is(1));
        assertThat("And it is the primaryfaust", tasks.get(0).getTargetFausts().contains("48959939"), is(true));

        //
        // Third round: Metadata for one of the related faust has been done. There is still only one in
        // the list of done Metakompas tasks.
        //
        fbiApiHandlerResponse.get("48959955").setMetakompassubject(CaseInformationUpdater.METAKOMPASDATA_PRESENT);
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(promatCase));
        created = getCaseWithId(promatCase.getId());
        tasks = getTasksWhereMetakompasIsPresent(created);
        assertThat("There is only one finished.", tasks.size(), is(1));
        assertThat("And it is the task with the primaryfaust", tasks.get(0).getTargetFausts().contains("48959939"), is(true));

        //
        // Fourth round: Metadata for both of the related faust has been done.
        // AND let's say we updated the case to PENDING_EXTERNAL.
        //
        promatCase.setStatus(CaseStatus.PENDING_EXTERNAL);
        entityManager.persist(promatCase);
        fbiApiHandlerResponse.get("48959912").setMetakompassubject(CaseInformationUpdater.METAKOMPASDATA_PRESENT);
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(promatCase));
        created = getCaseWithId(promatCase.getId());

        tasks = getTasksWhereMetakompasIsPresent(created);
        assertThat("They all are finished.", tasks.size(), is(2));

        created = getCaseWithId(promatCase.getId());
        assertThat("case closed", created.getStatus(), is(CaseStatus.APPROVED));

        // Delete the case so that we dont mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

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

    private BibliographicInformation getFbiApiResponseFromResource(String faust) throws IOException {
        return mapper.readValue(
                Files.readString(
                        Path.of(Objects.requireNonNull(CaseInformationUpdaterSideEffectsIT.class
                                        .getResource(String.format("/openformat/%s.json", faust)))
                .getPath())), BibliographicInformation.class);
    }
}
