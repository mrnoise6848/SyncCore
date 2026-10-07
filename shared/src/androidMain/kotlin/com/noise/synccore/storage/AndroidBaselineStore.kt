package com.noise.synccore.storage

import android.content.SharedPreferences

/** In-process atomic publication. SharedPreferences must not be shared across processes. */
class AndroidBaselineStore(private val preferences: SharedPreferences, private val key: String = "baseline-v1") : BaselineTextStore {
    override fun read(): String? = synchronized(lock) { preferences.getString(key, null) }
    override fun compareAndSet(expected: String?, updated: String): Boolean = synchronized(lock) {
        if (preferences.getString(key, null) != expected) false
        else preferences.edit().putString(key, updated).commit()
    }
    private companion object { val lock = Any() }
}
