package com.noise.synccore.domain.model

/** Read-only map with detached views; casts or mutated view collections cannot alter a snapshot. */
internal class SnapshotMap<K, V>(source: Map<K, V>) : Map<K, V> {
    private val backing = source.toMap()
    override val size: Int get() = backing.size
    override fun isEmpty(): Boolean = backing.isEmpty()
    override fun containsKey(key: K): Boolean = backing.containsKey(key)
    override fun containsValue(value: V): Boolean = backing.containsValue(value)
    override fun get(key: K): V? = backing[key]
    override val keys: Set<K> get() = backing.keys.toSet()
    override val values: Collection<V> get() = backing.values.toList()
    override val entries: Set<Map.Entry<K, V>> get() = backing.map { Entry(it.key,it.value) }.toSet()
    private data class Entry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>
    override fun equals(other: Any?): Boolean = backing == other
    override fun hashCode(): Int = backing.hashCode()
    override fun toString(): String = backing.toString()
}
