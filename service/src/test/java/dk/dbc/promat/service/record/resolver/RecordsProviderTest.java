package dk.dbc.promat.service.record.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.MarcBinding;
import dk.dbc.marc.binding.SubField;
import dk.dbc.promat.service.api.FbiApiHandler;
import dk.dbc.promat.service.api.RecordsProvider;
import dk.dbc.promat.service.connectors.FaustResolver;
import dk.dbc.promat.service.connectors.FaustResolverException;
import dk.dbc.promat.service.connectors.FbiApiConnector;
import dk.dbc.promat.service.connectors.FbiApiConnectorException;
import dk.dbc.promat.service.dto.RecordsListDto;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecordsProviderTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FaustResolver faustResolver;
    private FbiApiConnector fbiApiConnector;
    private RecordServiceConnector recordServiceConnector;
    private RecordsProvider provider;

    @BeforeEach
    void setUp() {
        faustResolver = mock(FaustResolver.class);
        fbiApiConnector = mock(FbiApiConnector.class);
        recordServiceConnector = mock(RecordServiceConnector.class);
        provider = new RecordsProvider(faustResolver, new FbiApiHandler(fbiApiConnector), recordServiceConnector);
    }

    // Stubs fbi-api's GraphQL response, so the real mapping in FbiApiHandler.recordInfo() is exercised.
    private void mockManifestation(String materialTypeGeneralCode, String materialTypeSpecificDisplay) throws FbiApiConnectorException {
        ObjectNode manifestation = MAPPER.createObjectNode();
        ArrayNode materialTypes = manifestation.putArray("materialTypes");
        ObjectNode materialType = materialTypes.addObject();
        materialType.putObject("materialTypeGeneral").put("code", materialTypeGeneralCode);
        if (materialTypeSpecificDisplay != null) {
            materialType.putObject("materialTypeSpecific").put("display", materialTypeSpecificDisplay);
        }
        ObjectNode root = MAPPER.createObjectNode();
        root.set("manifestation", manifestation);

        when(fbiApiConnector.execute(anyString(), anyMap(), any()))
                .thenAnswer(invocation -> MAPPER.treeToValue(root, invocation.getArgument(2, Class.class)));
    }

    @Test
    void isbn_materialtype_book() throws FaustResolverException, RecordServiceConnectorException, FbiApiConnectorException {
        String id = "9788712802846";
        String faust = "143328163";
        when(faustResolver.resolve(id)).thenReturn(Set.of(faust));
        mockManifestation("BOOKS", null);

        RecordsListDto recordsListDto = provider.getRecords(id);
        assertThat("One record is present", recordsListDto.getNumFound(), is(1));
        assertThat("Material type is book", recordsListDto.getRecords().getFirst().getTypes().getFirst().getMaterialType().toString(), is("BOOK"));
    }

    @Test
    void barcode_materialtype_movie() throws FaustResolverException, RecordServiceConnectorException, FbiApiConnectorException {
        String id = "5710768010115";
        String faust = "28775598";
        when(faustResolver.resolve(id)).thenReturn(Set.of(faust));
        mockManifestation("FILMS", null);

        RecordsListDto recordsListDto = provider.getRecords(id);
        assertThat("One record is present", recordsListDto.getNumFound(), is(1));
        assertThat("Material type is movie", recordsListDto.getRecords().getFirst().getTypes().getFirst().getMaterialType().toString(), is("MOVIE"));
    }

    @Test
    void barcode_materialtype_multimedia() throws FaustResolverException, RecordServiceConnectorException, FbiApiConnectorException {
        String id = "5026555432207";
        // Faust: 62765739
        String faust = "62765739";
        when(faustResolver.resolve(id)).thenReturn(Set.of(faust));
        mockManifestation("COMPUTER_GAMES", null);

        RecordsListDto recordsListDto = provider.getRecords(id);
        assertThat("One record is present", recordsListDto.getNumFound(), is(1));
        assertThat("Material type is multimmedia", recordsListDto.getRecords().getFirst().getTypes().getFirst().getMaterialType().toString(), is("MULTIMEDIA"));
    }

    @Test
    void faust_materialtype_movie_online() throws FaustResolverException, RecordServiceConnectorException, FbiApiConnectorException {
        String id = "135426296";
        String faust = "62765739";
        when(faustResolver.resolve(id)).thenReturn(Set.of(faust));
        mockManifestation("FILMS", "film (online)");

        RecordsListDto recordsListDto = provider.getRecords(id);
        assertThat("One record is present", recordsListDto.getNumFound(), is(1));
        assertThat("Material type is movie", recordsListDto.getRecords().getFirst().getTypes().getFirst().getMaterialType().toString(), is("MOVIE"));
        assertThat("Material specific type (online)", recordsListDto.getRecords().getFirst().getTypes().getFirst().getSpecificType(), is("film (online)"));
    }

    @Test
    void faust_materialtype_movie_dvd_or_blueray() throws FaustResolverException, RecordServiceConnectorException, FbiApiConnectorException {
        String id = "143424014";
        String faust = "143424014";
        when(faustResolver.resolve(id)).thenReturn(Set.of(faust));
        mockManifestation("FILMS", "film (dvd)");

        RecordsListDto recordsListDto = provider.getRecords(id);
        assertThat("One record is present", recordsListDto.getNumFound(), is(1));
        assertThat("Material type is movie", recordsListDto.getRecords().getFirst().getTypes().getFirst().getMaterialType().toString(), is("MOVIE"));
        assertThat("Material specific type (DVD or BlueRay)", recordsListDto.getRecords().getFirst().getTypes().getFirst().getSpecificType(), is("film (dvd)"));
    }

    @Test
    void faust_materialtype_multimedia() throws FaustResolverException, RecordServiceConnectorException, FbiApiConnectorException {
        String id = "143569608";
        String faust = "143569608";
        when(faustResolver.resolve(id)).thenReturn(Set.of(faust));
        mockManifestation("COMPUTER_GAMES", "playstation 5");

        RecordsListDto recordsListDto = provider.getRecords(id);
        assertThat("One record is present", recordsListDto.getNumFound(), is(1));
        assertThat("Material type is game", recordsListDto.getRecords().getFirst().getTypes().getFirst().getMaterialType().toString(), is("MULTIMEDIA"));
        assertThat("Material specific type (electronic material dvd-rom)", recordsListDto.getRecords().getFirst().getTypes().getFirst().getSpecificType(), is("playstation 5"));
    }

    @Test
    void fbiApiHasFullData_recordIncludesAllBibliographicFields() throws FaustResolverException, RecordServiceConnectorException, FbiApiConnectorException {
        String faust = "48951147";
        when(faustResolver.resolve(faust)).thenReturn(Set.of(faust));

        ObjectNode manifestation = MAPPER.createObjectNode();
        manifestation.putArray("creators").addObject().put("display", "Roger Crowley");
        manifestation.putArray("classifications").addObject().put("dk5Heading", "99.4").put("entryType", "MAIN_ENTRY");
        manifestation.putObject("edition").put("edition", "1. udgave");
        manifestation.putArray("identifiers").addObject().put("type", "ISBN").put("value", "9788771281118");
        ObjectNode materialType = manifestation.putArray("materialTypes").addObject();
        materialType.putObject("materialTypeGeneral").put("code", "BOOKS");
        materialType.putObject("materialTypeSpecific").put("display", "bog");
        manifestation.putObject("physicalDescription").put("summaryFull", "298 sider, ill.");
        manifestation.putArray("publisher").add("Rosenkilde & Bahnhof");
        manifestation.putObject("catalogueCodes").putArray("otherCatalogues").add("BKM201339");
        manifestation.putObject("titles").putArray("main").add("Konstantinopels fald");
        manifestation.putObject("materialSelection").putArray("selectionGroup").addObject().put("display", "Voksenafdelinger");
        manifestation.putArray("series").addObject().put("title", "En Serie").put("numberInSeries", "3");

        ObjectNode root = MAPPER.createObjectNode();
        root.set("manifestation", manifestation);
        when(fbiApiConnector.execute(anyString(), anyMap(), any()))
                .thenAnswer(invocation -> MAPPER.treeToValue(root, invocation.getArgument(2, Class.class)));

        RecordsListDto recordsListDto = provider.getRecords(faust);
        var record = recordsListDto.getRecords().getFirst();
        assertThat("Title", record.getTitle(), is("Konstantinopels fald"));
        assertThat("Creator", record.getCreator(), is("Roger Crowley"));
        assertThat("Publisher", record.getPublisher(), is("Rosenkilde & Bahnhof"));
        assertThat("Extent", record.getExtent(), is("298 sider, ill."));
        assertThat("Edition", record.getEdition(), is("1. udgave"));
        assertThat("Isbn", record.getIsbn(), is(List.of("9788771281118")));
        assertThat("Dk5", record.getDk5(), is(List.of("99.4")));
        assertThat("Series", record.getSeries(), is(List.of("En Serie, 3")));
        assertThat("TargetGroup", record.getTargetGroup(), is(List.of("Voksenafdelinger")));
        assertThat("CatalogCodes", record.getCatalogCodes(), is(List.of("BKM201339")));
        assertThat("Material type", record.getTypes().getFirst().getMaterialType().toString(), is("BOOK"));
        assertThat("Material specific type", record.getTypes().getFirst().getSpecificType(), is("bog"));
    }

    @Test
    void isbn_not_found() throws FaustResolverException, RecordServiceConnectorException, FbiApiConnectorException {
        String id = "9788764439999";
        // Faust: Ingen fundet
        when(faustResolver.resolve(id)).thenReturn(Set.of());

        RecordsListDto recordsListDto = provider.getRecords(id);
        assertThat("No records are present", recordsListDto.getNumFound(), is(0));
    }

    @Test
    void invalid_input() throws FaustResolverException, RecordServiceConnectorException, FbiApiConnectorException {
        String id = "9";
        when(faustResolver.resolve(id)).thenReturn(Set.of());

        RecordsListDto recordsListDto = provider.getRecords(id);
        assertThat("No records are present", recordsListDto.getNumFound(), is(0));
    }

    @Test
    void search_byTitleAndCreator_mapsHitsToRecords() throws FbiApiConnectorException {
        ObjectNode manifestation1 = MAPPER.createObjectNode();
        manifestation1.put("pid", "870970-basis:11111111");
        manifestation1.putObject("titles").putArray("main").add("Bogen om noget");
        manifestation1.putArray("creators").addObject().put("display", "Forfatter Et");
        manifestation1.putArray("materialTypes").addObject()
                .putObject("materialTypeGeneral").put("code", "BOOKS");

        ObjectNode manifestation2 = MAPPER.createObjectNode();
        manifestation2.put("pid", "870970-basis:22222222");
        manifestation2.putObject("titles").putArray("main").add("En anden bog");
        manifestation2.putArray("creators").addObject().put("display", "Forfatter Et");

        ObjectNode work1 = MAPPER.createObjectNode();
        work1.putObject("manifestations").putArray("bestRepresentations").add(manifestation1);
        ObjectNode work2 = MAPPER.createObjectNode();
        work2.putObject("manifestations").putArray("bestRepresentations").add(manifestation2);

        ObjectNode complexSearch = MAPPER.createObjectNode();
        complexSearch.put("hitcount", 2);
        complexSearch.putArray("works").add(work1).add(work2);

        ObjectNode root = MAPPER.createObjectNode();
        root.set("complexSearch", complexSearch);

        when(fbiApiConnector.execute(anyString(), anyMap(), any()))
                .thenAnswer(invocation -> MAPPER.treeToValue(root, invocation.getArgument(2, Class.class)));

        RecordsListDto result = provider.search("noget", "Forfatter Et", 100);
        assertThat("Two hits found", result.getNumFound(), is(2));
        assertThat("First hit faust", result.getRecords().get(0).getFaust(), is("11111111"));
        assertThat("First hit title", result.getRecords().get(0).getTitle(), is("Bogen om noget"));
        assertThat("First hit not marked primary", result.getRecords().get(0).isPrimary(), is(false));
        assertThat("Second hit faust", result.getRecords().get(1).getFaust(), is("22222222"));
        assertThat("Second hit title", result.getRecords().get(1).getTitle(), is("En anden bog"));
    }

    @Test
    void search_withNoSearchTerms_returnsEmptyWithoutCallingFbiApi() throws FbiApiConnectorException {
        RecordsListDto result = provider.search(null, "", 100);
        assertThat("No hits", result.getNumFound(), is(0));
        verifyNoInteractions(fbiApiConnector);
    }

    @Test
    void fbiApiHasNoData_rawrepoFallbackIncludesTitle() throws FaustResolverException, RecordServiceConnectorException, FbiApiConnectorException {
        String faust = "51785347";
        when(faustResolver.resolve(faust)).thenReturn(Set.of(faust));
        // fbi-api knows nothing about this manifestation
        when(fbiApiConnector.execute(anyString(), anyMap(), any()))
                .thenAnswer(invocation -> MAPPER.treeToValue(
                        MAPPER.createObjectNode().putNull("manifestation"), invocation.getArgument(2, Class.class)));
        when(recordServiceConnector.recordExists(RecordsProvider.DBC_AGENCY, faust)).thenReturn(true);
        MarcBinding marcBinding = new MarcBinding().addField(
                new DataField("245", "00").addSubField(new SubField('a', "Den lukkede bog")));
        when(recordServiceConnector.getRecordContentCollection(RecordsProvider.DBC_AGENCY, faust))
                .thenReturn(List.of(marcBinding));

        RecordsListDto recordsListDto = provider.getRecords(faust);
        assertThat("One record is present", recordsListDto.getNumFound(), is(1));
        assertThat("Faust is set", recordsListDto.getRecords().getFirst().getFaust(), is(faust));
        assertThat("Title is resolved from rawrepo", recordsListDto.getRecords().getFirst().getTitle(), is("Den lukkede bog"));
    }

    @Test
    void fbiApiHasNoData_rawrepoFallbackWithoutTitleField() throws FaustResolverException, RecordServiceConnectorException, FbiApiConnectorException {
        String faust = "51785347";
        when(faustResolver.resolve(faust)).thenReturn(Set.of(faust));
        when(fbiApiConnector.execute(anyString(), anyMap(), any()))
                .thenAnswer(invocation -> MAPPER.treeToValue(
                        MAPPER.createObjectNode().putNull("manifestation"), invocation.getArgument(2, Class.class)));
        when(recordServiceConnector.recordExists(RecordsProvider.DBC_AGENCY, faust)).thenReturn(true);
        when(recordServiceConnector.getRecordContentCollection(RecordsProvider.DBC_AGENCY, faust))
                .thenReturn(List.of(new MarcBinding()));

        RecordsListDto recordsListDto = provider.getRecords(faust);
        assertThat("One record is present", recordsListDto.getNumFound(), is(1));
        assertThat("Title is absent", recordsListDto.getRecords().getFirst().getTitle(), is(nullValue()));
    }
}
