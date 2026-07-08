package dk.dbc.promat.service.record.resolver;

import com.github.tomakehurst.wiremock.WireMockServer;
import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.promat.service.OpenformatMocks;
import dk.dbc.promat.service.api.OpenFormatHandler;
import dk.dbc.promat.service.api.RecordsProvider;
import dk.dbc.promat.service.connectors.FaustResolver;
import dk.dbc.promat.service.connectors.FaustResolverException;
import dk.dbc.promat.service.connectors.FaustResolverProducer;
import dk.dbc.promat.service.connectors.OpenFormatConnector;
import dk.dbc.promat.service.connectors.OpenFormatConnectorException;
import dk.dbc.promat.service.connectors.OpenFormatConnectorProducer;
import dk.dbc.promat.service.dto.RecordsListDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

class RecordsProviderTest {
    private static final UserAgent USER_AGENT = new UserAgent("PromatIT");
    private static WireMockServer wireMockServer;
    private static FaustResolver faustResolver;
    private static OpenFormatConnector connector;
    private static OpenFormatHandler openFormatHandler;
    private static RecordsProvider provider;

    @BeforeAll
    static void startWiremock() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        faustResolver  = FaustResolverProducer.produce("http://localhost:" + wireMockServer.port(), USER_AGENT);
        connector = OpenFormatConnectorProducer.produce("http://localhost:" + wireMockServer.port(), USER_AGENT);
        openFormatHandler = new OpenFormatHandler(connector);
        provider = new RecordsProvider(faustResolver, openFormatHandler);
        OpenformatMocks.mockOpenformatResponses(wireMockServer);
    }

    @AfterAll
    public static void stopWiremock() {
        wireMockServer.stop();
    }

    @Test
    void isbn_materialtype_book() throws FaustResolverException, OpenFormatConnectorException {
        RecordsListDto recordsListDto = provider.getRecords("9788712802846");
        assertThat("One record is present", recordsListDto.getNumFound(), is(1));
        assertThat("Material type is book", recordsListDto.getRecords().getFirst().getTypes().getFirst().getMaterialType().toString(), is("BOOK"));
    }

    @Test
    void faust_materialtype_movie_online() throws FaustResolverException, OpenFormatConnectorException {
        RecordsListDto recordsListDto = provider.getRecords("135426296");
        assertThat("One record is present", recordsListDto.getNumFound(), is(1));
        assertThat("Material type is movie", recordsListDto.getRecords().getFirst().getTypes().getFirst().getMaterialType().toString(), is("MOVIE"));
        assertThat("Material specific type (online)", recordsListDto.getRecords().getFirst().getTypes().getFirst().getSpecificType(), is("m xe"));
    }

    @Test
    void faust_materialtype_movie_dvd_or_blueray() throws FaustResolverException, OpenFormatConnectorException {
        RecordsListDto recordsListDto = provider.getRecords("143424014");
        assertThat("One record is present", recordsListDto.getNumFound(), is(1));
        assertThat("Material type is movie", recordsListDto.getRecords().getFirst().getTypes().getFirst().getMaterialType().toString(), is("MOVIE"));
        assertThat("Material specific type (DVD or BlueRay)", recordsListDto.getRecords().getFirst().getTypes().getFirst().getSpecificType(), is("m th"));
    }

    @Test
    void faust_materialtype_multimedia() throws FaustResolverException, OpenFormatConnectorException {
        RecordsListDto recordsListDto = provider.getRecords("143569608");
        assertThat("One record is present", recordsListDto.getNumFound(), is(1));
        assertThat("Material type is game", recordsListDto.getRecords().getFirst().getTypes().getFirst().getMaterialType().toString(), is("MULTIMEDIA"));
        assertThat("Material specific type (electronic material dvd-rom)", recordsListDto.getRecords().getFirst().getTypes().getFirst().getSpecificType(), is("t to"));
    }

    @Test
    void isbn_not_found() throws FaustResolverException, OpenFormatConnectorException {
        RecordsListDto recordsListDto = provider.getRecords("9788764439999");
        assertThat("No records are present", recordsListDto.getNumFound(), is(0));
    }

    @Test
    void invalid_input() throws FaustResolverException, OpenFormatConnectorException {
        RecordsListDto recordsListDto = provider.getRecords("9");
        assertThat("No records are present", recordsListDto.getNumFound(), is(0));
    }
}
