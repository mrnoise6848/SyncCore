package com.noise.synccore.benchmark

import com.noise.synccore.domain.change.ChangeSet
import com.noise.synccore.domain.model.*
import com.noise.synccore.domain.policy.*
import com.noise.synccore.engine.*
import kotlin.time.TimeSource

data class StageMeasurement(val stage: String, val samplesNs: List<Long>) {
    val medianNs: Long get() = samplesNs.sorted()[samplesNs.size / 2]
    val minimumNs: Long get() = samplesNs.min()
    val p95Ns: Long get() = samplesNs.sorted()[((samplesNs.size * 95 + 99) / 100 - 1).coerceAtMost(samplesNs.lastIndex)]
}
data class DatasetMeasurement(val entities: Int, val conflicts: Int, val operations: Int, val stages: List<StageMeasurement>)
data class BenchmarkReport(val warmups: Int, val iterations: Int, val datasets: List<DatasetMeasurement>)

object BenchmarkDatasets {
    /** Fixed mixed workload: edits, agreed edits, additions, deletions and edit/delete conflicts. */
    fun create(size: Int): SyncInputs {
        require(size > 0)
        val base = ArrayList<SyncEntity>(size)
        val local = ArrayList<SyncEntity>(size)
        val remote = ArrayList<SyncEntity>(size)
        repeat(size) { index ->
            val id = EntityId("entity-${index.toString().padStart(6, '0')}")
            val entity = SyncEntity(id, mapOf("title" to "Item $index", "group" to "${index % 7}"))
            val l = entity.edited(entity.fields + ("title" to "Local $index"))
            val r = entity.edited(entity.fields + ("title" to "Remote $index"))
            if (index % 10 != 5) base += entity
            when (index % 10) {
                0 -> { local += l; remote += entity }
                1 -> { local += entity; remote += r }
                2 -> { local += l; remote += r }
                3 -> remote += r
                4 -> remote += entity
                5 -> remote += r
                6 -> { local += l; remote += l }
                else -> { local += entity; remote += entity }
            }
        }
        return SyncInputs(SyncState(base), SyncState(local), SyncState(remote))
    }
}

class EngineBenchmark {
    fun run(sizes: List<Int> = listOf(100, 1_000, 10_000, 100_000), warmups: Int = 2, iterations: Int = 5): BenchmarkReport {
        require(warmups >= 0 && iterations > 0 && sizes.isNotEmpty())
        val measurements = sizes.map { size ->
            val inputs = BenchmarkDatasets.create(size)
            val changes = ChangeSet(ChangeDetector().detect(inputs))
            val planner = SyncPlanner()
            val choices = ResolutionChoices(ResolutionPolicy.KEEP_BOTH)
            val plan = planner.plan(inputs, choices)
            fun measure(stage: String, block: () -> Int): StageMeasurement {
                repeat(warmups) { check(block() >= 0) }
                val samples = List(iterations) {
                    val start = TimeSource.Monotonic.markNow()
                    check(block() >= 0)
                    start.elapsedNow().inWholeNanoseconds
                }
                return StageMeasurement(stage, samples)
            }
            val stages = listOf(
                measure("change detection") { ChangeDetector().detect(inputs).size },
                measure("conflict detection") { ConflictDetector().detect(changes).size },
                measure("planning (full pipeline)") { planner.plan(inputs, choices).operations.size },
                measure("guarded application") { PlanExecutor().apply(plan).appliedOperations },
            )
            DatasetMeasurement(size, plan.conflicts.size, plan.operations.size, stages)
        }
        return BenchmarkReport(warmups, iterations, measurements)
    }
}
