package com.noise.synccore.engine

import com.noise.synccore.domain.conflict.Conflict
import com.noise.synccore.domain.model.EntityId
import com.noise.synccore.domain.model.SyncEntity
import com.noise.synccore.domain.policy.ResolutionPolicy

data class Resolution(val entities: List<SyncEntity>, val resolved: Boolean)

class SyncResolver {
    /** KEEP_BOTH retains local identity and deterministically forks remote; never reuses an occupied id. */
    fun resolve(conflict: Conflict, policy: ResolutionPolicy?, reservedIds: MutableSet<EntityId>): Resolution {
        val change = conflict.change
        return when (policy) {
            null, ResolutionPolicy.SKIP -> Resolution(emptyList(), false)
            ResolutionPolicy.KEEP_LOCAL -> Resolution(listOfNotNull(change.local), true)
            ResolutionPolicy.KEEP_REMOTE -> Resolution(listOfNotNull(change.remote), true)
            ResolutionPolicy.KEEP_BOTH -> {
                val local = change.local
                val remote = change.remote
                if (local == null || remote == null) Resolution(listOfNotNull(local, remote), true)
                else {
                    val stem = "${change.id.value}~remote"
                    var id = EntityId(stem)
                    var suffix = 2L
                    while (id in reservedIds) {
                        id = EntityId("$stem-$suffix")
                        suffix++
                    }
                    reservedIds.add(id)
                    Resolution(listOf(local, remote.withId(id)), true)
                }
            }
        }
    }
}
