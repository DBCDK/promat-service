package dk.dbc.promat.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.MatchResult;


import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.client.WireMock.requestMatching;

public class OpenformatMocks {

    // Mock all responses for openformat requests for faust's in the 01999 series
    private static final String MOCK_FAUST_SERIES = "01999";
    private static final AtomicReference<String> faust = new AtomicReference<>();
    private static final Pattern pidPattern = Pattern.compile(":\"870970-basis:(\\d{6})");

    public static void mockOpenformatResponses(WireMockServer wireMockServer) {

        wireMockServer.stubFor(requestMatching(request -> {
            faust.set(extractFaust(request.getBodyAsString()));
            return MatchResult.of(request.getUrl().contains("api/v2/format") &&
                    faust.get() != null &&
                    faust.get().startsWith(MOCK_FAUST_SERIES));
        }).willReturn(
                ResponseDefinitionBuilder.responseDefinition()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(makeBody(faust.get()))));
    }

    private static String makeBody(String faust) {
        return "{\"objects\":[{\"promat\":[{\"formatted\":{\"format\":\"promat\",\"records\":[{\"elements\":" +
                "{\"faust\":[\"" + faust + "\"],\"creator\":[\"Some Creator\"],\"dk5\":[\"999.99\"],\"isbn\":" +
                "[\"99999999999\"],\"materialtypes\":{\"type\":[\"Ebog\"]},\"extent\":[\"999 sider\"],\"publisher\":" +
                "[\"Some publisher\"],\"edition\":[\"1. udgave\"],\"catalogcodes\":{\"code\":" + "" +
                "[\"DBF202134\",\"ACC202129\",\"BKM202134\",\"ERE202134\"]},\"title\":[\"Some title\"],\"targetgroup\":" + "" +
                "[\"v\"]}}]},\"mediaType\":\"application/json\"}]}],\"trackingId\":\"0\"}";
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
