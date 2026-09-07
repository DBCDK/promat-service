package dk.dbc.promat.service.record.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecordsProviderTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FaustResolver faustResolver;
    private FbiApiConnector fbiApiConnector;
    private RecordsProvider provider;

    @BeforeEach
    void setUp() {
        faustResolver = mock(FaustResolver.class);
        fbiApiConnector = mock(FbiApiConnector.class);
        RecordServiceConnector recordServiceConnector = mock(RecordServiceConnector.class);
        provider = new RecordsProvider(faustResolver, new FbiApiHandler(fbiApiConnector), recordServiceConnector);
    }

    // Stubs fbi-api's GraphQL response, so the real mapping in FbiApiHandler.materialTypeInfo() is exercised.
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
}
