package dev.retza.mak.export

import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceChangeKind
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.Recurrence
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekOverrideEntity
import dev.retza.mak.data.entity.WeekOverrideScope
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.SemesterBackup
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

sealed interface ImportSnapshotResult {
    data class Ready(val data: BackupData) : ImportSnapshotResult

    data class Invalid(val errors: List<String>) : ImportSnapshotResult
}

object ExportImporter {
    fun prepare(snapshot: ExportSnapshot): ImportSnapshotResult {
        if (snapshot.schemaVersion != ExportSchema.VERSION) {
            return ImportSnapshotResult.Invalid(
                listOf(
                    "Nieobsługiwana wersja pliku: ${snapshot.schemaVersion}. " +
                        "Obsługiwana wersja to ${ExportSchema.VERSION}."
                )
            )
        }

        val errors = mutableListOf<String>()
        val programIds = snapshot.studyPrograms.map { it.id }
        if (programIds.size != programIds.toSet().size) {
            errors += "Plik zawiera kierunki o powtórzonym identyfikatorze."
        }
        snapshot.studyPrograms.forEach { program ->
            if (program.name.isBlank()) {
                errors += "Kierunek ${program.id} nie ma nazwy."
            }
        }
        val knownProgramIds = programIds.toSet()

        val semesterIds = snapshot.semesters.map { it.id }
        if (semesterIds.size != semesterIds.toSet().size) {
            errors += "Plik zawiera semestry o powtórzonym identyfikatorze."
        }
        if (snapshot.semesters.count { it.isActive } > 1) {
            errors += "Plik wskazuje więcej niż jeden aktywny semestr."
        }

        checkUnique(
            errors,
            snapshot.semesters.flatMap { it.calendars }.map { it.id },
            "Plik zawiera kalendarze o powtórzonym identyfikatorze."
        )
        checkUnique(
            errors,
            snapshot.semesters.flatMap { it.programs }.map { it.id },
            "Plik zawiera przypisania kierunków o powtórzonym identyfikatorze."
        )
        checkUnique(
            errors,
            snapshot.semesters.flatMap { it.classes }.map { it.id },
            "Plik zawiera zajęcia o powtórzonym identyfikatorze."
        )
        checkUnique(
            errors,
            snapshot.semesters.flatMap { it.weekOverrides }.map { it.id },
            "Plik zawiera korekty tygodni o powtórzonym identyfikatorze."
        )
        checkUnique(
            errors,
            snapshot.semesters.flatMap { it.occurrenceNotes }.map { it.id },
            "Plik zawiera notatki o powtórzonym identyfikatorze."
        )
        checkUnique(
            errors,
            snapshot.semesters.flatMap { it.occurrenceChanges }.map { it.id },
            "Plik zawiera zmiany wystąpień o powtórzonym identyfikatorze."
        )
        checkUnique(
            errors,
            snapshot.semesters.flatMap { it.programs }.map { it.semesterId to it.studyProgramId },
            "Plik przypisuje ten sam kierunek do semestru więcej niż raz."
        )
        checkUnique(
            errors,
            snapshot.semesters.flatMap { it.weekOverrides }
                .map { Triple(it.academicCalendarId, it.weekStartDate, it.scope) },
            "Plik zawiera powtórzoną korektę tygodnia."
        )
        checkUnique(
            errors,
            snapshot.semesters.flatMap { it.occurrenceNotes }
                .map { Triple(it.semesterId, it.classId, it.occurrenceDate) },
            "Plik zawiera powtórzoną notatkę do wystąpienia."
        )
        checkUnique(
            errors,
            snapshot.semesters.flatMap { it.occurrenceChanges }
                .map { Triple(it.semesterId, it.classId, it.originalDate) },
            "Plik zawiera powtórzoną zmianę wystąpienia."
        )

        val semesters = snapshot.semesters.map { semester ->
            mapSemester(semester, knownProgramIds, errors)
        }

        if (errors.isNotEmpty()) {
            return ImportSnapshotResult.Invalid(errors.distinct())
        }

        val programs = snapshot.studyPrograms.map {
            StudyProgramEntity(id = it.id, name = it.name, color = it.color)
        }
        return ImportSnapshotResult.Ready(BackupData(studyPrograms = programs, semesters = semesters))
    }

    private fun mapSemester(
        semester: SemesterSnapshot,
        knownProgramIds: Set<Long>,
        errors: MutableList<String>
    ): SemesterBackup {
        val prefix = "Semestr ${semester.id}"
        if (semester.name.isBlank()) {
            errors += "$prefix nie ma nazwy."
        }

        val calendarIds = semester.calendars.map { it.id }.toSet()
        val calendars = mutableListOf<AcademicCalendarEntity>()
        semester.calendars.forEach { calendar ->
            if (calendar.semesterId != semester.id) {
                errors += "Kalendarz ${calendar.id} należy do innego semestru."
                return@forEach
            }
            val start = calendar.startDate.toLocalDateOrNull()
            val end = calendar.endDate.toLocalDateOrNull()
            val week = calendar.firstWeekType.toEnumOrNull<WeekType>()
            if (start == null || end == null) {
                errors += "Kalendarz ${calendar.id} ma niepoprawną datę."
                return@forEach
            }
            if (end.isBefore(start)) {
                errors += "Kalendarz ${calendar.id} kończy się przed początkiem."
                return@forEach
            }
            if (week == null) {
                errors += "Kalendarz ${calendar.id} ma niepoprawny pierwszy tydzień."
                return@forEach
            }
            calendars += AcademicCalendarEntity(calendar.id, semester.id, start, end, week)
        }

        val programIds = semester.programs.map { it.id }.toSet()
        val programs = mutableListOf<SemesterProgramEntity>()
        semester.programs.forEach { program ->
            if (program.semesterId != semester.id) {
                errors += "Przypisanie ${program.id} należy do innego semestru."
                return@forEach
            }
            if (program.studyProgramId !in knownProgramIds) {
                errors += "Przypisanie ${program.id} wskazuje nieistniejący kierunek."
                return@forEach
            }
            if (program.academicCalendarId !in calendarIds) {
                errors += "Przypisanie ${program.id} wskazuje nieistniejący kalendarz."
                return@forEach
            }
            programs += SemesterProgramEntity(
                id = program.id,
                semesterId = semester.id,
                studyProgramId = program.studyProgramId,
                academicCalendarId = program.academicCalendarId
            )
        }

        val classIds = semester.classes.map { it.id }.toSet()
        val classes = mutableListOf<ClassEntity>()
        semester.classes.forEach { item ->
            if (item.semesterId != semester.id) {
                errors += "Zajęcia ${item.id} należą do innego semestru."
                return@forEach
            }
            if (item.semesterProgramId !in programIds) {
                errors += "Zajęcia ${item.id} wskazują nieistniejące przypisanie kierunku."
                return@forEach
            }
            val day = item.dayOfWeek.toEnumOrNull<DayOfWeek>()
            val start = item.startTime.toLocalTimeOrNull()
            val end = item.endTime.toLocalTimeOrNull()
            val recurrence = item.recurrence.toEnumOrNull<Recurrence>()
            val date = item.date?.toLocalDateOrNull()
            if (item.name.isBlank()) errors += "Zajęcia ${item.id} nie mają nazwy."
            if (day == null) errors += "Zajęcia ${item.id} mają niepoprawny dzień tygodnia."
            if (start == null || end == null) errors += "Zajęcia ${item.id} mają niepoprawną godzinę."
            if (start != null && end != null && !end.isAfter(start)) {
                errors += "Zajęcia ${item.id} kończą się przed początkiem."
            }
            if (recurrence == null) errors += "Zajęcia ${item.id} mają niepoprawny cykl."
            if (recurrence == Recurrence.ONCE && date == null) {
                errors += "Zajęcia jednorazowe ${item.id} nie mają daty."
            }
            if (day == null || start == null || end == null || recurrence == null ||
                !end.isAfter(start) ||
                (recurrence == Recurrence.ONCE && date == null)
            ) {
                return@forEach
            }
            classes += ClassEntity(
                id = item.id,
                semesterId = semester.id,
                semesterProgramId = item.semesterProgramId,
                name = item.name,
                type = item.type,
                teacherName = item.teacherName,
                dayOfWeek = day,
                startTime = start,
                endTime = end,
                room = item.room,
                building = item.building,
                group = item.group,
                recurrence = recurrence,
                date = if (recurrence == Recurrence.ONCE) date else null,
                classNote = item.classNote
            )
        }

        val weekOverrides = mutableListOf<WeekOverrideEntity>()
        semester.weekOverrides.forEach { override ->
            if (override.semesterId != semester.id) {
                errors += "Korekta ${override.id} należy do innego semestru."
                return@forEach
            }
            if (override.academicCalendarId !in calendarIds) {
                errors += "Korekta ${override.id} wskazuje nieistniejący kalendarz."
                return@forEach
            }
            val date = override.weekStartDate.toLocalDateOrNull()
            val weekType = override.weekType.toEnumOrNull<WeekType>()
            val scope = override.scope.toEnumOrNull<WeekOverrideScope>()
            val invalidDate = date != null && date.dayOfWeek != DayOfWeek.MONDAY
            if (invalidDate) {
                errors += "Korekta ${override.id} musi zaczynać się w poniedziałek."
            }
            if (date == null || weekType == null || scope == null || invalidDate) {
                errors += "Korekta ${override.id} ma niepoprawne dane."
                return@forEach
            }
            weekOverrides += WeekOverrideEntity(
                id = override.id,
                semesterId = semester.id,
                academicCalendarId = override.academicCalendarId,
                weekStartDate = date,
                weekType = weekType,
                scope = scope
            )
        }

        val occurrenceNotes = mutableListOf<OccurrenceNoteEntity>()
        semester.occurrenceNotes.forEach { note ->
            if (note.semesterId != semester.id || note.classId !in classIds) {
                errors += "Notatka ${note.id} wskazuje nieistniejące zajęcia."
                return@forEach
            }
            val date = note.occurrenceDate.toLocalDateOrNull()
            val blank = note.body.isBlank()
            if (date == null) {
                errors += "Notatka ${note.id} ma niepoprawną datę."
            }
            if (blank) {
                errors += "Notatka ${note.id} jest pusta."
            }
            if (date == null || blank) {
                return@forEach
            }
            occurrenceNotes += OccurrenceNoteEntity(
                id = note.id,
                semesterId = semester.id,
                classId = note.classId,
                occurrenceDate = date,
                body = note.body
            )
        }

        val occurrenceChanges = mutableListOf<OccurrenceChangeEntity>()
        semester.occurrenceChanges.forEach { change ->
            if (change.semesterId != semester.id || change.classId !in classIds) {
                errors += "Zmiana ${change.id} wskazuje nieistniejące zajęcia."
                return@forEach
            }
            val originalDate = change.originalDate.toLocalDateOrNull()
            val kind = change.kind.toEnumOrNull<OccurrenceChangeKind>()
            val targetDate = change.targetDate?.toLocalDateOrNull()
            val startTime = change.startTime?.toLocalTimeOrNull()
            val endTime = change.endTime?.toLocalTimeOrNull()
            var valid = originalDate != null && kind != null
            if (originalDate == null || kind == null) {
                errors += "Zmiana ${change.id} ma niepoprawne dane."
            }
            if (change.targetDate != null && targetDate == null) {
                errors += "Zmiana ${change.id} ma niepoprawną datę docelową."
                valid = false
            }
            if (change.startTime != null && startTime == null) {
                errors += "Zmiana ${change.id} ma niepoprawną godzinę początku."
                valid = false
            }
            if (change.endTime != null && endTime == null) {
                errors += "Zmiana ${change.id} ma niepoprawną godzinę końca."
                valid = false
            }
            if (change.startTime != null || change.endTime != null) {
                if (startTime == null || endTime == null) {
                    if (startTime == null && change.startTime == null) {
                        errors += "Zmiana ${change.id} wymaga obu godzin."
                    }
                    if (endTime == null && change.endTime == null) {
                        errors += "Zmiana ${change.id} wymaga obu godzin."
                    }
                    valid = false
                } else if (!endTime.isAfter(startTime)) {
                    errors += "Zmiana ${change.id} ma koniec przed początkiem."
                    valid = false
                }
            }
            if (originalDate == null || kind == null || !valid) return@forEach
            occurrenceChanges += OccurrenceChangeEntity(
                id = change.id,
                semesterId = semester.id,
                classId = change.classId,
                originalDate = originalDate,
                kind = kind,
                targetDate = targetDate,
                newStartTime = startTime,
                newEndTime = endTime,
                newRoom = change.room,
                newBuilding = change.building,
                newTeacherName = change.teacherName,
                newNote = change.note
            )
        }

        return SemesterBackup(
            semester = SemesterEntity(id = semester.id, name = semester.name, isActive = semester.isActive),
            calendars = calendars,
            programs = programs,
            classes = classes,
            weekOverrides = weekOverrides,
            occurrenceNotes = occurrenceNotes,
            occurrenceChanges = occurrenceChanges
        )
    }
}

private fun <T> checkUnique(errors: MutableList<String>, values: List<T>, message: String) {
    if (values.size != values.toSet().size) {
        errors += message
    }
}

private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
    runCatching { enumValueOf<T>(this) }.getOrNull()

private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(this) }.getOrNull()

private fun String.toLocalTimeOrNull(): LocalTime? =
    runCatching { LocalTime.parse(this) }.getOrNull()
