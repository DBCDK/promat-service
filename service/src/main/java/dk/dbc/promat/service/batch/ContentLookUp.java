package dk.dbc.promat.service.batch;

import dk.dbc.promat.service.api.FulltextHandler;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentLookUp {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentLookUp.class);

    @Inject
    @ConfigProperty(name = "EMATERIAL_CONTENT_REPO")
    String contentRepo;

    public static HttpClient client = null;

    public ContentLookUp() {
        client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    }

    public Optional<String> lookUpContent(String faust) {
        final HttpResponse<InputStream> httpResponse;
        final String fullTextLink = String.format(contentRepo, faust);;

        try {
            LOGGER.info("Looking up ebook content via HEAD to {}", fullTextLink);
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(fullTextLink))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            var headResponse = client.send(request, HttpResponse.BodyHandlers.discarding());
            LOGGER.info("response.statusCode() = {}, {}",
                    headResponse.statusCode(),
                    (headResponse.statusCode() == 200 || headResponse.statusCode() == 302
                            ? "Content exists"
                            : "No content"));
            return (headResponse.statusCode() == 200 || headResponse.statusCode() == 302) ? Optional.of(fullTextLink) : Optional.empty();
        } catch (InterruptedException | IOException exception) {
            LOGGER.error("Unable to lookup '{}', error:", fullTextLink, exception);
            return Optional.empty();
        } catch(Exception exception) {
            LOGGER.error("Unknown exception when looking up ebook content: {}", exception.getMessage());
            return Optional.empty();
        }
    }
}
