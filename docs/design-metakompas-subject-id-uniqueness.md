# Design note: Metakompas subject ids are globally unique across the taxonomy

**Date of analysis:** 2026-09-18
**Service:** promat-service
**Status:** Confirmed. Load-bearing assumption in the current metakompas/buggi selection API design.
**Related code:** `taxonomy/dto/Taxonomy.java`, `taxonomy/TaxonomyPopulator.java`, `api/TaskSelections.java`, `api/Tasks.java`

---

## Summary

Every `Subject` in the Metakompas taxonomy tree (`taxonomy/dto/Subject.java`) has an `id`. This id is
**unique across the entire tree**, not just within the category/path it happens to be filed under. This
was not assumed - it was verified empirically against live data, and then traced to its actual source in
the upstream MARC record format.

This fact is why the `PUT tasks/{taskId}/metakompas` endpoint accepts a flat list of ids
(`List<Integer>`) with no accompanying path: the server resolves each id directly via a reverse index
(`Taxonomy.getById(int)`), instead of requiring the client to track and send the path to each subject it
selects.

## Why this matters

Before this was confirmed, the natural-looking request shape was `{ path: [...], ids: [...] }` - grouping
selected ids by the tree path they were found at, because the only lookup `Taxonomy` originally supported
was path-based (`getList(path)`). That shape works, but it pushes bookkeeping onto the client for no
reason if ids are already unique on their own: the client would have to track which path it found each
id under, purely so the server could re-derive information it can look up itself.

Confirming global uniqueness let the request collapse to a bare array of ids
(`PUT tasks/{taskId}/metakompas` body: `[42, 57, 103, 8, 9, 21]`), and let `Taxonomy` gain an id-keyed
reverse index (`byId`, populated alongside the existing tree in `Taxonomy.put(...)`) so a `Subject` -
including its `path`, needed when building the persisted `MetakompasSelectionEntry` - can be resolved from
an id alone.

## Evidence

Four independent checks, from least to most authoritative:

1. **Design intuition, not proof.** Each `Subject` is described in the taxonomy README as a "subject
   record" (agency `190004`, DBC's "Emnebase"). Library/bibliographic authority files conventionally give
   every heading its own uniquely-identified record (the same pattern as e.g. Library of Congress Subject
   Headings) - suggestive, but not evidence on its own, and an earlier guess that the id was simply the
   record's FAUST number (`001$a`) turned out to be wrong (see point 4).

2. **Live taxonomy tree, cross-checked.** Dumping the full `GET /taxonomy/tree` payload from a running
   instance and counting ids across every branch: **11,516 total ids, 11,516 unique, 0 duplicates.**

3. **Taxonomy Kafka topic (`promat-staging-subjects`).** Reading directly from the topic
   (`kafka-console-consumer ... --topic promat-staging-subjects --from-beginning`) shows the **Kafka
   message key equals the subject's `id`** on every sampled message. This matters structurally, not just
   as a coincidence: a Kafka topic can only hold one live value per key (a later message with the same key
   overwrites the earlier one), so the pipeline itself cannot produce two different subjects sharing an
   id - `ScheduledTaxonomyKafkaSync`'s cumulative map (`Map<String, Subject> subjects`, keyed by Kafka
   message key, `ScheduledTaxonomyKafkaSync.java:93-94`) enforces this by construction.

4. **Upstream rawrepo dump - the actual source.** Dumping agency `190004` directly from the record service
   (`POST rawrepo-record-service.../api/v1/dump`, body `{"agencies":[190004],"outputFormat":"JSON"}`) and
   parsing the raw MARC: **79,024 records, unique `001` (FAUST) count = 79,024, zero duplicates** - and,
   separately, the deleted `SubjectBuilder.java` (present before commit `86042526` "remove dm2 builder...",
   recoverable via `git show 86042526^:service/src/main/java/dk/dbc/promat/service/taxonomy/dto/SubjectBuilder.java`)
   shows exactly which field becomes `Subject.id`:

   ```java
   // q: 'ID of subject' (not a faust!) in subfield q
   dataField.getSubField(DataField.hasSubFieldCode('q'))
           .ifPresentOrElse(
                   subField -> handleID(pathSubject, subField.getData()),
                   () -> { throw new IllegalArgumentException("Missing ID ('q' field)"); });
   ```

   So `id` comes from MARC subfield **`x09$q`** - a dedicated, curator-assigned id, explicitly *not* the
   FAUST/`001` id. `handleID` in that same (deleted) class went further and **enforced** uniqueness at
   build time:

   ```java
   private void handleID(PathSubject pathSubject, String idAsString) {
       int id = Integer.parseInt(idAsString);
       if (id <= 0) { throw new IllegalArgumentException(...); }
       if (usedIds.contains(id)) {
           throw new IllegalArgumentException(String.format("Taxonomy ID %d is already in use", id));
       }
       usedIds.add(id);
       pathSubject.withId(id);
   }
   ```

   `usedIds` was a single set shared across the *entire* build (not scoped per category), so the old
   `DM2Builder` pipeline would hard-fail the whole taxonomy build on any collision, anywhere in the tree.
   Uniqueness here isn't incidental - it was a deliberate, validated invariant of the original pipeline,
   sourced from a field the taxonomy's curators specifically maintain for this purpose.

## Caveats

- **The guarantee now rests entirely on data-entry discipline in the source `x09$q` field, not on a
  database constraint or code-level validation upstream.** The old `DM2Builder`/`SubjectBuilder` pipeline
  validated this explicitly and would abort the whole build on a collision (see point 4 above). The
  current Kafka-based pipeline (`TaxonomyPopulator`, `ScheduledTaxonomyKafkaSync`) does not talk to that
  old validation at all - it never did, since the two pipelines never coexisted after the Kafka migration.
- To avoid silently trusting this forever, `TaxonomyPopulator.populate(...)` was given back an explicit
  duplicate-id guard - but tolerant, not fatal: it logs a warning and skips the second occurrence (keeping
  the first-seen subject for a given id), incrementing a `skippedDuplicateId` counter in its summary log,
  the same way it already handles an unresolvable category path. One bad/duplicate record can't take down
  a whole sync, but a collision is no longer silent either.
- If this assumption is ever violated in production data, the practical effect is: the *second* subject
  with a repeated id becomes permanently unreachable through the id-based lookup (`Taxonomy.getById`) and
  is dropped from the tree by `TaxonomyPopulator`, logged as a warning. It would not corrupt existing
  selections, since a `MetakompasSelectionEntry` is a persisted snapshot at time of write.

## Where this is used in code

- `taxonomy/dto/Taxonomy.java` - `byId` reverse index (built in `put(...)`), `getById(int)`.
- `taxonomy/TaxonomyPopulator.java` - duplicate-id detection/skip during tree population.
- `api/TaskSelections.java` - `resolveMetakompasSubject(Integer id)` resolves a `Subject` directly by id
  (no path needed), used by `writeMetakompasSelection(...)`.
- `api/Tasks.java` - `PUT tasks/{taskId}/metakompas` takes a bare `List<Integer>` request body.
