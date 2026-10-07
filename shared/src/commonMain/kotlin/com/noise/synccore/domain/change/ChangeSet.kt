package com.noise.synccore.domain.change

import com.noise.synccore.domain.model.SyncEntity

/** Branch counts are relative to base, not operation counts; a conflict can also be modified. */
enum class ChangeKind { UNCHANGED, ADDED, MODIFIED, DELETED }

data class ChangeCounts(val added: Int, val modified: Int, val deleted: Int)
data class ChangeSummary(val local: ChangeCounts, val remote: ChangeCounts, val conflicts: Int)

fun changeKind(base: SyncEntity?, current: SyncEntity?): ChangeKind = when {
    base == current -> ChangeKind.UNCHANGED
    base == null -> ChangeKind.ADDED
    current == null -> ChangeKind.DELETED
    else -> ChangeKind.MODIFIED
}

class ChangeSet(changes: List<Change>) {
    val changes: List<Change> = changes.sortedBy { it.id }
    init { require(changes.map { it.id }.distinct().size == changes.size) { "Duplicate change id" } }
    val summary: ChangeSummary
        get() {
            fun count(kinds: List<ChangeKind>) = ChangeCounts(
                kinds.count { it == ChangeKind.ADDED },
                kinds.count { it == ChangeKind.MODIFIED },
                kinds.count { it == ChangeKind.DELETED },
            )
            return ChangeSummary(
                count(changes.map { changeKind(it.base, it.local) }),
                count(changes.map { changeKind(it.base, it.remote) }),
                changes.count { it.status == ChangeStatus.CONFLICT },
            )
        }
    override fun equals(other: Any?): Boolean = other is ChangeSet && changes == other.changes
    override fun hashCode(): Int = changes.hashCode()
}
