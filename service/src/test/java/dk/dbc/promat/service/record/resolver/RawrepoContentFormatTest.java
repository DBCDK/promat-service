package dk.dbc.promat.service.record.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.promat.service.api.FbiApiHandler;
import dk.dbc.promat.service.api.RecordsProvider;
import dk.dbc.promat.service.connectors.FaustResolver;
import dk.dbc.promat.service.connectors.FbiApiConnector;
import dk.dbc.promat.service.dto.RecordsListDto;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import jakarta.ws.rs.client.ClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises {@link RecordsProvider#resolveTitle} against a real {@link RecordServiceConnector}
 * talking HTTP to a local WireMock server, unlike {@link RecordsProviderTest}, which mocks
 * {@code RecordServiceConnector} directly at the Java-object level and therefore never touches
 * real (de)serialization of a rawrepo-record-service response.
 * <p>
 * Background: DM3 (datawell-test) and DM2 (cisterne, fbstest) were found to default to
 * different response formats for the same {@code /content} call - JSON on DM3, marcxchange
 * XML on DM2 - while {@code MarcBinding} (what the connector deserializes the response into)
 * only carries Jackson bindings, no JAXB/XML support. Requesting {@code output-format=MARC_JSON}
 * explicitly (see {@code RecordsProvider.CONTENT_PARAMS}) makes both backends respond
 * identically; verified manually against live DM3 and DM2 instances with the same faust (not
 * reproduced here - see {@code RecordServiceConnectorCli} for that).
 * <p>
 * The two stubbed scenarios below are a snapshot of that manual observation, not a live
 * contract test against either backend - if record-service's actual behavior changes later,
 * these tests won't notice; re-verify with {@code RecordServiceConnectorCli} against real
 * endpoints if this area is touched again.
 */
class RawrepoContentFormatTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int DBC_AGENCY = RecordsProvider.DBC_AGENCY;
    private static final String FAUST = "22252852";

    private WireMockServer wireMockServer;
    private FaustResolver faustResolver;
    private FbiApiConnector fbiApiConnector;
    private RecordsProvider provider;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();

        faustResolver = mock(FaustResolver.class);
        fbiApiConnector = mock(FbiApiConnector.class);
        RecordServiceConnector recordServiceConnector = new RecordServiceConnector(
                ClientBuilder.newClient(), new UserAgent("rawrepo-content-format-test"), wireMockServer.baseUrl());
        provider = new RecordsProvider(faustResolver, new FbiApiHandler(fbiApiConnector), recordServiceConnector);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void resolveTitle_requestsMarcJsonExplicitly_worksRegardlessOfBackendDefault() throws Exception {
        when(faustResolver.resolve(FAUST)).thenReturn(Set.of(FAUST));
        // fbi-api knows nothing about this manifestation, forcing the rawrepo fallback.
        when(fbiApiConnector.execute(anyString(), anyMap(), any()))
                .thenAnswer(invocation -> MAPPER.treeToValue(
                        MAPPER.createObjectNode().putNull("manifestation"), invocation.getArgument(2, Class.class)));

        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1/record/" + DBC_AGENCY + "/" + FAUST + "/exists"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"value\":true}")));

        // Only stubbed when output-format=MARC_JSON is requested - an unstubbed request
        // (i.e. the param got dropped) falls through to WireMock's default 404.
        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1/records/" + DBC_AGENCY + "/" + FAUST + "/content"))
                .withQueryParam("output-format", equalTo("MARC_JSON"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"leader\":[\"0\",\"0\",\"0\",\"0\",\"0\",\"n\",\" \",\" \",\" \",\" \","
                                + "\"2\",\"2\",\"0\",\"0\",\"0\",\"0\",\"0\",\" \",\" \",\" \",\"4\",\"5\",\"0\",\"0\"],"
                                + "\"fields\":[{\"name\":\"245\",\"indicator\":[\"0\",\"0\"],"
                                + "\"subfields\":[{\"name\":\"a\",\"value\":\"Harry Potter og De Vises Sten\"}]}]}]")));

        RecordsListDto recordsListDto = provider.getRecords(FAUST);
        assertThat("One record is present", recordsListDto.getNumFound(), is(1));
        assertThat("Title is resolved via the MARC_JSON-requesting request",
                recordsListDto.getRecords().getFirst().getTitle(), is("Harry Potter og De Vises Sten"));
    }

    @Test
    void resolveTitle_backendRespondsWithXmlAnyway_degradesToNullTitleInsteadOfThrowing() throws Exception {
        when(faustResolver.resolve(FAUST)).thenReturn(Set.of(FAUST));
        when(fbiApiConnector.execute(anyString(), anyMap(), any()))
                .thenAnswer(invocation -> MAPPER.treeToValue(
                        MAPPER.createObjectNode().putNull("manifestation"), invocation.getArgument(2, Class.class)));

        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1/record/" + DBC_AGENCY + "/" + FAUST + "/exists"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"value\":true}")));

        // Mimics DM2's real, observed default for this endpoint (marcxchange XML) - matched on
        // path only, regardless of query string, to simulate "the backend ignores/predates
        // output-format" rather than "the app forgot to send it" (that's the other test above).
        // Before the fix, this made resolveTitle() throw an uncaught ProcessingException instead
        // of degrading gracefully like every other failure in this method.
        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1/records/" + DBC_AGENCY + "/" + FAUST + "/content"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBody("<?xml version='1.0' encoding='UTF-8'?>"
                                + "<collection xmlns='info:lc/xmlns/marcxchange-v1'/>")));

        RecordsListDto recordsListDto = provider.getRecords(FAUST);
        assertThat("Record is still returned", recordsListDto.getNumFound(), is(1));
        assertThat("Faust is set even though the title couldn't be resolved",
                recordsListDto.getRecords().getFirst().getFaust(), is(FAUST));
        assertThat("Title degrades to null instead of the lookup throwing",
                recordsListDto.getRecords().getFirst().getTitle(), is(nullValue()));
    }
}
