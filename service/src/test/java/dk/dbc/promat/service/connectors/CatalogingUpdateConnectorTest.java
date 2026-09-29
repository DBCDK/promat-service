package dk.dbc.promat.service.connectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import dk.dbc.commons.useragent.UserAgent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Runs the real HTTP call through the producer, against a stubbed update-service
class CatalogingUpdateConnectorTest {
    private static final String PATH = "/api/v1/updateservice";
    private static final String MARC = "<marcx:record/>";

    private WireMockServer wireMockServer;
    private CatalogingUpdateConnector connector;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        connector = CatalogingUpdateConnectorProducer.produce(wireMockServer.baseUrl(), "metakompas",
                "010100", "promat", "secret", new UserAgent("cataloging-update-connector-test"));
    }

    @AfterEach
    void tearDown() {
        connector.close();
        wireMockServer.stop();
    }

    @Test
    void sendsUpdateRequest() throws Exception {
        wireMockServer.stubFor(post(urlPathEqualTo(PATH)).willReturn(okJson("{\"updateStatusEnumDTO\":\"OK\"}")));

        connector.updateRecord("870970:12345678", MARC);

        wireMockServer.verify(postRequestedFor(urlPathEqualTo(PATH))
                .withHeader("Accept", equalTo("application/json"))
                .withRequestBody(matchingJsonPath("$.schemaName", equalTo("metakompas")))
                .withRequestBody(matchingJsonPath("$.authenticationDTO.groupId", equalTo("010100")))
                .withRequestBody(matchingJsonPath("$.authenticationDTO.userId", equalTo("promat")))
                .withRequestBody(matchingJsonPath("$.bibliographicRecordDTO.recordSchema", equalTo("info:lc/xmlns/marcxchange-v1")))
                .withRequestBody(matchingJsonPath("$.bibliographicRecordDTO.recordPacking", equalTo("xml")))
                .withRequestBody(matchingJsonPath("$.bibliographicRecordDTO.recordDataDTO.content[0]", equalTo(MARC)))
                .withRequestBody(matchingJsonPath("$.trackingId", matching("DBC_PROMAT_870970:12345678_.+"))));
    }

    @Test
    void failsOnNonOkUpdateStatus() {
        wireMockServer.stubFor(post(urlPathEqualTo(PATH)).willReturn(okJson("{\"updateStatusEnumDTO\":\"FAILED\"}")));

        assertThrows(CatalogingUpdateConnectorException.class, () -> connector.updateRecord("870970:12345678", MARC));
    }

    @Test
    void failsOnUnexpectedHttpStatus() {
        wireMockServer.stubFor(post(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(500)));

        assertThrows(CatalogingUpdateConnectorException.class, () -> connector.updateRecord("870970:12345678", MARC));
    }
}
