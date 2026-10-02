# 7. Detect and recover from shared-cache lock hangs

Date: 2026-10-02

## Status

Accepted

## Context

Threads in promat-service get stuck forever in `WriteLockManager.acquireLocksForClone`, waiting on an
entry in EclipseLink's shared cache that no live thread holds. Only a restart clears it.

- **2026-08-17 and 2026-08-19:** the 10-minute case update hung (see
  [the postmortem](../../../../docs/postmortem-2026-08-19-promat-batch-hang.md)).
- **2026-10-01:** the nightly editor reset hung at 01:15. The 10-minute case update and some
  `listCases` and `getAllReviewers` requests got stuck behind it until a manual restart at 08:40.
- **2026-10-02:** five `addTask` requests for a newly created case ran within 4 ms on both pods. One
  of them took `LOCK TABLE promatcase` and `LOCK TABLE promattask` (`EXCLUSIVE`) and then got stuck on a
  cache lock while holding them, so every write to cases and tasks on both pods waited for about
  15 minutes. Both pods also had stuck read requests. After a restart, the first burst of parallel
  `listCases` requests on the empty cache got stuck again within seconds, with no other thread using
  EclipseLink at all. Both pods got stuck again within minutes after the next restarts too.

So concurrent reads are enough to leave a lock behind. It is not limited to the batch jobs, commits or
Hazelcast, and it hits user requests on both pods. Nothing was logged in any of the incidents, and
each one was found by users or by accident.

Turning off the shared cache (`shared-cache-mode NONE`) removes the cause. It was turned down after the
postmortem because of performance concerns, and is being reconsidered separately. This ADR is about
noticing and recovering from the hang, and collecting the evidence needed to find its cause.

## Decision

- **Log every thread stuck on a cache lock.** `SharedCacheLivenessCheck` (MicroProfile `@Liveness`)
  watches for threads waiting in `WriteLockManager.acquireLocksForClone`. Normal waits there last
  milliseconds. When any thread, a request or a batch job, has waited for `SHARED_CACHE_LOG_AFTER_MINUTES`
  (default 5), it logs the thread's stack trace and every locked entry in the shared cache, found by
  `SharedCacheInspector`: the
  entity and its ID, the owning thread's name, whether that thread is still alive, and its stack if it
  is. A thread dump cannot show this, because EclipseLink tracks lock owners in its own fields, and a
  restart destroys it. The inspector uses EclipseLink internals, so it only reads, and if it fails, the
  error is logged and the liveness check still works.
- **Restart automatically only for stuck batch jobs.** The check reports DOWN on `/health/live` only
  when a batch job (an EJB timer thread) has waited for `SHARED_CACHE_BATCH_MAX_WAIT_MINUTES`
  (default 15). A liveness probe in gitops (`namespace/metascrum-prod/promat-service/promat-service.yml`)
  then restarts the pod. Stuck requests are only logged: on 2026-10-02 they got free by themselves after
  about 15 minutes, several times an hour on both pods, and restarted pods got stuck again within
  minutes, so restarting for them would restart the pods all the time. A stuck batch job waited for
  hours on 2026-10-01 and blocks all later runs, and only a restart clears it.
  A thread only counts if it is seen waiting on every probe for the whole period.
- **Bean-managed concurrency** (`@ConcurrencyManagement(BEAN)`) on `ScheduledCaseInformationUpdater` and
  `ScheduledUserUpdater`. With the default container write lock, a stuck timer call made every later
  call wait silently before entering the method. Now the existing `updateLock` is the only guard, and
  later runs log "Aborting update since update is already running".

## Consequences

- Every thread stuck on a cache lock for 5 minutes is logged, with the evidence needed to find the cause.
- A pod with a batch job stuck on a cache lock is restarted about 16-17 minutes after the wait started
  (15 minutes, plus up to 3 liveness failures 30 seconds apart), with no human involved. Shortly before,
  the readiness probe (`/health`, which includes liveness checks) takes the pod out of the load balancer.
- A stuck request does not restart the pod or take it out of the load balancer. The user waits until it
  gets free by itself, which has taken about 15 minutes.
- The check only looks at the service's own threads, never at the database or other services, so an
  outage elsewhere cannot cause restarts.
- The table locks in `createCase` and `addTask` can still turn a stuck request into an outage of all
  writes until it gets free. Replacing them is a separate change.
- The code change must not be deployed long before the gitops change: without the liveness probe, a
  stuck pod would leave the load balancer but never be restarted.
- The logged stack traces and cache-lock reports are the evidence for finding the cause, and should be
  read the first time they appear.

This does not remove the failure mode. Only turning off the shared cache does that.
