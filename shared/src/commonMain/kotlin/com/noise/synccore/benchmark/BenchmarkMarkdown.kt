package com.noise.synccore.benchmark

fun BenchmarkReport.toMarkdown(environment: String): String = buildString {
    appendLine("# SyncCore benchmark report")
    appendLine()
    appendLine(environment)
    appendLine()
    appendLine("Warm-ups per stage: $warmups. Measured samples: $iterations. Fixture generation excluded. Times are monotonic elapsed milliseconds.")
    appendLine()
    appendLine("| Identities | Conflicts | Operations | Stage | Median ms | Min ms | Empirical p95 ms |")
    appendLine("| ---: | ---: | ---: | --- | ---: | ---: | ---: |")
    for (dataset in datasets) for (stage in dataset.stages) {
        appendLine("| ${dataset.entities} | ${dataset.conflicts} | ${dataset.operations} | ${stage.stage} | ${stage.medianNs / 1_000_000.0} | ${stage.minimumNs / 1_000_000.0} | ${stage.p95Ns / 1_000_000.0} |")
    }
    appendLine()
    appendLine("Planning includes change/conflict detection and resolution. Guarded application includes canonical replanning. Samples are not independent process forks; GC and runtime warm-up can affect values. With five samples empirical p95 equals the maximum. Portable memory/allocation metrics are not available.")
}
