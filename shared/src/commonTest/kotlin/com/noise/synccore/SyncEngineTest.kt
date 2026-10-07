package com.noise.synccore

import com.noise.synccore.domain.change.*
import com.noise.synccore.domain.conflict.*
import com.noise.synccore.domain.model.*
import com.noise.synccore.domain.policy.*
import com.noise.synccore.domain.sync.*
import com.noise.synccore.engine.*
import com.noise.synccore.serialization.SyncCodec
import com.noise.synccore.simulator.*
import com.noise.synccore.storage.*
import kotlin.test.*

class SyncEngineTest {
    private val a = DemoScenarios.entity("Meeting")
    private val b = a.edited(mapOf("title" to "Local"))
    private val c = a.edited(mapOf("title" to "Remote"))
    private fun state(vararg entities: SyncEntity?) = SyncState(entities.filterNotNull())
    private fun inputs(base: SyncEntity?, local: SyncEntity?, remote: SyncEntity?) = SyncInputs(state(base), state(local), state(remote))
    private fun plan(base: SyncEntity?, local: SyncEntity?, remote: SyncEntity?, policy: ResolutionPolicy? = null) =
        SyncPlanner().plan(inputs(base, local, remote), ResolutionChoices(policy))

    @Test fun explicitTruthTable() {
        val cases = listOf(
            inputs(a,a,a) to ChangeStatus.UNCHANGED,
            inputs(null,b,null) to ChangeStatus.LOCAL_ONLY,
            inputs(null,null,c) to ChangeStatus.REMOTE_ONLY,
            inputs(a,b,a) to ChangeStatus.LOCAL_CHANGED,
            inputs(a,a,c) to ChangeStatus.REMOTE_CHANGED,
            inputs(a,b,b) to ChangeStatus.BOTH_CHANGED,
            inputs(null,b,b) to ChangeStatus.BOTH_CHANGED,
            inputs(a,null,null) to ChangeStatus.DELETED,
            inputs(a,null,a) to ChangeStatus.LOCAL_CHANGED,
            inputs(a,a,null) to ChangeStatus.REMOTE_CHANGED,
            inputs(a,b,c) to ChangeStatus.CONFLICT,
            inputs(null,b,c) to ChangeStatus.CONFLICT,
            inputs(a,null,c) to ChangeStatus.CONFLICT,
            inputs(a,b,null) to ChangeStatus.CONFLICT,
        )
        cases.forEach { (input, expected) -> assertEquals(expected, ChangeDetector().detect(input).single().status) }
        assertTrue(ChangeDetector().detect(inputs(null,null,null)).isEmpty())
    }

    @Test fun noChangeHasNoOperations() { assertTrue(plan(a,a,a).operations.isEmpty()) }
    @Test fun localEditUpdatesRemote() {
        val p = plan(a,b,a)
        assertEquals(listOf(OperationType.UPDATE_REMOTE), p.operations.map { it.type })
        assertEquals(state(b), PlanExecutor().apply(p).remote)
    }
    @Test fun remoteEditUpdatesLocal() { assertEquals(listOf(OperationType.UPDATE_LOCAL), plan(a,a,c).operations.map { it.type }) }
    @Test fun additionsPropagate() {
        assertEquals(OperationType.CREATE_REMOTE, plan(null,b,null).operations.single().type)
        assertEquals(OperationType.CREATE_LOCAL, plan(null,null,c).operations.single().type)
    }
    @Test fun oneSidedDeletionsPropagate() {
        assertEquals(OperationType.DELETE_REMOTE, plan(a,null,a).operations.single().type)
        assertEquals(OperationType.DELETE_LOCAL, plan(a,a,null).operations.single().type)
    }
    @Test fun agreedDeletionClearsBaseline() {
        val result = PlanExecutor().apply(plan(a,null,null))
        assertTrue(result.complete); assertEquals(state(), result.baseline)
    }
    @Test fun concurrentAgreementIsSafe() {
        val result = PlanExecutor().apply(plan(a,b,b))
        assertTrue(result.complete); assertEquals(state(b), result.baseline)
    }
    @Test fun defaultConflictNeverOverwrites() {
        val p = plan(a,b,c)
        val result = PlanExecutor().apply(p)
        assertEquals(OperationType.CONFLICT, p.operations.single().type)
        assertEquals(state(b), result.local); assertEquals(state(c), result.remote)
        assertEquals(state(a), result.baseline); assertFalse(result.complete)
    }
    @Test fun deleteModifyRequiresChoice() {
        val p = plan(a,null,c)
        assertEquals(ConflictType.DELETE_MODIFY_CONFLICT, p.conflicts.single().type)
        assertEquals(state(c), PlanExecutor().apply(p).remote)
        assertEquals(state(), PlanExecutor().apply(p).local)
    }
    @Test fun keepLocalAndRemoteSelectWholeSnapshots() {
        assertEquals(state(b), PlanExecutor().apply(plan(a,b,c,ResolutionPolicy.KEEP_LOCAL)).remote)
        assertEquals(state(c), PlanExecutor().apply(plan(a,b,c,ResolutionPolicy.KEEP_REMOTE)).local)
    }
    @Test fun explicitDeletionIsAllowed() {
        val result = PlanExecutor().apply(plan(a,null,c,ResolutionPolicy.KEEP_LOCAL))
        assertTrue(result.complete); assertEquals(state(), result.remote); assertEquals(state(), result.baseline)
    }
    @Test fun keepBothPreservesBothValues() {
        val result = PlanExecutor().apply(plan(a,b,c,ResolutionPolicy.KEEP_BOTH))
        assertTrue(result.complete)
        assertEquals(setOf("Local", "Remote"), result.local.entities.values.map { it.fields.getValue("title") }.toSet())
        assertEquals(b, result.local[a.id])
        assertEquals(c.withId(EntityId("meeting~remote")), result.local[EntityId("meeting~remote")])
    }
    @Test fun keepBothDeleteModifyPreservesSurvivor() {
        assertEquals(state(c), PlanExecutor().apply(plan(a,null,c,ResolutionPolicy.KEEP_BOTH)).local)
        assertEquals(state(b), PlanExecutor().apply(plan(a,b,null,ResolutionPolicy.KEEP_BOTH)).remote)
    }
    @Test fun copyIdsNeverOverwriteExistingEntities() {
        val occupied = a.withId(EntityId("meeting~remote"))
        val occupied2 = a.withId(EntityId("meeting~remote-2"))
        val p = SyncPlanner().plan(SyncInputs(state(a,occupied,occupied2), state(b,occupied,occupied2), state(c,occupied,occupied2)), ResolutionChoices(ResolutionPolicy.KEEP_BOTH))
        val result = PlanExecutor().apply(p)
        assertEquals(occupied, result.local[occupied.id]); assertEquals(occupied2, result.local[occupied2.id])
        assertEquals(c.withId(EntityId("meeting~remote-3")), result.local[EntityId("meeting~remote-3")])
    }
    @Test fun skipPreservesBaselineAndBothBranches() {
        val p = plan(a,b,c,ResolutionPolicy.SKIP)
        assertEquals(OperationType.SKIP, p.operations.single().type)
        assertEquals(PlanExecutor().apply(plan(a,b,c)), PlanExecutor().apply(p))
    }
    @Test fun conflictFieldsAreExact() {
        assertEquals(listOf("title"), plan(a,b,c).conflicts.single().fields)
        assertEquals(ConflictType.FIELD_CONFLICT, plan(a,b,c).conflicts.single().type)
        val disjoint = ScenarioCatalog.all.last()
        assertEquals(ConflictType.ENTITY_CONFLICT, SyncPlanner().plan(disjoint.inputs).conflicts.single().type)
    }
    @Test fun fieldRemovalIsARealChange() {
        val base = SyncEntity(a.id, mapOf("title" to "A", "room" to "B"))
        val local = base.edited(mapOf("title" to "A"))
        assertEquals(state(local), PlanExecutor().apply(plan(base,local,base)).remote)
    }
    @Test fun independentPoliciesAndPartialBaseline() {
        val second = a.withId(EntityId("second"))
        val local2 = b.withId(second.id); val remote2 = c.withId(second.id)
        val p = SyncPlanner().plan(SyncInputs(state(a,second),state(b,local2),state(c,remote2)), ResolutionChoices(overrides=mapOf(a.id to ResolutionPolicy.KEEP_LOCAL)))
        val result = PlanExecutor().apply(p)
        assertEquals(state(b,second), result.baseline)
        assertEquals(listOf(second.id), result.unresolved.map { it.id })
        assertEquals(remote2, result.remote[second.id])
    }
    @Test fun unknownResolutionIdIsRejected() {
        assertFailsWith<IllegalArgumentException> { SyncPlanner().plan(inputs(a,b,c), ResolutionChoices(overrides=mapOf(EntityId("unknown") to ResolutionPolicy.KEEP_LOCAL))) }
    }
    @Test fun staleSnapshotsAreRejected() {
        assertFailsWith<StalePlanException> { PlanExecutor().apply(plan(a,b,a), inputs(a,c,a)) }
        assertEquals("Remote", c.fields["title"])
    }
    @Test fun alteredPlanCannotDeleteAnUnchangedEntity() {
        val p = plan(a,a,a)
        assertFailsWith<InvalidPlanException> { PlanExecutor().apply(p.copy(operations=listOf(SyncOperation(OperationType.DELETE_REMOTE,a.id,a)))) }
        assertFailsWith<InvalidPlanException> { PlanExecutor().apply(plan(a,b,c).copy(operations=emptyList(),completedIds=listOf(a.id))) }
    }
    @Test fun inputCollectionsAreDetachedAndOrdered() {
        val fields = mutableMapOf("z" to "last", "a" to "first")
        val entity = SyncEntity(a.id,fields)
        fields["a"] = "changed"
        val entities = mutableListOf(entity)
        val snapshot = SyncState(entities)
        entities.clear()
        assertEquals(listOf("a","z"), entity.fields.keys.toList())
        assertEquals("first",snapshot[a.id]?.fields?.get("a"))
    }
    @Test fun mutatedViewsCannotChangeSnapshots() {
        assertFails { (a.fields as MutableMap<String,String>)["title"] = "injected" }
        val fieldsView = a.fields.entries.toMutableSet()
        fieldsView.clear()
        val snapshot = state(a)
        val entitiesView = snapshot.entities.values.toMutableList()
        entitiesView.clear()
        assertEquals("Meeting",a.fields["title"])
        assertEquals(mapOf("title" to "Meeting").entries,a.fields.entries)
        assertEquals(a,snapshot[a.id])
    }
    @Test fun invalidDomainValuesAreRejected() {
        assertFailsWith<IllegalArgumentException> { EntityId(" ") }
        assertFailsWith<IllegalArgumentException> { Revision(-1) }
        assertFailsWith<IllegalArgumentException> { SyncEntity(a.id,mapOf("" to "value")) }
        assertFailsWith<IllegalArgumentException> { state(a,a) }
    }
    @Test fun revisionsDoNotChooseWinners() {
        val newer = SyncEntity(c.id,c.fields,Revision(999))
        assertEquals(ChangeStatus.CONFLICT,plan(a,b,newer).changes.changes.single().status)
        assertSame(a,a.edited(a.fields)); assertEquals(Revision(1),b.revision)
        assertFailsWith<IllegalStateException> { Revision(Long.MAX_VALUE).next() }
        val revisionOnly = SyncEntity(a.id,a.fields,Revision(1))
        assertEquals(ChangeStatus.LOCAL_CHANGED,plan(a,revisionOnly,a).changes.changes.single().status)
    }
    @Test fun scenarioCatalogAndPoliciesReplayAndRoundTrip() {
        for (scenario in ScenarioCatalog.all) for (policy in listOf(null) + ResolutionPolicy.entries) {
            val run = SyncSimulator().run(scenario,ResolutionChoices(policy))
            assertTrue(DeterministicReplay.matches(run),scenario.name)
            assertEquals(run.plan,SyncCodec.decodePlan(SyncCodec.encodePlan(run.plan)))
            assertEquals(run.result,SyncCodec.decodeResult(SyncCodec.encodeResult(run.result)))
            assertEquals(run.plan.inputs,SyncCodec.decodeInputs(SyncCodec.encodeInputs(run.plan.inputs)))
            assertEquals(run.plan.changes,SyncCodec.decodeChanges(SyncCodec.encodeChanges(run.plan.changes)))
            assertEquals(run.plan.conflicts,SyncCodec.decodeConflicts(SyncCodec.encodeConflicts(run.plan.conflicts)))
            val record = DeterministicReplay.capture(run)
            assertEquals(run,DeterministicReplay.replay(SyncCodec.decodeReplay(SyncCodec.encodeReplay(record))))
            if (run.result.complete) {
                val next = SyncPlanner().plan(SyncInputs(run.result.baseline,run.result.local,run.result.remote))
                assertTrue(next.operations.isEmpty(),scenario.name)
            }
        }
    }
    @Test fun allSnapshotCombinationsPreserveSafety() {
        // 4^3 snapshots x 5 choices = 320 actual runs, without randomness.
        for (base in listOf(null,a,b,c)) for (local in listOf(null,a,b,c)) for (remote in listOf(null,a,b,c)) {
            for (policy in listOf(null) + ResolutionPolicy.entries) {
                val input = inputs(base,local,remote)
                val p = SyncPlanner().plan(input,ResolutionChoices(policy))
                val result = PlanExecutor().apply(p)
                val divergent = local != remote && local != base && remote != base
                assertEquals(if (divergent) 1 else 0,p.conflicts.size)
                if (divergent && (policy == null || policy == ResolutionPolicy.SKIP)) {
                    assertEquals(input.local,result.local); assertEquals(input.remote,result.remote)
                    assertEquals(input.base,result.baseline); assertFalse(result.complete)
                } else {
                    assertTrue(result.complete); assertEquals(result.local,result.remote); assertEquals(result.local,result.baseline)
                    if (divergent && policy == ResolutionPolicy.KEEP_LOCAL) assertEquals(state(local),result.local)
                    if (divergent && policy == ResolutionPolicy.KEEP_REMOTE) assertEquals(state(remote),result.remote)
                    if (divergent && policy == ResolutionPolicy.KEEP_BOTH) assertEquals(
                        listOfNotNull(local,remote).map { it.fields }.toSet(), result.local.entities.values.map { it.fields }.toSet())
                }
                assertEquals(p,SyncPlanner().plan(input,ResolutionChoices(policy)))
            }
        }
    }
    @Test fun insertionOrderDoesNotAffectPlansOrSerialization() {
        val input = ScenarioCatalog.all.first { it.name == "Multiple conflicts" }.inputs
        fun reverse(s: SyncState) = SyncState(s.entities.values.reversed())
        val reversed = SyncInputs(reverse(input.base),reverse(input.local),reverse(input.remote))
        val choices = ResolutionChoices(ResolutionPolicy.KEEP_BOTH)
        assertEquals(SyncPlanner().plan(input,choices),SyncPlanner().plan(reversed,choices))
        assertEquals(SyncCodec.encodeInputs(input),SyncCodec.encodeInputs(reversed))
    }
    @Test fun jsonEscapingUnicodeAndLongRevisionRoundTrip() {
        val entity = SyncEntity(EntityId("جلسه/😀"),mapOf("عنوان" to "quoted \"text\"\nslash \\ and emoji 😀", "empty" to ""),Revision(Long.MAX_VALUE))
        assertEquals(state(entity),SyncCodec.decodeState(SyncCodec.encodeState(state(entity))))
    }
    @Test fun malformedAndTamperedJsonIsRejected() {
        val json = SyncCodec.encodeState(state(a))
        assertFails { SyncCodec.decodeState("{") }
        assertFails { SyncCodec.decodeState(json.replace("\"version\":1","\"version\":2")) }
        assertFails { SyncCodec.decodeState(json.replace("\"revision\":0","\"revision\":-1")) }
        assertFails { SyncCodec.decodeState(json.replace("\"version\":1","\"unknown\":0,\"version\":1")) }
        assertFails { SyncCodec.decodeState("[".repeat(33)+"]".repeat(33)) }
        assertFails { SyncCodec.decodePlan(SyncCodec.encodePlan(plan(a,b,c)).replace("CONFLICT","SKIP")) }
    }
    @Test fun persistedBaselineReloadsAndRejectsLostUpdates() {
        val store = InMemoryBaselineStore(SyncCodec.encodeState(state(a)))
        val repo = SerializedBaselineRepository(store)
        val result = SyncSession(repo).apply(plan(a,b,a))
        assertEquals(state(b),result.baseline)
        assertEquals(state(b),SerializedBaselineRepository(store).load())
        assertFalse(repo.compareAndSet(state(a),state(c)))
        assertFailsWith<BaselineChangedException> { SyncSession(repo).apply(plan(a,c,a)) }
    }
    @Test fun skippedConflictsDoNotAdvancePersistedBaseline() {
        val repo = SerializedBaselineRepository(InMemoryBaselineStore(SyncCodec.encodeState(state(a))))
        val result = SyncSession(repo).apply(plan(a,b,c,ResolutionPolicy.SKIP))
        assertFalse(result.complete); assertEquals(state(a),repo.load())
    }
}
