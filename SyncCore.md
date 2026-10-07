# SyncCore — Final Kotlin Multiplatform Technical Showcase Specification

## 1. PROJECT GOAL

Build a **small, technically deep Kotlin Multiplatform synchronization engine showcase**.

This is NOT a consumer application.

The goal is to demonstrate:

* offline-first state management
* deterministic change detection
* synchronization planning
* conflict detection
* conflict resolution
* reproducible synchronization results
* Kotlin Multiplatform architecture
* shared business logic
* platform-independent domain logic
* measurable correctness

The core question is:

> **Can two independently modified states be synchronized safely and deterministically?**

---

# 2. CRITICAL EXECUTION RULES

You are implementing an **EXISTING Kotlin Multiplatform project**.

The current source code, project structure, Gradle configuration, Kotlin Multiplatform configuration, dependency versions, Android/iOS configuration, and build environment are considered valid.

Your task is to extend the existing project.

### NON-NEGOTIABLE

1. **Do NOT recreate the project from scratch.**
2. **Work directly on the existing source code.**
3. This project is **Kotlin Multiplatform**.
4. Shared business logic must live in `commonMain` whenever platform-independent.
5. Do NOT migrate this project to Flutter.
6. Do NOT migrate it to a pure Android-only project.
7. Do NOT change the existing Gradle version.
8. Do NOT change the Gradle Wrapper version.
9. Do NOT change AGP.
10. Do NOT change Kotlin version.
11. Do NOT change Java/JDK version.
12. Do NOT change existing Compose/plugin versions unnecessarily.
13. Do NOT change existing KMP plugin versions unnecessarily.
14. Do NOT change package/application IDs unnecessarily.
15. Do NOT upgrade or downgrade dependencies simply because newer versions exist.
16. Do NOT replace the current architecture without a strong technical reason.
17. Do NOT perform broad refactors unrelated to SyncCore.
18. Do NOT introduce unnecessary Gradle modules.
19. Do NOT introduce unnecessary dependencies.
20. Reuse existing project infrastructure whenever possible.
21. You are explicitly allowed to use mature open-source libraries, GitHub repositories, protocol references, KMP libraries, official Kotlin samples, and existing implementations when they reduce unnecessary work or improve correctness.
22. Before using external code, inspect license, compatibility, maintenance quality, and security implications.
23. Do not blindly copy large sections of another repository.
24. Prefer deterministic, simple, maintainable implementations over over-engineering.
25. Never use fake synchronization results in production/demo output.
26. Never hardcode fake conflicts as if they were real engine results.
27. **Do NOT run tests until ALL implementation phases are complete.**
28. You may inspect, create, or modify tests during implementation, but do not execute them before the final phase.
29. **Commit after every completed phase.**

---

# 3. PRODUCT TYPE

SyncCore is a **technical library/engine showcase**.

It should have:

* a shared synchronization engine
* a deterministic test/demo harness
* a small visualization/debug interface
* clear documentation
* reproducible scenarios

It should NOT become:

* Dropbox
* Google Drive
* Syncthing
* a cloud service
* a complete mobile file manager
* a production cloud backend

The engine itself is the product.

---

# 4. CORE IDEA

Three logical states exist:

```text
Base / Last Synced State
          +
Local State
          +
Remote State
```

The engine determines:

```text
UNCHANGED
LOCAL_ONLY
REMOTE_ONLY
LOCAL_CHANGED
REMOTE_CHANGED
BOTH_CHANGED
CONFLICT
DELETED
```

Then it produces a deterministic synchronization plan.

---

# 5. CORE PIPELINE

```text
Base State
Local State
Remote State
      ↓
Normalize
      ↓
Detect Changes
      ↓
Build Change Set
      ↓
Detect Conflicts
      ↓
Apply Resolution Policy
      ↓
Produce Sync Plan
      ↓
Deterministic Result
```

The engine must never silently overwrite conflicting data.

---

# 6. SAMPLE SCENARIO

Example:

```text
Base:
title = "Meeting"

Local:
title = "Important Meeting"

Remote:
title = "Team Meeting"
```

Result:

```text
CONFLICT

Local:
"Important Meeting"

Remote:
"Team Meeting"
```

The engine can then produce:

```text
KEEP_LOCAL
KEEP_REMOTE
KEEP_BOTH
SKIP
```

Future/optional merge policies may exist, but do not add complex text merging unless clearly useful.

---

# 7. ARCHITECTURE

Preferred logical structure:

```text
commonMain
│
├── domain
│   ├── model
│   ├── change
│   ├── conflict
│   ├── sync
│   └── policy
│
├── engine
│   ├── ChangeDetector
│   ├── ConflictDetector
│   ├── SyncPlanner
│   └── SyncResolver
│
└── serialization
```

Platform-specific source sets should contain only platform-specific concerns.

Example:

```text
androidMain
iosMain
```

should NOT contain business logic that can live in `commonMain`.

---

# 8. DOMAIN MODEL

Create platform-independent models.

Possible concepts:

```text
SyncEntity
EntityId
Version
EntityState
Change
ChangeSet
Conflict
ConflictType
ResolutionPolicy
SyncOperation
SyncPlan
SyncResult
```

Example conceptual model:

```text
Entity
 ├── id
 ├── version
 ├── fields
 └── metadata
```

Keep models serializable where practical.

Do not depend on Android/iOS classes.

---

# 9. PHASE 1 — EXISTING PROJECT INSPECTION

Inspect:

* Gradle Wrapper
* Gradle
* Kotlin
* KMP plugin
* source sets
* commonMain
* androidMain
* iosMain
* Compose Multiplatform if present
* existing dependencies
* architecture
* state management
* persistence
* serialization
* tests
* reusable components

Do not modify foundational versions.

Create/update:

```text
docs/architecture.md
```

Document:

* existing architecture
* KMP source-set strategy
* shared engine architecture
* platform boundary
* synchronization model

### Commit after completion.

---

# 10. PHASE 2 — CORE STATE MODEL

Implement the smallest useful shared state model.

Example:

```text
BaseState
LocalState
RemoteState
```

Represent deterministic entities.

Avoid coupling the model to files initially.

The first implementation should work entirely in memory.

### Commit after completion.

---

# 11. PHASE 3 — CHANGE DETECTION

Implement deterministic three-way change detection.

Input:

```text
Base
Local
Remote
```

Output:

```text
ChangeStatus
```

Examples:

```text
Base A
Local A
Remote A
→ UNCHANGED
```

```text
Base A
Local B
Remote A
→ LOCAL_CHANGED
```

```text
Base A
Local B
Remote C
→ CONFLICT
```

```text
Base A
Local A
Remote B
→ REMOTE_CHANGED
```

This logic must be completely deterministic.

### Commit after completion.

---

# 12. PHASE 4 — CHANGE SET

Represent detected changes explicitly.

Example:

```text
ChangeSet

Added
3

Modified
5

Deleted
1

Conflicts
2
```

Do not hide synchronization logic inside UI code.

### Commit after completion.

---

# 13. PHASE 5 — CONFLICT DETECTION

Implement explicit conflict models.

Possible categories:

```text
FIELD_CONFLICT
ENTITY_CONFLICT
DELETE_MODIFY_CONFLICT
RENAME_CONFLICT
```

Do not create dozens of artificial conflict types.

Start with the smallest useful model.

### Commit after completion.

---

# 14. PHASE 6 — RESOLUTION POLICIES

Implement explicit deterministic policies.

At minimum:

```text
KEEP_LOCAL
KEEP_REMOTE
KEEP_BOTH
SKIP
```

Optionally:

```text
LAST_WRITE_WINS
```

But only if timestamps/version semantics are clearly defined and documented.

Never use "latest timestamp wins" without explicitly defining clock assumptions.

### Commit after completion.

---

# 15. PHASE 7 — SYNC PLAN

Create a deterministic synchronization plan.

Example:

```text
Sync Plan

CREATE_REMOTE
UPDATE_REMOTE
DOWNLOAD_REMOTE
DELETE_REMOTE
CONFLICT
SKIP
```

Each operation should include enough information for a caller to execute it.

The engine should not directly perform filesystem/network operations.

It produces a plan.

### Commit after completion.

---

# 16. PHASE 8 — APPLY ENGINE

Implement a pure/safe plan application abstraction.

Conceptually:

```text
SyncPlan
   ↓
PlanExecutor
   ↓
Result
```

Keep actual persistence/transport abstract.

Do not implement WebDAV, HTTP, cloud services, or databases unless needed for the showcase.

The technical value is the synchronization core.

### Commit after completion.

---

# 17. PHASE 9 — SYNCHRONIZATION SIMULATOR

Create a small deterministic simulator.

Example:

```text
Scenario

Base:
A

Local:
B

Remote:
C

Resolution:
KEEP_LOCAL
```

Output:

```text
Result:
B

Operations:
UPDATE_REMOTE
```

Another:

```text
Base:
A

Local:
B

Remote:
C

Resolution:
KEEP_BOTH
```

Output:

```text
B + C
```

The simulator must use the real SyncCore engine.

Do not create separate fake logic for the demo.

### Commit after completion.

---

# 18. PHASE 10 — CONFLICT VISUALIZER

Create a small developer-focused UI or console/debug interface.

Example:

```text
SyncCore

Base
Title: Meeting

Local
Title: Important Meeting

Remote
Title: Team Meeting

Conflict detected

[Keep Local]
[Keep Remote]
[Keep Both]
[Skip]
```

The UI is only a visualization layer.

The synchronization engine remains independent from UI.

### Commit after completion.

---

# 19. PHASE 11 — DETERMINISTIC REPLAY

Every synchronization scenario should be reproducible.

Given the same:

```text
Base
Local
Remote
Policy
```

the engine must produce the same result.

No randomness.

No network dependency.

No current-time dependency unless explicitly supplied as input.

This enables reproducible evaluation.

### Commit after completion.

---

# 20. PHASE 12 — SERIALIZATION

Provide serialization for:

* states
* changes
* conflicts
* sync plans
* results

Use the project's existing serialization library where sufficient.

Kotlinx Serialization is preferred only if compatible with the existing project and genuinely useful.

Do not add a new serialization dependency if one already exists.

### Commit after completion.

---

# 21. PHASE 13 — PERSISTENT SYNC BASELINE

Add a minimal abstraction for storing:

> Last successfully synchronized state.

Example:

```text
SyncBaselineRepository
```

The engine should receive the baseline as data.

Do not couple the engine to a specific database.

Platform/storage implementation may remain separate.

### Commit after completion.

---

# 22. PHASE 14 — OPERATIONAL SAFETY

Prevent ambiguous destructive operations.

For example:

If:

```text
Local = changed
Remote = changed
```

do not silently choose a side.

Return:

```text
CONFLICT
```

If a delete/modify conflict occurs:

```text
Base exists
Local deleted
Remote modified
```

do not silently delete the remote version.

Require an explicit policy.

### Commit after completion.

---

# 23. PHASE 15 — VERSION / REVISION MODEL

Introduce a simple deterministic revision model.

Possible:

```text
revision
contentHash
modifiedAt
```

Do not assume wall-clock timestamps are always trustworthy.

Where timestamps are used, treat them as hints unless a strong ordering guarantee exists.

Document the semantics.

### Commit after completion.

---

# 24. PHASE 16 — PERFORMANCE

Benchmark the pure synchronization engine with:

```text
100 entities
1,000 entities
10,000 entities
100,000 entities
```

Measure where practical:

* planning latency
* memory
* allocations
* conflict detection cost

Do not prematurely optimize.

Do not add complex indexing unless measurements justify it.

### Commit after completion.

---

# 25. PHASE 17 — BENCHMARK REPORT

Create a reproducible benchmark report.

Example:

```text
10,000 entities

Change detection
12.4 ms

Conflict detection
4.7 ms

Sync planning
8.1 ms

Peak memory
...
```

Only report values generated by actual benchmark runs.

Do not fabricate metrics.

### Commit after completion.

---

# 26. PHASE 18 — TEST SCENARIO CATALOG

Create a set of deterministic scenarios.

Examples:

```text
01 — No changes
02 — Local-only change
03 — Remote-only change
04 — Both changed
05 — Local delete
06 — Remote delete
07 — Delete vs modify
08 — Multiple conflicts
09 — Keep local
10 — Keep remote
11 — Keep both
12 — Skip
```

Tests may be written now.

But they must NOT be executed until the end.

### Commit after completion.

---

# 27. PHASE 19 — KMP PLATFORM VALIDATION

Verify that shared synchronization logic is truly platform-independent.

The same scenario should execute from:

```text
Android
iOS
```

using the shared `commonMain` engine.

Platform code should only provide:

* storage
* UI
* platform-specific execution

where required.

### Commit after completion.

---

# 28. PHASE 20 — DOCUMENTATION

Create/update:

```text
docs/architecture.md
docs/sync-model.md
docs/change-detection.md
docs/conflicts.md
docs/resolution-policies.md
docs/performance.md
docs/kmp.md
docs/decisions/
```

Suggested decisions:

```text
001-common-main-engine.md
002-three-way-change-detection.md
003-explicit-conflict-resolution.md
004-no-implicit-delete.md
005-deterministic-replay.md
006-platform-boundaries.md
007-performance-strategy.md
```

### Commit after completion.

---

# 29. PHASE 21 — README / TECHNICAL SHOWCASE PRESENTATION

README should immediately explain the problem.

Recommended:

```text
# SyncCore

A deterministic Kotlin Multiplatform synchronization engine.

## The Problem

Offline clients eventually diverge.

When local and remote state both change,
a sync engine must detect the conflict
instead of silently overwriting data.

## Core Model

Base
+
Local
+
Remote

↓

Change Detection
↓

Conflict Detection
↓

Resolution Policy
↓

Sync Plan

## Example

Base: Meeting
Local: Important Meeting
Remote: Team Meeting

Result:
CONFLICT

## Features

## Architecture

## Deterministic Replay

## Conflict Policies

## Benchmarks

## KMP

## Limitations
```

Include diagrams where useful.

The README should make the technical depth obvious within the first minute.

### Commit after completion.

---

# 30. PHASE 22 — FINAL STATIC REVIEW

Review all implementation without running tests yet.

Check:

* commonMain purity
* platform boundaries
* deterministic behavior
* conflict correctness
* delete safety
* timestamp semantics
* serialization
* memory usage
* unnecessary abstractions
* unnecessary dependencies
* API design
* documentation
* benchmark code
* demo correctness

Remove temporary/debug code where appropriate.

### Commit after completion.

---

# 31. ABSOLUTE TESTING RULE

## DO NOT RUN TESTS UNTIL ALL IMPLEMENTATION PHASES ARE COMPLETE

During Phases 1–22:

Do NOT execute:

* unit tests
* integration tests
* platform tests
* benchmark tests
* UI tests

You may:

* inspect tests
* create tests
* modify tests
* review tests statically

But:

> **DO NOT EXECUTE TESTS UNTIL ALL IMPLEMENTATION PHASES ARE COMPLETE.**

This rule is intentional to reduce token/compute usage.

---

# 32. FINAL VERIFICATION

Only after Phase 22 is complete:

Run the final verification.

Execute:

* unit tests
* synchronization scenario tests
* conflict tests
* serialization tests
* platform integration tests
* benchmark suite
* static analysis
* Android build
* iOS build where configured

Do not change project versions just to make verification pass.

---

# 33. FINAL EVALUATION

Generate a final deterministic evaluation report.

Example:

```text
SyncCore Evaluation

Scenarios
100

Passed
100

Conflict cases
32

Conflict detection accuracy
100%

Average planning latency
...

10k entity benchmark
...

100k entity benchmark
...
```

Only report actual measurements.

Do not fabricate numbers.

---

# 34. FINAL MANUAL VALIDATION

Verify:

1. No-change scenario
2. Local-only modification
3. Remote-only modification
4. Both modified
5. Local deletion
6. Remote deletion
7. Delete vs modify
8. Keep local
9. Keep remote
10. Keep both
11. Skip
12. Multiple simultaneous conflicts
13. Deterministic replay
14. Serialization round-trip
15. Android execution
16. iOS execution where configured
17. Large dataset benchmark
18. Invalid input handling

Only claim scenarios that were actually verified.

---

# 35. GIT COMMIT RULE

After every completed phase:

```text
Review
 ↓
Remove temporary/debug code
 ↓
Update documentation
 ↓
Review git diff
 ↓
Commit
```

Create one meaningful commit per phase.

Example:

```text
feat(sync): add core state model
feat(sync): add three-way change detection
feat(sync): add conflict model
feat(sync): add resolution policies
feat(sync): add sync planner
feat(sync): add deterministic simulator
```

Do not create empty commits.

Do not combine unrelated phases.

---

# 36. TOKEN / COMPUTE EFFICIENCY

Optimize for token and compute efficiency.

* Do not reread the entire repository repeatedly.
* Inspect only relevant files for each phase.
* Reuse existing infrastructure.
* Avoid unnecessary explanations.
* Avoid unnecessary refactors.
* Avoid unnecessary dependencies.
* Do not run tests during implementation.
* Do not run repeated builds unnecessarily.
* Keep the synchronization core pure and compact.
* Prefer deterministic local simulation instead of expensive external infrastructure.
* Avoid implementing a real cloud service.

Token efficiency must never reduce:

* correctness
* determinism
* maintainability
* security
* KMP quality

---

# 37. CONTINUOUS IMPLEMENTATION REQUIREMENT

After starting implementation:

> **Continue automatically through ALL implementation phases until Phase 22 is complete.**

Do not stop after planning.

Do not stop after the first phase.

Do not ask for confirmation between phases.

Do not wait for the user.

If a genuine blocker occurs:

1. inspect existing code
2. inspect current dependencies
3. inspect Kotlin/KMP platform APIs
4. inspect mature open-source implementations
5. choose the least invasive compatible solution
6. document the limitation

Do not silently change foundational versions.

---

# 38. VERSION PROTECTION

The existing project configuration is valid.

Do NOT change:

```text
Gradle version
Gradle Wrapper version
AGP version
Kotlin version
KMP plugin version
Java/JDK version
Compose/plugin versions
Android configuration
iOS configuration
dependency versions
package/application IDs
```

unless the required feature is genuinely impossible otherwise.

Before considering a foundational change:

1. inspect current dependencies
2. inspect platform APIs
3. inspect compatible alternatives
4. inspect mature open-source implementations
5. select the least invasive solution

---

# 39. DEFINITION OF DONE

SyncCore is complete when:

* existing KMP project preserved
* Kotlin Multiplatform architecture is maintained
* shared engine is in commonMain
* platform-specific code is isolated
* state model works
* three-way change detection works
* change sets work
* conflict detection works
* resolution policies work
* sync plan works
* apply abstraction works
* deterministic simulator works
* deterministic replay works
* serialization works
* sync baseline abstraction works
* delete/modify conflicts are handled safely
* performance benchmark works
* benchmark report works
* scenario catalog exists
* Android integration works
* iOS integration works where configured
* documentation exists
* README exists
* every phase has a Git commit
* final tests pass
* final verification passes
* no fake benchmark values exist
* no fake sync results exist

---

# 40. FINAL REPORT

After all implementation phases and final verification:

## Implemented

* ...

## Architecture

* ...

## KMP

* ...

## Sync Engine

* ...

## Conflict Resolution

* ...

## Performance

* ...

## Benchmarks

* ...

## Tests

* ...

## Platform Validation

* Android: ...
* iOS: ...

## External Libraries / Open Source Reused

* ...

## Git Commits

* Phase 1: ...
* Phase 2: ...
* ...
* Phase 22: ...

## Known Limitations

* ...

## Final Status

* Complete / Incomplete

---

# 41. FINAL INSTRUCTION

Build **SyncCore as a small, deterministic, technically deep Kotlin Multiplatform synchronization engine**.

This is not a cloud-storage application.

This is not a file manager.

This is not a generic CRUD project.

The portfolio objective is to demonstrate:

```text
Kotlin Multiplatform
+
Offline-first thinking
+
Three-way change detection
+
Conflict resolution
+
Deterministic algorithms
+
Performance measurement
+
Shared business logic
```

The core engineering principle is:

> **Never silently overwrite divergent state.**

The central algorithm is:

> **Base + Local + Remote → Change Set → Conflict Set → Resolution → Sync Plan**

**Start implementation immediately.**

**Continue automatically through ALL implementation phases until Phase 22 is complete.**

**Commit after every completed phase.**

**Do NOT run any tests until all implementation phases are complete.**

Optimize token/compute usage without sacrificing correctness, determinism, maintainability, or engineering quality.
