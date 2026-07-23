package dk.dbc.promat.service;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

public class FaustResolverMocks {

    public static void mockFaustResolverResponses(WireMockServer wireMockServer) {
        wireMockServer.stubFor(get(urlPathMatching(".*api/v1/manifestations/(barcode|isbn-issn)/.*"))
                .atPriority(10)
                .willReturn(okJson("""
                        {
                          "items": [],
                          "trackingId": "000-000-000-000-000"
                        }
                        """)));
    }
}
