# Metakompas/Buggi: switching registration from Metakompasset to Promat

Status: transition in progress. Remove the flag and the old path using the checklist below once
the switch is done.

Related: ADR 0006 (checking Metakompas registration in the MARC record) and the cataloging
update-service registration ADR on the `mk-buggi-registration-update-service` branch.

## Background

Until now reviewers registered Metakompas (field 665) and Buggi (field 664) in Metakompasset, an
external app that calls update-service itself. Promat only found out afterwards:

- METAKOMPAS tasks: `CaseInformationUpdater` polls the record (rawrepo-record-service) until it has
  a Metakompas subject in 665, then sets `task.data = "true"` and approves the task.
- BUGGI tasks: Metakompasset calls `POST cases/{pid}/buggi`, which stores the tags as `TagList`
  JSON in `task.data` and approves the task.

The new flow lets reviewers register in Promat: `PUT tasks/{taskId}/metakompas|buggi` saves the
selection as JSON in `task.data`, and `PUT tasks/{taskId}/reading-experience/approve` registers it
in update-service and approves the task on success.

The two flows can't run at once. The polling would overwrite a saved selection with `"true"` (Promat's
own registration also writes 665), and a late call from Metakompasset would overwrite a saved Buggi
selection with a `TagList`.

## The flag

`METAKOMPAS_REGISTRATION` (read by `MetakompasRegistration`). Unset means `METAKOMPASSET`, the current
flow - only an environment switched to Promat needs it (`METAKOMPAS_REGISTRATION=PROMAT`):

| | `METAKOMPASSET` | `PROMAT` |
|---|---|---|
| Metakompas polling in `CaseInformationUpdater` | runs | skipped (the rest of the updater runs as usual) |
| `PUT tasks/{taskId}/metakompas\|buggi` | 409, `INVALID_STATE` | allowed, unless the task is already approved (409) |
| `PUT tasks/{taskId}/reading-experience/approve` | 409 (to be added with the approve flow) | allowed, unless the task is already approved (409) |
| `POST cases/{pid}/buggi` (Metakompasset) | allowed | 409, `INVALID_STATE` |

"Already approved means closed" covers every task finished before the switch, whatever its `data`
holds: `"true"` (registered via Metakompasset), a `TagList` (Buggi via Metakompasset), or anything on
a task approved by hand when its case was promoted from `PENDING_EXTERNAL`. No task is converted
between data formats.

The frontend mode (metakompas.dk link vs. Promat's selection UI) is switched by hand at the same time.

## Switching an environment to PROMAT

Prerequisites:

- The approve flow is merged, including the mode check and the "already approved" check (same as
  in `MetakompasAndBuggiTaskSelections.resolveTaskForSelection`).
- `UPDATE_SERVICE_URL`, `UPDATE_SCHEMA_NAME`, `UPDATE_NETPUNKT_GROUP/USER/PASSWORD` are set.
- Promat's netpunkt group has `AUTH_METACOMPASS` in VIP. Without it, update-service rejects any
  change to 664/665 ("missing.auth.meta.compass").

Prod:

1. Announce: "Metakompasset closes in 3 days". Prod stays on `METAKOMPASSET`.
2. Day 3: Metakompasset closes. Let `ScheduledCaseInformationUpdater` run once more (every 10
   minutes, 06-18 weekdays) to pick up the last registrations.
3. Set `METAKOMPAS_REGISTRATION=PROMAT` in gitops and switch the frontend mode.
4. Check for leftovers: METAKOMPAS tasks registered in Metakompasset but not picked up keep their
   case in `PENDING_EXTERNAL`. Promote those cases by hand (moving a case from `PENDING_EXTERNAL`
   to `APPROVED` approves all its tasks).

Unfinished tasks need nothing: after the switch they're simply done in Promat.

## When it's safe to remove the flag

- Every environment runs `PROMAT`, and prod has done so without problems for a while.
- No case is waiting for a registration made in Metakompasset:

```sql
-- Should return 0 rows
select c.id, c.status, t.id as task_id, t.taskfieldtype
from promatcase c
join casetasks ct on ct.case_id = c.id
join promattask t on t.id = ct.task_id
where t.taskfieldtype in ('METAKOMPAS', 'BUGGI')
  and t.approved is null
  and c.status = 'PENDING_EXTERNAL'
  and (t.data is null or t.data = '' or t.data = 'true');
```

## Removal checklist

promat-service:

- [ ] Delete `MetakompasRegistration` and every injection/usage of it (`CaseInformationUpdater`,
      `MetakompasAndBuggiTaskSelections`, `Cases`, and the approve flow).
- [ ] `CaseInformationUpdater`: delete the `if (metakompasRegistration.isMetakompasset())` block,
      `checkAndUpdateCaseWithMetakompasdata`, `isMetakompasRegistered`, `hasMetakompasRegistration`,
      `METAKOMPAS_FIELD`, `METAKOMPAS_SUBJECT_SUBFIELDS`, `RECORD_CONTENT_PARAMS`, the
      `RecordServiceConnector` injection and `METAKOMPASDATA_PRESENT`. **Keep** the
      `PENDING_EXTERNAL` → `APPROVED` block - the new flow relies on it unless the approve endpoint
      moves the case itself.
- [ ] `MetakompasAndBuggiTaskSelections.resolveTaskForSelection`: delete the mode check. **Keep** the
      "already approved" check - it still stops a registered task from being registered twice.
      Same in the approve flow.
- [ ] `Cases`: delete `approveBuggiTask` (`POST cases/{pid}/buggi`), `findBuggiCase`,
      `setApproveBuggiTask`, `INVALID_BUGGI_APPROVAL_STATES`, `PID_PATTERN` (only used there) and the
      `CONFLICT` import if unused.
- [ ] `MetakompasAndBuggiTaskSelections.writeBuggiSelection(PromatTask, TagList)` (only used by
      `setApproveBuggiTask`).
- [ ] Connector module: `PromatServiceConnector.approveBuggiTask`. Then `Tag`/`TagList` in the model
      module, if nothing else uses them.
- [ ] Tests: `CaseInformationUpdaterMetakompasTest`, the Metakompas tests in
      `CaseInformationUpdaterSideEffectsIT` (`testWaitForMetakompasData`,
      `testMetakompasSelectionIsLeftAloneWhenRegisteredInPromat`) and `getTasksWhereMetakompasIsPresent`,
      the record-service mock in `CaseInformationUpdaterTestBase` and the mode set there, the mode tests
      in `MetakompasAndBuggiTaskSelectionsTest` (the "already registered" test stays),
      `CaseTaskSelectionIT.testSelectionsRejectedWhileRegistrationHappensInMetakompasset` and
      `testLegacyBuggiEndpointSharesSelectionAcrossTargetFausts`, `CasesIT.testBuggiApproval` and
      `testCaseApprovalWithMoreThanOneBuggiTask`. Bring back PUT ITs that save selections
      (the container no longer runs `METAKOMPASSET`).
- [ ] Config: `METAKOMPAS_REGISTRATION` in `scripts/common` and `scripts/start-server`.
- [ ] `service/docs/openapi.yml`: drop the mode part of the 409 descriptions on the PUT endpoints.
- [ ] If not done already: apply the fbi-api cleanup (remove `subjects.dbcVerified` from the
      GraphQL queries, `FbiApiHandler.metakompassubject`, `BibliographicInformation.metakompassubject`
      and the WireMock fixtures) - nothing has read it since ADR 0006.
- [ ] Update `CLAUDE.md`/READMEs if they describe the polling.

Keep `rawrepo-record-service-connector`: `RecordsProvider` uses it too.

promat-frontend:

- [ ] Remove the manual mode switch and the metakompas.dk link (`TaskTable.js`,
      `enums.js` messages "…via https://metakompas.dk").
- [ ] `TaskTable.js`: base the "Er registreret" status on `task.approved` instead of
      `task.data === "true"`, and drop `renderTags`' legacy `TagList`/line parsing once no old tasks
      are shown.

gitops:

- [ ] Remove `METAKOMPAS_REGISTRATION` from every environment.

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
