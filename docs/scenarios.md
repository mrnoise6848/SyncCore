# Deterministic scenario catalog

The lab exposes 15 real scenarios: both modified, no changes, local modification, remote modification, local deletion, delete vs modify, remote deletion, local addition, remote addition, agreed concurrent edit, agreed deletion, independent additions, multiple conflicts, copy identity collision and disjoint field edits.

The common suite runs every scenario with no policy and KEEP_LOCAL, KEEP_REMOTE, KEEP_BOTH and SKIP (75 combinations), checks replay equality, JSON round-trips, and convergence idempotence. A separate exhaustive 4×4×4 snapshot grid with five policy choices covers 320 runs. Additional focused cases check stale/tampered plans, revision overflow, Unicode, malformed input, field removal, partial baseline publication and repository lost-update prevention. These counts describe implemented coverage; pass/fail results are reported only after final execution.
