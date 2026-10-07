package com.noise.synccore.domain.model

/** Stable identity. Whitespace and Unicode are preserved; no lossy trimming. */
data class EntityId(val value: String) : Comparable<EntityId> {
    init { require(value.isNotBlank()) { "Entity id must not be blank" } }
    override fun compareTo(other: EntityId): Int = value.compareTo(other.value)
}

/** Caller-supplied logical revision, never a wall-clock timestamp. */
data class Revision(val value: Long = 0) {
    init { require(value >= 0) { "Revision must be non-negative" } }
    fun next(): Revision {
        check(value < Long.MAX_VALUE) { "Revision exhausted" }
        return Revision(value + 1)
    }
}

/** A detached, deterministic snapshot. Collection properties must be treated as read-only. */
class SyncEntity(val id: EntityId, fields: Map<String, String>, val revision: Revision = Revision()) {
    val fields: Map<String, String> = fields.entries.sortedBy { it.key }.associate { it.toPair() }
    init { require(fields.keys.all { it.isNotBlank() }) { "Field names must not be blank" } }
    fun edited(newFields: Map<String, String>): SyncEntity =
        if (newFields == fields) this else SyncEntity(id, newFields, revision.next())
    fun withId(newId: EntityId): SyncEntity = SyncEntity(newId, fields, revision)
    override fun equals(other: Any?): Boolean = other is SyncEntity &&
        id == other.id && fields == other.fields && revision == other.revision
    override fun hashCode(): Int = 31 * (31 * id.hashCode() + fields.hashCode()) + revision.hashCode()
    override fun toString(): String = "SyncEntity(id=$id, revision=$revision, fields=$fields)"
}

class SyncState(entities: Collection<SyncEntity> = emptyList()) {
    val entities: Map<EntityId, SyncEntity>
    init {
        require(entities.map { it.id }.toSet().size == entities.size) { "Duplicate entity id" }
        this.entities = entities.sortedBy { it.id }.associateBy { it.id }
    }
    operator fun get(id: EntityId): SyncEntity? = entities[id]
    override fun equals(other: Any?): Boolean = other is SyncState && entities == other.entities
    override fun hashCode(): Int = entities.hashCode()
    override fun toString(): String = "SyncState(${entities.values})"
}

data class SyncInputs(val base: SyncState, val local: SyncState, val remote: SyncState)

/** Union order is independent of insertion order and platform. */
fun SyncInputs.entityIds(): List<EntityId> =
    (base.entities.keys + local.entities.keys + remote.entities.keys).sorted()
