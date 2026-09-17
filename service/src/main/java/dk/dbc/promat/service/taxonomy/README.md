# TAXONOMY SERVICE
Service for fetching all "subjects" records (agencyId 190004) records, and then to build a "taxonomy tree" used by
the frontend to enable the reviewer to select words and sentences that describe the material.

Subject records in 190004 records are structured this way (from praxis):
https://praxis.dbc.dk/andre-formater/px-aut0012.html/#pxx09kfe


## Fetching records
Every pod consumes a Kafka topic (`TAXONOMY_KAFKA_BOOTSTRAP_SERVERS`/`TAXONOMY_KAFKA_TOPIC`,
containing only metakompas taxonomy-relevant 190004 records) independently, straight into its
own in-memory tree - see `ScheduledTaxonomyKafkaSync`. Each pod uses its own Kafka consumer
group id, generated fresh on every restart (`<hostname>-<random UUID>`), so a fresh pod always
gets a full topic replay on its first sync, then only incremental (new/changed/tombstoned)
records on every subsequent hourly run for the rest of that pod's lifetime.

There is no database or shared state involved - if a pod restarts while Kafka is unreachable, it
serves an empty taxonomy until it successfully syncs again. See
[architecture/decisions](../../../../../../../../docs/architecture/decisions) (0002 through
0004) for how this evolved from an original HTTP-dump-based approach (`curl .../api/v1/dump`,
run every 15 minutes) through a DB-snapshot design and finally to this fully per-pod Kafka
design.

## Testing

`ScheduledTaxonomyKafkaSync` (the Kafka-consuming path itself - `syncTopic()`, tombstone
handling, the cumulative subject map) has no automated test coverage. There's no Kafka test
infrastructure anywhere in this repo (no `testcontainers-kafka` or similar), and
`ContainerTest`/`IntegrationTestIT` never configure `TAXONOMY_KAFKA_BOOTSTRAP_SERVERS`/
`TAXONOMY_KAFKA_TOPIC`, so this class never actually runs in the IT suite either - it just
early-returns on every scheduled `run()`. The one piece of this made genuinely, easily testable
by the refactor is `TaxonomyPopulator` (pure function, no external dependencies) - see
`TaxonomyPopulatorTest`.

As a side effect, the `/taxonomy/tree`, `/taxonomy/structure` and `/taxonomy/subtree*` REST
endpoints also currently have no IT coverage against real populated data - the IT that used to
exercise them (`TaxonomyTreeIT`) was removed along with the old HTTP-dump/`DM2Builder` path it
depended on (commit `86042526`, "remove dm2 builder, remove subtree search endpoint, simplify
gauge").

## Taxonomy tree
In current Metakompas solution the reviewer navigates menu structure like this:
```shell
{
  "ramme": {
    "handlingens tid udtrykt i ord": [],
    "handlingens tid udtrykt i tal": [],
    "geografisk sted": [],
    "fiktivt sted": [],
    "miljø": [],
    "genre": [],
    "univers": []
  },
  "handling": {
    "handler om": [],
    "navngivet hovedperson": [],
    "hovedperson(er) - beskrivelse": {
      "om hovedpersonen": [],
      "hovedpersonens karaktertræk": [],
      "hovedpersonens konflikt": []
    }
  },
  "fortælleteknik": {
    "skrivestil og struktur": [],
    "fortællerstemme": [],
    "tempo": []
  },
  "stemning": {
    "positiv": [],
    "humoristisk": [],
    "romantisk": [],
    "erotisk": [],
    "dramatisk": [],
    "trist": [],
    "uhyggelig": [],
    "fantasifuld": [],
    "tankevækkende": []
  }
}

```

Since this way of structuring are current hardcoded into 190004 records (praxis link above), we will stick to this approach for now.