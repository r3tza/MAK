package dev.retza.mak.widget

import dev.retza.mak.domain.ActivePlan
import dev.retza.mak.domain.Collision
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
        val weekLabel = plan.schedule.weekType?.let { "Tydzień ${it.name}" } ?: "Poza semestrem"
        if (plan.schedule.weekType == null) {
            return WidgetUiState.OutsideSemester(dateLabel, semesterName)
        }
        if (plan.schedule.occurrences.isEmpty()) {
            return WidgetUiState.EmptyDay(dateLabel, weekLabel)
        }

        val conflictsByOccurrence = widgetConflicts(plan.collisions)
        return WidgetUiState.Ready(
            dateLabel = dateLabel,
            weekLabel = weekLabel,
            collisionCount = plan.collisions.map(::widgetCollisionKey).toSet().size,
            items = plan.schedule.occurrences.map { occurrence ->
                WidgetOccurrenceUi(
                    id = occurrence.id,
                    startTime = occurrence.startTime.toString(),
                    endTime = occurrence.endTime.toString(),
                    name = occurrence.name,
                    courseName = occurrence.course?.name.orEmpty(),
                    courseColor = occurrence.course?.color,
                    roomLabel = listOfNotNull(
                        occurrence.room?.trim()?.ifEmpty { null } ?: "Sala niepodana",
                        occurrence.building
                    ).joinToString(", "),
                    teacherName = occurrence.teacher?.name,
                    conflicts = conflictsByOccurrence[occurrence.id].orEmpty(),
                    hasNote = !occurrence.classNote.isNullOrBlank() ||
                        !occurrence.occurrenceNoteBody.isNullOrBlank()
                )
            }
        )
    }
}

private fun widgetCollisionKey(collision: Collision): String {
    val occurrenceIds = listOf(collision.first.id, collision.second.id).sorted()
    return listOf(
        occurrenceIds[0],
        occurrenceIds[1],
        collision.overlapStart,
        collision.overlapEnd
    ).joinToString("|")
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
