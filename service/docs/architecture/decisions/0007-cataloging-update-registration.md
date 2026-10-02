# 7. Synchronous cataloging update-service registration for Metakompas and Buggi

Date: 2026-09-21

## Status

Accepted

## Context

Promat now stores reviewer selections for Metakompas and Buggi directly on the task. The next
step is to register those selections on the bibliographic record, replacing the manual/legacy
registration flow through Metakompasset.

The existing Metakompasset code showed the concrete MARC mapping and update-service behavior:

- Metakompas selections are written as compact MarcXchange `665` fields.
- All selected subjects and free-text suggestions go into one 665 for the work, with the generated
  sentences (`*a`) first; suggestions are split on `;`. Moods (`*n`) and the named main character
  (`*v`) get a 665 of their own, with their category in `*&`.
- Buggi selections are written as MarcXchange `664` fields, with the same tag-to-subfield
  mapping and zero-value handling as Metakompasset.
- Metakompasset called DBC's cataloging update-service with authentication, schema name,
  MarcXchange record data and tracking id.

The rawrepo update-service code and docs showed that update-service is the correct integration
point: it validates and persists MARC records to rawrepo, then enqueues changed records so the
rest of the rawrepo/downstream pipeline can react to the change.

update-service is a DBC-internal service with no unusual latency or reliability profile, and
this call happens on a user-triggered, one-off action (a reviewer approving their reading
experience selection), not as part of a batch of system-initiated work. The reviewer needs to
know right away whether the selection was registered, so a failure can be seen and retried.

## Decision

- Add a dedicated approval endpoint for reading experience tasks (READING_EXPERIENCE_ADULT/CHILD):
  `PUT /tasks/{taskId}/reading-experience/register`. Approving such a task means registering its
  selection on the record - the reviewer approves their own tasks, and the editor can do the same.
- Keep save and approval separate:
  - `PUT /tasks/{taskId}/reading-experience/adult` / `.../child` persist selections.
  - `PUT /tasks/{taskId}/reading-experience/register` registers the persisted selection in
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
- Build compact MarcXchange update records in Promat, mirroring Metakompasset's `toDanMARC2`:
  - Metakompas goes to MARC `665`: one field for the work with every selected subject and suggestion
    (suggestions first) and `*& lektor` last. Moods (`*n`) and the named main character (`*v`) each
    get a 665 of their own before it, e.g. `*n hyggelig *& positiv *& lektor`, as metakompasset's
    `SUBFIELD_REQUIRES_OWN_FIELD`. Subfields are written in that order, not sorted.
  - Buggi goes to MARC `664`.
  - Deliberate differences from Metakompasset: suggestions aren't split on `;` (one suggestion is one
    value, since Promat's UI takes one word per suggestion), and there are no main character
    descriptions (`*h`/`*j`/`*k`/`*l`), since that category was removed from the taxonomy.
    Metakompasset's generated sentences (`*a`) aren't supported yet; they're to be added
    separately.

## Consequences

- The frontend gets an immediate, actionable result: `200` means the task is approved and
  registered, an error response means it is not, with no separate status to poll for.
- A transient update-service failure is not retried automatically; the reviewer retries by
  calling approve again. Approving again - also to correct a selection - relies on
  update-service replacing the existing 665/664; verify on staging before prod.
- The service needs `UPDATE_SERVICE_URL` and both netpunkt logins in every environment, or it
  doesn't start.
- Records registered through Promat get the same 665/664 structure as records registered through
  Metakompasset, apart from the differences listed above.
