package com.noise.synccore.domain.sync

import com.noise.synccore.domain.conflict.Conflict
import com.noise.synccore.domain.model.SyncState

data class SyncResult(
    val local: SyncState,
    val remote: SyncState,
    val baseline: SyncState,
    val unresolved: List<Conflict>,
    val appliedOperations: Int,
) {
    val complete: Boolean get() = unresolved.isEmpty() && local == remote
}
