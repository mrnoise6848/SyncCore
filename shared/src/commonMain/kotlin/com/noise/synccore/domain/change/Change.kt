package com.noise.synccore.domain.change

import com.noise.synccore.domain.model.EntityId
import com.noise.synccore.domain.model.SyncEntity

enum class ChangeStatus {
    UNCHANGED, LOCAL_ONLY, REMOTE_ONLY, LOCAL_CHANGED, REMOTE_CHANGED, BOTH_CHANGED, CONFLICT, DELETED
}

data class Change(
    val id: EntityId,
    val base: SyncEntity?,
    val local: SyncEntity?,
    val remote: SyncEntity?,
    val status: ChangeStatus,
)
