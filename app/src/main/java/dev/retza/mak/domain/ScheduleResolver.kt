package dev.retza.mak.domain

import java.time.LocalDate

class ScheduleResolver(
    private val weekCalculator: WeekCalculator = WeekCalculator()
) {
    fun resolve(
        date: LocalDate,
        semester: Semester,
        classes: Collection<ClassItem>,
        courses: Collection<StudyProgram> = emptyList(),
        semesterPrograms: Collection<SemesterProgram> = emptyList(),
        calendars: Collection<AcademicCalendar> = emptyList(),
        teachers: Collection<Teacher> = emptyList(),
        weekOverrides: Collection<WeekOverride> = emptyList(),
        occurrenceChanges: Collection<OccurrenceChange> = emptyList(),
        occurrenceNotes: Collection<OccurrenceNote> = emptyList()
    ): ResolvedSchedule {
        val semesterClasses = classes.filter { it.semesterId == semester.id }
        val coursesById = courses.associateBy { it.id }
        val teachersById = teachers.filter { it.semesterId == semester.id }
            .associateBy { it.id }
        val calendarsById = calendars.associateBy { it.id }
        val programs = semesterPrograms.filter { it.semesterId == semester.id }

        fun calendarFor(classItem: ClassItem): AcademicCalendar? {
            val program = programs.firstOrNull { it.studyProgramId == classItem.courseId }
            return program?.let { calendarsById[it.academicCalendarId] }
        }

        val semesterCalendars = programs.mapNotNull { calendarsById[it.academicCalendarId] }
            .distinctBy { it.id }
        val fallbackCalendar = semester.toAcademicCalendar()
        val primaryCalendar = semesterCalendars.minByOrNull { it.id } ?: fallbackCalendar

        val changesByClass = occurrenceChanges
            .groupBy { it.classId }
            .mapValues { (_, changes) -> changes.associateBy { it.originalDate } }
        val notesByOccurrence = occurrenceNotes
            .groupBy { it.classId to it.occurrenceDate }
            .mapValues { (_, notes) -> notes.last() }

        val occurrences = semesterClasses
            .flatMap { classItem ->
                resolveClass(
                    classItem = classItem,
                    calendar = calendarFor(classItem) ?: fallbackCalendar,
                    date = date,
                    weekOverrides = weekOverrides,
                    changesByDate = changesByClass[classItem.id].orEmpty(),
                    courses = coursesById,
                    teachers = teachersById,
                    notesByOccurrence = notesByOccurrence
                )
            }
            .sortedWith(compareBy<PlannedOccurrence> { it.startTime }.thenBy { it.name }.thenBy { it.id })

        val week = (semesterCalendars + fallbackCalendar)
            .mapNotNull { weekCalculator.calculate(it, date, weekOverrides) }
            .firstOrNull()
            ?: weekCalculator.calculate(primaryCalendar, date, weekOverrides)

        return ResolvedSchedule(
            date = date,
            semesterId = semester.id,
            week = week,
            occurrences = occurrences
        )
    }

    fun resolveOccurrences(
        date: LocalDate,
        semester: Semester,
        classes: Collection<ClassItem>,
        courses: Collection<StudyProgram> = emptyList(),
        semesterPrograms: Collection<SemesterProgram> = emptyList(),
        calendars: Collection<AcademicCalendar> = emptyList(),
        teachers: Collection<Teacher> = emptyList(),
        weekOverrides: Collection<WeekOverride> = emptyList(),
        occurrenceChanges: Collection<OccurrenceChange> = emptyList(),
        occurrenceNotes: Collection<OccurrenceNote> = emptyList()
    ): List<PlannedOccurrence> = resolve(
        date,
        semester,
        classes,
        courses,
        semesterPrograms,
        calendars,
        teachers,
        weekOverrides,
        occurrenceChanges,
        occurrenceNotes
    ).occurrences

    private fun resolveClass(
        classItem: ClassItem,
        calendar: AcademicCalendar,
        date: LocalDate,
        weekOverrides: Collection<WeekOverride>,
        changesByDate: Map<LocalDate, OccurrenceChange>,
        courses: Map<String, StudyProgram>,
        teachers: Map<String, Teacher>,
        notesByOccurrence: Map<Pair<String, LocalDate>, OccurrenceNote>
    ): List<PlannedOccurrence> {
        if (classItem.recurrence == Recurrence.ONCE) {
            if (classItem.date != date) return emptyList()
            return listOf(
                render(
                    classItem = classItem,
                    actualDate = date,
                    originalDate = date,
                    change = null,
                    courses = courses,
                    teachers = teachers,
                    notesByOccurrence = notesByOccurrence
                )
            )
        }

        val week = weekCalculator.calculate(calendar, date, weekOverrides)
        val sourceDates = buildSet {
            if (week != null && isBaseOccurrence(classItem, date, week)) add(date)
            changesByDate.values
                .filter { it.kind == OccurrenceChangeKind.MODIFIED && it.targetDate == date }
                .map { it.originalDate }
                .filter {
                    val sourceWeek = weekCalculator.calculate(calendar, it, weekOverrides)
                    sourceWeek != null && isBaseOccurrence(classItem, it, sourceWeek)
                }
                .forEach(::add)
        }

        return sourceDates.mapNotNull { sourceDate ->
            val change = changesByDate[sourceDate]
            if (change?.kind == OccurrenceChangeKind.CANCELLED) return@mapNotNull null

            val actualDate = change?.targetDate ?: sourceDate
            if (actualDate != date) return@mapNotNull null

            render(
                classItem = classItem,
                actualDate = actualDate,
                originalDate = sourceDate,
                change = change,
                courses = courses,
                teachers = teachers,
                notesByOccurrence = notesByOccurrence
            )
        }
    }

    private fun isBaseOccurrence(
        classItem: ClassItem,
        date: LocalDate,
        week: WeekCalculation
    ): Boolean {
        if (classItem.dayOfWeek != date.dayOfWeek) return false
        return when (classItem.recurrence) {
            Recurrence.EVERY_WEEK -> true
            Recurrence.A_WEEK -> week.weekType == WeekType.A
            Recurrence.B_WEEK -> week.weekType == WeekType.B
            Recurrence.ONCE -> false
        }
    }

    private fun render(
        classItem: ClassItem,
        actualDate: LocalDate,
        originalDate: LocalDate,
        change: OccurrenceChange?,
        courses: Map<String, StudyProgram>,
        teachers: Map<String, Teacher>,
        notesByOccurrence: Map<Pair<String, LocalDate>, OccurrenceNote>
    ): PlannedOccurrence {
        val teacherId = change?.teacherId ?: classItem.teacherId
        val occurrenceNote = notesByOccurrence[classItem.id to actualDate]
        return PlannedOccurrence(
            classItem = classItem,
            date = actualDate,
            originalDate = originalDate,
            startTime = change?.startTime ?: classItem.startTime,
            endTime = change?.endTime ?: classItem.endTime,
            room = (change?.room ?: classItem.room)?.trim()?.ifEmpty { null },
            building = change?.building ?: classItem.building,
            teacher = teacherId?.let(teachers::get),
            course = courses[classItem.courseId],
            classNote = classItem.classNote,
            occurrenceNote = occurrenceNote,
            occurrenceChange = change
        )
    }
}
