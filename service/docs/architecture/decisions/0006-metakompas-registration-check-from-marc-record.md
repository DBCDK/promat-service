# 6. Check Metakompas registration in the MARC record, not in fbi-api

Date: 2026-09-28

## Status

Accepted

## Context

`CaseInformationUpdater` polls METAKOMPAS tasks: once every target faust has a Metakompas
registration, the task gets `data = "true"` and is approved, which lets a `PENDING_EXTERNAL`
case move on to `APPROVED` (and the task be paid).

From 2021 the answer came from OpenFormat's `promat` format, whose `metakompassubject` element
was its `has_subjectLaesekompas` field (OpenFormat repo,
`config/fields/has_subjectLaesekompas.json`): `true` if the record has a field 665 with a
non-empty value in one of the subfields `i q p m g u e h j k l f s r t n a v`.

When OpenFormat was replaced by fbi-api, that rule was approximated as "`subjects.dbcVerified`
is non-empty". This gives false positives. fbi-api's data (JED, built by `dbc-js`
`corepo-work-to-jed`, `marc/Subjects.marc.js`) puts regular cataloguing into `dbcVerified` too:
persons/corporations from 600/610 in DBC records and controlled subjects from 666/667, besides
some 665 subfields. Example: 870970-basis:143900673 has only a 600 (Estrid Hein) and 666
subjects - no Metakompas registration - yet `dbcVerified` is non-empty, so its task was
approved. JED also drops subfield `&`, so fbi-api can't express the Metakompas origin at all.

## Decision

- Read the record itself from rawrepo-record-service: agency 870970, `MERGED` (so DBC
  enrichments are included), `MARC_JSON` (the format `MarcBinding` can read). Only the record
  whose `001 *a` is the faust counts - the returned collection can also hold head/section and
  authority records.
- Use OpenFormat's rule unchanged: registered = a field 665 with a non-blank value in one of the
  subfields `i q p m g u e h j k l f s r t n a v`. This restores the behaviour from before the
  fbi-api switch.
- Not chosen: requiring `665 *& lektor`, the marker Metakompasset adds to every 665 it submits
  (metakompasset `DanMARC2Converter.toDanMARC2`, since 2018; also relied on by updateservice's
  `MetakompasHandler`). It is stricter than the old rule, and records whose 665 lacks the marker
  (e.g. older registrations) would leave their case in `PENDING_EXTERNAL` until promoted by hand.
- Skip tasks already marked registered (`data = "true"`) instead of looking them up again every
  run. Skip on `data`, not `approved`: a task can be approved by hand (case promoted from
  `PENDING_EXTERNAL`) before the registration exists, and should still end up marked registered.
  An existing approval date is kept.
- An empty `targetFausts` falls back to the case's primary faust, like `null` already did
  (before, an empty list matched vacuously and approved the task without any lookup).
- A failed lookup counts as not registered for that run; the rest of the case update proceeds.

## Consequences

- The updater depends on rawrepo-record-service as well as fbi-api: one lookup per target faust
  of each not-yet-registered METAKOMPAS task per run.
- Verified against production data: 870970-basis:143900673 (Estrid Hein, no 665) is not
  registered; 870970-basis:143850609 (Anders And, 665 fields from Metakompasset) is.
- fbi-api's `subjects.dbcVerified` / `BibliographicInformation.metakompassubject` are now unused.
  Removing them (queries, `FbiApiHandler`, WireMock fixtures) is left as a separate cleanup to
  keep this change small.
- This polling is transitional. Once Promat registers Metakompas/Buggi selections itself and
  approves the task on success (see the cataloging update-service registration decision on the
  `mk-buggi-registration-update-service` branch), it is only needed for cases started through
  Metakompasset, and can be removed when none of those remain.
