package com.noise.synccore

import com.noise.synccore.benchmark.*
import java.io.File
import kotlin.test.*

class EngineBenchmarkTest {
    @Test fun measuredReport() {
        val runtime = Runtime.getRuntime()
        val before = runtime.totalMemory() - runtime.freeMemory()
        val report = EngineBenchmark().run()
        val after = runtime.totalMemory() - runtime.freeMemory()
        assertEquals(listOf(100,1_000,10_000,100_000),report.datasets.map { it.entities })
        report.datasets.forEach { dataset ->
            assertEquals(dataset.entities / 5,dataset.conflicts)
            assertTrue(dataset.stages.all { it.samplesNs.size == 5 && it.minimumNs >= 0 })
        }
        val environment = "Runtime: ${System.getProperty("java.vm.name")} ${System.getProperty("java.version")} (${System.getProperty("java.vendor")}). OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")} ${System.getProperty("os.arch")}. Available processors: ${runtime.availableProcessors()}."
        val markdown = report.toMarkdown(environment) + "\nJVM used heap sampled before suite: $before bytes; after suite: $after bytes. These are not peak-memory or allocation metrics, and no forced GC was used.\n"
        val directory = File(requireNotNull(System.getProperty("synccore.reportDir")))
        directory.mkdirs()
        File(directory,"benchmark-report.md").writeText(markdown)
        File(directory,"benchmark-samples.csv").writeText(buildString {
            appendLine("entities,stage,sample,elapsed_ns")
            report.datasets.forEach { d -> d.stages.forEach { s -> s.samplesNs.forEachIndexed { index, ns ->
                appendLine("${d.entities},${s.stage},${index + 1},$ns")
            } } }
        })
        println(markdown)
    }
}
