package dk.dbc.promat.service.taxonomy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.MatchResult;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.requestMatching;
import static dk.dbc.promat.service.TestUtils.getResource;

public class RecordServiceMocks {
    private static final AtomicReference<String> agencyDump = new AtomicReference<>();
    private static WireMockServer wireMockServer;

    static {
        try {
            resetAgencyDump();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockRecordServiceAgencyDump(WireMockServer server) {
        wireMockServer = server;
        stubAgencyDump();
    }

    private static void stubAgencyDump() {
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(request.getUrl().contains("api/v1/dump")))
                .willReturn(
                        ResponseDefinitionBuilder.responseDefinition()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(agencyDump.get())));
    }

    public static void addToAgencyDump(String lines) {
        agencyDump.updateAndGet(current -> current + "\n" + lines);
        if (wireMockServer != null) stubAgencyDump();
    }



    public static String getAgencyDump() {
        return agencyDump.get();
    }

    public static void resetAgencyDump() throws IOException {
        agencyDump.set(getResource("/taxonomy/records/agency/dump/190004.txt"));
    }
}
