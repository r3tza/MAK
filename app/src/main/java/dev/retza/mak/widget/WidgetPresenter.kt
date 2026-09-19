package dev.retza.mak.widget

import dev.retza.mak.domain.ActivePlan
import dev.retza.mak.domain.CollisionRange
import dev.retza.mak.domain.collisionRanges
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class WidgetPresenter {
    fun present(
        date: LocalDate,
        semesterName: String,
        plan: ActivePlan
    ): WidgetUiState {
        val dateLabel = widgetDateLabel(date)
        val weekLabel = plan.schedule.weekType?.let { "Tydzień ${it.name}" } ?: "Poza semestrem"
        if (plan.schedule.weekType == null) {
            return WidgetUiState.OutsideSemester(dateLabel, semesterName)
        }
        if (plan.schedule.occurrences.isEmpty()) {
            return WidgetUiState.EmptyDay(dateLabel, weekLabel)
        }

        val rangesByOccurrence = collisionRanges(plan.collisions)
        return WidgetUiState.Ready(
            dateLabel = dateLabel,
            weekLabel = weekLabel,
            items = plan.schedule.occurrences.map { occurrence ->
                WidgetOccurrenceUi(
                    id = occurrence.id,
                    startTime = occurrence.startTime.toString(),
                    endTime = occurrence.endTime.toString(),
                    name = occurrence.name,
                    courseName = occurrence.course?.name.orEmpty(),
                    courseColor = occurrence.course?.color,
                    roomLabel = listOfNotNull(
                        occurrence.room ?: "Sala niepodana",
                        occurrence.building
                    ).joinToString(", "),
                    teacherName = occurrence.teacher?.name,
                    conflictLabel = rangesByOccurrence[occurrence.id]
                        ?.let(::formatConflictLabel),
                    hasNote = !occurrence.classNote.isNullOrBlank() ||
                        !occurrence.occurrenceNoteBody.isNullOrBlank()
                )
            }
        )
    }
}

private fun formatConflictLabel(ranges: List<CollisionRange>): String {
    val text = ranges.joinToString(", ") {
        "${it.start.format(conflictTimeFormatter)}-${it.end.format(conflictTimeFormatter)}"
    }
    return if (ranges.size == 1) "Kolizja $text" else "Kolizje: $text"
}

internal fun widgetDateLabel(date: LocalDate): String = date.format(widgetDateFormatter)

private val widgetDateFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("pl-PL"))
private val conflictTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
