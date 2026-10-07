package com.noise.synccore.simulator

import com.noise.synccore.domain.model.*
import com.noise.synccore.domain.policy.ResolutionChoices
import com.noise.synccore.domain.sync.*
import com.noise.synccore.engine.*

data class Scenario(val name: String, val description: String, val inputs: SyncInputs)
data class SimulationRun(val plan: SyncPlan, val result: SyncResult)

class SyncSimulator {
    fun run(scenario: Scenario, choices: ResolutionChoices = ResolutionChoices()): SimulationRun {
        val plan = SyncPlanner().plan(scenario.inputs, choices)
        return SimulationRun(plan, PlanExecutor().apply(plan))
    }
}

object DemoScenarios {
    fun entity(title: String, id: String = "meeting", revision: Long = 0): SyncEntity =
        SyncEntity(EntityId(id), mapOf("title" to title), Revision(revision))
    private fun state(entity: SyncEntity?) = SyncState(listOfNotNull(entity))
    private val meeting = entity("Meeting")
    private val local = entity("Important Meeting", revision = 1)
    private val remote = entity("Team Meeting", revision = 1)
    val all: List<Scenario> = listOf(
        Scenario("Both modified", "Independent edits require an explicit resolution.", SyncInputs(state(meeting), state(local), state(remote))),
        Scenario("No changes", "All snapshots agree; no operations are necessary.", SyncInputs(state(meeting), state(meeting), state(meeting))),
        Scenario("Local modification", "A local edit propagates to remote.", SyncInputs(state(meeting), state(local), state(meeting))),
        Scenario("Remote modification", "A remote edit propagates to local.", SyncInputs(state(meeting), state(meeting), state(remote))),
        Scenario("Local deletion", "Deletion propagates only when the other side is unchanged.", SyncInputs(state(meeting), state(null), state(meeting))),
        Scenario("Delete vs modify", "A deletion cannot silently destroy a concurrent edit.", SyncInputs(state(meeting), state(null), state(remote))),
    )
}
