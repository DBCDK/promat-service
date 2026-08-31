package dk.dbc.promat.service.batch;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.commons.kafka.consumer.TopicConsumer;
import dk.dbc.commons.kafka.consumer.TopicConsumerException;
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

// @Singleton: an EJB (Enterprise JavaBean) stereotype meaning "the container creates exactly
// ONE instance of this class for the whole application", shared by every caller - unlike a
// plain `new ScheduledTaxonomyKafkaSync()` which you'd never write yourself, the app server
// (Payara here) constructs it, manages its lifecycle, and hands it out via dependency
// injection (@Inject) wherever it's needed.
//
// @Startup means "construct this singleton eagerly, as part of application deployment" -
// without it, a @Singleton is only guaranteed to be constructed lazily, the first time
// something actually needs it, which could be much later (or never).
//
// @DependsOn("DatabaseMigrator") tells the EJB container "fully initialize the DatabaseMigrator
// singleton (running its own @PostConstruct to completion) before starting this one". Without
// this, the container is free to initialize @Startup singletons in any order - which caused a
// real bug during this project's testing: on a fresh database, this bean's @PostConstruct
// sometimes ran before DatabaseMigrator had created the taxonomy_category/taxonomy_subject
// tables, so the very first query failed with "relation does not exist". It never showed up
// against a database that already had the tables from a previous run, which is exactly why
// it went unnoticed until testing against a genuinely empty database.
@Startup
@Singleton
@DependsOn("DatabaseMigrator")
public class ScheduledTaxonomyKafkaSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTaxonomyKafkaSync.class);
    // Dedicated copy, tolerant of fields the topic's producer adds that we don't model here -
    // the shared JsonMapperProvider instance is also used for REST (de)serialization, where
    // unknown-property strictness is still wanted, so it must not be reconfigured globally.
    // .copy() clones the shared ObjectMapper's configuration into a brand new instance rather
    // than mutating the original in place - .configure(...) below only affects this copy.
    private static final ObjectMapper OBJECT_MAPPER = new JsonMapperProvider().getObjectMapper()
            .copy()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // @Inject is Jakarta CDI's (Contexts and Dependency Injection) core annotation: "the
    // container, not my own code, is responsible for supplying a value for this field". You
    // never call a constructor for these fields yourself.
    //
    // @ConfigProperty (from MicroProfile Config, a separate but related spec) injects the
    // value of an environment variable / system property / config source by name - this is
    // how TAXONOMY_KAFKA_BOOTSTRAP_SERVERS (see scripts/common) ends up here. Wrapping it in
    // Optional<String> instead of a plain String means "this config value might legitimately
    // be absent" - if it were just `String bootstrapServers`, a missing env var would either
    // fail deployment outright or inject null, depending on configuration; Optional forces
    // calling code (see run() below) to explicitly handle the "not configured" case.
    @Inject
    @ConfigProperty(name = "TAXONOMY_KAFKA_BOOTSTRAP_SERVERS")
    Optional<String> bootstrapServers;

    @Inject
    @ConfigProperty(name = "TAXONOMY_KAFKA_TOPIC")
    Optional<String> topic;

    // No @ConfigProperty here - this injects another CDI-managed bean (ServerRole, produced
    // elsewhere by ServerRoleFactory based on the pod's hostname), the same mechanism as
    // @Inject on its own always means "give me a bean", whether that bean is a config value,
    // an EJB, or a plain CDI bean.
    @Inject
    ServerRole serverRole;

    @Inject
    TaxonomyKafkaPersistence persistence;

    @Inject
    TaxonomyCache taxonomyCache;

    // @PostConstruct marks a method the container calls automatically exactly once, right
    // after this singleton has been constructed and all its @Inject fields have been
    // populated - this is the standard place to put "startup" logic in Jakarta EE, since the
    // constructor itself can't rely on injected fields being set yet (constructor injection
    // for singletons is possible but rarer; field injection like above resolves later).
    @PostConstruct
    void init() {
        LOGGER.info("Running initial taxonomy Kafka sync at startup");
        run();
    }

    // @Schedule is an EJB "timer service" annotation - a cron-like trigger the container
    // manages for you, no external scheduler needed. This particular expression
    // (second=0, minute=0, hour=*) fires once every hour, on the hour. persistent = false
    // means "don't survive a server restart" - if the app restarts mid-hour, it simply waits
    // for the next scheduled hour rather than trying to catch up a missed run (init() above
    // already covers "run once immediately on startup" separately).
    //
    // @TransactionAttribute(NOT_SUPPORTED) opts this method OUT of the EJB container's
    // automatic transaction management - by default, EJB methods run inside a container
    // transaction, but this method spends most of its time doing (non-transactional) Kafka
    // I/O; the actual database writes happen inside TaxonomyKafkaPersistence.applyToDatabase,
    // which has its own @TransactionAttribute(REQUIRES_NEW) - see that class for why.
    @Schedule(second = "0", minute = "0", hour = "*", persistent = false)
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
        // ConcurrentHashMap and AtomicInteger, not a plain HashMap/int: TopicConsumer.build(...)
        // below runs several worker threads in parallel (threads = 4), all reading/writing
        // these same variables concurrently. A plain HashMap or `int` counter would be
        // corrupted under concurrent access (lost updates, or worse - HashMap can enter an
        // infinite loop under concurrent modification); these "atomic"/"concurrent" variants
        // from java.util.concurrent are specifically designed to be safe to share across
        // threads without you having to write your own locking code.
        Map<Integer, KafkaTaxonomyItem> seenSubjects = new ConcurrentHashMap<>();
        AtomicInteger processedItems = new AtomicInteger();
        AtomicInteger tombstoneCount = new AtomicInteger();
        AtomicInteger parseErrorCount = new AtomicInteger();
        int threads = 4;

        try {
            // TopicConsumer.builder(...).build(threads, workerFactory) comes from the
            // dk.dbc.commons.kafka.consumer library: it hides the raw Kafka client behind a
            // "give me a function to call per (key, value) message" API. `workerNo -> { return
            // (key, value) -> {...}; }` is a lambda returning another lambda - the outer one
            // is a factory called once per worker thread (workerNo 0..3), and the inner one
            // is the actual per-message callback that factory produces. This project doesn't
            // use workerNo for anything, but the library's API requires a factory shape so it
            // can hand each thread its own independent worker instance if needed.
            TopicConsumer consumer = TopicConsumer.builder(bootstrapServers, topic)
                    .pollTimeout("5s")
                    .maxPendingJobsPrThread(1000)
                    .build(threads, workerNo -> {
                        return (key, value) -> {
                            // A Kafka message with a null value is a "tombstone" - the
                            // producer's way of saying "this key no longer has a value",
                            // conventionally used to represent deletion in a compacted topic.
                            if (value == null) {
                                processedItems.incrementAndGet();
                                tombstoneCount.incrementAndGet();
                                return;
                            }
                            processedItems.incrementAndGet();
                            try {
                                KafkaTaxonomyItem subject = parseKafkaTaxonomyItem(value);
                                subject.setSourceRecordId(key);
                                // Validated here, at ingestion, rather than later during
                                // persistence: catching a malformed record as early as
                                // possible keeps the rest of the pipeline able to assume
                                // "every KafkaTaxonomyItem in seenSubjects is well-formed",
                                // instead of every downstream consumer needing its own
                                // defensive null-checks.
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
            // .run() blocks the calling thread until the consumer has read everything
            // currently available on the topic (this library is designed for exactly this
            // "read the whole topic, then stop" use case, rather than an endless streaming
            // subscription) - by the time control returns here, all worker threads have
            // finished and seenSubjects/the counters hold their final values for this run.
            consumer.run();
            int nonTombstoneCount = processedItems.get() - tombstoneCount.get();
            if (nonTombstoneCount > 0 && seenSubjects.isEmpty()) {
                // Every non-tombstone record failed to parse - something is fundamentally
                // wrong (wrong topic, format changed entirely, etc). Applying this result
                // would delete every existing subject, so bail out instead.
                LOGGER.error("Taxonomy Kafka sync aborted for topic '{}': {} records processed but zero subjects could be parsed ({} parse errors); database was not updated",
                        topic, processedItems.get(), parseErrorCount.get());
                return;
            }
            if (parseErrorCount.get() > 0) {
                LOGGER.warn("Taxonomy Kafka sync for topic '{}' had {} parse errors; affected records were skipped, proceeding with {} successfully parsed subjects",
                        topic, parseErrorCount.get(), seenSubjects.size());
            }
            TaxonomyKafkaPersistence.PersistenceResult result = persistence.applyToDatabase(seenSubjects.values());
            taxonomyCache.refresh();
            if (result.deletionThresholdExceeded()) {
                LOGGER.error("Taxonomy Kafka sync for topic '{}' skipped deleting {} of {} existing subjects because it exceeded the delete-safety threshold - " +
                                "upserts were still applied, but this run likely did not see the full topic. Investigate before the next scheduled run.",
                        topic, result.subjectsThatWouldHaveBeenDeleted(), result.existingSubjectCountBeforeDelete());
            }
            LOGGER.info("Taxonomy Kafka sync completed for topic '{}': {} records processed, {} tombstones, {} parse errors, {} subjects seen, {} categories created, {} subjects inserted, {} subjects updated, {} subjects deleted, {} subjects skipped",
                    topic,
                    processedItems.get(),
                    tombstoneCount.get(),
                    parseErrorCount.get(),
                    seenSubjects.size(),
                    result.createdCategories(),
                    result.insertedSubjects(),
                    result.updatedSubjects(),
                    result.deletedSubjects(),
                    result.skippedSubjects());
        } catch (InterruptedException e) {
            // InterruptedException is Java's cooperative way of asking a thread to stop what
            // it's doing (e.g. the app server shutting down mid-sync). The convention when
            // you catch it but aren't going to propagate it further up the call stack is to
            // call Thread.currentThread().interrupt() - this re-sets the thread's "interrupted"
            // flag, so any other code further up that later checks it (or makes another
            // blocking call) still finds out the interrupt happened, instead of it being
            // silently swallowed here.
            LOGGER.warn("Taxonomy Kafka sync interrupted for topic '{}'", topic, e);
            Thread.currentThread().interrupt();
        } catch (TopicConsumerException e) {
            LOGGER.error("Taxonomy Kafka sync failed for topic '{}'", topic, e);
        } catch (Exception e) {
            // A catch-all after two more specific catches: Java tries catch blocks top to
            // bottom and uses the first one whose type matches, so this only catches
            // exceptions that AREN'T an InterruptedException or TopicConsumerException -
            // anything unexpected still gets logged with its full stack trace instead of
            // crashing the scheduled timer callback (which the container would otherwise
            // just log tersely and move on from).
            LOGGER.error("Taxonomy Kafka sync failed for topic '{}'", topic, e);
        }
    }

    private KafkaTaxonomyItem parseKafkaTaxonomyItem(String value) throws Exception {
        JsonNode root = OBJECT_MAPPER.readTree(value);
        JsonNode payload = root.has("value") ? root.get("value") : root;
        return OBJECT_MAPPER.treeToValue(payload, KafkaTaxonomyItem.class);
    }

    // A small "static nested class" used purely as a Jackson deserialization target - it has
    // no behaviour beyond plain getters/setters (which Jackson calls via reflection to
    // populate fields from JSON, the same mechanism JPA uses for entities). It's `static`
    // so it doesn't implicitly hold a reference to an enclosing ScheduledTaxonomyKafkaSync
    // instance (a non-static inner class would, needlessly, since this is just a data shape).
    // It's deliberately a separate, minimal type from the TaxonomySubject JPA entity - this
    // one's shape is dictated by what the Kafka topic's producer sends, while TaxonomySubject's
    // shape is dictated by the database schema; keeping them separate means either side can
    // change independently without the other needing to.
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
