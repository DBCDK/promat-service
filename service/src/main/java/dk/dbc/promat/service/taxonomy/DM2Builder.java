package dk.dbc.promat.service.taxonomy;

import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.marc.binding.MarcBinding;
import dk.dbc.promat.service.taxonomy.dto.PathSubject;
import dk.dbc.promat.service.taxonomy.dto.SubjectBuilder;
import dk.dbc.promat.service.taxonomy.dto.Taxonomy;
import dk.dbc.promat.service.taxonomy.dto.MarcUtils;
import jakarta.enterprise.inject.Vetoed;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.List;

@Vetoed
public class DM2Builder implements TaxonomyBuilder {
    static final Logger LOGGER = LoggerFactory.getLogger(DM2Builder.class);

    private HttpClient httpClient;

    String recordService;

    protected Duration readTimeout;

    public void initialize() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(JacksonFeature.class);
        clientConfig.property(ClientProperties.READ_TIMEOUT, readTimeout.toMillis());
        this.httpClient = HttpClient.create(ClientBuilder.newClient(clientConfig));
    }

    public DM2Builder() {}

    public DM2Builder(String url, Duration timeOut) {
        this.readTimeout = timeOut;
        this.recordService = url;
        initialize();
    }

    public void buildTaxonomy(Taxonomy taxonomy) throws TaxonomyException, IOException {
        String body = """
                {"agencies":[190004],"outputFormat":"JSON"}
                """;
        HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(recordService)
                .withPathElements("api", "v1", "dump")
                .withData(body, "application/json");

        try (Response response = httpPost.execute()) {
            if (response.getStatus() >= 400) {
                throw new TaxonomyException("Problem getting subjects base from record-service.", response);
            }
            try (InputStream inputStream = response.readEntity(InputStream.class)) {
                build(inputStream, taxonomy);
            }
        }
    }

    void build(InputStream inputStream, Taxonomy taxonomy) throws IOException {
        SubjectBuilder subjectBuilder = new SubjectBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            int i = 0;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    try {
                        MarcBinding marcBinding = MarcUtils.toMarcBinding(line);
                        List<PathSubject> pathSubjects = subjectBuilder.build(marcBinding);
                        pathSubjects.forEach(pathSubject -> taxonomy.put(pathSubject, pathSubject.getPath()));
                    } catch (TaxonomyException e) {
                        LOGGER.warn("Error building taxonomy", e);
                    }
                    i = i + 1;
                }
            }
            LOGGER.info("Traversed: {} records", i);
        }
    }
}
