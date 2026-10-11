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
    val date = draftDate.toLocalDateOrNull() ?: return OccurrenceEditDecision.InvalidDateTime
    val start = draftStartTime.toLocalTimeOrNull() ?: return OccurrenceEditDecision.InvalidDateTime
    val end = draftEndTime.toLocalTimeOrNull() ?: return OccurrenceEditDecision.InvalidDateTime
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

enum class OccurrenceDraftError {
    DATE_OUTSIDE_CALENDAR,
    END_NOT_AFTER_START
}

/** Without a calendar the date is not restricted, like [dateInCalendar]. */
fun validateOccurrenceDraft(
    slot: OccurrenceSlot,
    calendar: AcademicCalendar?
): Set<OccurrenceDraftError> {
    val errors = linkedSetOf<OccurrenceDraftError>()
    if (!dateInCalendar(slot.date, calendar)) {
        errors += OccurrenceDraftError.DATE_OUTSIDE_CALENDAR
    }
    if (!slot.endTime.isAfter(slot.startTime)) errors += OccurrenceDraftError.END_NOT_AFTER_START
    return errors
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

internal fun normalizeNote(value: String?): String? = value?.trim()?.ifEmpty { null }

internal fun normalizeRoom(room: String?): String? = room?.trim()?.ifEmpty { null }
