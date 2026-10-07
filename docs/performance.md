# Performance methodology

`EngineBenchmark` generates 100, 1,000, 10,000 and 100,000 identities in a fixed mixed workload. Fixture generation is outside timed regions. Each stage uses two warm-ups and five samples by default. Stages: change detection (including identity union sorting), conflict detection on a precomputed ChangeSet, full planning, and guarded application (including canonical replanning). A monotonic clock is used only by the measuring harness, never by engine decisions.

The baseline algorithm sorts identities and snapshots: O(n log n) time, O(n + field data) space. Field conflict inspection is linear in each entity's field count plus sorting the field union. KEEP_BOTH reserves the full identity union and generated ids. Report medians, minima and empirical p95; with five samples p95 is the maximum, not a statistically precise tail estimate. This is an in-process showcase benchmark, not JMH. Runtime, GC, warm-up and hardware affect results.

Memory/allocation instrumentation is platform-dependent and is not claimed by the portable harness. JVM verification records sampled heap usage separately; it is not peak memory or an allocation count. No performance thresholds are asserted in functional tests. Measurements are deferred until all 22 implementation phases are complete.
