package dk.dbc.promat.service.batch;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.commons.kafka.consumer.TopicConsumer;
import dk.dbc.promat.service.persistence.JsonMapperProvider;
import dk.dbc.promat.service.taxonomy.TaxonomyCache;
import dk.dbc.promat.service.taxonomy.TaxonomyPopulator;
import dk.dbc.promat.service.taxonomy.dto.Subject;
import dk.dbc.promat.service.taxonomy.dto.Taxonomy;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

// Every pod consumes the whole TAXONOMY_KAFKA_TOPIC topic independently, straight into memory.
// Runs at startup and hourly.
//
// groupId is unique per pod and fresh on every restart (hostname + a UUID). Two pods must never
// share a group id - Kafka would split the topic's partitions between them, so neither pod
// would see the whole topic. A fresh id every restart means a restarted pod always starts a
// clean read rather than resuming a previous incarnation's committed offsets.
//
// A fresh group id has no committed offset yet, so the first run does a full topic replay
// (auto.offset.reset=earliest); every run after that, for the rest of this pod's lifetime, only
// sees new/changed/tombstoned messages. Since later runs only see deltas, this class keeps a
// persistent, cumulative view of every subject currently known (subjects field, keyed by the
// Kafka message key, since tombstones carry no subject id) and rebuilds the full Taxonomy from
// that complete view after every run.
//
// Kafka messages parse directly into taxonomy.dto.Subject via Jackson's fluent-setter support.
@Startup
@Singleton
public class ScheduledTaxonomyKafkaSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTaxonomyKafkaSync.class);
    // A separate copy, tolerant of unknown fields - the shared ObjectMapper is also used for
    // REST (de)serialization, where unknown-property strictness is still wanted.
    private static final ObjectMapper OBJECT_MAPPER = new JsonMapperProvider().getObjectMapper()
            .copy()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Inject
    @ConfigProperty(name = "TAXONOMY_KAFKA_BOOTSTRAP_SERVERS")
    Optional<String> bootstrapServers;

    @Inject
    @ConfigProperty(name = "TAXONOMY_KAFKA_TOPIC")
    Optional<String> topic;

    @Inject
    @ConfigProperty(name = "HOSTNAME", defaultValue = "promat-service")
    String hostname;

    @Inject
    TaxonomyCache taxonomyCache;

    @Inject
    MetricRegistry metricRegistry;

    static final Metadata syncFailureCounterMetadata = Metadata.builder()
            .withName("promat_service_taxonomy_kafka_sync_failures")
            .withDescription("Number of taxonomy Kafka sync runs that failed or were aborted")
            .withUnit("failures")
            .build();

    // Cumulative across every scheduled run for this pod's lifetime, keyed by Kafka message key.
    private final Map<String, Subject> subjects = new ConcurrentHashMap<>();

    private String groupId;
    private volatile LocalDateTime lastSuccessfulSyncAt;

    @PostConstruct
    void init() {
        groupId = hostname + "-" + UUID.randomUUID();
        LOGGER.info("Taxonomy Kafka sync starting with consumer group id '{}'", groupId);
        LOGGER.info("Running initial taxonomy Kafka sync at startup");
        run();
    }

    public LocalDateTime getLastSuccessfulSyncAt() {
        return lastSuccessfulSyncAt;
    }

    // persistent = false: don't try to catch up a missed run after a restart.
    @Schedule(second = "0", minute = "0", hour = "*", persistent = false)
    // NOT_SUPPORTED: nothing here touches the database - this is pure Kafka I/O plus in-memory
    // state.
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void run() {
        String bootstrapServersValue = bootstrapServers.orElse("");
        String topicValue = topic.orElse("");
        if (bootstrapServersValue.isBlank() || topicValue.isBlank()) {
            LOGGER.info("Skipping taxonomy Kafka sync because TAXONOMY_KAFKA_BOOTSTRAP_SERVERS or TAXONOMY_KAFKA_TOPIC is not configured");
            return;
        }

        syncTopic(bootstrapServersValue, topicValue);
    }

    void syncTopic(String bootstrapServers, String topic) {
        AtomicInteger processedItems = new AtomicInteger();
        AtomicInteger tombstoneCount = new AtomicInteger();
        AtomicInteger parseErrorCount = new AtomicInteger();
        AtomicInteger appliedUpdateCount = new AtomicInteger();
        int threads = 4;

        try {
            TopicConsumer consumer = TopicConsumer.builder(bootstrapServers, topic)
                    .groupId(groupId)
                    .pollTimeout("5s")
                    .maxPendingJobsPrThread(1000)
                    .build(threads, workerNo -> {
                        return (key, value) -> {
                            // null value = Kafka tombstone (deletion).
                            if (value == null) {
                                processedItems.incrementAndGet();
                                tombstoneCount.incrementAndGet();
                                subjects.remove(key);
                                return;
                            }
                            processedItems.incrementAndGet();
                            try {
                                Subject subject = parseSubject(value);
                                if (isInvalid(subject, key, topic, parseErrorCount)) {
                                    return;
                                }
                                subjects.put(key, subject);
                                appliedUpdateCount.incrementAndGet();
                            } catch (Exception e) {
                                parseErrorCount.incrementAndGet();
                                LOGGER.warn("Skipping taxonomy Kafka record with key '{}' from topic '{}' due to parse/build failure",
                                        key, topic, e);
                            }
                        };
                    });
            consumer.run();
            SyncStats stats = new SyncStats(processedItems.get(), tombstoneCount.get(), parseErrorCount.get());

            if (stats.nonTombstoneCount() > 0 && appliedUpdateCount.get() == 0) {
                // Records came in but none were usable - leave the current state untouched.
                LOGGER.error("Taxonomy Kafka sync aborted for topic '{}': {} records processed but zero subjects could be applied ({} parse errors); taxonomy was not updated",
                        topic, stats.processed(), stats.parseErrors());
                metricRegistry.counter(syncFailureCounterMetadata).inc();
                return;
            }
            if (stats.parseErrors() > 0) {
                LOGGER.warn("Taxonomy Kafka sync for topic '{}' had {} parse errors; affected records were skipped",
                        topic, stats.parseErrors());
            }

            Taxonomy newTaxonomy = new Taxonomy();
            TaxonomyPopulator.populate(newTaxonomy, subjects.values());
            taxonomyCache.set(newTaxonomy);
            lastSuccessfulSyncAt = LocalDateTime.now();

            LOGGER.info("Taxonomy Kafka sync completed for topic '{}': {} records processed this run ({} new/updated, {} tombstoned, {} parse errors), {} subjects tracked in total",
                    topic, stats.processed(), appliedUpdateCount.get(), stats.tombstones(), stats.parseErrors(), subjects.size());
        } catch (InterruptedException e) {
            LOGGER.warn("Taxonomy Kafka sync interrupted for topic '{}'", topic, e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.error("Taxonomy Kafka sync failed for topic '{}'", topic, e);
            metricRegistry.counter(syncFailureCounterMetadata).inc();
        }
    }

    private Subject parseSubject(String value) throws Exception {
        JsonNode root = OBJECT_MAPPER.readTree(value);
        JsonNode payload = root.has("value") ? root.get("value") : root;
        return OBJECT_MAPPER.treeToValue(payload, Subject.class);
    }

    // Checked separately (not just caught as a parse failure) since a missing id/title is
    // valid JSON, just unusable - each gets its own specific log message instead of a generic
    // "parse/build failure".
    private boolean isInvalid(Subject subject, String key, String topic, AtomicInteger parseErrorCount) {
        if (subject.getId() <= 0) {
            parseErrorCount.incrementAndGet();
            LOGGER.warn("Skipping taxonomy Kafka record with key '{}' from topic '{}': missing or non-positive \"id\"", key, topic);
            return true;
        }
        if (subject.getTitle() == null) {
            parseErrorCount.incrementAndGet();
            LOGGER.warn("Skipping taxonomy Kafka record with key '{}' from topic '{}': missing \"title\"", key, topic);
            return true;
        }
        return false;
    }

    private record SyncStats(int processed, int tombstones, int parseErrors) {
        int nonTombstoneCount() {
            return processed - tombstones;
        }
    }
}
