package dev.retza.mak.ui.settings

import dev.retza.mak.data.repository.AcademicCalendarRecord
import dev.retza.mak.data.repository.ClassRecord
import dev.retza.mak.data.repository.OccurrenceChangeRecord
import dev.retza.mak.domain.OccurrenceChangeKind
import dev.retza.mak.data.repository.OccurrenceNoteRecord
import dev.retza.mak.domain.Recurrence
import dev.retza.mak.data.repository.SemesterRecord
import dev.retza.mak.data.repository.SemesterProgramRecord
import dev.retza.mak.data.repository.StudyProgramRecord
import dev.retza.mak.data.repository.WeekOverrideRecord
import dev.retza.mak.data.repository.FreeDayRecord
import dev.retza.mak.domain.WeekOverrideScope
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.sync.ClassField
import dev.retza.mak.sync.PlanDifference
import dev.retza.mak.sync.PlanMergeProblem
import dev.retza.mak.sync.PlanRowKey
import dev.retza.mak.sync.PlanSide
import dev.retza.mak.sync.rows
import dev.retza.mak.ui.dayMonthFormatter
import dev.retza.mak.ui.dayNames
import dev.retza.mak.ui.timeFormatter
import java.time.LocalDate
import java.time.LocalTime

/** One difference as the user reads it: what it is and the value on each side. */
data class SyncDifferenceUi(
    val title: String,
    val subtitle: String,
    val phone: String,
    val drive: String
)

/** Describes differences with names, days and values taken from both plans of the question. */
internal class SyncDifferenceLabels(phone: BackupData, drive: BackupData) {
    private val phoneRows = phone.rows()
    private val driveRows = drive.rows()

    fun describe(difference: PlanDifference): SyncDifferenceUi {
        val onPhone = difference.phoneKey?.let(phoneRows::get)
        val onDrive = difference.driveKey?.let(driveRows::get)
        val row = onPhone ?: onDrive ?: error("A difference has a row on at least one side")
        val side = if (onPhone != null) PlanSide.PHONE else PlanSide.DRIVE
        val title = titleOf(row, side)
        val field = difference.field
        if (field != null && onPhone is ClassRecord && onDrive is ClassRecord) {
            return SyncDifferenceUi(
                title = title,
                subtitle = "${fieldLabel(field)}, ${slot(onPhone)}",
                phone = fieldValue(field, onPhone, PlanSide.PHONE),
                drive = fieldValue(field, onDrive, PlanSide.DRIVE)
            )
        }
        return when (row) {
            is ClassRecord -> classDifference(title, onPhone as ClassRecord?, onDrive as ClassRecord?)
            else -> SyncDifferenceUi(
                title = title,
                subtitle = subtitleOf(row, side),
                phone = onPhone?.let { valueOf(it, PlanSide.PHONE) } ?: MISSING,
                drive = onDrive?.let { valueOf(it, PlanSide.DRIVE) } ?: MISSING
            )
        }
    }

    /** „Statystyka” wymaga „Ekonometria”; both names come from the side the row was kept from. */
    fun describe(problem: PlanMergeProblem): String {
        val child = rowOf(problem.child, problem.childSide)?.let { titleOf(it, problem.childSide) } ?: "Wpis"
        val parentSide = if (problem.childSide == PlanSide.PHONE) PlanSide.PHONE else PlanSide.DRIVE
        val parent = (rowOf(problem.parent, parentSide) ?: rowOf(problem.parent, opposite(parentSide)))
            ?.let { titleOf(it, parentSide) } ?: "usunięty wpis"
        return "„$child” wymaga: $parent. Wybierz tę samą wersję przy obu pozycjach."
    }

    private fun opposite(side: PlanSide) = if (side == PlanSide.PHONE) PlanSide.DRIVE else PlanSide.PHONE

    private fun rowOf(key: PlanRowKey, side: PlanSide): Any? = if (side == PlanSide.PHONE) phoneRows[key] else driveRows[key]

    private inline fun <reified T> lookup(key: PlanRowKey, side: PlanSide): T? =
        (rowOf(key, side) ?: rowOf(key, opposite(side))) as? T

    private fun className(id: Long, side: PlanSide) =
        lookup<ClassRecord>(PlanRowKey(dev.retza.mak.sync.PlanRowKind.CLASS, id), side)?.name ?: "Zajęcia"

    private fun semesterName(id: Long, side: PlanSide) =
        lookup<SemesterRecord>(PlanRowKey(dev.retza.mak.sync.PlanRowKind.SEMESTER, id), side)?.name ?: "semestr"

    private fun programName(id: Long, side: PlanSide) =
        lookup<StudyProgramRecord>(PlanRowKey(dev.retza.mak.sync.PlanRowKind.STUDY_PROGRAM, id), side)?.name ?: "kierunek"

    private fun assignmentProgramName(id: Long, side: PlanSide) =
        lookup<SemesterProgramRecord>(PlanRowKey(dev.retza.mak.sync.PlanRowKind.SEMESTER_PROGRAM, id), side)
            ?.let { programName(it.studyProgramId, side) } ?: "kierunek"

    private fun titleOf(row: Any, side: PlanSide): String = when (row) {
        is ClassRecord -> row.name
        is OccurrenceNoteRecord -> className(row.classId, side)
        is OccurrenceChangeRecord -> className(row.classId, side)
        is StudyProgramRecord -> row.name
        is SemesterRecord -> row.name
        is AcademicCalendarRecord -> "Kalendarz semestru ${semesterName(row.semesterId, side)}"
        is SemesterProgramRecord -> programName(row.studyProgramId, side)
        is WeekOverrideRecord -> "Korekta tygodnia od ${date(row.weekStartDate)}"
        is FreeDayRecord -> row.name ?: "Dni wolne"
        else -> "Wpis"
    }

    private fun subtitleOf(row: Any, side: PlanSide): String = when (row) {
        is OccurrenceNoteRecord -> "Notatka do terminu, ${date(row.occurrenceDate)}"
        is OccurrenceChangeRecord -> "Zmiana terminu, ${date(row.originalDate)}"
        is StudyProgramRecord -> "Kierunek"
        is SemesterRecord -> "Semestr"
        is AcademicCalendarRecord -> "Daty semestru i tydzień A/B"
        is SemesterProgramRecord -> "Kierunek w semestrze ${semesterName(row.semesterId, side)}"
        is WeekOverrideRecord -> "Semestr ${semesterName(row.semesterId, side)}"
        is FreeDayRecord -> "Dni wolne, ${semesterName(row.semesterId, side)}"
        else -> ""
    }

    private fun valueOf(row: Any, side: PlanSide): String = when (row) {
        is OccurrenceNoteRecord -> row.body
        is OccurrenceChangeRecord -> changeValue(row)
        is StudyProgramRecord -> "${row.name}, kolor ${row.color}"
        is SemesterRecord -> row.name
        is AcademicCalendarRecord ->
            "Od ${date(row.startDate)} ${row.startDate.year} do ${date(row.endDate)} ${row.endDate.year}, pierwszy tydzień ${row.firstWeekType}"
        is SemesterProgramRecord -> "Przypisany"
        is WeekOverrideRecord -> "Tydzień ${row.weekType}, " +
            if (row.scope == WeekOverrideScope.ONE_WEEK) "tylko ten tydzień" else "od tego tygodnia"
        is FreeDayRecord ->
            if (row.startDate == row.endDate) date(row.startDate)
            else "${date(row.startDate)} - ${date(row.endDate)}"
        else -> ""
    }

    private fun changeValue(change: OccurrenceChangeRecord): String {
        if (change.kind == OccurrenceChangeKind.CANCELLED) return "Odwołane"
        val parts = listOfNotNull(
            change.targetDate?.let { "przeniesione na ${date(it)}" },
            if (change.startTime != null && change.endTime != null) "${time(change.startTime)}-${time(change.endTime)}" else null,
            change.room?.let { "sala $it" },
            change.building?.let { "budynek $it" },
            change.teacherName,
            change.note
        )
        return parts.joinToString(", ").replaceFirstChar { it.uppercase() }.ifEmpty { "Zmienione" }
    }

    /** A class on one side only; a class on both sides differs field by field. */
    private fun classDifference(title: String, onPhone: ClassRecord?, onDrive: ClassRecord?): SyncDifferenceUi {
        val any = onPhone ?: onDrive!!
        return SyncDifferenceUi(
            title = title,
            subtitle = "Zajęcia, ${slot(any)}",
            phone = onPhone?.let(::classSummary) ?: MISSING,
            drive = onDrive?.let(::classSummary) ?: MISSING
        )
    }

    private fun fieldLabel(field: ClassField) = when (field) {
        ClassField.NAME -> "Nazwa"
        ClassField.PROGRAM -> "Kierunek"
        ClassField.TYPE -> "Typ"
        ClassField.SLOT -> "Termin"
        ClassField.TEACHER -> "Prowadzący"
        ClassField.ROOM -> "Sala"
        ClassField.BUILDING -> "Budynek"
        ClassField.GROUP -> "Grupa"
        ClassField.NOTE -> "Notatka do zajęć"
    }

    private fun fieldValue(field: ClassField, row: ClassRecord, side: PlanSide): String = when (field) {
        ClassField.NAME -> row.name
        ClassField.PROGRAM -> assignmentProgramName(row.semesterProgramId, side)
        ClassField.TYPE -> row.type
        ClassField.SLOT -> "${slot(row)}, ${recurrence(row)}"
        ClassField.TEACHER -> row.teacherName
        ClassField.ROOM -> row.room
        ClassField.BUILDING -> row.building
        ClassField.GROUP -> row.group
        ClassField.NOTE -> row.classNote
    }?.takeIf { it.isNotBlank() } ?: EMPTY_FIELD

    private fun classSummary(row: ClassRecord) =
        listOfNotNull(slot(row), recurrence(row), row.room?.let { "sala $it" }).joinToString(", ")

    private fun slot(row: ClassRecord) = (row.date?.let(::date) ?: dayNames.getValue(row.dayOfWeek)) +
        ", ${time(row.startTime)}-${time(row.endTime)}"

    private fun recurrence(row: ClassRecord) = when (row.recurrence) {
        Recurrence.EVERY_WEEK -> "co tydzień"
        Recurrence.A_WEEK -> "tydzień A"
        Recurrence.B_WEEK -> "tydzień B"
        Recurrence.ONCE -> "jednorazowo"
    }

    private fun date(value: LocalDate) = dayMonthFormatter.format(value)

    private fun time(value: LocalTime) = timeFormatter.format(value)

    private companion object {
        const val MISSING = "Brak"
        const val EMPTY_FIELD = "Puste"
    }
}
