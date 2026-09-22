package dev.retza.mak.ui

import dev.retza.mak.domain.OccurrenceChangeKind
import dev.retza.mak.domain.PlannedOccurrence
import dev.retza.mak.domain.Recurrence
import dev.retza.mak.ui.components.ClassItemUi
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val polishLocale = Locale.forLanguageTag("pl-PL")
internal val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", polishLocale)
internal val todayTitleFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", polishLocale)
internal val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", polishLocale)
internal val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", polishLocale)
internal val dayNames = linkedMapOf(
    DayOfWeek.MONDAY to "Poniedziałek",
    DayOfWeek.TUESDAY to "Wtorek",
    DayOfWeek.WEDNESDAY to "Środa",
    DayOfWeek.THURSDAY to "Czwartek",
    DayOfWeek.FRIDAY to "Piątek",
    DayOfWeek.SATURDAY to "Sobota",
    DayOfWeek.SUNDAY to "Niedziela"
)

internal fun classCountLabel(count: Int): String = when {
    count == 1 -> "1 zajęcie"
    count in 2..4 -> "$count zajęcia"
    else -> "$count zajęć"
}

internal fun PlannedOccurrence.toUi(conflictLabel: String?): ClassItemUi {
    val cancelled = occurrenceChange?.kind == OccurrenceChangeKind.CANCELLED
    val modified = occurrenceChange?.kind == OccurrenceChangeKind.MODIFIED
    val oneOff = classItem.recurrence == Recurrence.ONCE
    return ClassItemUi(
        id = id,
        name = name,
        type = classItem.type,
        courseName = studyProgram?.name.orEmpty(),
        courseColor = studyProgram?.color,
        startTime = startTime.toString(),
        endTime = endTime.toString(),
        room = room?.trim()?.ifEmpty { null },
        building = building,
        teacherName = teacherName,
        weekLabel = when (classItem.recurrence) {
            Recurrence.A_WEEK -> "Tydzień A"
            Recurrence.B_WEEK -> "Tydzień B"
            Recurrence.ONCE -> "Jednorazowe"
            Recurrence.EVERY_WEEK -> null
        },
        classNote = classNote?.takeIf { it.isNotBlank() },
        occurrenceNote = occurrenceNoteBody?.takeIf { it.isNotBlank() },
        statusBadge = when {
            cancelled -> "Odwołane"
            oneOff -> "Jednorazowe"
            modified -> "Zmienione"
            else -> null
        },
        isCancelled = cancelled,
        isModified = modified,
        isOneOff = oneOff,
        conflictLabel = conflictLabel
    )
}
