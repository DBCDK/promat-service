# 7. Synchronous cataloging update-service registration for Metakompas and Buggi

Date: 2026-09-21

## Status

Accepted

Supersedes an earlier version of this decision that registered selections asynchronously
through a queue table and a scheduled sender.

## Context

Promat now stores reviewer selections for Metakompas and Buggi directly on the task. The next
step is to register those selections on the bibliographic record, replacing the manual/legacy
registration flow through Metakompasset.

The existing Metakompasset code showed the concrete MARC mapping and update-service behavior:

- Metakompas selections are written as compact MarcXchange `665` fields.
- Free-text Metakompas suggestions are split on `;` and merged into the same MARC field as
  validated taxonomy terms for the same category path.
- Buggi selections are written as MarcXchange `664` fields, with the same tag-to-subfield
  mapping and zero-value handling as Metakompasset.
- Metakompasset called DBC's cataloging update-service with authentication, schema name,
  MarcXchange record data and tracking id.

The rawrepo update-service code and docs showed that update-service is the correct integration
point: it validates and persists MARC records to rawrepo, then enqueues changed records so the
rest of the rawrepo/downstream pipeline can react to the change.

update-service is a DBC-internal service with no unusual latency or reliability profile, and
this call happens on a user-triggered, one-off action (a reviewer registering their reading
experience selection), not as part of a batch of system-initiated work. An earlier version of
this decision queued the registration through a `catalogingupdaterequest` table and a
`ScheduledCatalogingUpdateSender` running every 5 minutes, following the queue-and-scheduler
pattern used elsewhere in promat-service for fire-and-forget system-initiated work (e.g.
notifications, reminders). For this interactive action, that added latency to feedback the
reviewer needs immediately, and gave no way to surface a failure without also building a
separate status-polling endpoint.

## Decision

- Add a dedicated approval endpoint for reading experience tasks (READING_EXPERIENCE_ADULT/CHILD):
  `PUT /tasks/{taskId}/reading-experience/approve`. Approving such a task means registering its
  selection on the record - the reviewer approves their own tasks, and the editor can do the same.
- Keep save and approval separate:
  - `PUT /tasks/{taskId}/reading-experience/adult` / `.../child` persist selections.
  - `PUT /tasks/{taskId}/reading-experience/approve` registers the persisted selection in
    update-service synchronously, within the request.
- Only set `PromatTask.approved` after update-service returns OK. A failure returns an error
  to the caller instead of approving the task; it can be retried by calling approve again.
  Approving again (a correction) keeps the original approval date.
- Saving and approving are allowed in any case status - also after the review is exported, since the
  selection is registered on the record directly, independently of the export - except when the case
  is CLOSED, DELETED, REVERTED, PENDING_REVERT or PENDING_CLOSE (409), as with Metakompasset's
  `cases/{pid}/buggi`.
- The tasks aren't approved together with the case (`internalTask = false`). When the editor
  approves a case whose reading experience tasks aren't approved yet, the case waits in
  `PENDING_READING_EXPERIENCE` until they are, as old Metakompas/Buggi tasks make it wait in
  `PENDING_EXTERNAL`.
- Call update-service's REST endpoint (`POST {UPDATE_SERVICE_URL}/api/v1/updateservice`) directly
  with Promat's own `FailSafeHttpClient`, using only the `updateserviceDTO` classes. The shared
  `updateservice-rest-connector` is built against `dbc-commons-httpclient` 2.0 (Java 11) and fails
  at runtime with promat's 21.x.
- Mirror Metakompasset's request: schema `metakompas` (makes update-service merge the fields into
  the existing record), a separate netpunkt login for Metakompas and for Buggi
  (`METAKOMPAS_NETPUNKT_*`, `BUGGI_NETPUNKT_*`), and the rawrepo queue provider in
  `extraRecordData` (`UPDATE_PROVIDER_NAME`, default `fbs-update` as in Metakompasset - the same in
  every environment; which update-service is used is decided by `UPDATE_SERVICE_URL`).
- Build compact MarcXchange update records in Promat, mirroring the behavior observed in
  Metakompasset:
  - Metakompas goes to MARC `665`. Moods (`*n`) and the named main character (`*v`) get a 665 of
    their own with their category in `*&` (e.g. `*& positiv *& lektor *n hyggelig`), as
    metakompasset's `SUBFIELD_REQUIRES_OWN_FIELD`; other subjects share a field per category path.
  - Unlike Metakompasset, suggestions aren't split on `;`: one suggestion is one value, since Promat's
    UI takes one word per suggestion.
  - Buggi goes to MARC `664`.

## Consequences

- The frontend gets an immediate, actionable result: `200` means the task is approved and
  registered, an error response means it is not, with no separate status to poll for.
- A transient update-service failure is not retried automatically; the reviewer retries by
  calling approve again. Approving again - also to correct a selection - relies on
  update-service replacing the existing 665/664; verify on staging before prod.
- The service needs `UPDATE_SERVICE_URL` and both netpunkt logins in every environment, or it
  doesn't start.
- No queue table, status enum, or scheduled sender is needed for this integration.
- The MARC mapping still follows the behavior observed in Metakompasset.
