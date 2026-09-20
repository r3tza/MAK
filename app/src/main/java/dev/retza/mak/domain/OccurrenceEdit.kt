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
) {
    fun normalized(): OccurrenceSlot = copy(room = normalizeRoom(room))
}

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
    val baseSlot = base.normalized()
    val currentSlot = current.normalized()

    val result = when {
        draft == currentSlot -> OccurrenceEditResult.NoChange
        hasChange && draft == baseSlot -> OccurrenceEditResult.Restored
        draft.date != baseSlot.date -> OccurrenceEditResult.Moved
        else -> OccurrenceEditResult.Modified
    }
    return OccurrenceEditDecision.Ready(result, draft)
}

fun occurrenceRoomOverride(baseRoom: String?, draftRoom: String?): String? {
    val base = normalizeRoom(baseRoom)
    val draft = normalizeRoom(draftRoom)
    return when {
        draft != null -> draft
        base != null -> ""
        else -> null
    }
}

fun noteContentChanged(draft: String, stored: String?): Boolean =
    normalizeNote(draft) != normalizeNote(stored)

private fun normalizeNote(value: String?): String? = value?.trim()?.ifEmpty { null }

internal fun normalizeRoom(room: String?): String? = room?.trim()?.ifEmpty { null }

private fun parseDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value.trim()) }.getOrNull()

private fun parseTime(value: String): LocalTime? =
    runCatching { LocalTime.parse(value.trim()) }.getOrNull()
