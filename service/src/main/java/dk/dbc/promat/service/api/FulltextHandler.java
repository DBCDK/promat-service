/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Class representing a one-time streaming retrieval of fulltext content
 * pointed to by given link.
 *
 * This class is not thread safe.
 */
public class FulltextHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FulltextHandler.class);
    static final int BUFFER_SIZE = 8192;

    private static final Pattern CONTENT_DISPOSITION_FILENAME_PATTERN = Pattern.compile(
            "filename\\*?=['\"]?(?:UTF-\\d['\"]*)?([^;\\r\\n\"']*)['\"]?;?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final String fulltextLink;
    private final HttpResponse<InputStream> httpResponse;
    private final String filename;

    public FulltextHandler(String fulltextLink) {

        // Rewrite old style dmat links
        LOGGER.info("New FulltextHandler with link: {}", fulltextLink);
        if (fulltextLink.startsWith("http://dmat.dbc.dk")) {
            LOGGER.info("Rewriting link from old dmat");
            this.fulltextLink = fulltextLink.replace("http://dmat.dbc.dk", "http://service.dmat.dbc.dk");
        } else {
            this.fulltextLink = fulltextLink;
        }
        LOGGER.info("Using fulltextlink: {}", this.fulltextLink);

        var client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        var request = HttpRequest.newBuilder()
                .uri(URI.create(this.fulltextLink))
                .build();

        try {
            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Unable to access " + this.fulltextLink, e);
        }
        filename = getFilenameFromResponseHeaders(httpResponse);
    }

    /**
     * Streams fulltext content into given {@link OutputStream}.
     * This method can only be called once.
     * @param os output sink for fulltext content
     */
    public void getFulltext(OutputStream os) {
        try {
            try (var is = httpResponse.body()) {
                var buf = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = is.read(buf)) > 0) {
                    os.write(buf, 0, bytesRead);
                }
                os.flush();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read content for " + fulltextLink, e);
        }
    }

    public String getFilename() {
        return filename;
    }

    private String getFilenameFromResponseHeaders(HttpResponse<InputStream> httpResponse) {
        var responseHeaders = httpResponse.headers();
        var filenameFromContentDispositionHeader = getFilenameFromContentDispositionHeader(
                responseHeaders.firstValue("Content-Disposition").orElse(""));
        return filenameFromContentDispositionHeader.orElse("download.dat");
    }

    static Optional<String> getFilenameFromContentDispositionHeader(String headerValue) {
        var matcher = CONTENT_DISPOSITION_FILENAME_PATTERN.matcher(headerValue);
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(1));
        }
        return Optional.empty();
    }
}
