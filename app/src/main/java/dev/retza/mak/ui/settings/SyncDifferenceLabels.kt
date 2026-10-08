package dev.retza.mak.ui.settings

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
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.sync.ClassField
import dev.retza.mak.sync.PlanDifference
import dev.retza.mak.sync.PlanMergeProblem
import dev.retza.mak.sync.PlanRowKey
import dev.retza.mak.sync.PlanSide
import dev.retza.mak.sync.rows
import dev.retza.mak.ui.dayNames
import dev.retza.mak.ui.polishLocale
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
        if (field != null && onPhone is ClassEntity && onDrive is ClassEntity) {
            return SyncDifferenceUi(
                title = title,
                subtitle = "${fieldLabel(field)}, ${slot(onPhone)}",
                phone = fieldValue(field, onPhone, PlanSide.PHONE),
                drive = fieldValue(field, onDrive, PlanSide.DRIVE)
            )
        }
        return when (row) {
            is ClassEntity -> classDifference(title, onPhone as ClassEntity?, onDrive as ClassEntity?)
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
        lookup<ClassEntity>(PlanRowKey(dev.retza.mak.sync.PlanRowKind.CLASS, id), side)?.name ?: "Zajęcia"

    private fun semesterName(id: Long, side: PlanSide) =
        lookup<SemesterEntity>(PlanRowKey(dev.retza.mak.sync.PlanRowKind.SEMESTER, id), side)?.name ?: "semestr"

    private fun programName(id: Long, side: PlanSide) =
        lookup<StudyProgramEntity>(PlanRowKey(dev.retza.mak.sync.PlanRowKind.STUDY_PROGRAM, id), side)?.name ?: "kierunek"

    private fun assignmentProgramName(id: Long, side: PlanSide) =
        lookup<SemesterProgramEntity>(PlanRowKey(dev.retza.mak.sync.PlanRowKind.SEMESTER_PROGRAM, id), side)
            ?.let { programName(it.studyProgramId, side) } ?: "kierunek"

    private fun titleOf(row: Any, side: PlanSide): String = when (row) {
        is ClassEntity -> row.name
        is OccurrenceNoteEntity -> className(row.classId, side)
        is OccurrenceChangeEntity -> className(row.classId, side)
        is StudyProgramEntity -> row.name
        is SemesterEntity -> row.name
        is AcademicCalendarEntity -> "Kalendarz semestru ${semesterName(row.semesterId, side)}"
        is SemesterProgramEntity -> programName(row.studyProgramId, side)
        is WeekOverrideEntity -> "Korekta tygodnia od ${date(row.weekStartDate)}"
        else -> "Wpis"
    }

    private fun subtitleOf(row: Any, side: PlanSide): String = when (row) {
        is OccurrenceNoteEntity -> "Notatka do terminu, ${date(row.occurrenceDate)}"
        is OccurrenceChangeEntity -> "Zmiana terminu, ${date(row.originalDate)}"
        is StudyProgramEntity -> "Kierunek"
        is SemesterEntity -> "Semestr"
        is AcademicCalendarEntity -> "Daty semestru i tydzień A/B"
        is SemesterProgramEntity -> "Kierunek w semestrze ${semesterName(row.semesterId, side)}"
        is WeekOverrideEntity -> "Semestr ${semesterName(row.semesterId, side)}"
        else -> ""
    }

    private fun valueOf(row: Any, side: PlanSide): String = when (row) {
        is OccurrenceNoteEntity -> row.body
        is OccurrenceChangeEntity -> changeValue(row)
        is StudyProgramEntity -> "${row.name}, kolor ${row.color}"
        is SemesterEntity -> row.name
        is AcademicCalendarEntity ->
            "Od ${date(row.startDate)} ${row.startDate.year} do ${date(row.endDate)} ${row.endDate.year}, pierwszy tydzień ${row.firstWeekType}"
        is SemesterProgramEntity -> "Przypisany"
        is WeekOverrideEntity -> "Tydzień ${row.weekType}, " +
            if (row.scope == WeekOverrideScope.ONE_WEEK) "tylko ten tydzień" else "od tego tygodnia"
        else -> ""
    }

    private fun changeValue(change: OccurrenceChangeEntity): String {
        if (change.kind == OccurrenceChangeKind.CANCELLED) return "Odwołane"
        val parts = listOfNotNull(
            change.targetDate?.let { "przeniesione na ${date(it)}" },
            if (change.newStartTime != null && change.newEndTime != null) "${time(change.newStartTime)}-${time(change.newEndTime)}" else null,
            change.newRoom?.let { "sala $it" },
            change.newBuilding?.let { "budynek $it" },
            change.newTeacherName,
            change.newNote
        )
        return parts.joinToString(", ").replaceFirstChar { it.uppercase() }.ifEmpty { "Zmienione" }
    }

    /** A class on one side only; a class on both sides differs field by field. */
    private fun classDifference(title: String, onPhone: ClassEntity?, onDrive: ClassEntity?): SyncDifferenceUi {
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

    private fun fieldValue(field: ClassField, row: ClassEntity, side: PlanSide): String = when (field) {
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

    private fun classSummary(row: ClassEntity) =
        listOfNotNull(slot(row), recurrence(row), row.room?.let { "sala $it" }).joinToString(", ")

    private fun slot(row: ClassEntity) = (row.date?.let(::date) ?: dayNames.getValue(row.dayOfWeek)) +
        ", ${time(row.startTime)}-${time(row.endTime)}"

    private fun recurrence(row: ClassEntity) = when (row.recurrence) {
        Recurrence.EVERY_WEEK -> "co tydzień"
        Recurrence.A_WEEK -> "tydzień A"
        Recurrence.B_WEEK -> "tydzień B"
        Recurrence.ONCE -> "jednorazowo"
    }

    private fun date(value: LocalDate) = dateFormatter.format(value)

    private fun time(value: LocalTime) = timeFormatter.format(value)

    private companion object {
        const val MISSING = "Brak"
        const val EMPTY_FIELD = "Puste"
        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", polishLocale)
        val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
