package dk.dbc.promat.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.client.WireMock.requestMatching;

/**
 * Generic fbi-api GraphQL mock, used as a catch-all fallback for fausts that aren't covered by a
 * recorded WireMock mapping (see mappings/mapping-promat-graphql-*.json + __files/body-promat-graphql-*.json).
 * Recorded mappings match on exact request body and use the default (higher) priority, so they are
 * always tried first; this stub only kicks in when no recording matches.
 */
public class FbiApiMocks {

    private static final String TRANSFORMER_NAME = "fbiApi-body";
    private static final Pattern pidPattern = Pattern.compile(":\"870970-basis:(\\d{6,8})");
    private static final Set<String> notFoundFausts = ConcurrentHashMap.newKeySet();

    /**
     * Registers one or more fausts for which the mock should simulate fbi-api not knowing the
     * manifestation (i.e. {@code manifestation: null}), regardless of what the recorded/generic
     * mock would otherwise have returned.
     */
    public static void mockNotFound(String... fausts) {
        notFoundFausts.addAll(Set.of(fausts));
    }

    /**
     * Clears all fausts previously registered via {@link #mockNotFound(String...)}.
     */
    public static void resetNotFoundFausts() {
        notFoundFausts.clear();
    }

    public static ResponseTransformerV2 bodyTransformer() {
        return new ResponseTransformerV2() {
            @Override
            public Response transform(Response response, ServeEvent serveEvent) {
                String faust = extractFaust(serveEvent.getRequest().getBodyAsString());
                return Response.Builder.like(response)
                        .but()
                        .body(notFoundFausts.contains(faust) ? makeNotFoundBody() : makeBody(faust))
                        .build();
            }

            @Override
            public String getName() {
                return TRANSFORMER_NAME;
            }

            @Override
            public boolean applyGlobally() {
                return false;
            }
        };
    }

    public static void mockFbiApiResponses(WireMockServer wireMockServer) {

        wireMockServer.stubFor(requestMatching(request -> {
            String faust = extractFaust(request.getBodyAsString());
            return MatchResult.of(request.getUrl().contains("/promat/graphql") &&
                    faust != null);
        }).atPriority(10).willReturn(
                ResponseDefinitionBuilder.responseDefinition()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withTransformers(TRANSFORMER_NAME)));
    }

    // Shape matches fbi-api's GraphQL response envelope, as consumed by
    // FbiApiHandler/FbiApiConnector: {"data": {"manifestation": {...}}}
    private static String makeBody(String faust) {
        return String.format("""
                {
                  "data" : {
                    "manifestation" : {
                      "pid" : "870970-basis:%s",
                      "creators" : [ { "display" : "Roger Crowley" } ],
                      "classifications" : [ { "dk5Heading" : "Det Byzantinske Riges historie", "entryType" : "MAIN_ENTRY" } ],
                      "edition" : { "edition" : "1. udgave" },
                      "identifiers" : [ { "type" : "ISBN", "value" : "9788771281118" } ],
                      "materialTypes" : [ {
                        "materialTypeGeneral" : { "code" : "BOOKS", "display" : "bøger" },
                        "materialTypeSpecific" : { "code" : "BOOK", "display" : "bog" }
                      } ],
                      "physicalDescription" : { "summaryFull" : "298 sider, ill." },
                      "publisher" : [ "Rosenkilde & Bahnhof" ],
                      "catalogueCodes" : { "nationalBibliography" : [ "DBF201339" ], "otherCatalogues" : [ "BKM201339", "ACC201333" ] },
                      "titles" : { "main" : [ "Konstantinopels fald" ] },
                      "materialSelection" : { "selectionGroup" : [ { "display" : "Voksenafdelinger" } ] },
                      "subjects" : { "dbcVerified" : [ { "type" : "TOPIC", "display" : "historie", "local" : false } ] }
                    }
                  }
                }
                """, faust);
    }

    private static String makeNotFoundBody() {
        return """
                {
                  "data" : {
                    "manifestation" : null
                  }
                }
                """;
    }

    private static String extractFaust(String body) {
        Matcher matcher = pidPattern.matcher(body);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }
}
