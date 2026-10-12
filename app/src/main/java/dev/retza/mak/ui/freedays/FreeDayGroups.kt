package dev.retza.mak.ui.freedays

import dev.retza.mak.data.repository.FreeDayRecord
import dev.retza.mak.data.repository.SemesterConfiguration
import dev.retza.mak.text.polishPlural
import dev.retza.mak.ui.daysLabel
import java.time.LocalDate

/** Free days saved together: the same name and dates in one or more calendars (`FEATURES.md`, „Dni wolne”). */
data class FreeDayGroup(
    val ids: List<Long>,
    val calendarIds: List<Long>,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val name: String?
)

fun groupFreeDays(records: List<FreeDayRecord>): List<FreeDayGroup> =
    records
        .groupBy { Triple(it.name, it.startDate, it.endDate) }
        .map { (key, rows) ->
            FreeDayGroup(
                ids = rows.map { it.id }.sorted(),
                calendarIds = rows.map { it.academicCalendarId }.sorted(),
                startDate = key.second,
                endDate = key.third,
                name = key.first
            )
        }
        .sortedWith(compareBy({ it.startDate }, { it.endDate }, { it.name.orEmpty() }))

/** Counts ranges and the distinct dates they cover, so overlapping ranges do not count a day twice. */
fun freeDaysSummary(groups: List<FreeDayGroup>): String {
    if (groups.isEmpty()) return "Brak"
    val dates = groups.flatMapTo(HashSet()) { group ->
        generateSequence(group.startDate) { it.plusDays(1) }.takeWhile { !it.isAfter(group.endDate) }
    }
    val rangeNoun = polishPlural(groups.size, "zakres", "zakresy", "zakresów")
    return "${groups.size} $rangeNoun, ${daysLabel(dates.size)}"
}

/** A calendar named by the study programs that use it, in assignment order. */
internal fun SemesterConfiguration.calendarName(calendarId: Long): String =
    programs
        .filter { it.academicCalendarId == calendarId }
        .sortedBy { it.id }
        .mapNotNull { assignment -> studyPrograms.firstOrNull { it.id == assignment.studyProgramId }?.name }
        .joinToString(", ")
        .ifEmpty { "Kalendarz" }
