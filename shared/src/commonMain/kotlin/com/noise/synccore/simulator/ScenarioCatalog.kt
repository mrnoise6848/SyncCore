package com.noise.synccore.simulator

import com.noise.synccore.domain.model.*

object ScenarioCatalog {
    private fun state(vararg entities: SyncEntity) = SyncState(entities.toList())
    private val a = DemoScenarios.entity("Meeting")
    private val b = a.edited(mapOf("title" to "Important Meeting"))
    private val c = a.edited(mapOf("title" to "Team Meeting"))
    val all: List<Scenario> = DemoScenarios.all + listOf(
        Scenario("Remote deletion", "An unchanged local copy accepts a remote deletion.", SyncInputs(state(a), state(a), state())),
        Scenario("Local addition", "A new local identity is created remotely.", SyncInputs(state(), state(b), state())),
        Scenario("Remote addition", "A new remote identity is downloaded locally.", SyncInputs(state(), state(), state(c))),
        Scenario("Agreed concurrent edit", "Identical concurrent snapshots converge without conflict.", SyncInputs(state(a), state(b), state(b))),
        Scenario("Agreed deletion", "Both replicas removed the same baseline identity.", SyncInputs(state(a), state(), state())),
        Scenario("Independent additions", "Two different additions use the same identity.", SyncInputs(state(), state(b), state(c))),
        Scenario("Multiple conflicts", "Each identity can use a different policy.", SyncInputs(
            state(a, a.withId(EntityId("agenda"))), state(b, b.withId(EntityId("agenda"))), state(c, c.withId(EntityId("agenda"))))),
        Scenario("Copy identity collision", "Keep Both must preserve an existing remote-copy identity.", SyncInputs(
            state(a, a.withId(EntityId("meeting~remote"))), state(b, a.withId(EntityId("meeting~remote"))), state(c, a.withId(EntityId("meeting~remote"))))),
        Scenario("Disjoint field edits", "Whole-entity policies require a choice even for disjoint edits.", SyncInputs(
            state(SyncEntity(a.id, mapOf("title" to "Meeting", "room" to "A"))),
            state(SyncEntity(a.id, mapOf("title" to "Important Meeting", "room" to "A"), Revision(1))),
            state(SyncEntity(a.id, mapOf("title" to "Meeting", "room" to "B"), Revision(1))))),
    )
}
