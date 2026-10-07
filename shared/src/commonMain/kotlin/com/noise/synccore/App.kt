package com.noise.synccore

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.noise.synccore.domain.model.*
import com.noise.synccore.domain.policy.*
import com.noise.synccore.simulator.*

private val syncColors = darkColorScheme(
    primary = Color(0xFF7DE2CB), background = Color(0xFF10151C),
    surface = Color(0xFF19222D), secondary = Color(0xFFA8BFFF),
)

@Composable
fun App() {
    MaterialTheme(colorScheme = syncColors) {
        var replayStatus by remember { mutableStateOf<String?>(null) }
        var scenarioIndex by remember { mutableStateOf(0) }
        var defaultPolicy by remember { mutableStateOf<ResolutionPolicy?>(null) }
        var overrides by remember { mutableStateOf<Map<EntityId, ResolutionPolicy>>(emptyMap()) }
        val scenario = ScenarioCatalog.all[scenarioIndex]
        val choices = remember(defaultPolicy, overrides) { ResolutionChoices(defaultPolicy, overrides) }
        val run = remember(scenario, choices) { SyncSimulator().run(scenario, choices) }
        LaunchedEffect(run) { replayStatus = null }
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .safeContentPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("SYNCCORE / ENGINE LAB", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text("Divergence, made explicit.", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text("Base + Local + Remote → Changes → Conflicts → Plan", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScenarioCatalog.all.forEachIndexed { index, item ->
                    FilterChip(selected = index == scenarioIndex, onClick = {
                        scenarioIndex = index; defaultPolicy = null; overrides = emptyMap()
                    }, label = { Text(item.name) })
                }
            }
            Text(scenario.description, color = MaterialTheme.colorScheme.onBackground)
            SnapshotGroup(listOf("BASE / LAST SYNC" to scenario.inputs.base, "LOCAL" to scenario.inputs.local, "REMOTE" to scenario.inputs.remote))
            val summary = run.plan.changes.summary
            Section("Detection") {
                Text("Local: +${summary.local.added}  ~${summary.local.modified}  −${summary.local.deleted}")
                Text("Remote: +${summary.remote.added}  ~${summary.remote.modified}  −${summary.remote.deleted}")
                run.plan.changes.changes.forEach { Text("${it.id.value} · ${it.status}") }
                Text("${run.plan.conflicts.size} conflicts · ${run.result.unresolved.size} unresolved", color = MaterialTheme.colorScheme.primary)
            }
            Section("Resolution policy") {
                Text("Choose explicitly. Skip preserves both sides and the previous baseline.")
                PolicyButtons(defaultPolicy) { defaultPolicy = it }
                TextButton(onClick = { defaultPolicy = null; overrides = emptyMap() }) { Text("Reset choices") }
            }
            run.plan.conflicts.forEach { conflict ->
                Section("${conflict.id.value} / ${conflict.type}") {
                    Text("Conflicting fields: ${conflict.fields.joinToString().ifEmpty { "entity-level divergence" }}")
                    Text("Local: ${conflict.change.local?.fields ?: "deleted"}")
                    Text("Remote: ${conflict.change.remote?.fields ?: "deleted"}")
                    PolicyButtons(choices.forEntity(conflict.id)) { overrides = overrides + (conflict.id to it) }
                }
            }
            Section("Sync plan · ${run.plan.operations.size} operations") {
                if (run.plan.operations.isEmpty()) Text("No operations required.")
                run.plan.operations.forEach { operation ->
                    Text("${operation.type} / ${operation.id.value}", style = MaterialTheme.typography.labelLarge)
                    operation.entity?.let { Text("${it.fields} · revision ${it.revision.value}") }
                }
            }
            Text(if (run.result.complete) "CONVERGED" else "AWAITING RESOLUTION", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
            SnapshotGroup(listOf("RESULT / LOCAL" to run.result.local, "RESULT / REMOTE" to run.result.remote, "NEXT BASELINE" to run.result.baseline))
            OutlinedButton(onClick = { replayStatus = if (DeterministicReplay.matches(run)) "Replay identical: plan and result match." else "Replay mismatch." }) { Text("Replay these inputs") }
            replayStatus?.let { Text(it, color = MaterialTheme.colorScheme.onBackground) }
            Text("Pure shared engine · No network · No implicit overwrite", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PolicyButtons(selected: ResolutionPolicy?, onSelect: (ResolutionPolicy) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ResolutionPolicy.entries.forEach { policy ->
            FilterChip(selected = selected == policy, onClick = { onSelect(policy) }, label = { Text(policy.name.replace('_', ' ')) })
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun SnapshotGroup(states: List<Pair<String, SyncState>>) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 700.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                states.forEach { (title, state) -> Box(Modifier.weight(1f)) { Snapshot(title, state) } }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                states.forEach { (title, state) -> Snapshot(title, state) }
            }
        }
    }
}

@Composable
private fun Snapshot(title: String, state: SyncState) {
    Section(title) {
        if (state.entities.isEmpty()) Text("∅ / no entities")
        state.entities.values.forEach { entity ->
            Text(entity.id.value, style = MaterialTheme.typography.labelLarge)
            entity.fields.forEach { (key, value) -> Text("$key: $value") }
            Text("revision ${entity.revision.value}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
