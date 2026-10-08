package dev.retza.mak.sync

import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekOverrideEntity
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.SemesterBackup
import dev.retza.mak.export.ExportImporter
import dev.retza.mak.export.ExportSnapshot
import dev.retza.mak.export.ImportSnapshotResult
import java.time.LocalDate

sealed interface PlanMergeResult {
    data class Ready(val data: BackupData) : PlanMergeResult

    /** Chosen rows point at rows another choice left out. */
    data class Problems(val problems: List<PlanMergeProblem>) : PlanMergeResult

    /** The rows fit together but break a rule of the plan, for example two notes for one occurrence. */
    data class Invalid(val errors: List<String>) : PlanMergeResult
}

/** [child] stays in the plan but [parent], which it needs, does not; both keys are the original numbers. */
data class PlanMergeProblem(
    val child: PlanRowKey,
    val childSide: PlanSide,
    val parent: PlanRowKey,
    val childDifference: PlanDifference?,
    val parentDifference: PlanDifference?
)

/**
 * Starts from [phone] and applies the version picked for each difference: [PlanSide.PHONE] keeps the
 * phone row (or its absence), [PlanSide.DRIVE] takes the Drive row (or its absence). A Drive entry that
 * collides with a phone number gets a new number, and the Drive rows that point at it follow.
 */
fun mergePlans(
    phone: BackupData,
    drive: BackupData,
    differences: List<PlanDifference>,
    picks: Map<PlanDifference, PlanSide>,
    today: LocalDate
): PlanMergeResult {
    require(picks.keys.containsAll(differences)) { "Every difference needs a picked side." }
    val phoneRows = phone.rows()
    val driveRows = drive.rows()
    val result = LinkedHashMap(phoneRows)
    // Where each kept row came from, so references and problems use the right numbers.
    val origin = phoneRows.keys.associateWith { Origin(it, PlanSide.PHONE) }.toMutableMap()
    val differenceOf = mutableMapOf<Origin, PlanDifference>()
    val nextId = PlanRowKind.entries.associateWith { kind ->
        (phoneRows.keys + driveRows.keys).filter { it.kind == kind }.maxOfOrNull { it.id } ?: 0L
    }.toMutableMap()
    val renumbered = mutableMapOf<PlanRowKey, Long>()

    differences.forEach { difference ->
        val side = picks.getValue(difference)
        val key = difference.key
        when {
            difference.collision && difference.phoneKey != null -> {
                differenceOf[Origin(key, PlanSide.PHONE)] = difference
                if (side == PlanSide.DRIVE) {
                    result.remove(key)
                    origin.remove(key)
                }
            }
            difference.field != null -> {
                // The row keeps the phone's number and references; only the picked field may come from Drive.
                differenceOf.putIfAbsent(Origin(key, PlanSide.PHONE), difference)
                if (side == PlanSide.DRIVE) {
                    val current = result.getValue(key) as ClassEntity
                    result[key] = difference.field.copy(from = driveRows.getValue(key) as ClassEntity, into = current)
                }
            }
            difference.collision -> {
                differenceOf[Origin(key, PlanSide.DRIVE)] = difference
                if (side == PlanSide.DRIVE) {
                    val newId = nextId.getValue(key.kind) + 1
                    nextId[key.kind] = newId
                    renumbered[key] = newId
                    val newKey = PlanRowKey(key.kind, newId)
                    result[newKey] = driveRows.getValue(key)
                    origin[newKey] = Origin(key, PlanSide.DRIVE)
                }
            }
            else -> {
                differenceOf[Origin(key, if (side == PlanSide.DRIVE) PlanSide.DRIVE else PlanSide.PHONE)] = difference
                if (side == PlanSide.DRIVE) {
                    val row = driveRows[key]
                    if (row == null) {
                        result.remove(key)
                        origin.remove(key)
                    } else {
                        result[key] = row
                        origin[key] = Origin(key, PlanSide.DRIVE)
                    }
                }
            }
        }
    }

    // Drive rows keep pointing at Drive rows, so a renumbered Drive entry takes its children along.
    val merged = result.mapValues { (key, row) ->
        val from = origin.getValue(key)
        if (from.side == PlanSide.PHONE) return@mapValues row
        val withId = renumbered[from.key]?.let { row.withId(it) } ?: row
        withId.withReferences { kind, id -> renumbered[PlanRowKey(kind, id)] ?: id }
    }

    val problems = merged.flatMap { (key, row) ->
        row.references().filter { it !in merged }.map { parent ->
            val from = origin.getValue(key)
            PlanMergeProblem(
                child = from.key,
                childSide = from.side,
                parent = parent,
                childDifference = differenceOf[from],
                // A collision has one difference per side; the child needs the one from its own side.
                parentDifference = differences.filter { it.key == parent }.let { candidates ->
                    candidates.firstOrNull { (if (from.side == PlanSide.PHONE) it.phoneKey else it.driveKey) != null }
                        ?: candidates.firstOrNull()
                }
            )
        }
    }
    if (problems.isNotEmpty()) return PlanMergeResult.Problems(problems)

    val activeId = phone.semesters.firstOrNull { it.semester.isActive }?.semester?.id
    val data = merged.toBackupData()
    val withActive = if (activeId != null && merged.containsKey(PlanRowKey(PlanRowKind.SEMESTER, activeId))) {
        data.withActiveSemester(activeId)
    } else {
        data.withActiveSemester(data.semesterCovering(today))
    }
    return when (val checked = ExportImporter.prepare(ExportSnapshot.from(withActive))) {
        is ImportSnapshotResult.Ready -> PlanMergeResult.Ready(withActive)
        is ImportSnapshotResult.Invalid -> PlanMergeResult.Invalid(checked.errors)
    }
}

private data class Origin(val key: PlanRowKey, val side: PlanSide)

private fun Any.withId(id: Long): Any = when (this) {
    is StudyProgramEntity -> copy(id = id)
    is SemesterEntity -> copy(id = id)
    is AcademicCalendarEntity -> copy(id = id)
    is SemesterProgramEntity -> copy(id = id)
    is ClassEntity -> copy(id = id)
    is WeekOverrideEntity -> copy(id = id)
    is OccurrenceNoteEntity -> copy(id = id)
    is OccurrenceChangeEntity -> copy(id = id)
    else -> error("Unknown plan row ${this::class.simpleName}")
}

private fun Any.withReferences(map: (PlanRowKind, Long) -> Long): Any = when (this) {
    is StudyProgramEntity, is SemesterEntity -> this
    is AcademicCalendarEntity -> copy(semesterId = map(PlanRowKind.SEMESTER, semesterId))
    is SemesterProgramEntity -> copy(
        semesterId = map(PlanRowKind.SEMESTER, semesterId),
        studyProgramId = map(PlanRowKind.STUDY_PROGRAM, studyProgramId),
        academicCalendarId = map(PlanRowKind.CALENDAR, academicCalendarId)
    )
    is ClassEntity -> copy(
        semesterId = map(PlanRowKind.SEMESTER, semesterId),
        semesterProgramId = map(PlanRowKind.SEMESTER_PROGRAM, semesterProgramId)
    )
    is WeekOverrideEntity -> copy(
        semesterId = map(PlanRowKind.SEMESTER, semesterId),
        academicCalendarId = map(PlanRowKind.CALENDAR, academicCalendarId)
    )
    is OccurrenceNoteEntity -> copy(semesterId = map(PlanRowKind.SEMESTER, semesterId), classId = map(PlanRowKind.CLASS, classId))
    is OccurrenceChangeEntity -> copy(semesterId = map(PlanRowKind.SEMESTER, semesterId), classId = map(PlanRowKind.CLASS, classId))
    else -> error("Unknown plan row ${this::class.simpleName}")
}

/** The rows this row needs in the plan. */
internal fun Any.references(): List<PlanRowKey> = when (this) {
    is StudyProgramEntity, is SemesterEntity -> emptyList()
    is AcademicCalendarEntity -> listOf(PlanRowKey(PlanRowKind.SEMESTER, semesterId))
    is SemesterProgramEntity -> listOf(
        PlanRowKey(PlanRowKind.SEMESTER, semesterId),
        PlanRowKey(PlanRowKind.STUDY_PROGRAM, studyProgramId),
        PlanRowKey(PlanRowKind.CALENDAR, academicCalendarId)
    )
    is ClassEntity -> listOf(
        PlanRowKey(PlanRowKind.SEMESTER, semesterId),
        PlanRowKey(PlanRowKind.SEMESTER_PROGRAM, semesterProgramId)
    )
    is WeekOverrideEntity -> listOf(
        PlanRowKey(PlanRowKind.SEMESTER, semesterId),
        PlanRowKey(PlanRowKind.CALENDAR, academicCalendarId)
    )
    is OccurrenceNoteEntity -> listOf(PlanRowKey(PlanRowKind.SEMESTER, semesterId), PlanRowKey(PlanRowKind.CLASS, classId))
    is OccurrenceChangeEntity -> listOf(PlanRowKey(PlanRowKind.SEMESTER, semesterId), PlanRowKey(PlanRowKind.CLASS, classId))
    else -> error("Unknown plan row ${this::class.simpleName}")
}

private fun Map<PlanRowKey, Any>.toBackupData(): BackupData {
    val all = values
    return BackupData(
        studyPrograms = all.filterIsInstance<StudyProgramEntity>().sortedBy { it.id },
        semesters = all.filterIsInstance<SemesterEntity>().sortedBy { it.id }.map { semester ->
            SemesterBackup(
                semester = semester,
                calendars = all.filterIsInstance<AcademicCalendarEntity>().filter { it.semesterId == semester.id }.sortedBy { it.id },
                programs = all.filterIsInstance<SemesterProgramEntity>().filter { it.semesterId == semester.id }.sortedBy { it.id },
                classes = all.filterIsInstance<ClassEntity>().filter { it.semesterId == semester.id }.sortedBy { it.id },
                weekOverrides = all.filterIsInstance<WeekOverrideEntity>().filter { it.semesterId == semester.id }.sortedBy { it.id },
                occurrenceNotes = all.filterIsInstance<OccurrenceNoteEntity>().filter { it.semesterId == semester.id }.sortedBy { it.id },
                occurrenceChanges = all.filterIsInstance<OccurrenceChangeEntity>().filter { it.semesterId == semester.id }.sortedBy { it.id }
            )
        }
    )
}
