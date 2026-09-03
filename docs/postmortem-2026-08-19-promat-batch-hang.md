# Postmortem: promat-service batch jobs hang silently on an EclipseLink cache lock

**Date of analysis:** 2026-08-19
**Service:** promat-service (`promat-service-0`, namespace `metascrum-prod`)
**Severity:** High. Silent, total loss of scheduled case maintenance for ~19 to ~23 hours per occurrence.
**Status:** Root cause identified. Item 1 mitigation applied to the working tree, not yet merged.
**Evidence:** `payara.thread_dump.20260819.001.txt`, `promat-service-0.log`

---

## Summary

The `ScheduledCaseInformationUpdater` batch job deadlocks on an orphaned EclipseLink level-2 shared-cache lock and never recovers. The wait it blocks on has **no timeout**, so the thread stays parked until the pod is restarted. Because the block happens inside a `@Singleton` EJB, all subsequent timer firings are refused entry by the container, and the job's own "something is wrong" alarm never fires. The service keeps serving REST traffic normally throughout, so the failure is completely invisible from the outside.

This has happened on at least two consecutive days (17 and 18 August), and it recurs after every pod restart.

---

## Impact

While hung, none of the following run:

- `updateCaseInformation()` (every 10 min, 06-18, Mon-Fri). Case data is not refreshed from OpenFormat. Weekcodes, catalog codes and bibliographic data go stale.
- Automatic status transitions stop. Cases do not move into `PENDING_EXPORT`, and cases whose weekcode moved to a later week are not moved back to `APPROVED` (`CaseInformationUpdater.java:119-127`). This has direct downstream consequences for dataIO export.
- `updateCaseAssignedEditor()` (nightly 01:15) shares the same lock and is also dead.

REST API traffic is unaffected, which is precisely why nobody noticed.

Measured outage windows:

| Run | Hung at | Restarted | Batch dead for |
|---|---|---|---|
| Aug 17 06:50 start | 2026-08-17 12:10 | 2026-08-18 07:35 | ~19h 25m |
| Aug 18 07:40 start | unknown, early | still hung at dump | 22h 46m and counting |

---

## Timeline

| Time (CEST) | Event |
|---|---|
| 2026-08-17 06:50:15 | Pod starts, joins a 2-member Hazelcast cluster (`Members {size:2, ver:260}`) |
| 2026-08-17 07:00 | First case-update pass. 416 active cases, ~8s per pass |
| 2026-08-17 07:00-12:10 | 32 passes complete normally. 13,336 case updates logged, zero errors from the updater |
| 2026-08-17 12:10:06 | **Last pass ever.** All 422 cases processed, so the loop ran to completion. The hang follows, in the flush or the commit. Log volume collapses from ~20,000/h to ~1,600/h |
| 2026-08-17 12:10 - 2026-08-18 07:35 | Batch dead. REST traffic continues normally at ~1,000/h |
| 2026-08-18 07:35:47 | Pod killed abruptly mid-request. No SIGTERM, no graceful shutdown sequence in the log |
| 2026-08-18 07:40:00 | New JVM starts |
| 2026-08-18 07:50:07 | `__ejb-thread-pool12` created, later found hung |
| 2026-08-19 06:25:44 | Thread dump taken. Thread still hung |

---

## Root cause

Three conditions combine. All three are required, and all three are present.

### 1. An EclipseLink wait that can never expire

`__ejb-thread-pool12` is parked in `WriteLockManager.acquireLocksForClone`, reached from `ScheduledCaseInformationUpdater.getCasesForUpdate()` at line 78:

```
java.lang.Thread.State: WAITING (on object monitor)
	at java.lang.Object.wait(java.base@21.0.2/Object.java:366)
	at org.eclipse.persistence.internal.helper.WriteLockManager.acquireLocksForClone(WriteLockManager.java:184)
	- locked <0x0000000087247fb8> (a HardCacheWeakIdentityMap$ReferenceCacheKey)
	...
	at dk.dbc.promat.service.batch.ScheduledCaseInformationUpdater.getCasesForUpdate(ScheduledCaseInformationUpdater.java:78)
```

The EclipseLink code at that point is:

```java
synchronized (toWaitOn) {
    if (toWaitOn.isAcquired()) {
        toWaitOn.wait(ConcurrencyUtil.SINGLETON.getAcquireWaitTime());
    }
}
```

`ConcurrencyUtil.DEFAULT_ACQUIRE_WAIT_TIME` is `0L`, and `Object.wait(0)` blocks forever. The thread dump proves the argument really was zero: the state is `WAITING`, not `TIMED_WAITING`, which is exactly how HotSpot distinguishes `wait(0)` from `wait(n>0)`.

EclipseLink's own comment on this loop is candid about the history: *"we threads frozen here forever inside of the wait that used to have no timeout"*.

### 2. The lock is held by nobody

`__ejb-thread-pool12` is the **only thread in the entire 272-thread JVM with any `org.eclipse.persistence` frame**. Every other EJB worker and all 53 HTTP workers are parked on empty queues. Nothing is RUNNABLE in application code and nothing is blocked in a socket read.

So `isAcquired()` returns true while no live thread owns the CacheKey. Nobody will ever call `notifyAll()` on it. The lock is orphaned.

This is invisible by design: EclipseLink `CacheKey` locking uses an internal `activeThread` plus depth counter inside `ConcurrencyManager`, **not** a Java monitor. The JVM does not release it when the owning thread dies or is recycled, and a thread dump cannot show who holds it.

### 3. Configuration that makes orphaned shared locks possible

From `service/src/main/resources/META-INF/persistence.xml`:

```xml
<shared-cache-mode>DISABLE_SELECTIVE</shared-cache-mode>
<property name="eclipselink.cache.coordination.protocol"
          value="fish.payara.persistence.eclipselink.cache.coordination.HazelcastPublishingTransportManager"/>
<property name="eclipselink.cache.coordination.channel" value="promatPUChannel"/>
```

- `DISABLE_SELECTIVE` combined with **zero `@Cacheable` annotations anywhere in the codebase** means every entity lives in the shared L2 cache and every entity has a shared, lockable CacheKey.
- Cache coordination is active across a real 2-member cluster. Remote merges execute on pooled Hazelcast operation threads (the dump shows 64 partition-operation and 32 generic-operation threads). Any merge that does not reach its release path leaks a CacheKey lock permanently and silently.

### Code patterns that maximise exposure

- `PromatCase.GET_CASES_FOR_UPDATE_QUERY` selects **all** non-terminal cases with no limit, and clones every one of them into a UnitOfWork every 10 minutes.
- `CaseInformationUpdater.updateCaseInformation()` is `@TransactionAttribute(REQUIRES_NEW)` but receives a `PromatCase` that is still managed by the **outer** transaction's persistence context (`ScheduledCaseInformationUpdater.java:57-60`). A nested independent transaction writing an entity the outer UnitOfWork still tracks is a well-known route to conflicting cache-key locking.
- `openFormatHandler.format()` and `ContentLookUp` perform **external HTTP calls inside** that `REQUIRES_NEW` transaction, so a slow upstream holds cache locks, and any resulting JTA timeout is rolled back off-thread.
- `PromatCase` has three bare `@OneToOne` mappings (`reviewer`, `editor`, `creator`), and JPA defaults `@OneToOne` to `EAGER`. Locks cascade across non-lazy mappings, so **every case clone also locked one `Reviewer` and two `Editor` rows**. See the scoping section below, because this materially widens the lock surface and probably changes which entity actually held the orphaned lock.

### The two runs hung in different places

This is worth stating explicitly, because it constrains the diagnosis more than any single stack trace does.

| Run | Where it hung | Evidence |
|---|---|---|
| Aug 17 | **After** the per-case loop completed, in `entityManager.flush()` (`ScheduledCaseInformationUpdater.java:63`) or the container's transaction commit | The 12:10 pass logged all 422 cases, ending on case 525146. The 12:00 pass logged 421, ending on 525145, so 525146 was a newly created case and the last in the run. No further batch output follows |
| Aug 18 to 19 | At the **very first** `getCasesForUpdate()` query, before any case was processed | Thread dump stack, `ScheduledCaseInformationUpdater.java:78` |

Same mechanism, two unrelated code locations, one at the start of a pass and one at the end. The failure is therefore **not tied to any particular statement**. Whichever operation first touches a poisoned cache key is the one that hangs. Any theory that pins this on a specific query, a specific case, or a specific line is ruled out by this pairing.

It also shifts the prime suspect for where the lock gets **orphaned**. Transaction commit is where EclipseLink merges every changed object into the shared cache and takes **write** locks via `WriteLockManager.acquireRequiredLocks`, as opposed to the short-lived read locks of the clone path. The Aug 17 run hanging precisely at the flush or commit boundary points at the merge path rather than the read path as the place where locks are left behind.

---

## Why this became a daily problem in the last week

The defect itself is old. **No code change explains the change in frequency.** The last commit touching this path is `47116a70` (23 July), and the only recent change, `f3ef631a` (16 August), is a catch-widening in `ScheduledTaxonomyUpdater`, whose package never touches the `EntityManager`. It is unrelated.

What changed is the **rate at which the race is attempted**, not the code. Ranked by confidence:

### 1. Growth in the active case count (highest confidence)

`GET_CASES_FOR_UPDATE` is unbounded and reprocesses **every** active case every 10 minutes, whether or not it changed. Measured in the Aug 17 run:

- 416 to 422 active cases per pass, observably climbing during working hours
- **2,496 case updates per hour**, each in its own `REQUIRES_NEW` transaction
- 3,181 OpenFormat calls and 1,848 content lookups per hour

Every one of those case writes merges into the shared L2 cache and broadcasts a coordination message over Hazelcast to the peer node. The number of orphaning opportunities per day is **directly proportional to the active case count**. Each additional active case adds 6 merges per hour and roughly 72 per working day. A backlog that grows by a few dozen cases moves the expected time-to-hang from days into hours, which is exactly the "suddenly daily" signature.

### 2. Unconditional writes amplify the volume by roughly 56%

`CaseInformationUpdater.java:130` writes catalog codes whenever the upstream returns a non-empty list, with **no comparison against the current value**. The log shows 1,408 such writes per hour out of 2,496 updates. Most of these entities are almost certainly unchanged. Every one still produces a merge and a cluster-wide coordination broadcast.

### 3. Restart frequency

Each restart re-arms the batch and buys a fresh opportunity to hang. The Hazelcast cluster version is `ver:260`, which indicates heavy historical membership churn. The Aug 18 pod died abruptly with no graceful shutdown, which is the signature of a SIGKILL such as a container-level OOMKill or a hard pod deletion rather than a JVM fault. More restarts means more hangs.

### 4. Peer-node write traffic

Writes on `promat-service-1` are broadcast to `promat-service-0` and merged there on Hazelcast threads, concurrently with pod-0's batch pass. Increased REST write traffic raises collision probability independently of case growth.

### To confirm, check outside this repo

- Active case count trend over the last 30 days (the single most valuable number)
- Pod restart history and whether any pod was OOMKilled
- Whether the replica count changed recently. Going from 1 to 2 replicas would flip cache coordination from inert to active and would explain a sudden onset on its own
- OpenFormat and `dmat.dbc.dk` latency trend

---

## Detection gap

This is the most important lesson. Three separate safety nets all failed to fire.

1. **The job's own alarm is dead code in this scenario.** `ScheduledCaseInformationUpdater` guards itself with a static `ReentrantLock` and logs `"Aborting update since update is already running"` on contention (line 49-52). That message appears **zero** times in 25 hours of logs covering a 19-hour outage. The bean is a `@Singleton` with no `@ConcurrencyManagement` or `@Lock` annotation, so it defaults to container-managed concurrency with `@Lock(WRITE)`. The container refuses or skips every later timer firing **before** the method body is entered, so `tryLock()` is never reached.

2. **The static lock is never released.** `tryLock()` at line 49 succeeded, and the `unlock()` in the `finally` at line 68 is unreachable because the thread never leaves line 55. Both scheduled methods on the bean are therefore dead for the lifetime of the JVM.

3. **No health signal covers batch liveness.** Readiness and liveness probes only exercise the HTTP layer, which stayed perfectly healthy. The only external symptom was a 20x drop in log volume, which nothing was watching.

This failure is also **not** the same class as `f3ef631a`. That commit caught escaping exceptions. This code path never throws.

---

## Immediate mitigation

Restart the pod. Nothing in-JVM can clear an orphaned EclipseLink CacheKey lock.

---

## What the item 1 mitigation actually does

All of the following was verified against the EclipseLink 4.0.1 source rather than assumed. It matters because the intuitive reading, "adding a timeout makes it stop hanging", is only half right.

### The safety net was always present, and the default made it unreachable

`acquireLocksForClone` calls `ConcurrencyUtil.determineIfReleaseDeferredLockAppearsToBeDeadLocked(...)` on **every iteration** of its retry loop. That detector fires once the thread has been in the loop longer than `maxAllowedSleepTime` (default 40,000 ms) and then throws, because `allowConcurrencyExceptionToBeFiredUp` defaults to `true` and the call site passes `ALLOW_INTERRUPTED_EXCEPTION_TO_BE_FIRED_UP_TRUE`.

With `waittime` left at its default of `0`, the thread parks in `Object.wait(0)` and **never returns to the top of the loop**, so the detector is never reached again. EclipseLink ships a deadlock detector that its own default setting makes impossible to run. Raising the wait time above zero is what puts the detector back in play, which is exactly why the EclipseLink documentation ties deadlock detection to this same property.

### Placement of the property

The field initializer reads `System.getProperty(...)`, so `ConcurrencyUtil` alone suggests this must be a JVM flag. It does not have to be. `EntityManagerSetupImpl.updateConcurrencyManagerWaitTime()` reads `PersistenceUnitProperties.CONCURRENCY_MANAGER_ACQUIRE_WAIT_TIME` (identical string, `eclipselink.concurrency.manager.waittime`) from the persistence-unit map and calls `ConcurrencyUtil.SINGLETON.setAcquireWaitTime(...)`. The `persistence.xml` placement therefore takes effect. Note that the target is a JVM-wide singleton, so the setting applies to the whole persistence layer and not only to the batch job.

### Timing

The detector is only consulted once per loop iteration, and iterations are paced by the configured wait:

| Elapsed | Loop iteration | Result |
|---|---|---|
| 0s | check 1, elapsed 0 | wait 10s |
| 10s | check 2 | wait 10s |
| 20s | check 3 | wait 10s |
| 30s | check 4 | wait 10s |
| ~40s | check 5, elapsed > 40,000 ms | **throws** |

**Detection and abort takes 40 to 50 seconds**, depending on where timing lands relative to the strict `elapsedTime > maxAllowedSleepTimeMs` comparison.

`MAXTRIES` is 10,000. At 10 seconds per try that bound alone would be roughly 27.8 hours, which would have been no real improvement over the hang. It is never reached, because the 40 second detector fires first.

### Sequence at the moment it fires

1. The detector throws `InterruptedException`. It is thrown outside the inner `try` that ignores wait interrupts, so it reaches the outer handler
2. `acquireLocksForClone` converts it to `ConcurrencyException.maxTriesLockOnCloneExceded`, and its `finally` releases the read locks that thread held
3. It propagates through `getResultList()` into the existing `catch(Exception)` at `ScheduledCaseInformationUpdater.java:64` and is logged
4. The `finally` at line 68 finally runs, so the static `updateLock` is **released** instead of being held for the lifetime of the JVM
5. The next scheduled firing follows within 10 minutes

### Detection is guaranteed. Resolution is not.

This is the important caveat, and the reason item 1 is not sufficient on its own.

**Transient contention**, meaning a genuine concurrent merge from the peer node, is now resolved by the retry inside those 40 seconds. The pass completes normally and nothing is logged. This is probably the common case and it now self-heals silently.

**A genuinely orphaned lock**, which is what the thread dump captured, is *not* cleared by this change. The dead CacheKey remains in the shared cache. The next pass 10 minutes later hits the same key, spends another 40 to 50 seconds, and fails again. Because the failure occurs in `getCasesForUpdate()`, which clones every active case in a single query, the whole pass fails and **zero cases are updated**. The result is a repeating 10-minute error loop that persists until the pod is restarted.

| | Before | With item 1 |
|---|---|---|
| Thread | hung forever | aborts in 40-50s |
| Static `updateLock` | held forever | released |
| Log output | zero | ERROR plus EclipseLink diagnostic dump |
| Batch recovery | requires restart | self-heals if contention was transient, still requires restart if the lock is truly orphaned |
| Time to notice | 22h 46m and counting | under a minute, **if something is watching** |

The last row is both the win and the catch. The failure becomes loud, but only to a reader. This is why item 2 is a necessary companion rather than a nice-to-have: item 1 alone produces a service that announces the problem clearly and then keeps failing quietly to an empty room. If actual self-recovery is wanted rather than visibility, item 5 is the change that removes the orphanable cache key altogether.

### Correction: the runtime upgrade (item 3) does not self-heal either

An earlier revision of this document claimed that on EclipseLink 4.0.7 "this deployment would have self-healed". **That was wrong**, and it is worth recording why, because the same mistake is easy to repeat.

4.0.7's `acquireLocksForClone` is structurally identical to 4.0.1's. It calls the same deadlock detector on every iteration, waits, retries `acquireLockAndRelatedLocks`, and throws `ConcurrencyException` on `MAXTRIES` or on the detector's `InterruptedException`. Neither version contains any mechanism to force-release a lock whose owner is gone. A search for force-release, deadlock-breaking or recovery routines across both versions returns nothing.

So an upgrade bounds the wait and nothing more. The orphaned CacheKey survives, the next pass hits it again, and the batch still needs a restart. It **self-detects, not self-heals**, exactly as item 1 does.

Two further details make the upgrade a weaker version of item 1 on this specific path rather than a stronger one:

- 4.0.7 does not call `getAcquireWaitTime()` in `WriteLockManager` at all. The await is hard-coded to `MAX_WAIT`, which is 600,000 ms. The item 1 property has no effect on this code path after upgrading.
- Because the loop therefore only iterates every 10 minutes, the detector's 40 second threshold is not evaluated until the first wake-up. Detection takes about **10 minutes on 4.0.7 with default settings**, against about **40 to 50 seconds on 4.0.1 with item 1 applied**.

The detector defaults are identical in both versions (`maxAllowedSleepTime` 40,000 ms, both exception flags `true`), so the difference is entirely down to how often the loop gets to run.

The upgrade is still worth doing for the usual reasons, and it removes the need to carry a non-default EclipseLink property. It has been moved to P2 because it neither fixes this failure nor detects it faster. **Only item 5 removes the failure mode.**

---

## Scoping the cache fix: which entities must leave the shared cache

Item 5 is the only remediation that removes the failure mode rather than bounding or announcing it. The obvious minimal form, marking just `PromatCase` as `@Cacheable(false)`, is **not sufficient on its own**. The reason is worth recording.

### The mechanism does work

`ObjectBuilder.buildObjectInUnitOfWork` branches on `cachePolicy.shouldIsolateObjectsInUnitOfWork()`, and EclipseLink's javadoc states the choice directly: `buildWorkingCopyCloneFromRow` (bypassing shared cache) versus `buildWorkingCopyCloneNormally` (placing the result in the shared cache). The captured stack went through `buildWorkingCopyCloneNormally`. An isolated descriptor takes the other branch and **never reaches `acquireLocksForClone` at all**.

Under `DISABLE_SELECTIVE`, `@Cacheable(false)` is what makes a descriptor isolated.

### But locks cascade across the eager graph

`acquireLockAndRelatedLocks` calls `traverseRelatedLocks`, which recurses into related objects. It only walks non-indirect mappings ("If all mappings have indirection short-circuit"), so lazy relationships are exempt and eager ones are not. For `PromatCase`:

| Field | Mapping | Fetch | Locked during clone |
|---|---|---|---|
| `reviewer` | bare `@OneToOne` | **EAGER** | yes |
| `editor` | bare `@OneToOne` | **EAGER** | yes |
| `creator` | bare `@OneToOne` | **EAGER** | yes |
| `subjects` | `@OneToMany` | lazy | no |
| `tasks` | `@OneToMany` | lazy | no |

None of the three `@OneToOne` fields specifies `fetch`, and JPA defaults `@OneToOne` to `EAGER`. So each of the ~420 case clones per pass was also acquiring locks on one `Reviewer` and two `Editor` rows.

### This probably changes which entity held the orphaned lock

The dump records the contended key only as a `HardCacheWeakIdentityMap$ReferenceCacheKey` and does not name the entity. Given that a single `Reviewer` row is referenced by many cases and that `Reviewers reviewers (GET)` dominated the surviving REST traffic, a shared user row is a far more contended key than any individual case. **The orphaned lock was plausibly on a `Reviewer` or `Editor`, not on a `PromatCase`.**

If that is right, isolating only `PromatCase` moves the hang instead of removing it. The batch query would stop cloning cases through the shared cache, but the eager `reviewer`, `editor` and `creator` would still resolve through their own cacheable descriptors and back into `acquireLocksForClone`.

### Minimum effective scope

`@Cacheable(false)` on `PromatCase`, `Reviewer`, `Editor` and `PromatUser` at minimum. At that point `shared-cache-mode NONE` is simpler, leaves nothing to reason about, and cannot be silently undone by someone adding a new eager relationship later.

### Functional impact of turning the shared cache off

| Area | Effect |
|---|---|
| Application semantics | **No change.** The L1 persistence context still guarantees identity and dirty tracking inside a transaction, and no application code reads the L2 cache |
| Optimistic locking | **No interaction.** There is no `@Version` field anywhere in the model |
| Database load | **Higher.** Every read goes to PostgreSQL. The batch reads ~420 cases every 10 minutes, each also fetching three user rows, plus REST traffic. This is the number to watch |
| Hazelcast traffic | **Lower.** Non-cached entities generate no cache-coordination broadcasts, which is precisely the traffic driving the orphaning |
| Correctness | **Better, not worse.** Two nodes currently share a best-effort coordinated cache with no optimistic locking, so a dropped or delayed invalidation lets a node serve a stale case with nothing able to detect the conflict. Removing the L2 cache removes that entire class of bug |

The last row is the one that should settle the decision. The L2 cache is buying little here, because the batch re-reads everything every 10 minutes regardless, and it is costing both this hang and an undetectable staleness risk.

---

## Action items

| # | Action | Rationale | Priority |
|---|---|---|---|
| 1 | Set `eclipselink.concurrency.manager.waittime` to a non-zero value, for example `10000` | **Applied to the working tree.** Converts a permanent silent hang into a 40-50s abort with a logged error, and frees the static `updateLock`. Guarantees detection, not resolution. See the section above | P0 |
| 2 | Add batch liveness monitoring: export a "seconds since last successful pass" metric and alert when it exceeds ~30 min during scheduled hours | The outage was invisible for 19 hours. This is the gap that let it persist. **Promoted in practice by item 1**, which makes the failure loud but still only to a reader | P0 |
| 3 | Upgrade Payara so EclipseLink is 4.0.7 or newer | Removes the need for the item 1 setting, because 4.0.7 hard-codes a 10 minute bound and ignores the property on this path. It **detects** rather than heals, and detects more slowly than item 1 does. See the correction below | P2 |
| 4 | Only write when values actually changed, starting with catalog codes at `CaseInformationUpdater.java:130` | Removes an estimated 56% of merges and cluster coordination broadcasts, cutting the exposure rate proportionally. **Written and then reverted**, to keep the incident PR to a single config line. It reduces frequency only and plays no part in recovery, so it is genuinely independent of item 1 | P1 |
| 5 | **Set `shared-cache-mode` to `NONE`**, or `@Cacheable(false)` across `PromatCase`, `Reviewer`, `Editor` and `PromatUser` at minimum | **The only item that removes the failure mode** rather than bounding or announcing it. Also removes a silent stale-read risk, since two nodes share a best-effort coordinated cache with no `@Version` anywhere in the model. Marking only `PromatCase` is not sufficient. See the scoping section | P0 |
| 6 | Move `openFormatHandler.format()` and `ContentLookUp` calls outside the transaction, and paginate `GET_CASES_FOR_UPDATE` | Stops external HTTP latency from being held across cache locks, and bounds the working set per transaction | P2 |
| 7 | Replace the static `ReentrantLock` guard with an explicit `@AccessTimeout` plus a holder timestamp, so a stuck run is reported rather than silently skipped | The current guard cannot fire in the scenario it was written for | P2 |
| 8 | Investigate why pods are being killed without graceful shutdown, and the separate hourly taxonomy build failures (see below) | Restart frequency drives hang frequency | P2 |

---

## Separate issue found (not the cause of this incident)

The taxonomy build fails every hour, steadily, 4 times per refresh:

- 4,100 x `SubjectBuilder Unable to (fully) create: N. Error: No enum constant dk.dbc.promat.service.taxonomy.dto.PathTrans...`
- 300 x `Taxonomy ID N is already in use`
- 100 x `Missing ID ('q' field)`
- 100 x `DM2Builder Error building taxonomy`

The `No enum constant` failures indicate **upstream taxonomy data now contains a value the code does not know about**. This is worth investigating on its own, and its timing may be worth correlating with "the last week". It is confirmed **not** related to this hang: the `taxonomy` package contains no `EntityManager` usage and takes no cache locks. `f3ef631a` made these failures survivable but did not address the underlying data mismatch.

---

## Hypotheses ruled out

Recorded so they do not get re-investigated.

### The Jersey and Grizzly I/O exceptions are not the cause

`promat-service-0.log` contains a `MappableException: java.io.IOException` and a Grizzly `GRIZZLY0013` NPE. Both are unrelated to the lock leak.

| Entry | Time | Verdict |
|---|---|---|
| `GRIZZLY0013` NPE in `ServerInputBuffer.getThreadPool` | 2026-08-17 06:54:08 | 5 hours **before** the hang. Raw socket read path, before the request reaches Jersey |
| `MappableException`, `Write timeout exceeded when trying to flush the data` | 2026-08-17 15:25:05 | 3 hours **after** the hang. `ServerRuntime$Responder.writeResponse`, a slow HTTP client |

Neither stack contains a single application, EJB, transaction or EclipseLink frame. More fundamentally, the mechanism does not work: EclipseLink acquires and releases cache locks inside the clone and merge operations, both of which complete within the transaction boundary, and Jersey serializes the response only after that boundary has closed. A write failure there has no lock-holding critical section to interrupt.

### The taxonomy failures are not the cause

Covered in the separate-issue section above. The `taxonomy` package contains no `EntityManager` usage and takes no cache locks.

---

## What we could not determine

The precise event that orphaned the CacheKey is not recoverable from the available evidence. The log from the hung JVM was not captured. `promat-service-0.log` ends at 07:35:47 on Aug 18, and the hung JVM started at 07:40:00 on Aug 18, so that log covers the **previous** run only.

The **orphaning event** remains unidentified, but its location is now narrowed. The Aug 17 run hung at the flush or commit boundary, which makes the merge path (`WriteLockManager.acquireRequiredLocks`, holding write locks) a better suspect than the clone path (short-lived read locks released in `finally`). A remote cache-coordination merge arriving on a recycled Hazelcast operation thread remains the leading hypothesis, and it is still unproven.

The **entity** behind the orphaned key is likewise unconfirmed. The dump names its type only as `HardCacheWeakIdentityMap$ReferenceCacheKey`. The scoping section argues it was more likely a `Reviewer` or `Editor` than a `PromatCase`, but that is inference from contention patterns, not direct evidence.

To capture this next time, before restarting a hung pod collect: the full container log, a thread dump, and ideally a heap dump. The heap would let us read the orphaned `CacheKey.activeThread` field directly and identify which thread and which entity leaked the lock, settling both questions definitively.
