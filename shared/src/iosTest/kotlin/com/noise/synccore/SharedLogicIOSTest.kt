package com.noise.synccore

import com.noise.synccore.domain.policy.*
import com.noise.synccore.serialization.SyncCodec
import com.noise.synccore.simulator.*
import com.noise.synccore.storage.*
import platform.Foundation.NSUserDefaults
import kotlin.test.*

class SharedLogicIOSTest {
    @Test fun iosTargetRunsSharedEngine() {
        val run = SyncSimulator().run(DemoScenarios.all.first(),ResolutionChoices(ResolutionPolicy.KEEP_BOTH))
        assertTrue(run.result.complete)
        assertEquals(2,run.result.local.entities.size)
        assertEquals(run.plan,SyncCodec.decodePlan(SyncCodec.encodePlan(run.plan)))
        assertTrue(DeterministicReplay.matches(run))
    }
    @Test fun appleBaselineSurvivesRepositoryRecreation() {
        val defaults = NSUserDefaults.standardUserDefaults
        val key = "synccore.verification.baseline"
        defaults.removeObjectForKey(key)
        try {
            val run = SyncSimulator().run(DemoScenarios.all.first(),ResolutionChoices(ResolutionPolicy.KEEP_LOCAL))
            val repository = SerializedBaselineRepository(AppleBaselineStore(defaults,key))
            assertTrue(repository.compareAndSet(repository.load(),run.result.baseline))
            assertEquals(run.result.baseline,SerializedBaselineRepository(AppleBaselineStore(defaults,key)).load())
            assertFalse(repository.compareAndSet(DemoScenarios.all.first().inputs.base,run.result.remote))
        } finally { defaults.removeObjectForKey(key) }
    }
}
