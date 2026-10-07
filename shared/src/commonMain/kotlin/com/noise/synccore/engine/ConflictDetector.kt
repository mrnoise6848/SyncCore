package com.noise.synccore.engine

import com.noise.synccore.domain.change.ChangeSet
import com.noise.synccore.domain.change.ChangeStatus
import com.noise.synccore.domain.conflict.Conflict
import com.noise.synccore.domain.conflict.ConflictType

class ConflictDetector {
    fun detect(changes: ChangeSet): List<Conflict> = changes.changes
        .filter { it.status == ChangeStatus.CONFLICT }
        .map { change ->
            val base = change.base?.fields.orEmpty()
            val local = change.local?.fields.orEmpty()
            val remote = change.remote?.fields.orEmpty()
            val overlapping = (base.keys + local.keys + remote.keys).sorted().filter { field ->
                local[field] != base[field] && remote[field] != base[field] && local[field] != remote[field]
            }
            val type = when {
                change.local == null || change.remote == null -> ConflictType.DELETE_MODIFY_CONFLICT
                overlapping.isNotEmpty() -> ConflictType.FIELD_CONFLICT
                else -> ConflictType.ENTITY_CONFLICT
            }
            Conflict(change, type, overlapping)
        }
}
