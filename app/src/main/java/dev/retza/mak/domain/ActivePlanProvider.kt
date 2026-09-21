package dev.retza.mak.domain

import java.time.LocalDate

data class ActivePlanData(
    val semester: Semester,
    val classes: Collection<ClassItem>,
    val courses: Collection<Course> = emptyList(),
    val teachers: Collection<Teacher> = emptyList(),
    val weekOverrides: Collection<WeekOverride> = emptyList(),
    val occurrenceChanges: Collection<OccurrenceChange> = emptyList(),
    val occurrenceNotes: Collection<OccurrenceNote> = emptyList()
)

data class ActivePlan(
    val schedule: ResolvedSchedule,
    val collisions: List<Collision>
)

@org.koin.core.annotation.Single
class ActivePlanProvider(
    private val resolver: ScheduleResolver = ScheduleResolver(),
    private val collisionDetector: CollisionDetector = CollisionDetector()
) {
    fun resolve(data: ActivePlanData, date: LocalDate): ActivePlan {
        val schedule = resolver.resolve(
            date = date,
            semester = data.semester,
            classes = data.classes,
            courses = data.courses,
            teachers = data.teachers,
            weekOverrides = data.weekOverrides,
            occurrenceChanges = data.occurrenceChanges,
            occurrenceNotes = data.occurrenceNotes
        )
        return ActivePlan(
            schedule = schedule,
            collisions = collisionDetector.detect(schedule)
        )
    }
}
