package dk.dbc.promat.service.batch;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.commons.kafka.consumer.TopicConsumer;
import dk.dbc.promat.service.cluster.ServerRole;
import dk.dbc.promat.service.persistence.JsonMapperProvider;
import dk.dbc.promat.service.taxonomy.TaxonomyCache;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.DependsOn;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

// Reads the whole TAXONOMY_KAFKA_TOPIC topic and hands the result to TaxonomyKafkaPersistence
// to write as a snapshot. Runs at startup and hourly; only on the PRIMARY cluster node.
//
// GOTCHA: no groupId is set on the TopicConsumer below, which (confirmed by decompiling
// dbc-commons-kafka-consumer) makes it assign()+seekToBeginning() instead of tracking
// committed offsets - every run is a full topic replay, not incremental deltas since the last
// run. That's deliberate: it means TaxonomySnapshot always reflects the *complete* current
// state of the topic in one go, so there's no accumulated-state bug to worry about across
// restarts - but it does mean this job's cost scales with the whole topic, every time.
//
// GOTCHA: @DependsOn("DatabaseMigrator") is required, not decorative - without it the EJB
// container is free to start @Startup singletons in any order, and this bean's first query
// has actually hit "relation does not exist" on a fresh database when it ran before
// DatabaseMigrator had created taxonomy_snapshot. Only shows up against a genuinely empty
// database, which is why it went unnoticed for a while.
@Startup
@Singleton
@DependsOn("DatabaseMigrator")
public class ScheduledTaxonomyKafkaSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTaxonomyKafkaSync.class);
    // A dedicated copy of the shared ObjectMapper, tolerant of unknown fields: the topic's
    // producer may add fields we don't model, and the shared instance is also used for REST
    // (de)serialization, where unknown-property strictness is still wanted there.
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
    ServerRole serverRole;

    @Inject
    TaxonomyKafkaPersistence persistence;

    @Inject
    TaxonomyCache taxonomyCache;

    @PostConstruct
    void init() {
        LOGGER.info("Running initial taxonomy Kafka sync at startup");
        run();
    }

    // persistent = false: if the app restarts mid-hour, it waits for the next scheduled hour
    // rather than trying to catch up a missed run (init() above already covers "run once on
    // startup" separately).
    @Schedule(second = "0", minute = "0", hour = "*", persistent = false)
    // NOT_SUPPORTED: this method spends most of its time on non-transactional Kafka I/O; the
    // actual database write happens inside TaxonomyKafkaPersistence.applyToDatabase, which has
    // its own REQUIRES_NEW transaction.
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void run() {
        String bootstrapServersValue = bootstrapServers.orElse("");
        String topicValue = topic.orElse("");
        if (serverRole != ServerRole.PRIMARY) {
            LOGGER.debug("Skipping taxonomy Kafka sync on secondary node");
            return;
        }
        if (bootstrapServersValue.isBlank() || topicValue.isBlank()) {
            LOGGER.info("Skipping taxonomy Kafka sync because TAXONOMY_KAFKA_BOOTSTRAP_SERVERS or TAXONOMY_KAFKA_TOPIC is not configured");
            return;
        }

        syncTopic(bootstrapServersValue, topicValue);
    }

    void syncTopic(String bootstrapServers, String topic) {
        // ConcurrentHashMap/AtomicInteger, not HashMap/int: TopicConsumer runs 4 worker
        // threads below, all reading/writing these concurrently.
        Map<Integer, KafkaTaxonomyItem> seenSubjects = new ConcurrentHashMap<>();
        AtomicInteger processedItems = new AtomicInteger();
        AtomicInteger tombstoneCount = new AtomicInteger();
        AtomicInteger parseErrorCount = new AtomicInteger();
        int threads = 4;

        try {
            TopicConsumer consumer = TopicConsumer.builder(bootstrapServers, topic)
                    .pollTimeout("5s")
                    .maxPendingJobsPrThread(1000)
                    .build(threads, workerNo -> {
                        return (key, value) -> {
                            // A null value is Kafka's tombstone convention: this key no longer
                            // has a value on a compacted topic.
                            if (value == null) {
                                processedItems.incrementAndGet();
                                tombstoneCount.incrementAndGet();
                                return;
                            }
                            processedItems.incrementAndGet();
                            try {
                                KafkaTaxonomyItem subject = parseKafkaTaxonomyItem(value);
                                subject.setSourceRecordId(key);
                                // Validated here rather than downstream, so everything that
                                // ends up in seenSubjects can be assumed well-formed already.
                                if (subject.getId() <= 0) {
                                    parseErrorCount.incrementAndGet();
                                    LOGGER.warn("Skipping taxonomy Kafka record with key '{}' from topic '{}': missing or non-positive \"id\"",
                                            key, topic);
                                    return;
                                }
                                if (subject.getTitle() == null) {
                                    parseErrorCount.incrementAndGet();
                                    LOGGER.warn("Skipping taxonomy Kafka record with key '{}' from topic '{}': missing \"title\"",
                                            key, topic);
                                    return;
                                }
                                seenSubjects.put(subject.getId(), subject);
                            } catch (Exception e) {
                                parseErrorCount.incrementAndGet();
                                LOGGER.warn("Skipping taxonomy Kafka record with key '{}' from topic '{}' due to parse/build failure",
                                        key, topic, e);
                            }
                        };
                    });
            // Blocks until the whole topic has been read - see the class-level gotcha on why
            // this is a full replay every run.
            consumer.run();
            int nonTombstoneCount = processedItems.get() - tombstoneCount.get();
            if (nonTombstoneCount > 0 && seenSubjects.isEmpty()) {
                // Every non-tombstone record failed to parse - something is fundamentally
                // wrong (wrong topic, format changed entirely). Bail out rather than writing
                // an empty snapshot over a good one.
                LOGGER.error("Taxonomy Kafka sync aborted for topic '{}': {} records processed but zero subjects could be parsed ({} parse errors); database was not updated",
                        topic, processedItems.get(), parseErrorCount.get());
                return;
            }
            if (parseErrorCount.get() > 0) {
                LOGGER.warn("Taxonomy Kafka sync for topic '{}' had {} parse errors; affected records were skipped, proceeding with {} successfully parsed subjects",
                        topic, parseErrorCount.get(), seenSubjects.size());
            }
            TaxonomyKafkaPersistence.PersistenceResult result = persistence.applyToDatabase(seenSubjects.values());
            if (result.thresholdExceeded()) {
                LOGGER.error("Taxonomy Kafka sync for topic '{}' left the existing snapshot untouched: replacing it would have dropped subject count from {} to {}, " +
                                "which exceeds the delete-safety threshold - this run likely did not see the full topic. Investigate before the next scheduled run.",
                        topic, result.previousSubjectCount(), result.newSubjectCount());
                return;
            }
            taxonomyCache.refresh();
            LOGGER.info("Taxonomy Kafka sync completed for topic '{}': {} records processed, {} tombstones, {} parse errors, {} subjects written to snapshot",
                    topic,
                    processedItems.get(),
                    tombstoneCount.get(),
                    parseErrorCount.get(),
                    result.writtenSubjects());
        } catch (InterruptedException e) {
            LOGGER.warn("Taxonomy Kafka sync interrupted for topic '{}'", topic, e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.error("Taxonomy Kafka sync failed for topic '{}'", topic, e);
        }
    }

    private KafkaTaxonomyItem parseKafkaTaxonomyItem(String value) throws Exception {
        JsonNode root = OBJECT_MAPPER.readTree(value);
        JsonNode payload = root.has("value") ? root.get("value") : root;
        return OBJECT_MAPPER.treeToValue(payload, KafkaTaxonomyItem.class);
    }

    // Jackson deserialization target for one Kafka message - also, unmodified, the exact JSON
    // shape stored in TaxonomySnapshot.data (see DbTaxonomyBuilder, which reuses this same
    // class to read the snapshot back rather than keeping a second near-identical DTO).
    public static class KafkaTaxonomyItem {
        private String title;
        private List<String> note = new ArrayList<>();
        private List<String> path = new ArrayList<>();
        private int id;
        private boolean oftenUsed;
        private String ref;
        private String sourceRecordId;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<String> getNote() {
            return note;
        }

        public void setNote(List<String> note) {
            this.note = note;
        }

        public List<String> getPath() {
            return path;
        }

        public void setPath(List<String> path) {
            this.path = path;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public boolean isOftenUsed() {
            return oftenUsed;
        }

        public void setOftenUsed(boolean oftenUsed) {
            this.oftenUsed = oftenUsed;
        }

        public String getRef() {
            return ref;
        }

        public void setRef(String ref) {
            this.ref = ref;
        }

        public String getSourceRecordId() {
            return sourceRecordId;
        }

        public void setSourceRecordId(String sourceRecordId) {
            this.sourceRecordId = sourceRecordId;
        }
    }
}
