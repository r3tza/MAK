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
        occurrenceNotes: Collection<OccurrenceNote> = emptyList(),
        freeDays: Collection<FreeDay> = emptyList()
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
        val freeDaysByCalendar = freeDays.groupBy { it.academicCalendarId }

        fun isFreeDay(calendarId: Long, day: LocalDate) =
            freeDaysByCalendar[calendarId].orEmpty().any { day in it }

        val resolved = semesterClasses
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
                    notesByOccurrence = notesByOccurrence,
                    isFree = { isFreeDay(calendar.id, it) }
                )
            }
            .flatten()
            .sortedWith(compareBy<PlannedOccurrence> { it.startTime }.thenBy { it.name }.thenBy { it.id })
        val (cancelled, occurrences) = resolved.partition { it.status == OccurrenceStatus.CANCELLED }

        val weeks = semesterCalendars
            .mapNotNull { calendar ->
                weekCalculator.calculate(calendar, date, weekOverrides)?.let { calendar.id to it }
            }
            .toMap()
        val weekTypes = weeks.values.map { it.weekType }.distinct()
        val weekStatus = when {
            weekTypes.isEmpty() -> WeekStatus.OutsideSemester
            weekTypes.size > 1 -> WeekStatus.Mixed
            else -> WeekStatus.Single(weekTypes.single())
        }

        return ResolvedSchedule(
            date = date,
            semesterId = semester.id,
            occurrences = occurrences,
            weekStatus = weekStatus,
            cancelled = cancelled,
            weeksByCalendarId = weeks,
            freeDaysByCalendarId = weeks.keys.mapNotNull { id ->
                freeDaysByCalendar[id]?.firstOrNull { date in it }?.let { id to it }
            }.toMap()
        )
    }

    private fun resolveClass(
        classItem: ClassItem,
        calendar: AcademicCalendar,
        date: LocalDate,
        weekOverrides: Collection<WeekOverride>,
        changesByDate: Map<LocalDate, OccurrenceChange>,
        studyPrograms: Map<Long, StudyProgram>,
        assignments: Map<Long, SemesterProgram>,
        notesByOccurrence: Map<Pair<Long, LocalDate>, OccurrenceNote>,
        isFree: (LocalDate) -> Boolean
    ): List<PlannedOccurrence> {
        if (classItem.recurrence == Recurrence.ONCE) {
            if (!classItem.hasBaseOccurrenceOn(date, calendar, weekOverrides, weekCalculator)) return emptyList()
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

        val sourceDates = buildSet {
            if (classItem.hasBaseOccurrenceOn(date, calendar, weekOverrides, weekCalculator)) add(date)
            changesByDate.values
                .filter { it.kind == OccurrenceChangeKind.MODIFIED && it.targetDate == date }
                .map { it.originalDate }
                .filter { classItem.hasBaseOccurrenceOn(it, calendar, weekOverrides, weekCalculator) }
                .forEach(::add)
        }

        return sourceDates.mapNotNull { sourceDate ->
            val change = changesByDate[sourceDate]
            val actualDate = change?.targetDate ?: sourceDate
            if (actualDate != date) return@mapNotNull null
            // A moved occurrence is how a class is made up, so only one that stays on a free day is skipped.
            val isMoved = change?.targetDate != null && change.targetDate != sourceDate
            if (!isMoved && isFree(sourceDate)) return@mapNotNull null

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

    private fun render(
        classItem: ClassItem,
        actualDate: LocalDate,
        originalDate: LocalDate,
        change: OccurrenceChange?,
        studyPrograms: Map<Long, StudyProgram>,
        assignments: Map<Long, SemesterProgram>,
        notesByOccurrence: Map<Pair<Long, LocalDate>, OccurrenceNote>
    ): PlannedOccurrence {
        // Occurrence notes belong to the occurrence, so they follow it when it is moved.
        val occurrenceNote = notesByOccurrence[classItem.id to originalDate]
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
