package com.noise.synccore.storage

import platform.Foundation.NSLock
import platform.Foundation.NSUserDefaults

/** Atomic within this process and adapter family. User defaults are not a cross-process transaction. */
class AppleBaselineStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
    private val key: String = "synccore.baseline-v1",
) : BaselineTextStore {
    override fun read(): String? {
        lock.lock()
        return try { defaults.stringForKey(key) } finally { lock.unlock() }
    }
    override fun compareAndSet(expected: String?, updated: String): Boolean {
        lock.lock()
        return try {
            if (defaults.stringForKey(key) != expected) false
            else { defaults.setObject(updated, forKey = key); true }
        } finally { lock.unlock() }
    }
    private companion object { val lock = NSLock() }
}
