package dk.dbc.promat.service.batch;

import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.MarcBinding;
import dk.dbc.marc.binding.SubField;
import dk.dbc.promat.service.MetakompasRegistration;
import dk.dbc.promat.service.api.BibliographicInformation;
import dk.dbc.promat.service.api.FbiApiHandler;
import dk.dbc.promat.service.api.RecordsProvider;
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
import dk.dbc.rawrepo.record.RecordServiceConnector;
import org.junit.jupiter.api.Test;

import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
                        "48959939", getFbiApiResponseFromResource("48959939"),
                        "48959912", getFbiApiResponseFromResource("48959912"),
                        "48959955", getFbiApiResponseFromResource("48959955")
                );

        PromatCase promatCase = getCaseWithId(created.getId());
        ScheduledCaseInformationUpdater upd = configure();
        FbiApiHandler fbiApiHandler = mock(FbiApiHandler.class);
        upd.caseInformationUpdater.fbiApiHandler = fbiApiHandler;
        when(fbiApiHandler.format(anyString()))
                .thenAnswer(invocationOnMock -> fbiApiHandlerResponse.get(invocationOnMock.getArgument(0)));

        // Fausts whose record carries a Metakompas registration (a subject in 665). Every record
        // has a regular cataloguing subject (666) as well, which must not count as a registration.
        Set<String> registeredFausts = new HashSet<>();
        RecordServiceConnector recordServiceConnector = mock(RecordServiceConnector.class);
        upd.caseInformationUpdater.recordServiceConnector = recordServiceConnector;
        when(recordServiceConnector.getRecordContentCollection(anyInt(), anyString(), any(RecordServiceConnector.Params.class)))
                .thenAnswer(invocationOnMock -> {
                    String faust = invocationOnMock.getArgument(1);
                    MarcBinding marcBinding = new MarcBinding()
                            .addField(new DataField("001", "00").addSubField(new SubField('a', faust)))
                            .addField(new DataField("666", "00").addSubField(new SubField('s', "Danmark")));
                    if (registeredFausts.contains(faust)) {
                        marcBinding.addField(new DataField("665", "00")
                                .addSubField(new SubField('&', "lektor"))
                                .addSubField(new SubField('n', "hyggelig")));
                    }
                    return List.of(marcBinding);
                });

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
        registeredFausts.add("48959939");
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(promatCase));
        created = getCaseWithId(promatCase.getId());
        List<PromatTask> tasks = getTasksWhereMetakompasIsPresent(created);
        assertThat("There is only one finished.", tasks.size(), is(1));
        assertThat("And it is the primaryfaust", tasks.get(0).getTargetFausts().contains("48959939"), is(true));

        //
        // Third round: Metadata for one of the related faust has been done. There is still only one in
        // the list of done Metakompas tasks.
        //
        registeredFausts.add("48959955");
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
        registeredFausts.add("48959912");
        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(promatCase));
        created = getCaseWithId(promatCase.getId());

        tasks = getTasksWhereMetakompasIsPresent(created);
        assertThat("They all are finished.", tasks.size(), is(2));

        created = getCaseWithId(promatCase.getId());
        assertThat("case closed", created.getStatus(), is(CaseStatus.APPROVED));

        // The primary faust was looked up in rounds one and two only - once found registered, its
        // task is skipped. Every lookup is of the merged 870970 record as MARC JSON.
        verify(recordServiceConnector, times(2)).getRecordContentCollection(eq(RecordsProvider.DBC_AGENCY), eq("48959939"),
                argThat(params -> params.getMode().equals(Optional.of(RecordServiceConnector.Params.Mode.MERGED))
                        && params.getOutputFormat().equals(Optional.of(RecordServiceConnector.Params.OutputFormat.MARC_JSON))));

        // Delete the case so that we dont mess up payments and dataio-export tests
        deleteTestCase(created.getId());
    }

    @Test
    public void testMetakompasSelectionIsLeftAloneWhenRegisteredInPromat() throws Exception {
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("48959939")
                .withTitle("Title for 48959939")
                .withDetails("Details for 48959939")
                .withMaterialType(MaterialType.BOOK)
                .withTasks(List.of(new TaskDto()
                        .withTaskType(TaskType.GROUP_2_100_UPTO_199_PAGES)
                        .withTaskFieldType(TaskFieldType.METAKOMPAS)
                        .withTargetFausts(List.of("48959939"))))
                .withDeadline("2024-08-07")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1);
        PromatCase created = postAndAssert("v1/api/cases", dto, PromatCase.class, Response.Status.CREATED);
        PromatCase promatCase = getCaseWithId(created.getId());

        // A selection saved through tasks/{taskId}/metakompas, on a record that already has a 665
        String selection = "{\"entries\":[],\"suggestions\":[{\"path\":[\"stemning\",\"positiv\"],\"title\":\"hyggelig\"}]}";
        PromatTask task = PromatTaskUtils.getTasksOfType(promatCase, TaskFieldType.METAKOMPAS).get(0);
        task.setData(selection);

        ScheduledCaseInformationUpdater upd = configure();
        upd.caseInformationUpdater.metakompasRegistration = new MetakompasRegistration(MetakompasRegistration.Mode.PROMAT);
        upd.caseInformationUpdater.fbiApiHandler = mockFbiApiHandler(getFbiApiResponseFromResource("48959939"));
        RecordServiceConnector recordServiceConnector = mock(RecordServiceConnector.class);
        upd.caseInformationUpdater.recordServiceConnector = recordServiceConnector;
        when(recordServiceConnector.getRecordContentCollection(anyInt(), anyString(), any(RecordServiceConnector.Params.class)))
                .thenReturn(List.of(new MarcBinding()
                        .addField(new DataField("001", "00").addSubField(new SubField('a', "48959939")))
                        .addField(new DataField("665", "00").addSubField(new SubField('n', "hyggelig")))));

        persistenceContext.run(() -> upd.caseInformationUpdater.updateCaseInformation(promatCase));

        assertThat("selection kept", task.getData(), is(selection));
        assertThat("not approved by the updater", task.getApproved(), is(nullValue()));
        verify(recordServiceConnector, never()).getRecordContentCollection(anyInt(), anyString(), any(RecordServiceConnector.Params.class));

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
