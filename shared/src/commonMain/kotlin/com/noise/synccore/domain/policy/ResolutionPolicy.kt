package com.noise.synccore.domain.policy

import com.noise.synccore.domain.model.EntityId
import com.noise.synccore.domain.model.SnapshotMap

enum class ResolutionPolicy { KEEP_LOCAL, KEEP_REMOTE, KEEP_BOTH, SKIP }

/** Absence of a choice means unresolved. Per-entity choices override the optional default. */
class ResolutionChoices(
    val default: ResolutionPolicy? = null,
    overrides: Map<EntityId, ResolutionPolicy> = emptyMap(),
) {
    val overrides: Map<EntityId, ResolutionPolicy> = SnapshotMap(overrides.entries.sortedBy { it.key }.associate { it.toPair() })
    fun forEntity(id: EntityId): ResolutionPolicy? = overrides[id] ?: default
    override fun equals(other: Any?): Boolean = other is ResolutionChoices && default == other.default && overrides == other.overrides
    override fun hashCode(): Int = 31 * (default?.hashCode() ?: 0) + overrides.hashCode()
}
