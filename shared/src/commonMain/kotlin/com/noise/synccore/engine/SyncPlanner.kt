package com.noise.synccore.engine

import com.noise.synccore.domain.change.*
import com.noise.synccore.domain.model.*
import com.noise.synccore.domain.policy.*
import com.noise.synccore.domain.sync.*

class SyncPlanner {
    fun plan(inputs: SyncInputs, choices: ResolutionChoices = ResolutionChoices()): SyncPlan {
        val changes = ChangeSet(ChangeDetector().detect(inputs))
        val conflicts = ConflictDetector().detect(changes)
        val byId = conflicts.associateBy { it.id }
        require(choices.overrides.keys.all { it in byId }) { "Resolution override must reference a conflict" }
        val reserved = inputs.entityIds().toMutableSet()
        val completed = mutableListOf<EntityId>()
        val operations = mutableListOf<SyncOperation>()
        val resolver = SyncResolver()
        fun reconcile(id: EntityId, desired: SyncEntity?) {
            for (side in SyncSide.entries) {
                val current = if (side == SyncSide.LOCAL) inputs.local[id] else inputs.remote[id]
                if (current == desired) continue
                val type = when (side) {
                    SyncSide.LOCAL -> when {
                        desired == null -> OperationType.DELETE_LOCAL
                        current == null -> OperationType.CREATE_LOCAL
                        else -> OperationType.UPDATE_LOCAL
                    }
                    SyncSide.REMOTE -> when {
                        desired == null -> OperationType.DELETE_REMOTE
                        current == null -> OperationType.CREATE_REMOTE
                        else -> OperationType.UPDATE_REMOTE
                    }
                }
                operations += SyncOperation(type, id, current, desired)
            }
            completed += id
        }
        for (change in changes.changes) {
            if (change.status == ChangeStatus.CONFLICT) {
                val policy = choices.forEntity(change.id)
                val resolution = resolver.resolve(byId.getValue(change.id), policy, reserved)
                if (!resolution.resolved) {
                    operations += SyncOperation(if (policy == ResolutionPolicy.SKIP) OperationType.SKIP else OperationType.CONFLICT, change.id)
                } else {
                    reconcile(change.id, resolution.entities.firstOrNull { it.id == change.id })
                    resolution.entities.filter { it.id != change.id }.forEach { reconcile(it.id, it) }
                }
            } else {
                val desired = when (change.status) {
                    ChangeStatus.LOCAL_ONLY, ChangeStatus.LOCAL_CHANGED -> change.local
                    ChangeStatus.REMOTE_ONLY, ChangeStatus.REMOTE_CHANGED -> change.remote
                    else -> change.local // unchanged, agreed concurrent change or agreed deletion
                }
                reconcile(change.id, desired)
            }
        }
        return SyncPlan(inputs, choices, changes, conflicts,
            operations.sortedWith(compareBy<SyncOperation> { it.id }.thenBy { it.type.ordinal }),
            completed.sorted())
    }
}
