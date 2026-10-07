package com.noise.synccore.domain.sync

import com.noise.synccore.domain.change.ChangeSet
import com.noise.synccore.domain.conflict.Conflict
import com.noise.synccore.domain.model.*
import com.noise.synccore.domain.policy.ResolutionChoices

enum class SyncSide { LOCAL, REMOTE }
enum class OperationType(val side: SyncSide?) {
    CREATE_LOCAL(SyncSide.LOCAL), UPDATE_LOCAL(SyncSide.LOCAL), DELETE_LOCAL(SyncSide.LOCAL),
    CREATE_REMOTE(SyncSide.REMOTE), UPDATE_REMOTE(SyncSide.REMOTE), DELETE_REMOTE(SyncSide.REMOTE),
    CONFLICT(null), SKIP(null),
}

data class SyncOperation(
    val type: OperationType,
    val id: EntityId,
    val expected: SyncEntity? = null,
    val entity: SyncEntity? = null,
)

data class SyncPlan(
    val inputs: SyncInputs,
    val choices: ResolutionChoices,
    val changes: ChangeSet,
    val conflicts: List<Conflict>,
    val operations: List<SyncOperation>,
    val completedIds: List<EntityId>,
)
