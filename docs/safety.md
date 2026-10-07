# Operational safety

A divergent entity is never automatically overwritten. The default emits `CONFLICT`; `SKIP` emits a marker with no mutations. Delete/modify conflicts require the same explicit choice as edit/edit conflicts. KEEP_LOCAL/KEEP_REMOTE may intentionally select deletion; KEEP_BOTH on delete/modify preserves the surviving entity at its original id.

Plans include input snapshots, per-side preconditions and exact replacement payloads. Before application the executor compares current snapshots to planned inputs and independently recomputes the canonical plan. Forged/edited/deserialized operations, omitted conflicts, changed policies, invalid deletion payloads, duplicate operations and unjustified baseline changes are rejected before any result is published. This extra planning pass is a deliberate safety cost measured separately from planning benchmarks.

In-memory execution is atomic because only detached result snapshots are returned. It does not guarantee atomic writes to files or servers. Transport adapters need their own revision guards, retries and transaction semantics. Baseline storage compare-and-set fails if another session published first. Only converged entity entries advance; skipped conflicts retain their previous baseline.
