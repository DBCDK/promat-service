# 4. Drop the DB snapshot; every pod consumes Kafka independently

Date: 2026-09-11

## Status

Accepted

## Context

In review, the design from [0003](0003-primary-only-kafka-sync-with-db-snapshot.md) - one
`PRIMARY` pod harvesting Kafka and persisting a snapshot for every other pod to read - was
questioned on two fronts: storing an ~11,500-subject JSON blob in a single database row felt
architecturally heavy for data Kafka itself already retains durably, and the whole
persistence layer (table, migration, entity, a dedicated persistence class with its own
transaction handling and a safety-threshold guard, plus a second always-on scheduled job just
to keep re-reading that one row) was a lot of moving parts for what it bought: surviving a pod
restart while Kafka happens to be briefly unreachable.

## Decision

- Drop the DB snapshot entirely. Every pod now consumes the Kafka topic directly and
  independently, straight into its own in-memory tree - no shared or persisted state.
- Each pod gets its own Kafka consumer group id, generated once per pod incarnation:
  `<hostname>-<random UUID>`. Unique per pod (two pods must never share a group id - Kafka
  would split the topic's partitions between them via normal group rebalancing, so neither
  would see the whole topic) and, importantly, unique on every restart, so a restarted pod
  never resumes from a previous incarnation's possibly-stale committed offsets - it always
  starts clean.
- Using a group id (instead of the previous groupless `assign()`+`seekToBeginning()`) means
  Kafka's own broker-side offset tracking gives each pod a full topic replay on its first sync
  after (re)start (no committed offset yet for a brand-new group,
  `auto.offset.reset=earliest`), then only new/changed/tombstoned messages on every subsequent
  hourly run, for the rest of that pod's lifetime.
- Because later runs only deliver deltas, `ScheduledTaxonomyKafkaSync` keeps a persistent,
  cumulative in-memory map of subjects across scheduled runs (keyed by the Kafka message key,
  not the subject's own numeric id - a tombstone carries no value to parse an id from), and
  rebuilds the full in-memory tree from that complete map after every run.
- The `>15%`-drop delete-safety threshold from 0003 is dropped entirely - it compared "new full
  read" against "previous full state", a comparison that doesn't translate to an incremental
  delta.
- Accepted trade-off: a pod that restarts while Kafka is unreachable serves an empty taxonomy
  until it successfully syncs again. No DB fallback.
- The staleness gauge is kept, but per-pod and in-memory
  (`promat_service_taxonomy_last_sync_age`, sourced from each pod's own last-successful-sync
  timestamp) rather than DB-sourced.
- `ScheduledTaxonomyUpdater` (the old every-15-minutes DB-refresher) now skips itself entirely
  whenever Kafka is configured - it's only still relevant to the dm2/`DM2Builder` fallback path
  from [0002](0002-metakompas-taxonomy-harvest-decisions.md).

## Consequences

- Removes an entire persistence layer: the `taxonomy_snapshot` table and its migration, the
  `TaxonomySnapshot` entity, `TaxonomyKafkaPersistence`, and `DbTaxonomyBuilder` (repurposed
  into a stateless `TaxonomyPopulator` - same placement logic reused, just with the DB read
  dropped from the front of it). The old `TaxonomySnapshotGauge` is replaced by
  `TaxonomySyncAgeGauge`.
- Simpler system overall: no table/migration to maintain, no cross-transaction coordination
  between the Kafka-consuming job and a DB write, one less always-on scheduled job once Kafka
  is configured.
- New failure mode, deliberately accepted: pod restart + Kafka outage = empty taxonomy until
  Kafka recovers and a sync succeeds. Previously covered by the DB snapshot.
- New minor consideration: since every pod's Kafka read is now fully independent rather than
  all pods converging on one shared DB-written snapshot, pods can transiently diverge from each
  other if one pod's read is briefly incomplete - self-healing at that pod's next successful
  sync, but not instantly consistent cluster-wide the way the DB-backed design was.
- Still no automated test coverage of the Kafka-consuming path itself (same gap as 0003) - one
  new unit test was added for `TaxonomyPopulator`, the one piece of this made newly and easily
  testable (a pure function now, no `EntityManager`) by the refactor.

## Restoring the DB-persisted approach, if ever needed

The commit that introduced the design in [0003](0003-primary-only-kafka-sync-with-db-snapshot.md)
is `af55afdf` ("Add Kafka-driven taxonomy sync and DB-backed builder", 2026-08-31) on this
branch - `git diff af55afdf~1 af55afdf` is the precise, authoritative source for every file
this ADR describes in prose, and is a better restoration reference than reconstructing it from
this document. The commit that removes it is whichever commit lands this ADR's own change -
find it with `git log --oneline -- service/docs/architecture/decisions/0004-drop-db-snapshot-every-pod-consumes-kafka.md`
and diff against its parent. Reverting/cherry-picking `af55afdf` back on top of the code at that
point would substantially restore the original design (expect to redo the merge with whatever's
changed in `ScheduledTaxonomyKafkaSync`/`TaxonomyBuilderProducer`/`ScheduledTaxonomyUpdater`
since).

For a from-scratch reconstruction without git, the pieces to recreate are:
- Migration `V53__taxonomy_snapshot.sql`: `CREATE TABLE taxonomy_snapshot (id integer PRIMARY
  KEY, data text NOT NULL, subject_count integer NOT NULL, updated_at timestamptz NOT NULL
  DEFAULT now())` - always exactly one row, `id = 1`.
- A `TaxonomySnapshot` JPA entity matching that table (register it in **both** persistence
  units in `persistence.xml` - this project requires every entity listed explicitly there).
- A `TaxonomyKafkaPersistence` `@Stateless` EJB, `applyToDatabase(Collection<KafkaTaxonomyItem>)`
  running `@TransactionAttribute(REQUIRES_NEW)`, that finds-or-creates the single
  `TaxonomySnapshot` row, guarded by a delete-safety check
  (`TAXONOMY_SUBJECT_DELETE_THRESHOLD_PERCENT`, default `15`): refuse the write if the new
  subject count is more than that percent lower than the existing row's `subject_count`.
- `DbTaxonomyBuilder implements TaxonomyBuilder`: reads the single `TaxonomySnapshot` row,
  deserializes `data` back into `List<KafkaTaxonomyItem>`, and places each into the tree - this
  is exactly what `TaxonomyPopulator.populate(...)` does today, just add the DB read back in
  front of it.
- `TaxonomyBuilderProducer`: re-add the branch producing `DbTaxonomyBuilder` whenever
  `TAXONOMY_KAFKA_BOOTSTRAP_SERVERS`/`TAXONOMY_KAFKA_TOPIC` are both configured, ahead of the
  `DM2Builder` fallback.
- `ScheduledTaxonomyKafkaSync`: re-add the `PRIMARY`-only gate (via `ServerRole`), drop the
  per-pod consumer group id (go back to groupless `assign()`+`seekToBeginning()`, full replay
  every run), and call `TaxonomyKafkaPersistence.applyToDatabase(...)` +
  `taxonomyCache.refresh()` instead of building/pushing a `Taxonomy` directly.
- `ScheduledTaxonomyUpdater`: remove the "skip when Kafka is configured" check added in this
  ADR, so it goes back to refreshing `TaxonomyCache` from the DB-backed builder on every pod,
  every 15 minutes.
- A `TaxonomySnapshotGauge` reading `updated_at` off the DB row instead of
  `TaxonomySyncAgeGauge`'s per-pod in-memory timestamp.
