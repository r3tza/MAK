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
        val schedule = schedule(data.visibleTo(display), date)
        return ActivePlan(
            schedule = schedule,
            collisions = collisionDetector.detect(schedule, display.minimumBreakMinutes)
        )
    }

    /** The classes of [date] without collisions, for views that only mark days; [data] is already filtered. */
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

/**
 * The plan without the classes of study programs hidden on this phone. Their assignments stay, so the
 * week of the semester is still known when every program is hidden.
 */
fun ActivePlanData.visibleTo(display: PlanDisplaySettings): ActivePlanData {
    if (display.hiddenProgramIds.isEmpty()) return this
    val hiddenAssignmentIds = hiddenAssignments(display).mapTo(mutableSetOf()) { it.id }
    return copy(classes = classes.filterNot { it.semesterProgramId in hiddenAssignmentIds })
}

/** Assignments of the active semester whose study program is hidden on this phone. */
fun ActivePlanData.hiddenAssignments(display: PlanDisplaySettings): List<SemesterProgram> =
    semesterPrograms.filter { it.studyProgramId in display.hiddenProgramIds }

/** True when the semester has study programs and every one of them is hidden on this phone. */
fun ActivePlanData.allProgramsHidden(display: PlanDisplaySettings): Boolean =
    semesterPrograms.isNotEmpty() && hiddenAssignments(display).size == semesterPrograms.size
