package dk.dbc.promat.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import jakarta.ws.rs.core.HttpHeaders;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.requestMatching;
import static dk.dbc.promat.service.api.Users.IDP_EDITOR_RIGHT_NAME;
import static dk.dbc.promat.service.api.Users.IDP_PRODUCT_NAME;
import static dk.dbc.promat.service.api.Users.IDP_REVIEWER_RIGHT_NAME;

public class AuthMocks {
    /**
     * Helper method to mock responses to various authentication requests with
     * 'AdgangsPlatformen'
     *
     * @param wireMockServer The wiremock server instance
     *
     * authtoken         status          usertype      promatId     culr-id          login-type
     * ----------------------------------------------------------------------------------------
     * 1-2-3-4-5         active=true     EDITOR        12           53               bibliotek
     * 2-3-4-5-6         active=true     EDITOR        13           klnp             netpunkt
     * 6-5-4-3-2         active=true     EDITOR        13           klnp             entraDbc (userId=klnp@dbc.dk, initials=klnp)
     * 7-6-5-4-3         active=true     EDITOR        n/a          n/a              entraDbc (initials=nomatch, no matching promatuser)
     * 8-7-6-5-4         active=true     EDITOR        n/a          n/a              entraDbc (userId=klnp@otherdomain.com, initials=klnp - non-dbc.dk domain rejected)
     * 9-8-7-6-5         active=true     EDITOR        n/a          n/a              entraDbc (userId=klnp@dbc.dk, initials=different - inconsistent claims)
     * 6-7-8-9-0         active=false
     */
    public static void mockAuthenticationResponses(WireMockServer wireMockServer) throws IOException {

        // Mock logged-in editor with id=13, using 'netpunkt login' (userid+agency)
        mockAuth(wireMockServer, "2-3-4-5-6", 200, 200, IDP_PRODUCT_NAME, IDP_EDITOR_RIGHT_NAME);

        /* Mock logged-in editor with id=13, using 'entraDbc login' (userId returned as email, initials=klnp) */
        mockAuth(wireMockServer, "6-5-4-3-2", 200, 200, IDP_PRODUCT_NAME, IDP_EDITOR_RIGHT_NAME);

        /* Mock entraDbc login where initials (nomatch) has no matching promatuser row */
        mockAuth(wireMockServer, "7-6-5-4-3", 200, 200, IDP_PRODUCT_NAME, IDP_EDITOR_RIGHT_NAME);

        /* Mock entraDbc login where userId is an unrelated, non-dbc.dk domain - even though initials
           (klnp) matches a promatuser row, the domain check must reject this before any DB lookup */
        mockAuth(wireMockServer, "8-7-6-5-4", 200, 200, IDP_PRODUCT_NAME, IDP_EDITOR_RIGHT_NAME);

        /* Mock entraDbc login where initials (different) disagrees with the local-part of userId
           (klnp) - the cross-claim consistency check must reject this before any DB lookup */
        mockAuth(wireMockServer, "9-8-7-6-5", 200, 200, IDP_PRODUCT_NAME, IDP_EDITOR_RIGHT_NAME);

        // Mock logged-in reviewer with id=2, using 'netpunkt login' (userid+agency)
        mockAuth(wireMockServer, "3-4-5-6-7", 200, 200, IDP_PRODUCT_NAME, IDP_REVIEWER_RIGHT_NAME);

        // Mock logged-in user not found in promat, using 'netpunkt login' (userid+agency)
        mockAuth(wireMockServer, "4-5-6-7-8", 200, 200, IDP_PRODUCT_NAME, IDP_REVIEWER_RIGHT_NAME);

        // Mock logged-in user not found in promat, using 'netpunkt login' (userid+agency)
        mockAuth(wireMockServer, "5-6-7-8-9", 200, 200, IDP_PRODUCT_NAME, IDP_REVIEWER_RIGHT_NAME);

        // Mock logged-in reviewer with id=2 having no promat rights, using 'netpunkt login' (userid+agency)
        mockAuth(wireMockServer, "5-4-3-2-1", 200, 200);

        // Mock logged-in editor with id=13 with incorrect right as reviewer, using 'netpunkt login' (userid+agency)
        mockAuth(wireMockServer, "4-3-2-1-0", 200, 200, IDP_PRODUCT_NAME, IDP_REVIEWER_RIGHT_NAME);

        // Mock logged-in reviewer with id=2 with incorrect right as editor, using 'netpunkt login' (userid+agency)
        mockAuth(wireMockServer, "3-2-1-0-9", 200, 200, IDP_PRODUCT_NAME, IDP_EDITOR_RIGHT_NAME);

        // Mock logged-out user
        mockAuthenticationResponseForLoggedOutUser(wireMockServer);

        // Mock for FBI login
        mockFBILoginAuth(wireMockServer, "123456789", "abcdef");
    }







    private static void mockAuthenticationResponseForLoggedOutUser(WireMockServer wireMockServer) {

        // Mock /introspection endpoint
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("6-7-8-9-0")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{\"active\":false}")));

        // Mock /userinfo endpoint
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/userinfo") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("6-7-8-9-0")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(401)
                        .withBody("""
                        {q
                          "error": "invalid_token",
                          "error_description": "Invalid token: access token is invalid"
                        }
                        """)));

    }

    public static void mockAuth(WireMockServer server, String token, int introspectStatus, int userInfoStatus, String... args) throws IOException {
        String userInfo = String.format(Files.readString(
                Path.of(Objects.requireNonNull(AuthMocks.class.getResource("/users/" + token + "-userinfo.json")).getPath())), (Object[]) args);
        String introspectInfo = Files.readString(
                Path.of(Objects.requireNonNull(AuthMocks.class.getResource("/users/" + token + "-introspect.json")).getPath()));

        server.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/oauth/introspection") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue(token)
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(introspectStatus)
                        .withBody(introspectInfo)));


        server.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/userinfo") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue(token)
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(userInfoStatus)
                        .withBody(userInfo)));

    }

    public static void mockFBILoginAuth(WireMockServer server, String clientId, String clientSecret) {
        String credentials = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        server.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/oauth/token") &&
                                request.getHeader(HttpHeaders.AUTHORIZATION).contains(credentials)
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"1234567\", \"expiresIn\": 3000000000 }")));
    }
}