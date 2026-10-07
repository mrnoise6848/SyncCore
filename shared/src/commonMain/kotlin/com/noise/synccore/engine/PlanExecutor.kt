package com.noise.synccore.engine

import com.noise.synccore.domain.model.*
import com.noise.synccore.domain.sync.*

class InvalidPlanException : IllegalArgumentException("Plan differs from canonical engine output")

class StalePlanException : IllegalStateException("Plan inputs differ from current snapshots; replan before applying")

/** Pure, atomic in-memory application. A transport adapter must independently provide equivalent guards. */
class PlanExecutor {
    fun apply(plan: SyncPlan, current: SyncInputs = plan.inputs): SyncResult {
        if (current != plan.inputs) throw StalePlanException()
        if (SyncPlanner().plan(plan.inputs, plan.choices) != plan) throw InvalidPlanException()
        val mutations = plan.operations.filter { it.type.side != null }
        require(mutations.map { it.type.side to it.id }.distinct().size == mutations.size) { "Duplicate mutation" }
        for (operation in mutations) {
            val state = if (operation.type.side == SyncSide.LOCAL) current.local else current.remote
            if (state[operation.id] != operation.expected) throw StalePlanException()
            require(operation.entity == null || operation.entity.id == operation.id) { "Payload id mismatch" }
        }
        val local = current.local.entities.toMutableMap()
        val remote = current.remote.entities.toMutableMap()
        for (operation in mutations) {
            val destination = if (operation.type.side == SyncSide.LOCAL) local else remote
            if (operation.entity == null) destination.remove(operation.id)
            else destination[operation.id] = operation.entity
        }
        val baseline = current.base.entities.toMutableMap()
        for (id in plan.completedIds) {
            check(local[id] == remote[id]) { "Resolved entity did not converge" }
            val entity = local[id]
            if (entity == null) baseline.remove(id) else baseline[id] = entity
        }
        val unresolvedIds = plan.operations.filter { it.type.side == null }.map { it.id }.toSet()
        return SyncResult(SyncState(local.values), SyncState(remote.values), SyncState(baseline.values),
            plan.conflicts.filter { it.id in unresolvedIds }, mutations.size)
    }
}
