# 7. Synchronous update-service registration of reading experience tasks

Date: 2026-09-21

## Status

Accepted

## Context

Reading experience tasks (`READING_EXPERIENCE_ADULT`/`CHILD`) store the reviewer's selection on the
task. The selection has to be registered on the bibliographic record, which for the old METAKOMPAS
and BUGGI tasks is done by Metakompasset:

- The adult selection (Metakompas taxonomy) is written as MarcXchange `665` fields, the child
  selection (Buggi vocabulary) as `664` fields.
- Metakompasset sends them to DBC's cataloging update-service, which validates and stores the record
  in rawrepo and enqueues the change for the rest of the pipeline.

update-service is a DBC-internal service with no unusual latency or reliability profile, and
registration is a user-triggered, one-off action (registering a selection), not batch work. The
reviewer needs to know right away whether the selection was registered, so a failure can be seen
and retried.

## Decision

- Add a dedicated registration endpoint for reading experience tasks:
  `POST /tasks/{taskId}/reading-experience/register`. Registering a task's selection on the record is
  what approves the task - the reviewer registers their own tasks, and the editor can do the same.
- Keep save and registration separate:
  - `PUT /tasks/{taskId}/reading-experience/adult` / `.../child` persist selections.
  - `POST /tasks/{taskId}/reading-experience/register` registers the persisted selection in
    update-service synchronously, within the request.
- Only set `PromatTask.approved` after update-service returns OK. A failure returns an error
  to the caller instead of approving the task; it can be retried by calling register again.
  Registering again (a correction) keeps the original approval date.
- Saving and registering are allowed in any case status - also after the review is exported, since the
  selection is registered on the record directly, independently of the export - except when the case
  is CLOSED, DELETED, REVERTED, PENDING_REVERT or PENDING_CLOSE (409), as with Metakompasset's
  `cases/{pid}/buggi`.
- The tasks aren't approved together with the case (`internalTask = false`). When the editor
  approves a case whose reading experience tasks aren't registered yet, the case waits in
  `PENDING_READING_EXPERIENCE` until they are, as old METAKOMPAS/BUGGI tasks make it wait in
  `PENDING_EXTERNAL`. As from `PENDING_EXTERNAL`, the editor can approve the case by hand, e.g. when a
  task can't be registered: its reading experience tasks are then approved without being registered.
- Call update-service's REST endpoint (`POST {UPDATE_SERVICE_URL}/api/v1/updateservice`) directly
  with Promat's own `FailSafeHttpClient`, using only the `updateserviceDTO` classes. The shared
  `updateservice-rest-connector` is built against `dbc-commons-httpclient` 2.0 (Java 11) and fails
  at runtime with promat's 21.x.
- Send the same request as Metakompasset: schema `metakompas` (makes update-service merge the fields
  into the existing record), Metakompasset's netpunkt logins - one for adult, one for child
  (`METAKOMPAS_NETPUNKT_*`, `BUGGI_NETPUNKT_*`) - and the rawrepo queue provider in
  `extraRecordData` (`UPDATE_PROVIDER_NAME`, default `fbs-update` as in Metakompasset - the same in
  every environment; which update-service is used is decided by `UPDATE_SERVICE_URL`).
- Build compact MarcXchange update records in Promat, mirroring Metakompasset's `toDanMARC2`:
  - Adult goes to MARC `665`: one field for the work with every selected subject and suggestion
    and `*& lektor`. Moods (`*n`) and the named main character (`*v`) each get a 665 of their own
    before it, e.g. `*& positiv *& lektor *n hyggelig`, as Metakompasset's `SUBFIELD_REQUIRES_OWN_FIELD`.
  - As Metakompasset, every field's subfields are sorted by code (`*&` first); subfields with the
    same code keep their order, suggestions before selected subjects.
  - Child goes to MARC `664`: one field per tag, with its value in `*y`; tags with value 0 are left out.
  - Deliberate differences from Metakompasset: suggestions aren't split on `;` (one suggestion is one
    value, since Promat's UI takes one word per suggestion), and there are no main character
    descriptions (`*h`/`*j`/`*k`/`*l`), since that category was removed from the taxonomy.
    Metakompasset's generated sentences (`*a`) aren't carried over, since they're a deprecated
    feature.

## Consequences

- The frontend gets an immediate, actionable result: `200` means the task is approved and
  registered, an error response means it is not, with no separate status to poll for.
- A transient update-service failure is not retried automatically; the reviewer retries by
  calling register again.
- Verified against update-service on fbstest:
  - Registering again replaces the record's 665 (or 664) fields, so a retry or a correction doesn't
    add duplicates. A registration without fields leaves the existing ones in place.
  - Adult and child registrations don't touch each other's fields.
  - Only records owned by DBC (`996 *a DBC`) can be updated with these logins; common records owned by
    a public library are rejected ("Du har ikke ret til at opdatere fællesskabsposten"), as in
    Metakompasset.
- The service needs `UPDATE_SERVICE_URL` and both netpunkt logins in every environment, or it fails
  to deploy.
- Records registered through Promat get the same 665/664 structure as records registered through
  Metakompasset, apart from the differences listed above.
- This targets DM2's update-service. DM3 has its own endpoint
  (`POST /api/v1/metacompass/{agencyid}/{bibliographicrecordid}`, replacing all 665 fields), which
  registration must move to at the DM3 switch; no DM3 endpoint writing 664 has been found yet.
