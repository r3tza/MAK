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
        resolve(inputs.data.visibleTo(inputs.display), date, inputs.display.minimumBreakMinutes)

    fun resolve(data: ActivePlanData, date: LocalDate, display: PlanDisplaySettings): ActivePlan =
        resolve(data.visibleTo(display), date, display.minimumBreakMinutes)

    /** For a caller that resolves many days of one plan and filters it once. */
    fun resolve(visible: VisiblePlanData, date: LocalDate, minimumBreakMinutes: Int): ActivePlan {
        val schedule = schedule(visible, date)
        return ActivePlan(
            schedule = schedule,
            collisions = collisionDetector.detect(schedule, minimumBreakMinutes)
        )
    }

    /** The classes of [date] without collisions, for views that only mark days. */
    fun schedule(visible: VisiblePlanData, date: LocalDate): ResolvedSchedule {
        val data = visible.data
        return resolver.resolve(
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
}

/** Plan data without the classes of study programs hidden on this phone; only [visibleTo] makes one. */
@JvmInline
value class VisiblePlanData internal constructor(val data: ActivePlanData)

/**
 * The plan without the classes of study programs hidden on this phone. Their assignments stay, so the
 * week of the semester is still known when every program is hidden.
 */
fun ActivePlanData.visibleTo(display: PlanDisplaySettings): VisiblePlanData {
    if (display.hiddenProgramIds.isEmpty()) return VisiblePlanData(this)
    val hiddenAssignmentIds = hiddenAssignments(display).mapTo(mutableSetOf()) { it.id }
    return VisiblePlanData(copy(classes = classes.filterNot { it.semesterProgramId in hiddenAssignmentIds }))
}

/** Assignments of the active semester whose study program is hidden on this phone. */
fun ActivePlanData.hiddenAssignments(display: PlanDisplaySettings): List<SemesterProgram> =
    semesterPrograms.filter { it.studyProgramId in display.hiddenProgramIds }

/** Assignments shown on this phone: filter options and the calendar legend leave out hidden ones. */
fun ActivePlanData.visibleAssignments(display: PlanDisplaySettings): List<SemesterProgram> =
    semesterPrograms.filterNot { it.studyProgramId in display.hiddenProgramIds }

/** True when the semester has study programs and every one of them is hidden on this phone. */
fun ActivePlanData.allProgramsHidden(display: PlanDisplaySettings): Boolean =
    semesterPrograms.isNotEmpty() && semesterPrograms.all { it.studyProgramId in display.hiddenProgramIds }
