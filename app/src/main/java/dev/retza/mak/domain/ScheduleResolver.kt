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
        weekOverrides: Collection<WeekOverride> = emptyList(),
        occurrenceChanges: Collection<OccurrenceChange> = emptyList(),
        occurrenceNotes: Collection<OccurrenceNote> = emptyList()
    ): ResolvedSchedule {
        val semesterClasses = classes.filter { it.semesterId == semester.id }
        val studyProgramsById = courses.associateBy { it.id }
        val calendarsById = calendars.associateBy { it.id }
        val assignments = semesterPrograms.filter { it.semesterId == semester.id }
        val assignmentsById = assignments.associateBy { it.id }

        fun calendarFor(classItem: ClassItem): AcademicCalendar? {
            val assignment = assignmentsById[classItem.semesterProgramId] ?: return null
            return calendarsById[assignment.academicCalendarId]
        }

        val semesterCalendars = assignments.mapNotNull { calendarsById[it.academicCalendarId] }
            .distinctBy { it.id }

        val changesByClass = occurrenceChanges
            .groupBy { it.classId }
            .mapValues { (_, changes) -> changes.associateBy { it.originalDate } }
        val notesByOccurrence = occurrenceNotes
            .groupBy { it.classId to it.occurrenceDate }
            .mapValues { (_, notes) -> notes.last() }

        val occurrences = semesterClasses
            .mapNotNull { classItem ->
                val calendar = calendarFor(classItem) ?: return@mapNotNull null
                resolveClass(
                    classItem = classItem,
                    calendar = calendar,
                    date = date,
                    weekOverrides = weekOverrides,
                    changesByDate = changesByClass[classItem.id].orEmpty(),
                    studyPrograms = studyProgramsById,
                    assignments = assignmentsById,
                    notesByOccurrence = notesByOccurrence
                )
            }
            .flatten()
            .sortedWith(compareBy<PlannedOccurrence> { it.startTime }.thenBy { it.name }.thenBy { it.id })

        val weeks = semesterCalendars
            .mapNotNull { calendar ->
                weekCalculator.calculate(calendar, date, weekOverrides)?.let { calendar.id to it }
            }
            .toMap()
        val distinctWeekTypes = weeks.values.map { it.weekType }.distinct()
        val hasMixedWeekTypes = distinctWeekTypes.size > 1
        val sharedWeek = if (hasMixedWeekTypes) null else weeks.values.firstOrNull()

        return ResolvedSchedule(
            date = date,
            semesterId = semester.id,
            week = sharedWeek,
            occurrences = occurrences,
            hasMixedWeekTypes = hasMixedWeekTypes
        )
    }

    fun resolveOccurrences(
        date: LocalDate,
        semester: Semester,
        classes: Collection<ClassItem>,
        courses: Collection<StudyProgram> = emptyList(),
        semesterPrograms: Collection<SemesterProgram> = emptyList(),
        calendars: Collection<AcademicCalendar> = emptyList(),
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
        studyPrograms: Map<String, StudyProgram>,
        assignments: Map<String, SemesterProgram>,
        notesByOccurrence: Map<Pair<String, LocalDate>, OccurrenceNote>
    ): List<PlannedOccurrence> {
        if (classItem.recurrence == Recurrence.ONCE) {
            if (classItem.date != date) return emptyList()
            if (date.isBefore(calendar.startDate) || date.isAfter(calendar.endDate)) return emptyList()
            return listOf(
                render(
                    classItem = classItem,
                    actualDate = date,
                    originalDate = date,
                    change = null,
                    studyPrograms = studyPrograms,
                    assignments = assignments,
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
                studyPrograms = studyPrograms,
                assignments = assignments,
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
        studyPrograms: Map<String, StudyProgram>,
        assignments: Map<String, SemesterProgram>,
        notesByOccurrence: Map<Pair<String, LocalDate>, OccurrenceNote>
    ): PlannedOccurrence {
        val occurrenceNote = notesByOccurrence[classItem.id to actualDate]
        val studyProgram = assignments[classItem.semesterProgramId]
            ?.studyProgramId
            ?.let(studyPrograms::get)
        return PlannedOccurrence(
            classItem = classItem,
            date = actualDate,
            originalDate = originalDate,
            startTime = change?.startTime ?: classItem.startTime,
            endTime = change?.endTime ?: classItem.endTime,
            room = (change?.room ?: classItem.room)?.trim()?.ifEmpty { null },
            building = change?.building ?: classItem.building,
            teacherName = change?.teacherName ?: classItem.teacherName,
            studyProgram = studyProgram,
            classNote = classItem.classNote,
            occurrenceNote = occurrenceNote,
            occurrenceChange = change
        )
    }
}
