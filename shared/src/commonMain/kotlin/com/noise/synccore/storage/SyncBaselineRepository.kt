package com.noise.synccore.storage

import com.noise.synccore.domain.model.SyncState
import com.noise.synccore.domain.sync.*
import com.noise.synccore.engine.PlanExecutor
import com.noise.synccore.serialization.SyncCodec

/** Adapters must provide atomic compare-and-set for their documented execution scope. */
interface BaselineTextStore {
    fun read(): String?
    fun compareAndSet(expected: String?, updated: String): Boolean
}

interface SyncBaselineRepository {
    fun load(): SyncState
    fun compareAndSet(expected: SyncState, updated: SyncState): Boolean
}

/** JSON survives recreation of the repository when its backing store is durable. */
class SerializedBaselineRepository(private val store: BaselineTextStore) : SyncBaselineRepository {
    override fun load(): SyncState = store.read()?.let(SyncCodec::decodeState) ?: SyncState()
    override fun compareAndSet(expected: SyncState, updated: SyncState): Boolean {
        val raw = store.read()
        val existing = raw?.let(SyncCodec::decodeState) ?: SyncState()
        return existing == expected && store.compareAndSet(raw, SyncCodec.encodeState(updated))
    }
}

/** Sequential in-memory adapter for the lab and tests; not a durable store. */
class InMemoryBaselineStore(initial: String? = null) : BaselineTextStore {
    private var value = initial
    override fun read(): String? = value
    override fun compareAndSet(expected: String?, updated: String): Boolean {
        if (value != expected) return false
        value = updated
        return true
    }
}

class BaselineChangedException : IllegalStateException("Stored baseline changed; reload and replan")

/** Publishes only successfully converged entities; unresolved entries retain the old baseline. */
class SyncSession(private val repository: SyncBaselineRepository) {
    fun apply(plan: SyncPlan): SyncResult {
        if (repository.load() != plan.inputs.base) throw BaselineChangedException()
        val result = PlanExecutor().apply(plan)
        if (!repository.compareAndSet(plan.inputs.base, result.baseline)) throw BaselineChangedException()
        return result
    }
}
