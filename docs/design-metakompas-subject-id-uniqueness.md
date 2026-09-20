# Design note: Metakompas subject ids are globally unique

**Date:** 2026-09-18
**Status:** Confirmed
**Related code:** `taxonomy/dto/Taxonomy.java`, `taxonomy/TaxonomyPopulator.java`,
`model/dto/MetakompasSelectionRequest.java`, `api/TaskSelections.java`, `api/Tasks.java`

## Decision

Metakompas subject ids are treated as globally unique across the taxonomy tree.

Selected existing subjects in `PUT tasks/{taskId}/metakompas` are therefore resolved by id through
`Taxonomy.getById(int)`. The submitted path is not used to find selected ids.

The request is still grouped by path:

```json
[
  {
    "path": ["handling", "handler om"],
    "ids": [42, 57],
    "suggestions": ["newly suggested term"]
  }
]
```

The path is needed for `suggestions`, because new/free-text suggestions do not have taxonomy ids yet.
Invalid paths are rejected before the selection is saved.

## Rationale

The uniqueness assumption was checked against the live taxonomy tree and Kafka subject topic: sampled and
dumped subjects had no duplicate taxonomy ids. The old DM2 builder also enforced uniqueness globally while
building subjects from MARC `x09$q`.

Because ids are unique, clients do not need to send a path so the server can resolve an existing subject.
The server can look up the subject directly and persist the canonical subject snapshot, including the
subject's own path.

## Consequences

- `Taxonomy` maintains a `byId` reverse index.
- `TaskSelections.resolveMetakompasSubject(Integer id)` validates selected ids against the current taxonomy.
- `MetakompasSelectionRequest.path` is validated and exists to place id-less suggestions, not to resolve selected ids.
- `TaxonomyPopulator` logs and skips duplicate ids if bad source data ever appears.
