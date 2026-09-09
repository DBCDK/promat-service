-- Durable "last known good" snapshot of the taxonomy subjects consumed from the taxonomy
-- Kafka topic (see ScheduledTaxonomyKafkaSync). The category tree itself is hardcoded in
-- Taxonomy.java, same as before this feature - only the subjects are dynamic, so only the
-- subjects need to survive a service restart. Always exactly one row (id = 1).
CREATE TABLE taxonomy_snapshot
(
    id            integer PRIMARY KEY NOT NULL,
    data          text NOT NULL,
    subject_count integer NOT NULL,
    updated_at    timestamptz NOT NULL DEFAULT now()
);
