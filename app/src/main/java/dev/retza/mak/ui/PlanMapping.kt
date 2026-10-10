package dev.retza.mak.ui

import dev.retza.mak.text.polishPlural
import dev.retza.mak.domain.PlannedOccurrence
import dev.retza.mak.domain.Recurrence
import dev.retza.mak.domain.WeekStatus
import dev.retza.mak.ui.components.ClassItemUi
import dev.retza.mak.ui.components.toUi
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", AppLocale)
internal val todayTitleFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", AppLocale)
internal val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", AppLocale)
internal val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMMM", AppLocale)
internal val shortDateWithYearFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", AppLocale)
internal val longDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", AppLocale)
internal val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
internal val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", AppLocale)

internal fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

internal fun String.toLocalTimeOrNull(): LocalTime? = runCatching { LocalTime.parse(this) }.getOrNull()
internal val dayNames = linkedMapOf(
    DayOfWeek.MONDAY to "Poniedziałek",
    DayOfWeek.TUESDAY to "Wtorek",
    DayOfWeek.WEDNESDAY to "Środa",
    DayOfWeek.THURSDAY to "Czwartek",
    DayOfWeek.FRIDAY to "Piątek",
    DayOfWeek.SATURDAY to "Sobota",
    DayOfWeek.SUNDAY to "Niedziela"
)

// Two-letter names fit seven day cells at 320 dp and a large font scale.
internal val shortDayNames = linkedMapOf(
    DayOfWeek.MONDAY to "Pn",
    DayOfWeek.TUESDAY to "Wt",
    DayOfWeek.WEDNESDAY to "Śr",
    DayOfWeek.THURSDAY to "Cz",
    DayOfWeek.FRIDAY to "Pt",
    DayOfWeek.SATURDAY to "So",
    DayOfWeek.SUNDAY to "Nd"
)

internal fun classCountLabel(count: Int): String =
    "$count ${polishPlural(count, "zajęcie", "zajęcia", "zajęć")}"

internal fun PlannedOccurrence.toUi(conflictLabel: String?, conflictWith: String? = null): ClassItemUi {
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
        status = status.toUi(),
        conflictLabel = conflictLabel,
        conflictWith = conflictWith
    )
}

internal fun WeekStatus.label(): String = when (this) {
    is WeekStatus.Single -> "Tydzień ${type.name}"
    WeekStatus.Mixed -> "Różne tygodnie"
    WeekStatus.OutsideSemester -> "Poza semestrem"
}
