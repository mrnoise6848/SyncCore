package com.noise.synccore.simulator

import com.noise.synccore.domain.model.SyncInputs
import com.noise.synccore.domain.policy.ResolutionChoices

/** Everything needed to reproduce a run; no time, random seed, environment or network input. */
data class ReplayRecord(val inputs: SyncInputs, val choices: ResolutionChoices)

object DeterministicReplay {
    fun capture(run: SimulationRun): ReplayRecord = ReplayRecord(run.plan.inputs, run.plan.choices)
    fun replay(record: ReplayRecord): SimulationRun = SyncSimulator().run(
        Scenario("Replay", "Recorded snapshots and choices", record.inputs), record.choices,
    )
    fun matches(run: SimulationRun): Boolean = replay(capture(run)) == run
}
