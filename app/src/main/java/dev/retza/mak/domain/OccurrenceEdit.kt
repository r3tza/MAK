package dev.retza.mak.domain

import java.time.LocalDate
import java.time.LocalTime

enum class OccurrenceEditResult {
    NoChange,
    Modified,
    Moved,
    Restored
}

data class OccurrenceSlot(
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val room: String?
)

sealed interface OccurrenceEditDecision {
    data class Ready(
        val result: OccurrenceEditResult,
        val slot: OccurrenceSlot
    ) : OccurrenceEditDecision

    data object InvalidDateTime : OccurrenceEditDecision
}

fun decideOccurrenceEdit(
    base: OccurrenceSlot,
    current: OccurrenceSlot,
    hasChange: Boolean,
    draftDate: String,
    draftStartTime: String,
    draftEndTime: String,
    draftRoom: String
): OccurrenceEditDecision {
    val date = parseDate(draftDate) ?: return OccurrenceEditDecision.InvalidDateTime
    val start = parseTime(draftStartTime) ?: return OccurrenceEditDecision.InvalidDateTime
    val end = parseTime(draftEndTime) ?: return OccurrenceEditDecision.InvalidDateTime
    val draft = OccurrenceSlot(date, start, end, normalizeRoom(draftRoom))

    val result = when {
        draft == current -> OccurrenceEditResult.NoChange
        hasChange && draft == base -> OccurrenceEditResult.Restored
        draft.date != current.date -> OccurrenceEditResult.Moved
        else -> OccurrenceEditResult.Modified
    }
    return OccurrenceEditDecision.Ready(result, draft)
}

private fun normalizeRoom(room: String): String? = room.trim().ifEmpty { null }

private fun parseDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value.trim()) }.getOrNull()

private fun parseTime(value: String): LocalTime? =
    runCatching { LocalTime.parse(value.trim()) }.getOrNull()
