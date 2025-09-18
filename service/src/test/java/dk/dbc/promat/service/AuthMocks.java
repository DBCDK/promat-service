package dk.dbc.promat.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.MatchResult;


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
     * 6-7-8-9-0         active=false
     */
    public static void mockAuthenticationResponses(WireMockServer wireMockServer) {

        // Mock logged-in editor with id=13, using 'netpunkt login' (userid+agency)
        mockAuthenticationResponseForEditor13(wireMockServer);

        // Mock logged-in reviewer with id=2, using 'netpunkt login' (userid+agency)
        mockAuthenticationResponseForReviewer2(wireMockServer);

        // Mock logged-in user not found in promat, using 'netpunkt login' (userid+agency)
        mockAuthenticationResponseForUnknownReviewerUserId(wireMockServer);

        // Mock logged-in user not found in promat, using 'netpunkt login' (userid+agency)
        mockAuthenticationResponseForUnknownReviewerAgency(wireMockServer);

        // Mock logged-in reviewer with id=2 having no promat rights, using 'netpunkt login' (userid+agency)
        mockAuthenticationResponseForReviewer2WithRevokedRights(wireMockServer);

        // Mock logged-in editor with id=13 with incorrect right as reviewer, using 'netpunkt login' (userid+agency)
        mockAuthenticationResponseForEditor13WithReviewerRights(wireMockServer);

        // Mock logged-in reviewer with id=2 with incorrect right as editor, using 'netpunkt login' (userid+agency)
        mockAuthenticationResponseForReviewer2WithEditorRights(wireMockServer);

        // Mock logged-out user
        mockAuthenticationResponseForLoggedOutUser(wireMockServer);
    }

    public static void mockAuthenticationResponseForEditor13(WireMockServer wireMockServer) {
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/oauth/introspection") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("2-3-4-5-6")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"active\": true," +
                                "  \"clientId\": \"123-456-789-23456\"," +
                                "  \"expires\": \"2029-01-13T12:58:53.967Z\"," +
                                "  \"agency\": \"790900\"," +
                                "  \"uniqueId\": null," +
                                "  \"search\": {" +
                                "    \"profile\": \"opac\"," +
                                "    \"agency\": \"790900\"" +
                                "  }," +
                                "  \"type\": \"authorized\"," +
                                "  \"name\": \"Promat prod\"," +
                                "  \"contact\": {" +
                                "    \"owner\": {" +
                                "      \"name\": \"Henrik Witt Hansen\"," +
                                "      \"email\": \"hwha@dbc.dk\"," +
                                "      \"phone\": \"\"" +
                                "    }" +
                                "  }" +
                                "}")));

        // Editor authorized using 'netpunkts login', so we have the IDP field
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/userinfo") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("2-3-4-5-6")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"attributes\": {" +
                                "    \"serviceStatus\": {" +
                                "      \"borchk\": \"ok\"," +
                                "      \"culr\": \"ok\"" +
                                "    }," +
                                "    \"userId\": \"klnp\"," +
                                "    \"municipality\": \"909\"," +
                                "    \"netpunktAgency\": \"790900\"," +
                                "    \"municipalityAgencyId\": \"790900\"," +
                                "    \"dbcidp\": [" +
                                "      {" +
                                "        \"agencyId\": \"790900\"," +
                                "        \"rights\": [" +
                                "          {" +
                                "            \"productName\": \"" + IDP_PRODUCT_NAME + "\"," +
                                "            \"name\": \"" + IDP_EDITOR_RIGHT_NAME + "\"," +
                                "            \"description\": \"adgang til Promat som DBC redaktør\"" +
                                "          }" +
                                "        ]" +
                                "      }" +
                                "    ]" +
                                "  }" +
                                "}")));
    }

    public static void mockAuthenticationResponseForReviewer2(WireMockServer wireMockServer) {
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/oauth/introspection") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("3-4-5-6-7")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"active\": true," +
                                "  \"clientId\": \"123-456-789-34567\"," +
                                "  \"expires\": \"2029-01-13T12:58:53.967Z\"," +
                                "  \"agency\": \"790900\"," +
                                "  \"uniqueId\": null," +
                                "  \"search\": {" +
                                "    \"profile\": \"opac\"," +
                                "    \"agency\": \"790900\"" +
                                "  }," +
                                "  \"type\": \"authorized\"," +
                                "  \"name\": \"Promat prod\"," +
                                "  \"contact\": {" +
                                "    \"owner\": {" +
                                "      \"name\": \"Henrik Witt Hansen\"," +
                                "      \"email\": \"hwha@dbc.dk\"," +
                                "      \"phone\": \"\"" +
                                "    }" +
                                "  }" +
                                "}")));

        // Editor authorized using 'netpunkts login', so we have the IDP field
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/userinfo") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("3-4-5-6-7")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"attributes\": {" +
                                "    \"serviceStatus\": {" +
                                "      \"borchk\": \"ok\"," +
                                "      \"culr\": \"ok\"" +
                                "    }," +
                                "    \"userId\": \"axel52\"," +
                                "    \"municipality\": \"909\"," +
                                "    \"netpunktAgency\": \"820010\"," +
                                "    \"municipalityAgencyId\": \"820020\"," +
                                "    \"dbcidp\": [" +
                                "      {" +
                                "        \"agencyId\": \"820010\"," +
                                "        \"rights\": [" +
                                "          {" +
                                "            \"productName\": \"" + IDP_PRODUCT_NAME + "\"," +
                                "            \"name\": \"" + IDP_REVIEWER_RIGHT_NAME + "\"," +
                                "            \"description\": \"adgang til Promat som ekstern anmelder\"" +
                                "          }" +
                                "        ]" +
                                "      }" +
                                "    ]" +
                                "  }" +
                                "}")));
    }

    public static void mockAuthenticationResponseForUnknownReviewerUserId(WireMockServer wireMockServer) {
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/oauth/introspection") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("4-5-6-7-8")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"active\": true," +
                                "  \"clientId\": \"123-456-789-34567\"," +
                                "  \"expires\": \"2029-01-13T12:58:53.967Z\"," +
                                "  \"agency\": \"790900\"," +
                                "  \"uniqueId\": null," +
                                "  \"search\": {" +
                                "    \"profile\": \"opac\"," +
                                "    \"agency\": \"790900\"" +
                                "  }," +
                                "  \"type\": \"authorized\"," +
                                "  \"name\": \"Promat prod\"," +
                                "  \"contact\": {" +
                                "    \"owner\": {" +
                                "      \"name\": \"Henrik Witt Hansen\"," +
                                "      \"email\": \"hwha@dbc.dk\"," +
                                "      \"phone\": \"\"" +
                                "    }" +
                                "  }" +
                                "}")));

        // Editor authorized using 'netpunkts login', so we have the IDP field
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/userinfo") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("4-5-6-7-8")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"attributes\": {" +
                                "    \"serviceStatus\": {" +
                                "      \"borchk\": \"ok\"," +
                                "      \"culr\": \"ok\"" +
                                "    }," +
                                "    \"userId\": \"axel53\"," +
                                "    \"municipality\": \"909\"," +
                                "    \"netpunktAgency\": \"820010\"," +
                                "    \"municipalityAgencyId\": \"820020\"," +
                                "    \"dbcidp\": [" +
                                "      {" +
                                "        \"agencyId\": \"820010\"," +
                                "        \"rights\": [" +
                                "          {" +
                                "            \"productName\": \"" + IDP_PRODUCT_NAME + "\"," +
                                "            \"name\": \"" + IDP_REVIEWER_RIGHT_NAME + "\"," +
                                "            \"description\": \"adgang til Promat som ekstern anmelder\"" +
                                "          }" +
                                "        ]" +
                                "      }" +
                                "    ]" +
                                "  }" +
                                "}")));
    }

    public static void mockAuthenticationResponseForUnknownReviewerAgency(WireMockServer wireMockServer) {
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/oauth/introspection") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("5-6-7-8-9")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"active\": true," +
                                "  \"clientId\": \"123-456-789-34567\"," +
                                "  \"expires\": \"2029-01-13T12:58:53.967Z\"," +
                                "  \"agency\": \"790900\"," +
                                "  \"uniqueId\": null," +
                                "  \"search\": {" +
                                "    \"profile\": \"opac\"," +
                                "    \"agency\": \"790900\"" +
                                "  }," +
                                "  \"type\": \"authorized\"," +
                                "  \"name\": \"Promat prod\"," +
                                "  \"contact\": {" +
                                "    \"owner\": {" +
                                "      \"name\": \"Henrik Witt Hansen\"," +
                                "      \"email\": \"hwha@dbc.dk\"," +
                                "      \"phone\": \"\"" +
                                "    }" +
                                "  }" +
                                "}")));

        // Editor authorized using 'netpunkts login', so we have the IDP field
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/userinfo") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("5-6-7-8-9")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"attributes\": {" +
                                "    \"serviceStatus\": {" +
                                "      \"borchk\": \"ok\"," +
                                "      \"culr\": \"ok\"" +
                                "    }," +
                                "    \"userId\": \"axel52\"," +
                                "    \"municipality\": \"909\"," +
                                "    \"netpunktAgency\": \"820030\"," +
                                "    \"municipalityAgencyId\": \"820020\"," +
                                "    \"dbcidp\": [" +
                                "      {" +
                                "        \"agencyId\": \"820010\"," +
                                "        \"rights\": [" +
                                "          {" +
                                "            \"productName\": \"" + IDP_PRODUCT_NAME + "\"," +
                                "            \"name\": \"" + IDP_REVIEWER_RIGHT_NAME + "\"," +
                                "            \"description\": \"adgang til Promat som ekstern anmelder\"" +
                                "          }" +
                                "        ]" +
                                "      }" +
                                "    ]" +
                                "  }" +
                                "}")));
    }

    public static void mockAuthenticationResponseForReviewer2WithRevokedRights(WireMockServer wireMockServer) {
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/oauth/introspection") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("5-4-3-2-1")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"active\": true," +
                                "  \"clientId\": \"123-456-789-34567\"," +
                                "  \"expires\": \"2029-01-13T12:58:53.967Z\"," +
                                "  \"agency\": \"790900\"," +
                                "  \"uniqueId\": null," +
                                "  \"search\": {" +
                                "    \"profile\": \"opac\"," +
                                "    \"agency\": \"790900\"" +
                                "  }," +
                                "  \"type\": \"authorized\"," +
                                "  \"name\": \"Promat prod\"," +
                                "  \"contact\": {" +
                                "    \"owner\": {" +
                                "      \"name\": \"Henrik Witt Hansen\"," +
                                "      \"email\": \"hwha@dbc.dk\"," +
                                "      \"phone\": \"\"" +
                                "    }" +
                                "  }" +
                                "}")));

        // Editor authorized using 'netpunkts login', so we have the IDP field
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/userinfo") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("5-4-3-2-1")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"attributes\": {" +
                                "    \"serviceStatus\": {" +
                                "      \"borchk\": \"ok\"," +
                                "      \"culr\": \"ok\"" +
                                "    }," +
                                "    \"userId\": \"axel52\"," +
                                "    \"municipality\": \"909\"," +
                                "    \"netpunktAgency\": \"820010\"," +
                                "    \"municipalityAgencyId\": \"820020\"," +
                                "    \"dbcidp\": [" +
                                "      {" +
                                "        \"agencyId\": \"820010\"," +
                                "        \"rights\": [" +
                                "          {" +
                                "            \"productName\": \"DMAT\"," +
                                "            \"name\": \"READ\"," +
                                "            \"description\": \"adgang til dmat\"" +
                                //           No Promat rights
                                "          }" +
                                "        ]" +
                                "      }" +
                                "    ]" +
                                "  }" +
                                "}")));
    }

    public static void mockAuthenticationResponseForEditor13WithReviewerRights(WireMockServer wireMockServer) {
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/oauth/introspection") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("4-3-2-1-0")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"active\": true," +
                                "  \"clientId\": \"123-456-789-23456\"," +
                                "  \"expires\": \"2029-01-13T12:58:53.967Z\"," +
                                "  \"agency\": \"790900\"," +
                                "  \"uniqueId\": null," +
                                "  \"search\": {" +
                                "    \"profile\": \"opac\"," +
                                "    \"agency\": \"790900\"" +
                                "  }," +
                                "  \"type\": \"authorized\"," +
                                "  \"name\": \"Promat prod\"," +
                                "  \"contact\": {" +
                                "    \"owner\": {" +
                                "      \"name\": \"Henrik Witt Hansen\"," +
                                "      \"email\": \"hwha@dbc.dk\"," +
                                "      \"phone\": \"\"" +
                                "    }" +
                                "  }" +
                                "}")));

        // Editor authorized using 'netpunkts login', so we have the IDP field
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/userinfo") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("4-3-2-1-0")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"attributes\": {" +
                                "    \"serviceStatus\": {" +
                                "      \"borchk\": \"ok\"," +
                                "      \"culr\": \"ok\"" +
                                "    }," +
                                "    \"userId\": \"klnp\"," +
                                "    \"municipality\": \"909\"," +
                                "    \"netpunktAgency\": \"790900\"," +
                                "    \"municipalityAgencyId\": \"790900\"," +
                                "    \"dbcidp\": [" +
                                "      {" +
                                "        \"agencyId\": \"790900\"," +
                                "        \"rights\": [" +
                                "          {" +
                                "            \"productName\": \"" + IDP_PRODUCT_NAME + "\"," +
                                "            \"name\": \"" + IDP_REVIEWER_RIGHT_NAME + "\"," +
                                "            \"description\": \"adgang til Promat som ekstern anmelder\"" +
                                "          }" +
                                "        ]" +
                                "      }" +
                                "    ]" +
                                "  }" +
                                "}")));
    }

    public static void mockAuthenticationResponseForReviewer2WithEditorRights(WireMockServer wireMockServer) {
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/oauth/introspection") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("3-2-1-0-9")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"active\": true," +
                                "  \"clientId\": \"123-456-789-34567\"," +
                                "  \"expires\": \"2029-01-13T12:58:53.967Z\"," +
                                "  \"agency\": \"790900\"," +
                                "  \"uniqueId\": null," +
                                "  \"search\": {" +
                                "    \"profile\": \"opac\"," +
                                "    \"agency\": \"790900\"" +
                                "  }," +
                                "  \"type\": \"authorized\"," +
                                "  \"name\": \"Promat prod\"," +
                                "  \"contact\": {" +
                                "    \"owner\": {" +
                                "      \"name\": \"Henrik Witt Hansen\"," +
                                "      \"email\": \"hwha@dbc.dk\"," +
                                "      \"phone\": \"\"" +
                                "    }" +
                                "  }" +
                                "}")));

        // Editor authorized using 'netpunkts login', so we have the IDP field
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains("/userinfo") &&
                                request.queryParameter("access_token").isPresent() &&
                                request.queryParameter("access_token").containsValue("3-2-1-0-9")
                ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{" +
                                "  \"attributes\": {" +
                                "    \"serviceStatus\": {" +
                                "      \"borchk\": \"ok\"," +
                                "      \"culr\": \"ok\"" +
                                "    }," +
                                "    \"userId\": \"axel52\"," +
                                "    \"municipality\": \"909\"," +
                                "    \"netpunktAgency\": \"820010\"," +
                                "    \"municipalityAgencyId\": \"820020\"," +
                                "    \"dbcidp\": [" +
                                "      {" +
                                "        \"agencyId\": \"820010\"," +
                                "        \"rights\": [" +
                                "          {" +
                                "            \"productName\": \"" + IDP_PRODUCT_NAME + "\"," +
                                "            \"name\": \"" + IDP_EDITOR_RIGHT_NAME + "\"," +
                                "            \"description\": \"adgang til Promat som DBC redaktør\"" +
                                "          }" +
                                "        ]" +
                                "      }" +
                                "    ]" +
                                "  }" +
                                "}")));
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
                        .withBody("{" +
                                "  \"error\": \"invalid_token\"," +
                                "  \"error_description\": \"Invalid token: access token is invalid\"" +
                                "}")));
    }
}
