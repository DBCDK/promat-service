package dk.dbc.promat.service.record.resolver;

import com.github.tomakehurst.wiremock.WireMockServer;
import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.promat.service.FaustResolverMocks;
import dk.dbc.promat.service.FbiApiMocks;
import dk.dbc.promat.service.RecordServiceMocks;
import dk.dbc.promat.service.api.FbiApiHandler;
import dk.dbc.promat.service.api.RecordsProvider;
import dk.dbc.promat.service.connectors.FaustResolver;
import dk.dbc.promat.service.connectors.FaustResolverException;
import dk.dbc.promat.service.connectors.FaustResolverProducer;
import dk.dbc.promat.service.connectors.FbiApiConnector;
import dk.dbc.promat.service.connectors.FbiApiConnectorException;
import dk.dbc.promat.service.connectors.FbiApiConnectorProducer;
import dk.dbc.promat.service.dto.RecordsListDto;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.time.Instant;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecordsProviderTest {
    private static final UserAgent USER_AGENT = new UserAgent("PromatIT");
    private static WireMockServer wireMockServer;
    private static FaustResolver faustResolver;
    private static FbiApiConnector fbiApiConnector;
    private static FbiApiHandler fbiApiHandler;
    private static RecordServiceConnector recordServiceConnector;
    private static RecordsProvider provider;

    @BeforeAll
    static void startWiremock() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        String wiremockHostUrl = "http://localhost:" + wireMockServer.port();
        faustResolver  = FaustResolverProducer.produce("http://172.17.33.94:8081", USER_AGENT);
        //faustResolver  = FaustResolverProducer.produce(wiremockHostUrl, USER_AGENT);
        fbiApiConnector = FbiApiConnectorProducer.produce("http://172.17.33.94:8082", wiremockHostUrl, "clientID", "clientSecret", USER_AGENT);
        //fbiApiConnector = FbiApiConnectorProducer.produce(wiremockHostUrl, wiremockHostUrl, "clientID", "clientSecret", USER_AGENT);
        fbiApiConnector.setAccessToken("6063a04eb7be36ab8d2df2e4564e91754a6d683e", Instant.MAX);
        fbiApiHandler = new FbiApiHandler(fbiApiConnector);
        recordServiceConnector = mock(RecordServiceConnector.class);
        provider = new RecordsProvider(faustResolver, fbiApiHandler, recordServiceConnector);
        try {
            when(provider.recordServiceConnector.recordExists(RecordsProvider.DBC_AGENCY, "123456789"))
                    .thenReturn(false);
        } catch (RecordServiceConnectorException e) {
            throw new RuntimeException(e);
        }
        FbiApiMocks.mockFbiApiResponses(wireMockServer);
        FaustResolverMocks.mockFaustResolverResponses(wireMockServer);
        RecordServiceMocks.setRecordExists(wireMockServer, false);
    }

    @AfterAll
    public static void stopWiremock() {
        wireMockServer.stop();
    }

    @Test
    void isbn_materialtype_book() throws FaustResolverException, RecordServiceConnectorException, FbiApiConnectorException {
        RecordsListDto recordsListDto = provider.getRecords("9788712802846");
        assertThat("One record is present", recordsListDto.getNumFound(), is(1));
        assertThat("Material type is book", recordsListDto.getRecords().getFirst().getTypes().getFirst().getMaterialType().toString(), is("BOOK"));
    }

    @Test
    void barcode_materialtype_movie() throws FaustResolverException, RecordServiceConnectorException, FbiApiConnectorException {
        RecordsListDto recordsListDto = provider.getRecords("5710768010115");
        assertThat("One record is present", recordsListDto.getNumFound(), is(1));
        assertThat("Material type is movie", recordsListDto.getRecords().getFirst().getTypes().getFirst().getMaterialType().toString(), is("MOVIE"));
    }
    @Test
    void barcode_materialtype_multimedia() throws FaustResolverException, RecordServiceConnectorException, FbiApiConnectorException {
        RecordsListDto recordsListDto = provider.getRecords("5026555432207");
        assertThat("One record is present", recordsListDto.getNumFound(), is(1));
        assertThat("Material type is multimmedia", recordsListDto.getRecords().getFirst().getTypes().getFirst().getMaterialType().toString(), is("MULTIMEDIA"));
    }

    @Test
    void faust_materialtype_movie_online() throws FaustResolverException, RecordServiceConnectorException, FbiApiConnectorException {
        RecordsListDto recordsListDto = provider.getRecords("135426296");
        assertThat("One record is present", recordsListDto.getNumFound(), is(1));
        assertThat("Material type is movie", recordsListDto.getRecords().getFirst().getTypes().getFirst().getMaterialType().toString(), is("MOVIE"));
        assertThat("Material specific type (online)", recordsListDto.getRecords().getFirst().getTypes().getFirst().getSpecificType(), is("film (online)"));
    }

    @Test
    void faust_materialtype_movie_dvd_or_blueray() throws FaustResolverException, RecordServiceConnectorException, FbiApiConnectorException {
        RecordsListDto recordsListDto = provider.getRecords("143424014");
        assertThat("One record is present", recordsListDto.getNumFound(), is(1));
        assertThat("Material type is movie", recordsListDto.getRecords().getFirst().getTypes().getFirst().getMaterialType().toString(), is("MOVIE"));
        assertThat("Material specific type (DVD or BlueRay)", recordsListDto.getRecords().getFirst().getTypes().getFirst().getSpecificType(), is("film (dvd)"));
    }

    @Test
    void faust_materialtype_multimedia() throws FaustResolverException, RecordServiceConnectorException, FbiApiConnectorException {
        RecordsListDto recordsListDto = provider.getRecords("143569608");
        assertThat("One record is present", recordsListDto.getNumFound(), is(1));
        assertThat("Material type is game", recordsListDto.getRecords().getFirst().getTypes().getFirst().getMaterialType().toString(), is("MULTIMEDIA"));
        assertThat("Material specific type (electronic material dvd-rom)", recordsListDto.getRecords().getFirst().getTypes().getFirst().getSpecificType(), is("playstation 5"));
    }

    @Test
    void isbn_not_found() throws FaustResolverException, RecordServiceConnectorException, FbiApiConnectorException {
        RecordsListDto recordsListDto = provider.getRecords("9788764439999");
        assertThat("No records are present", recordsListDto.getNumFound(), is(0));
    }

    @Test
    void invalid_input() throws FaustResolverException, RecordServiceConnectorException, FbiApiConnectorException {
        RecordsListDto recordsListDto = provider.getRecords("9");
        assertThat("No records are present", recordsListDto.getNumFound(), is(0));
    }
}
