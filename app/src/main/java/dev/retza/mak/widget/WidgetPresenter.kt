package dev.retza.mak.widget

import dev.retza.mak.domain.ActivePlan
import dev.retza.mak.domain.Collision
import dev.retza.mak.domain.uniqueCollisionCount
import dev.retza.mak.domain.PlannedOccurrence
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class WidgetPresenter {
    fun present(
        date: LocalDate,
        semesterName: String,
        plan: ActivePlan
    ): WidgetUiState {
        val dateLabel = widgetDateLabel(date)
        val weekType = plan.schedule.weekType
        val mixedWeekTypes = plan.schedule.hasMixedWeekTypes
        if (weekType == null && !mixedWeekTypes) {
            return WidgetUiState.OutsideSemester(dateLabel, semesterName)
        }
        val weekLabel = if (mixedWeekTypes) {
            "Różne tygodnie"
        } else {
            weekType?.let { "Tydzień ${it.name}" } ?: "Poza semestrem"
        }
        if (plan.schedule.occurrences.isEmpty()) {
            return WidgetUiState.EmptyDay(dateLabel, weekLabel)
        }

        val conflictsByOccurrence = widgetConflicts(plan.collisions)
        return WidgetUiState.Ready(
            dateLabel = dateLabel,
            weekLabel = weekLabel,
            collisionCount = uniqueCollisionCount(plan.collisions),
            items = plan.schedule.occurrences.map { occurrence ->
                WidgetOccurrenceUi(
                    id = occurrence.id,
                    startTime = occurrence.startTime.toString(),
                    endTime = occurrence.endTime.toString(),
                    name = occurrence.name,
                    courseName = occurrence.studyProgram?.name.orEmpty(),
                    courseColor = occurrence.studyProgram?.color,
                    roomLabel = listOfNotNull(
                        occurrence.room?.trim()?.ifEmpty { null } ?: "Sala niepodana",
                        occurrence.building
                    ).joinToString(", "),
                    teacherName = occurrence.teacherName,
                    conflicts = conflictsByOccurrence[occurrence.id].orEmpty(),
                    hasNote = !occurrence.classNote.isNullOrBlank() ||
                        !occurrence.occurrenceNoteBody.isNullOrBlank()
                )
            }
        )
    }
}

internal fun widgetDateLabel(date: LocalDate): String = date.format(widgetDateFormatter)

private data class WidgetConflictCandidate(
    val start: LocalTime,
    val end: LocalTime,
    val otherOccurrenceName: String
)

private fun widgetConflicts(
    collisions: Collection<Collision>
): Map<String, List<WidgetConflictUi>> {
    val byOccurrence = linkedMapOf<String, MutableSet<WidgetConflictCandidate>>()
    collisions.forEach { collision ->
        byOccurrence.addConflict(collision.first, collision.overlapStart, collision.overlapEnd, collision.second)
        byOccurrence.addConflict(collision.second, collision.overlapStart, collision.overlapEnd, collision.first)
    }
    return byOccurrence.mapValues { (_, candidates) ->
        candidates
            .sortedWith(compareBy<WidgetConflictCandidate> { it.start }.thenBy { it.end }
                .thenBy { it.otherOccurrenceName })
            .map { candidate ->
                WidgetConflictUi(
                    timeRange = "${candidate.start.format(conflictTimeFormatter)}-" +
                        candidate.end.format(conflictTimeFormatter),
                    otherOccurrenceName = candidate.otherOccurrenceName
                )
            }
    }
}

private fun MutableMap<String, MutableSet<WidgetConflictCandidate>>.addConflict(
    occurrence: PlannedOccurrence,
    start: LocalTime,
    end: LocalTime,
    otherOccurrence: PlannedOccurrence
) {
    getOrPut(occurrence.id) { linkedSetOf() }.add(
        WidgetConflictCandidate(
            start = start,
            end = end,
            otherOccurrenceName = otherOccurrence.name
        )
    )
}

private val widgetDateFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("pl-PL"))
private val conflictTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
