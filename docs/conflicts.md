# Conflict detection

A conflict occurs when local and remote differ and neither matches the base. Concurrent identical entities safely agree. Each `Conflict` retains the full `Change` triple, so original branch values and deletion are visible to callers.

`DELETE_MODIFY_CONFLICT`: one branch removed a baseline entity while the other changed it. `FIELD_CONFLICT`: both branches changed at least one same field to different values; the sorted field list identifies that overlap. `ENTITY_CONFLICT`: concurrent entities diverge without overlapping field edits (or in revision alone). Disjoint edits still require a whole-entity policy; no implicit field merge is implemented. Field absence differs from an empty string.

Changing a title/name is a field edit, not a heuristic rename. Changing stable identity is deletion plus addition. Unknown policy overrides are rejected rather than accidentally applied to another entity.
