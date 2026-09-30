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
this call happens on a user-triggered, one-off action (a reviewer approving their reading
experience selection), not as part of a batch of system-initiated work. An earlier version of
this decision queued the registration through a `catalogingupdaterequest` table and a
`ScheduledCatalogingUpdateSender` running every 5 minutes, following the queue-and-scheduler
pattern used elsewhere in promat-service for fire-and-forget system-initiated work (e.g.
notifications, reminders). For this interactive action, that added latency to feedback the
reviewer needs immediately, and gave no way to surface a failure without also building a
separate status-polling endpoint.

## Decision

- Add a dedicated approval endpoint: `PUT /tasks/{taskId}/reading-experience/approve`.
- Keep save and reading-experience approval separate:
  - `PUT /tasks/{taskId}/metakompas` / `PUT /tasks/{taskId}/buggi` persist selections.
  - `PUT /tasks/{taskId}/reading-experience/approve` registers the persisted selection in
    update-service synchronously, within the request.
- Only set `PromatTask.approved` after update-service returns OK. A failure returns an error
  to the caller instead of approving the task; the reviewer can retry by calling approve again.
- Use the shared Java `updateservice-rest-connector`, not hand-written SOAP XML.
- Build compact MarcXchange update records in Promat, mirroring the behavior observed in
  Metakompasset:
  - Metakompas goes to MARC `665`.
  - Buggi goes to MARC `664`.

## Consequences

- The frontend gets an immediate, actionable result: `200` means the task is approved and
  registered, an error response means it is not, with no separate status to poll for.
- A transient update-service failure is not retried automatically; the reviewer retries by
  calling approve again, which is safe since registration is idempotent per task/faust.
- No queue table, status enum, or scheduled sender is needed for this integration.
- The MARC mapping still follows the behavior observed in Metakompasset.
