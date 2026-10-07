package com.noise.synccore.domain.conflict

import com.noise.synccore.domain.change.Change
import com.noise.synccore.domain.model.EntityId

enum class ConflictType { FIELD_CONFLICT, ENTITY_CONFLICT, DELETE_MODIFY_CONFLICT }

data class Conflict(val change: Change, val type: ConflictType, val fields: List<String>) {
    val id: EntityId get() = change.id
}
