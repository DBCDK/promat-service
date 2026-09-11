# 3. Primary-only Kafka sync with a DB-persisted snapshot

Date: 2026-08-31

## Status

Superseded by [0004](0004-drop-db-snapshot-every-pod-consumes-kafka.md)

## Context

Following on from the "dm3" direction in [0002](0002-metakompas-taxonomy-harvest-decisions.md), the taxonomy
subject data became available on a Kafka topic (`TAXONOMY_KAFKA_TOPIC`) and promat needed a
concrete way to consume it into the in-memory taxonomy tree every pod serves over REST.

At the time, consuming the whole topic (~11,500 subjects on the real topic) independently on
every pod, every sync, looked wasteful - the same multi-thousand-message read and parse work
repeated on every pod for no benefit, since all pods need to end up serving the same tree
anyway. We also wanted a pod that restarts while Kafka happens to be briefly unreachable to
still have a taxonomy to serve, rather than come up empty.

## Decision

- Only the cluster's `PRIMARY` node (decided via `ServerRole`, itself decided from `HOSTNAME`
  ending in `-0`) runs the Kafka consumption job, `ScheduledTaxonomyKafkaSync` - hourly, plus
  once at startup. It reads the whole topic every run (no Kafka consumer group -
  `assign()`+`seekToBeginning()`).
- The result is persisted as a single-row JSON blob in a new `taxonomy_snapshot` Postgres table
  (`TaxonomySnapshot` entity, migration `V53`), written by `TaxonomyKafkaPersistence` in its own
  `REQUIRES_NEW` transaction (kept separate from the largely non-transactional Kafka-consuming
  work).
- A `>15%`-subject-drop delete-safety threshold
  (`TAXONOMY_SUBJECT_DELETE_THRESHOLD_PERCENT`, default `15`) refuses to overwrite the snapshot
  if a run's subject count looks like a bad/partial Kafka read rather than a real change.
- Every pod (PRIMARY and SECONDARY alike) independently rebuilds its own in-memory tree by
  reading that one shared DB row (`DbTaxonomyBuilder`), driven by a separate job,
  `ScheduledTaxonomyUpdater`, running on every pod every 15 minutes and at startup - this is
  what actually protects against "pod restarts while Kafka is unreachable": the DB row is still
  there even if Kafka isn't.
- A MicroProfile Metrics gauge, `TaxonomySnapshotGauge`
  (`promat_service_taxonomy_snapshot_age`), exposes how stale the DB-persisted snapshot is.
- `TaxonomyBuilderProducer` picks `DbTaxonomyBuilder` whenever
  `TAXONOMY_KAFKA_BOOTSTRAP_SERVERS`/`TAXONOMY_KAFKA_TOPIC` are both configured, otherwise falls
  back to the dm2 `DM2Builder` from 0002 if `RECORD_SERVICE` is set.

## Consequences

- Only one pod ever does the Kafka-consuming work per sync cycle, and the result is durable and
  cluster-shared - any pod restart, or a temporary Kafka outage, is covered by the DB row.
- Real added complexity: a new table and migration, a JPA entity, a dedicated persistence class
  with its own transaction semantics and a safety-threshold guard, a metrics gauge reading from
  the DB, and a second always-on scheduled job (`ScheduledTaxonomyUpdater`) whose only purpose,
  once Kafka was configured, was to keep re-reading that one DB row on every pod.
- No automated test coverage was ever added for this path - `ScheduledTaxonomyKafkaSync`,
  `TaxonomyKafkaPersistence`, `DbTaxonomyBuilder` and `TaxonomySnapshotGauge` all had zero
  unit/integration tests; the design was verified manually against the real Kafka topic instead.
- Storing the harvested subjects (a multi-megabyte JSON blob, full replay every run) as a `text`
  column (not `jsonb`, to sidestep an EclipseLink/PGobject binding issue hit at the time) in a
  single database row later raised review questions about whether persisting this in the
  database at all was warranted, given Kafka itself already durably retains the source data -
  see [0004](0004-drop-db-snapshot-every-pod-consumes-kafka.md).
