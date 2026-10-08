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

    private fun classDifference(title: String, onPhone: ClassEntity?, onDrive: ClassEntity?): SyncDifferenceUi {
        val any = onPhone ?: onDrive!!
        if (onPhone == null || onDrive == null) {
            return SyncDifferenceUi(
                title = title,
                subtitle = "Zajęcia, ${slot(any)}",
                phone = onPhone?.let(::classSummary) ?: MISSING,
                drive = onDrive?.let(::classSummary) ?: MISSING
            )
        }
        val fields = classFields(onPhone, PlanSide.PHONE).zip(classFields(onDrive, PlanSide.DRIVE))
            .filter { (phone, drive) -> phone.second != drive.second }
        return SyncDifferenceUi(
            title = title,
            subtitle = (fields.map { it.first.first } + slot(onPhone)).joinToString(", "),
            phone = fields.joinToString("; ") { it.first.second ?: "brak" },
            drive = fields.joinToString("; ") { it.second.second ?: "brak" }
        )
    }

    /** Label and value of every field the user edits, in the order of the class form. */
    private fun classFields(row: ClassEntity, side: PlanSide): List<Pair<String, String?>> = listOf(
        "Nazwa" to row.name,
        "Kierunek" to assignmentProgramName(row.semesterProgramId, side),
        "Typ" to row.type,
        "Termin" to slot(row),
        "Tygodnie" to recurrence(row),
        "Prowadzący" to row.teacherName,
        "Sala" to row.room,
        "Budynek" to row.building,
        "Grupa" to row.group,
        "Notatka do zajęć" to row.classNote
    )

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
        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", polishLocale)
        val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
