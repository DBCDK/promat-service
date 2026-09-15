# 5. Remove the unused taxonomy subtree search endpoint

Date: 2026-09-15

## Status

Accepted

## Context

`POST /taxonomy/subtree/search` (`TaxonomyService.searchTaxonomySubtree`, backed by
`Taxonomy.searchList(String[] path, String query, int limit)`) let a caller search subject
titles within a subtree, case-insensitively, capped at 50 results. It existed specifically to
avoid the cost of `getList()`'s approach - converting every raw entry in a subtree to a
`Subject` object on every call - for subtrees large enough that this would be wasteful (e.g.
"handling->handler om" has thousands of subjects on the real topic): `searchList()` filtered
the raw map entries by title first, and only converted the (at most `limit`) matches.

Confirmed unused: no test coverage anywhere in the repo, no usage from the `connector` module,
and no other code in this repo referencing it - and confirmed with the reviewer that the
frontend doesn't call this endpoint either.

## Decision

- Remove `TaxonomyService.searchTaxonomySubtree` and `Taxonomy.searchList(...)`, along with the
  now-unused `SEARCH_RESULT_LIMIT` constant and `QueryParam` import.
- Update `service/docs/openapi.yml` to drop the corresponding path. This file is hand-maintained
  (copied into the WAR's `META-INF` via `maven-war-plugin`, not generated from the JAX-RS
  annotations) - removing the Java endpoint alone left Swagger UI still advertising and serving
  it until this was caught and fixed separately.
- `getList()` (used by `/taxonomy/subtree` and `/taxonomy/subtree/{alias}`) is untouched - it
  still converts every entry fresh, uncached, on every call. That's a separate, independent
  design decision (documented as a comment on `getList()` itself), not something this removal
  changes.

## Consequences

- One less REST endpoint and method, no behavior anyone was relying on.
- If subtree search is ever needed again, it should be rebuilt against today's data source
  (`TaxonomyCache`, populated by `ScheduledTaxonomyKafkaSync`), not by restoring anything from
  the old `DM2Builder`-era HTTP-fetch pipeline - see below.

## Restoring subtree search, if ever needed

Do **not** restore it via `DM2Builder`/`TaxonomyBuilder`/`TaxonomyBuilderProducer` - that whole
pipeline is gone (removed in the same commit as this change) and has no relationship to how
search itself worked. `searchList()` never called into any of those classes; it only ever read
from the same in-memory `Taxonomy` tree that `getList()` still reads from today, which is
populated by `ScheduledTaxonomyKafkaSync` regardless of whether search exists. Restoring search
needs zero interaction with that history.

`searchList()` was introduced in `af55afdf` ("Add Kafka-driven taxonomy sync and DB-backed
builder", 2026-08-31), refined in `9e6f967a`, and removed in `86042526` ("remove dm2 builder,
remove subtree search endpoint, simplify gauge", 2026-09-15) - `git show 86042526 --
service/src/main/java/dk/dbc/promat/service/taxonomy/dto/Taxonomy.java
service/src/main/java/dk/dbc/promat/service/api/TaxonomyService.java` shows exactly what to add
back. Concretely:

- On `Taxonomy`: re-add a method that takes a path, a query string and a limit; fetches the raw
  `List<LinkedHashMap<String,Object>>` at that path (the same private `getList(List<String>)`
  helper `getList()`/`get()`/`put()` already use); filters by case-insensitive substring match on
  `title`; limits *before* mapping to `Subject` (that ordering is the whole point - it's what
  avoids `getList()`'s per-call conversion cost for large subtrees); then maps matches to
  `Subject`.
- On `TaxonomyService`: re-add a `@POST @Path("subtree/search")` method taking a `@QueryParam("q")`
  query string and a `List<String>` path body, calling the above, same 400/404 handling as
  before (missing `q`, unresolvable path).
- Add the endpoint back to `service/docs/openapi.yml` - remember this file needs the manual edit
  every time, it will not pick this up on its own.
