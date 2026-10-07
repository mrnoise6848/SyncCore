# Three-way change detection

Union all input identities and sort by Kotlin string order. For each identity compare the entire entity (fields and revision) against its base. Field map insertion order is irrelevant.

| Base | Local | Remote | Status |
| --- | --- | --- | --- |
| A | A | A | UNCHANGED |
| absent | B | absent | LOCAL_ONLY |
| absent | absent | B | REMOTE_ONLY |
| A | B | A | LOCAL_CHANGED |
| A | A | B | REMOTE_CHANGED |
| A | B | B | BOTH_CHANGED |
| absent | B | B | BOTH_CHANGED |
| A | absent | absent | DELETED |
| A | absent | A | LOCAL_CHANGED (deletion) |
| A | A | absent | REMOTE_CHANGED (deletion) |
| A | B | C | CONFLICT |
| A | absent | B | CONFLICT |
| absent | B | C | CONFLICT |

`ChangeSet.summary` reports added, modified and deleted counts independently per branch, relative to base. Conflict count overlaps those branch counts; it is not an extra exclusive category. A shared deletion is one deleted item on each branch and may require no replica mutation.

Ordering is stable across collection insertion orders, Android/JVM and Kotlin/Native. String equality is exact, including Unicode normalization differences. There is no content-hash-only equality and no hash-collision shortcut.
