package com.noise.synccore.engine

import com.noise.synccore.domain.change.Change
import com.noise.synccore.domain.change.ChangeStatus
import com.noise.synccore.domain.model.SyncInputs
import com.noise.synccore.domain.model.entityIds

class ChangeDetector {
    fun detect(inputs: SyncInputs): List<Change> = inputs.entityIds().map { id ->
        val base = inputs.base[id]
        val local = inputs.local[id]
        val remote = inputs.remote[id]
        val status = when {
            local == base && remote == base -> ChangeStatus.UNCHANGED
            base == null && remote == null -> ChangeStatus.LOCAL_ONLY
            base == null && local == null -> ChangeStatus.REMOTE_ONLY
            local == null && remote == null -> ChangeStatus.DELETED
            local == remote -> ChangeStatus.BOTH_CHANGED
            remote == base -> ChangeStatus.LOCAL_CHANGED
            local == base -> ChangeStatus.REMOTE_CHANGED
            else -> ChangeStatus.CONFLICT
        }
        Change(id, base, local, remote, status)
    }
}
