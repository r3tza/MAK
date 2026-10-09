package dev.retza.mak.domain

import java.time.LocalDate

data class ActivePlanData(
    val semester: Semester,
    val classes: Collection<ClassItem>,
    val courses: Collection<StudyProgram> = emptyList(),
    val semesterPrograms: Collection<SemesterProgram> = emptyList(),
    val calendars: Collection<AcademicCalendar> = emptyList(),
    val weekOverrides: Collection<WeekOverride> = emptyList(),
    val occurrenceChanges: Collection<OccurrenceChange> = emptyList(),
    val occurrenceNotes: Collection<OccurrenceNote> = emptyList()
)

/** The plan data together with the phone settings that change how it resolves. */
data class ActivePlanInputs(
    val data: ActivePlanData,
    val display: PlanDisplaySettings
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
    fun resolve(inputs: ActivePlanInputs, date: LocalDate): ActivePlan =
        resolve(inputs.data, date, inputs.display)

    fun resolve(data: ActivePlanData, date: LocalDate, display: PlanDisplaySettings): ActivePlan {
        val schedule = schedule(data, date)
        return ActivePlan(
            schedule = schedule,
            collisions = collisionDetector.detect(schedule, display.minimumBreakMinutes)
        )
    }

    /** The classes of [date] without collisions, for views that only mark days. */
    fun schedule(data: ActivePlanData, date: LocalDate): ResolvedSchedule =
        resolver.resolve(
            date = date,
            semester = data.semester,
            classes = data.classes,
            courses = data.courses,
            semesterPrograms = data.semesterPrograms,
            calendars = data.calendars,
            weekOverrides = data.weekOverrides,
            occurrenceChanges = data.occurrenceChanges,
            occurrenceNotes = data.occurrenceNotes
        )
}
