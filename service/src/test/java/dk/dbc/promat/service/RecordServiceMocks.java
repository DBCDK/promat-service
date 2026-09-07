package dk.dbc.promat.service;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.MatchResult;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.tomakehurst.wiremock.client.WireMock.requestMatching;

public class RecordServiceMocks {
    private static final AtomicBoolean recordExists = new AtomicBoolean(false);
    /**
     * Mocks rawrepo-record-service's /api/v1/record/{agencyId}/{bibliographicRecordId}/exists
     * for any agency/faust, responding with {"value": true|false} as set by {@link #setRecordExists}.
     */
    private static void stubRecordExists(WireMockServer wireMockServer) {
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(request.getUrl().contains("/api/v1/record/") && request.getUrl().endsWith("/exists")))
                .willReturn(
                        ResponseDefinitionBuilder.responseDefinition()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(String.format("{\"value\":%s}", recordExists.get()))));
    }

    public static void setRecordExists(WireMockServer wireMockServer, boolean value) {
        recordExists.set(value);
        if (wireMockServer != null) {
            stubRecordExists(wireMockServer);
        }
    }
}
