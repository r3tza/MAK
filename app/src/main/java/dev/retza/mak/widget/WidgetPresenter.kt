package dev.retza.mak.widget

import dev.retza.mak.domain.ActivePlan
import dev.retza.mak.domain.WeekStatus
import dev.retza.mak.domain.collisionLabels
import dev.retza.mak.domain.uniqueCollisionCount
import java.time.LocalDate
import java.time.LocalTime
import dev.retza.mak.ui.label
import dev.retza.mak.ui.todayTitleFormatter

class WidgetPresenter {
    fun present(
        date: LocalDate,
        semesterName: String,
        plan: ActivePlan,
        now: LocalTime
    ): WidgetUiState {
        val dateLabel = widgetDateLabel(date)
        val weekStatus = plan.schedule.weekStatus
        if (weekStatus == WeekStatus.OutsideSemester) {
            return WidgetUiState.OutsideSemester(dateLabel, semesterName)
        }
        val weekLabel = weekStatus.label()
        if (plan.schedule.occurrences.isEmpty()) {
            return WidgetUiState.EmptyDay(dateLabel, weekLabel)
        }

        val labelsByOccurrence = collisionLabels(plan.collisions)
        val nextId = plan.schedule.occurrences.firstOrNull { it.startTime.isAfter(now) }?.id
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
                    conflictLabel = labelsByOccurrence[occurrence.id],
                    classNote = occurrence.classNote?.trim()?.ifEmpty { null },
                    occurrenceNote = occurrence.occurrenceNoteBody?.trim()?.ifEmpty { null },
                    phase = when {
                        !now.isBefore(occurrence.endTime) -> WidgetOccurrencePhase.Past
                        !now.isBefore(occurrence.startTime) -> WidgetOccurrencePhase.Current
                        occurrence.id == nextId -> WidgetOccurrencePhase.Next
                        else -> WidgetOccurrencePhase.Scheduled
                    }
                )
            }
        )
    }
}

internal fun widgetDateLabel(date: LocalDate): String = date.format(todayTitleFormatter)
