package com.noise.synccore

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.noise.synccore.domain.policy.*
import com.noise.synccore.simulator.*
import com.noise.synccore.storage.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidEngineIntegrationTest {
    @Test fun realAndroidStorageAndSharedEngine() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("synccore-verification",Context.MODE_PRIVATE)
        assertTrue(preferences.edit().clear().commit())
        try {
            val run = SyncSimulator().run(DemoScenarios.all.first(),ResolutionChoices(ResolutionPolicy.KEEP_BOTH))
            assertTrue(run.result.complete)
            assertTrue(DeterministicReplay.matches(run))
            val repository = SerializedBaselineRepository(AndroidBaselineStore(preferences))
            assertTrue(repository.compareAndSet(repository.load(),run.result.baseline))
            assertEquals(run.result.baseline,SerializedBaselineRepository(AndroidBaselineStore(preferences)).load())
        } finally { preferences.edit().clear().commit() }
    }
}
