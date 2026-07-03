package dk.dbc.promat.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.client.WireMock.requestMatching;

public class OpenformatMocks {

    private static final String MOCK_FAUST_SERIES = "01999";
    private static final Pattern pidPattern = Pattern.compile(":\"870970-basis:(\\d{6,8})");

    public static ResponseTransformerV2 bodyTransformer() {
        return new ResponseTransformerV2() {
            @Override
            public Response transform(Response response, ServeEvent serveEvent) {
                String faust = extractFaust(serveEvent.getRequest().getBodyAsString());
                return Response.Builder.like(response)
                        .but()
                        .body(makeBody(faust))
                        .build();
            }

            @Override
            public String getName() {
                return "openformat-body";
            }

            @Override
            public boolean applyGlobally() {
                return false;
            }
        };
    }

    public static void mockOpenformatResponses(WireMockServer wireMockServer) {

        wireMockServer.stubFor(requestMatching(request -> {
            String faust = extractFaust(request.getBodyAsString());
            return MatchResult.of(request.getUrl().contains("api/v2/format") &&
                    faust != null &&
                    faust.startsWith(MOCK_FAUST_SERIES));
        }).willReturn(
                ResponseDefinitionBuilder.responseDefinition()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withTransformers("openformat-body")));
    }

    private static String makeBody(String faust) {
        return String.format("""
                {
                  "objects": [
                    {
                      "promat": [
                        {
                          "formatted": {
                            "format": "promat",
                            "records": [
                              {
                                "elements": {
                                  "faust": [
                                    "%s"
                                  ],
                                  "creator": [
                                    "Krogholm, Peter"
                                  ],
                                  "dk5": [
                                    "sk"
                                  ],
                                  "isbn": [
                                    "9788723576941"
                                  ],
                                  "materialtypes": {
                                    "type": [
                                      "Bog"
                                    ]
                                  },
                                  "materialtypesDetail": {
                                    "type": [
                                      "a xx"
                                    ]
                                  },
                                  "extent": [
                                    "70 sider"
                                  ],
                                  "publisher": [
                                    "Kbh., Alinea, 2026"
                                  ],
                                  "edition": [
                                    "1. udgave"
                                  ],
                                  "series": [
                                    "Håb (Alinea)",
                                    "Læseklub. Sort"
                                  ],
                                  "catalogcodes": {
                                    "code": [
                                      "DBF202627",
                                      "ACC202620",
                                      "BKM202627"
                                    ]
                                  },
                                  "title": [
                                    "Død og levende"
                                  ],
                                  "targetgroup": [
                                    "b s"
                                  ]
                                }
                              }
                            ]
                          },
                          "mediaType": "application/json"
                        }
                      ]
                    }
                  ],
                  "trackingId": "d2ab7e34-c5fb-4d00-ab8f-c461a2f301cb"
                }
                """, faust);
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
