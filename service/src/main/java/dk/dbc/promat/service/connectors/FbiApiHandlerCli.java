package dk.dbc.promat.service.connectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.promat.service.api.FbiApiHandler;

/**
 * Manual test tool for {@link FbiApiHandler}. Not part of the deployed application.
 * <p>
 * Usage: FbiApiHandlerCli &lt;client-id&gt; &lt;client-secret&gt; &lt;faust&gt;
 * <p>
 * fbi-api URL and login URL can be overridden via the FBI_API_URL and FBI_API_LOGIN_URL
 * environment variables.
 */
public class FbiApiHandlerCli {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: FbiApiHandlerCli <client-id> <client-secret> <faust>");
            System.exit(1);
        }
        final String clientId = args[0];
        final String clientSecret = args[1];
        final String faust = args[2];

        final String baseUrl = env("FBI_API_URL", "https://fbi-api.dbc.dk");
        final String loginUrl = env("FBI_API_LOGIN_URL", "https://login.bib.dk");

        final FbiApiConnector connector = FbiApiConnectorProducer.produce(
                baseUrl, loginUrl, clientId, clientSecret, new UserAgent("fbi-api-cli"));
        try {
            final FbiApiHandler handler = new FbiApiHandler().withConnector(connector);
            final FbiApiConnector.PromatElements data = handler.format(faust, FbiApiConnector.PromatElements.class);
            System.out.println(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data));
        } finally {
            connector.close();
        }
    }

    private static String env(String name, String defaultValue) {
        final String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
