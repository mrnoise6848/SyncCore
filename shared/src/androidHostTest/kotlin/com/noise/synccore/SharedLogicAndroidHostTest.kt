package com.noise.synccore

import com.noise.synccore.domain.policy.*
import com.noise.synccore.serialization.SyncCodec
import com.noise.synccore.simulator.*
import kotlin.test.*

class SharedLogicAndroidHostTest {
    @Test fun androidTargetRunsSharedEngine() {
        val run = SyncSimulator().run(DemoScenarios.all.first(),ResolutionChoices(ResolutionPolicy.KEEP_BOTH))
        assertTrue(run.result.complete)
        assertEquals(2,run.result.local.entities.size)
        assertEquals(run.plan,SyncCodec.decodePlan(SyncCodec.encodePlan(run.plan)))
        assertTrue(DeterministicReplay.matches(run))
    }
}
