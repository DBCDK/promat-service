# 7. Make the batch jobs avoid, detect and recover from shared-cache lock hangs

Date: 2026-10-01

## Status

Accepted

## Context

The scheduled batch jobs have hung on an orphaned EclipseLink shared-cache lock at least three times:
on 2026-08-17 and 2026-08-19 (see
[the postmortem](../../../../docs/postmortem-2026-08-19-promat-batch-hang.md)), and on 2026-10-01.

On 2026-10-01 the nightly editor reset (`updateCaseAssignedEditor`, 01:15) got stuck in
`WriteLockManager.acquireLocksForClone` while loading cases. No live thread held the lock. Requests
to `listCases` and `getAllReviewers` on the same pod got stuck on the same lock. The 10-minute case
update stopped too, and no case was updated from 06:00 until the pod was restarted at 08:40.
Nothing was logged at any point.

The postmortem found that the lock is held by a thread that no longer exists, that nothing in the JVM
can release it, and that only a restart clears it. It could not identify what leaves the lock behind.

Its main fix is to turn off the shared cache (`shared-cache-mode NONE`). That was discussed with Henrik
after the postmortem and turned down because of performance concerns, so the shared cache stays.
This ADR is about what we can do while keeping it.

Three problems in our own code made the hangs worse:

1. **One large commit per pass.** `CaseInformationUpdater.updateCaseInformation(PromatCase)` and
   `clearEditor(PromatCase)` were `REQUIRES_NEW`, but `CaseInformationUpdater` had no `EntityManager`.
   The methods changed entities that belonged to the outer batch transaction, so their own transactions
   committed nothing. All changes from a pass (about 630 cases) were merged into the shared cache in one
   commit at the end, and one failing case rolled back the whole pass. `UserUpdater.deactivateEditor`
   and `deactivateReviewer` had the same pattern. The postmortem names this large merge as the most
   likely place for locks to be left behind.
2. **A stuck run blocked everything silently.** The scheduler beans were `@Singleton` with the default
   container write lock. While one timer call was stuck, every later call waited on the container lock
   before entering the method. The beans' own guard (`updateLock.tryLock()`, which logs "Aborting update
   since update is already running") never ran.
3. **Needless cache writes at the same time.** `ScheduledUserUpdater` re-deactivated the same ~100
   users every night, setting a new `deactivated` date each time. It ran at 01:15, the same second as
   the editor reset, which loads cases together with their editors and reviewers.

## Decision

- **One transaction per case and per user.** The batch jobs query IDs only (`select c.id ...`) and run
  without an outer transaction (`NOT_SUPPORTED`). For each ID, `CaseInformationUpdater` and `UserUpdater`
  load the entity with their own `EntityManager` in a `REQUIRES_NEW` transaction, update it and commit.
  A failing case is logged and rolled back on its own, and the pass continues.
  `updateCaseInformation(PromatCase)` still exists for the `POST cases/{id}/update` endpoint, and now
  joins the caller's transaction, which is what effectively happened before.
- **Bean-managed concurrency** (`@ConcurrencyManagement(BEAN)`) on `ScheduledCaseInformationUpdater` and
  `ScheduledUserUpdater`. The existing `updateLock` is the only guard, so a stuck run makes later runs
  log "Aborting update" instead of waiting silently.
- **Skip users that are already deactivated** (`deactivated IS NULL` in the query), and do the filtering
  in the database instead of loading every user.
- **Run the editor reset at 01:45** instead of 01:15, apart from `ScheduledUserUpdater`.
- **Restart automatically when a job is stuck.** The jobs report to `BatchJobMonitor` when they start,
  after each case or user, and when they finish. `BatchJobLivenessCheck` (MicroProfile `@Liveness`)
  reports DOWN on `/health/live` when a running job has made no progress for `BATCH_JOB_MAX_IDLE_MINUTES`
  (default 15). A liveness probe in gitops (`namespace/metascrum-prod/promat-service/promat-service.yml`)
  then restarts the pod.
- **Log who holds the lock before the restart.** The first time a job is reported as stuck, the check logs
  the stuck thread's stack trace and every locked entry in the shared cache, found by
  `SharedCacheInspector`: the entity and its ID, the owning thread's name, whether that thread is still alive, and its
  stack if it is. A thread dump cannot show this, and a restart destroys it. The thread name tells
  us which kind of thread left the lock behind (Hazelcast cache sync, a batch job, or a REST request),
  which is what the postmortem could not find out. The inspector uses EclipseLink internals, so it only
  reads, and if it fails, the error is logged and the liveness check still works.

## Consequences

- Each case is committed as soon as it is processed, in a small transaction. A pass no longer ends with
  one large merge into the shared cache, and a failing case no longer undoes the whole pass.
- The batch jobs' main queries return IDs, so they no longer build entities through the shared cache.
  Each case is still loaded through it, one at a time.
- If a job hangs anyway, the pod is restarted about 16-17 minutes after the last progress (15 minutes
  idle, plus up to 3 liveness failures 30 seconds apart), with no human involved. Until then, the
  readiness probe (`/health`, which includes liveness checks) takes the pod out of the load balancer,
  so user requests go to the other pod.
- The liveness check only looks at the jobs' own progress, never at the database or other services.
  A slow pass that keeps making progress is not restarted, and an outage elsewhere does not cause
  restarts.
- The code change must not be deployed long before the gitops change: without the liveness probe,
  a stuck pod would leave the load balancer but never be restarted.
- An automatic restart throws away the stuck JVM, so there is no chance to take a heap dump. The
  logged stack trace and the cache-lock report are the evidence that remains, and should be read
  after the first automatic restart to find the actual cause.
- `ScheduledUserUpdater` writes only users that actually need deactivating, normally none.

This reduces the most likely cause and makes a hang recover by itself. It does not remove the failure
mode: only turning off the shared cache does that. If the hangs continue, that discussion should be
reopened with measurements from staging (the large `listCases` calls and one batch pass, with and
without the cache, and with `@BatchFetch` on `PromatCase.reviewer`, `editor` and `creator`).
