package com.noise.synccore

import com.noise.synccore.domain.model.*
import com.noise.synccore.domain.policy.*
import com.noise.synccore.serialization.SyncCodec
import com.noise.synccore.simulator.*
import java.io.File
import kotlin.test.*

class EngineEvaluationTest {
    @Test fun evaluateActualRuns() {
        var runs = 0; var conflictCases = 0; var converged = 0; var unresolved = 0
        fun verify(scenario: Scenario, policy: ResolutionPolicy?) {
            val run = SyncSimulator().run(scenario,ResolutionChoices(policy))
            assertTrue(DeterministicReplay.matches(run))
            assertEquals(run.plan,SyncCodec.decodePlan(SyncCodec.encodePlan(run.plan)))
            assertEquals(run.result,SyncCodec.decodeResult(SyncCodec.encodeResult(run.result)))
            if (run.result.complete) { assertEquals(run.result.local,run.result.remote); converged++ }
            else { assertTrue(run.result.unresolved.isNotEmpty()); unresolved++ }
            if (run.plan.conflicts.isNotEmpty()) conflictCases++
            runs++
        }
        ScenarioCatalog.all.forEach { s -> (listOf(null)+ResolutionPolicy.entries).forEach { verify(s,it) } }
        val a = DemoScenarios.entity("A")
        val states = listOf(null,a,a.edited(mapOf("title" to "B")),a.edited(mapOf("title" to "C")))
        fun state(e: SyncEntity?) = SyncState(listOfNotNull(e))
        for (b in states) for (l in states) for (r in states) for (p in listOf(null)+ResolutionPolicy.entries) {
            val input = SyncInputs(state(b),state(l),state(r))
            val divergent = l != r && l != b && r != b
            val actual = SyncSimulator().run(Scenario("Grid","Exhaustive snapshots",input),ResolutionChoices(p))
            assertEquals(divergent,actual.plan.conflicts.isNotEmpty())
            verify(Scenario("Grid","Exhaustive snapshots",input),p)
        }
        assertEquals(395,runs)
        val directory = File(requireNotNull(System.getProperty("synccore.reportDir")))
        directory.mkdirs()
        File(directory,"scenario-evaluation.md").writeText("""
            # Deterministic scenario evaluation

            Actual Android/JVM host runs: $runs. Passed: $runs. Cases containing conflicts: $conflictCases.
            Converged outcomes: $converged. Safely unresolved outcomes: $unresolved.

            Includes 75 catalog/policy combinations and 320 exhaustive snapshot/policy combinations.
            Each evaluated run checks replay, plan/result JSON round-trip and either replica agreement or explicit unresolved conflicts.
            The 320-case grid also checks conflict presence against independent three-way divergence predicates.
            These finite-case results do not claim correctness for all possible inputs, device UI behavior or production transport.
        """.trimIndent()+"\n")
    }
}
