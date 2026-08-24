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

public class FbiApiMocks {

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
                return "FbiApi-body";
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
                    faust != null &&
                    faust.startsWith(MOCK_FAUST_SERIES));
        }).willReturn(
                ResponseDefinitionBuilder.responseDefinition()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withTransformers("fbiApi-body")));
    }

    private static String makeBody(String faust) {
        return String.format("""
                {
                   "faust" : [ "%s" ],
                   "creator" : [ "Roger Crowley" ],
                   "dk5" : [ "Det Byzantinske Riges historie" ],
                   "isbn" : [ "9788771281118" ],
                   "materialtypes" : {
                     "type" : [ "BOOKS" ]
                   },
                   "materialtypesDetail" : {
                     "type" : [ "BOOK" ]
                   },
                   "extent" : [ "298 sider, ill." ],
                   "publisher" : [ "Rosenkilde & Bahnhof" ],
                   "edition" : [ "1. udgave" ],
                   "series" : [ ],
                   "catalogcodes" : {
                     "code" : [ "DBF201339", "BKM201339", "ACC201333" ]
                   },
                   "title" : [ "Konstantinopels fald" ],
                   "targetgroup" : [ "Voksenafdelinger" ],
                   "metakompassubject" : [ "true" ]
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
