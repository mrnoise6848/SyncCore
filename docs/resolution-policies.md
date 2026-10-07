# Explicit resolution policies

| Choice | Meaning |
| --- | --- |
| No choice | Emit CONFLICT; preserve both sides and old baseline |
| KEEP_LOCAL | Converge to the whole local entity, including explicit deletion |
| KEEP_REMOTE | Converge to the whole remote entity, including explicit deletion |
| KEEP_BOTH | Preserve local identity and create a remote copy; on delete/modify preserve the survivor |
| SKIP | Emit SKIP; preserve both sides and old baseline |

Global defaults apply only to conflicts. Per-identity overrides take precedence. There is no clock-based winner or automatic merge.

KEEP_BOTH reserves all IDs from base, local and remote, even IDs later deleted. For concurrent existing entities, the remote copy tries `<original>~remote`, then `~remote-2`, `~remote-3`, etc. Conflicts are processed in sorted identity order, and generated IDs are immediately reserved. Existing entities are never overwritten by copy creation. Copied revisions and fields are preserved. On delete/modify there is only one surviving value; it remains at the original identity. A converged result replayed as the next baseline does not create extra copies.
