# Metakompas/Buggi: switching registration from Metakompasset to Promat

Status: transition in progress. Remove the old task types' handling using the checklist below once
no task of those types is waiting for Metakompasset.

Related: ADR 0006 (checking Metakompas registration in the MARC record) and ADR 0007 (registering
in update-service).

## Background

Until now reviewers registered Metakompas (field 665) and Buggi (field 664) in Metakompasset, an
external app that calls update-service itself. Promat only found out afterwards:

- METAKOMPAS tasks: `CaseInformationUpdater` polls the record (rawrepo-record-service) until it has
  a Metakompas subject in 665, then sets `task.data = "true"` and approves the task.
- BUGGI tasks: Metakompasset calls `POST cases/{pid}/buggi`, which stores the tags as `TagList`
  JSON in `task.data` and approves the task.

Registration in Promat uses its own task types, so the two flows run side by side - per task,
not per environment:

| | `METAKOMPAS` / `BUGGI` | `READING_EXPERIENCE_ADULT` / `READING_EXPERIENCE_CHILD` |
|---|---|---|
| Registered in | Metakompasset | Promat |
| Selection saved through | - | `PUT tasks/{taskId}/reading-experience/adult\|child` |
| Polling, `POST cases/{pid}/buggi` | as before | not touched |
| Approved | by the polling / `POST cases/{pid}/buggi`; case waits in `PENDING_EXTERNAL` | not with the case - own endpoint and `PENDING_READING_EXPERIENCE` come with the registration branch |
| Added to an `APPROVED` case | case goes to `PENDING_EXTERNAL` | no status change yet (see above) |
| Payment | `PayCategory.METAKOMPAS` / `BUGGI` | the same |

A case may have tasks of both sets, e.g. so an editor can try both flows during the transition. Then
the polling for its METAKOMPAS task also takes a 665 Promat registers for a READING_EXPERIENCE_ADULT
task on the same faust as the METAKOMPAS task's registration, and approves it.

Tasks registered in Promat can be changed and registered again, which relies on update-service
replacing the existing 665/664 (verify on staging before prod).

## Rolling out

Prerequisites:

- Approving and registering the new tasks in update-service (registration branch) is merged.
- `UPDATE_SERVICE_URL` (including `/UpdateService/rest`), `METAKOMPAS_NETPUNKT_GROUP/USER/PASSWORD`
  and `BUGGI_NETPUNKT_GROUP/USER/PASSWORD` are set - in every environment, since the service
  doesn't start without them. The netpunkt values are the ones Metakompasset uses (separate
  logins for Metakompas and Buggi).
- Both netpunkt groups have `AUTH_METACOMPASS` in VIP. Without it, update-service rejects any
  change to 664/665 ("missing.auth.meta.compass").

Prod:

1. Deploy promat-service, and the model/connector jars. Nothing creates the new task types yet.
2. Rebuild and deploy every consumer of the connector: `TaskFieldType` is deserialized with a
   default `ObjectMapper`, which fails on unknown enum values. Known consumer: dmat-service
   (`getCase()` in `StatusHandler` and `ExportedPromatReviewsUpdater`).
3. Deploy the frontend that creates the new types on new cases. Old cases keep their tasks, done
   in Metakompasset as before.
4. Metakompasset stays open until no old task is waiting for it (see below). To close it sooner,
   change the remaining old tasks to the new types (they hold no data yet) and move their cases from `PENDING_EXTERNAL` to `PENDING_ISSUES`, so the reviewer can
   fill them in.

## When it's safe to remove the old task types

- Metakompasset is closed.
- No case is waiting for a registration made in Metakompasset:

```sql
-- Should return 0 rows
select c.id, c.status, t.id as task_id, t.taskfieldtype
from promatcase c
join casetasks ct on ct.case_id = c.id
join promattask t on t.id = ct.task_id
where t.taskfieldtype in ('METAKOMPAS', 'BUGGI')
  and t.approved is null
  and c.status not in ('CLOSED', 'DELETED', 'REVERTED');
```

## Removal checklist

promat-service:

- [ ] `CaseInformationUpdater`: delete `checkAndUpdateCaseWithMetakompasdata`,
      `isMetakompasRegistered`, `hasMetakompasRegistration`, `METAKOMPAS_FIELD`,
      `METAKOMPAS_SUBJECT_SUBFIELDS`, `RECORD_CONTENT_PARAMS`, the `RecordServiceConnector` injection,
      `METAKOMPASDATA_PRESENT` and the `PENDING_EXTERNAL` → `APPROVED` block.
- [ ] `Cases`: delete `approveBuggiTask` (`POST cases/{pid}/buggi`), `findBuggiCase`,
      `setApproveBuggiTask`, `INVALID_BUGGI_APPROVAL_STATES`, `PID_PATTERN` (only used there), the
      and the METAKOMPAS/BUGGI handling in `calculateStatus` and `addTask`.
- [ ] `TaskFieldType`: mark `METAKOMPAS` and `BUGGI` `@Deprecated` - old tasks keep them for payment
      history, as with `BIBLIOGRAPHIC` and `GENRE`.
- [ ] `MetakompasAndBuggiTaskSelections.writeBuggiSelection(PromatTask, TagList)` (only used by
      `setApproveBuggiTask`).
- [ ] Connector module: `PromatServiceConnector.approveBuggiTask`. Then `Tag`/`TagList` in the model
      module, if nothing else uses them.
- [ ] Tests: `CaseInformationUpdaterMetakompasTest`, the Metakompas tests in
      `CaseInformationUpdaterSideEffectsIT` (`testWaitForMetakompasData`,
      `testMetakompasSelectionIsLeftAloneWhenRegisteredInPromat`) and `getTasksWhereMetakompasIsPresent`,
      the record-service mock in `CaseInformationUpdaterTestBase`,
      `CaseTaskSelectionIT.testSelectionsRejectedOnTasksRegisteredInMetakompasset` and
      `testLegacyBuggiEndpointSharesSelectionAcrossTargetFausts`, `CasesIT.testBuggiApproval` and
      `testCaseApprovalWithMoreThanOneBuggiTask`.
- [ ] `service/docs/openapi.yml`: `POST cases/{pid}/buggi`.
- [ ] If not done already: apply the fbi-api cleanup (remove `subjects.dbcVerified` from the
      GraphQL queries, `FbiApiHandler.metakompassubject`, `BibliographicInformation.metakompassubject`
      and the WireMock fixtures) - nothing has read it since ADR 0006.
- [ ] Update `CLAUDE.md`/READMEs if they describe the polling.

Keep `rawrepo-record-service-connector`: `RecordsProvider` uses it too.

promat-frontend:

- [ ] Remove the metakompas.dk link (`TaskTable.js`, `enums.js` messages "…via https://metakompas.dk")
      and the creation of METAKOMPAS/BUGGI tasks.
- [ ] `TaskTable.js`: base the "Er registreret" status on `task.approved` instead of
      `task.data === "true"`, and drop `renderTags`' legacy `TagList`/line parsing once no old tasks
      are shown.

gitops:

- [ ] Remove `METAKOMPAS_REGISTRATION` from any environment that sets it - it's no longer read.

## Things learned along the way

- fbi-api can't tell whether a record has a Metakompas registration: `subjects.dbcVerified` mixes 665
  with regular cataloguing (600/610, 666/667), and JED drops subfield `&`. That caused false positives
  (ADR 0006).
- Registered = a 665 with a non-blank value in one of `i q p m g u e h j k l f s r t n a v` - the rule
  OpenFormat's `promat` format used (`has_subjectLaesekompas`).
- promat-service uses `rawrepo-record-service-connector` 21.x (the rawrepo-v3 codebase) against DM2's
  record service (cisterne). The REST API is the same in DM2 and DM3; request `MARC_JSON` explicitly,
  since DM2 defaults to marcxchange XML. Verified against cisterne, fbstest (DM2) and datawell-test (DM3).
- DM3 still uses 665, but one subject per field and with an extra `*2` subfield, and without the
  666 copies DM2's update-service makes of `*& LEKTOR` 665 fields.
- DM2's update-service (what the registration branch calls: `POST /api/v1/updateservice`, schema
  `metakompas`) merges via opencat-business `metacompass`; whether it replaces existing 665 fields
  wasn't verified in code. DM3's `POST /api/v1/metacompass/{agencyid}/{bibliographicrecordid}` replaces
  all 665 fields; Promat's registration must move to it at the DM3 switch. No DM3 endpoint writing 664
  (Buggi) was found.
- `updateservice-rest-connector` is built against `dbc-commons-httpclient` 2.0 (Java 11), which clashes
  with promat's 21.x; the registration branch calls update-service directly instead.
