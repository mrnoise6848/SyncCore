package com.noise.synccore.serialization

import com.noise.synccore.domain.change.*
import com.noise.synccore.domain.conflict.*
import com.noise.synccore.domain.model.*
import com.noise.synccore.domain.policy.*
import com.noise.synccore.domain.sync.*
import com.noise.synccore.simulator.ReplayRecord
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable private data class Document(val version: Int, val type: String, val data: JsonElement)
@Serializable private data class FieldDto(val key: String, val value: String)
@Serializable private data class EntityDto(val id: String, val revision: Long, val fields: List<FieldDto>)
@Serializable private data class StateDto(val entities: List<EntityDto>)
@Serializable private data class InputsDto(val base: StateDto, val local: StateDto, val remote: StateDto)
@Serializable private data class ChoiceDto(val id: String, val policy: String)
@Serializable private data class ChoicesDto(val default: String?, val overrides: List<ChoiceDto>)
@Serializable private data class ChangeDto(val id: String, val base: EntityDto?, val local: EntityDto?, val remote: EntityDto?, val status: String)
@Serializable private data class ChangesDto(val changes: List<ChangeDto>)
@Serializable private data class ConflictDto(val change: ChangeDto, val type: String, val fields: List<String>)
@Serializable private data class OperationDto(val type: String, val id: String, val expected: EntityDto?, val entity: EntityDto?)
@Serializable private data class PlanDto(val inputs: InputsDto, val choices: ChoicesDto, val changes: ChangesDto,
    val conflicts: List<ConflictDto>, val operations: List<OperationDto>, val completedIds: List<String>)
@Serializable private data class ResultDto(val local: StateDto, val remote: StateDto, val baseline: StateDto,
    val unresolved: List<ConflictDto>, val appliedOperations: Int)
@Serializable private data class ReplayDto(val inputs: InputsDto, val choices: ChoicesDto)

/** Versioned, canonical JSON. No platform types or generated serializers leak into domain APIs. */
object SyncCodec {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false; isLenient = false }
    private const val MAX_DOCUMENT_CHARS = 32 * 1024 * 1024
    private const val MAX_ENTITIES = 100_000

    private inline fun <reified T> encode(type: String, value: T): String =
        json.encodeToString(Document(1, type, json.encodeToJsonElement(value))).also {
            require(it.length <= MAX_DOCUMENT_CHARS) { "Document too large" }
        }
    private inline fun <reified T> decode(type: String, text: String): T {
        require(text.length <= MAX_DOCUMENT_CHARS) { "Document too large" }
        // Bound nesting before invoking the recursive JSON decoder; escaped quotes do not change depth.
        var depth = 0
        var quoted = false
        var escaped = false
        for (char in text) {
            if (quoted) {
                if (escaped) escaped = false else if (char == '\\') escaped = true else if (char == '"') quoted = false
            } else when (char) {
                '"' -> quoted = true
                '{', '[' -> { depth++; require(depth <= 32) { "Document nesting too deep" } }
                '}', ']' -> depth--
            }
        }
        val document = json.decodeFromString<Document>(text)
        require(document.version == 1 && document.type == type) { "Unsupported document version or type" }
        return json.decodeFromJsonElement(document.data)
    }

    fun encodeState(value: SyncState): String = encode("state", value.dto())
    fun decodeState(text: String): SyncState = decode<StateDto>("state", text).model()
    fun encodeInputs(value: SyncInputs): String = encode("inputs", value.dto())
    fun decodeInputs(text: String): SyncInputs = decode<InputsDto>("inputs", text).model()
    fun encodeChanges(value: ChangeSet): String = encode("changes", value.dto())
    fun decodeChanges(text: String): ChangeSet = decode<ChangesDto>("changes", text).model()
    fun encodeConflicts(value: List<Conflict>): String = encode("conflicts", value.sortedBy { it.id }.map { it.dto() })
    fun decodeConflicts(text: String): List<Conflict> = decode<List<ConflictDto>>("conflicts", text).map { it.model() }.sortedBy { it.id }
    fun encodePlan(value: SyncPlan): String = encode("plan", value.dto())
    fun decodePlan(text: String): SyncPlan = decode<PlanDto>("plan", text).model()
    fun encodeResult(value: SyncResult): String = encode("result", ResultDto(value.local.dto(), value.remote.dto(), value.baseline.dto(), value.unresolved.map { it.dto() }, value.appliedOperations))
    fun decodeResult(text: String): SyncResult = decode<ResultDto>("result", text).let {
        require(it.appliedOperations >= 0) { "Invalid operation count" }
        SyncResult(it.local.model(), it.remote.model(), it.baseline.model(), it.unresolved.map { c -> c.model() }, it.appliedOperations)
    }
    fun encodeReplay(value: ReplayRecord): String = encode("replay", ReplayDto(value.inputs.dto(), value.choices.dto()))
    fun decodeReplay(text: String): ReplayRecord = decode<ReplayDto>("replay", text).let { ReplayRecord(it.inputs.model(), it.choices.model()) }

    private fun SyncEntity.dto() = EntityDto(id.value, revision.value, fields.map { FieldDto(it.key, it.value) })
    private fun EntityDto.model(): SyncEntity {
        require(id.length <= 4096 && fields.size <= 1000) { "Entity limits exceeded" }
        require(fields.map { it.key }.distinct().size == fields.size) { "Duplicate field name" }
        require(fields.all { it.key.length <= 4096 && it.value.length <= 1_048_576 }) { "Field limits exceeded" }
        return SyncEntity(EntityId(id), fields.associate { it.key to it.value }, Revision(revision))
    }
    private fun SyncState.dto() = StateDto(entities.values.map { it.dto() })
    private fun StateDto.model(): SyncState {
        require(entities.size <= MAX_ENTITIES) { "Too many entities" }
        return SyncState(entities.map { it.model() })
    }
    private fun SyncInputs.dto() = InputsDto(base.dto(), local.dto(), remote.dto())
    private fun InputsDto.model() = SyncInputs(base.model(), local.model(), remote.model())
    private fun ResolutionChoices.dto() = ChoicesDto(default?.name, overrides.map { ChoiceDto(it.key.value, it.value.name) })
    private fun ChoicesDto.model(): ResolutionChoices {
        require(overrides.map { it.id }.distinct().size == overrides.size) { "Duplicate policy override" }
        return ResolutionChoices(default?.let { ResolutionPolicy.valueOf(it) }, overrides.associate { EntityId(it.id) to ResolutionPolicy.valueOf(it.policy) })
    }
    private fun Change.dto() = ChangeDto(id.value, base?.dto(), local?.dto(), remote?.dto(), status.name)
    private fun ChangeDto.model(): Change {
        val identity = EntityId(id)
        val b = base?.model(); val l = local?.model(); val r = remote?.model()
        require(listOfNotNull(b, l, r).isNotEmpty() && listOfNotNull(b, l, r).all { it.id == identity }) { "Invalid change identity" }
        return Change(identity, b, l, r, ChangeStatus.valueOf(status))
    }
    private fun ChangeSet.dto() = ChangesDto(changes.map { it.dto() })
    private fun ChangesDto.model() = ChangeSet(changes.map { it.model() })
    private fun Conflict.dto() = ConflictDto(change.dto(), type.name, fields)
    private fun ConflictDto.model() = Conflict(change.model(), ConflictType.valueOf(type), fields)
    private fun SyncOperation.dto() = OperationDto(type.name, id.value, expected?.dto(), entity?.dto())
    private fun OperationDto.model() = SyncOperation(OperationType.valueOf(type), EntityId(id), expected?.model(), entity?.model())
    private fun SyncPlan.dto() = PlanDto(inputs.dto(), choices.dto(), changes.dto(), conflicts.map { it.dto() }, operations.map { it.dto() }, completedIds.map { it.value })
    private fun PlanDto.model() = SyncPlan(inputs.model(), choices.model(), changes.model(), conflicts.map { it.model() }, operations.map { it.model() }, completedIds.map { EntityId(it) })
}
