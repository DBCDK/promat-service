package dk.dbc.promat.service.taxonomy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.commons.kafka.consumer.TopicConsumer;
import dk.dbc.commons.kafka.consumer.TopicConsumerException;
import dk.dbc.promat.service.taxonomy.dto.PathSubject;
import dk.dbc.promat.service.taxonomy.dto.Taxonomy;

import java.io.IOException;

public class DM3Builder implements TaxonomyBuilder{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DM3Builder.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final String bootstrapServers;
    private final String topic;
    private String groupId;
    private int threads;
    private String commitInterval;
    private String apiTimeout;
    private String pollTimeout;
    private String requestTimeout;
    private String sessionTimeout;
    private String maxRequestSize;
    private int maxPending;

    public DM3Builder(String bootstrapServers, String topic) {
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
        this.groupId = "dm3-promat-metakompas-consumer";
        this.threads = 2;
        this.commitInterval = "20s";
        this.apiTimeout = "3m";
        this.pollTimeout = "1m";
        this.requestTimeout = "150s";
        this.sessionTimeout = "5m";
        this.maxRequestSize = "250M";
        this.maxPending = 5;
    }

    @Override
    public void buildTaxonomy(Taxonomy taxonomy) throws TaxonomyException, IOException {
        TopicConsumer consumer = TopicConsumer.builder(bootstrapServers, topic)
                .groupId(groupId)
                .commitInterval(commitInterval)
                .apiTimeout(apiTimeout)
                .pollTimeout(pollTimeout)
                .requestTimeout(requestTimeout)
                .sessionTimeout(sessionTimeout)
                .maxRequestSize(maxRequestSize)
                .maxPendingJobs(maxPending)
                .build(threads, i -> (key, value) -> {
                    try {
                        PathSubject subject = objectMapper.readValue(value, PathSubject.class);
                        taxonomy.put(subject, subject.getPath());
                    } catch (JsonProcessingException e) {
                        LOGGER.error("Unable to parse json:{}", value, e);
                    }
                });
        try {
            consumer.run();
        } catch (TopicConsumerException e) {
            throw new TaxonomyException(e);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted", e);
            Thread.currentThread().interrupt();
        }

    }
}
