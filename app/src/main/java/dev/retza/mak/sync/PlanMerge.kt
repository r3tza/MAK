package dev.retza.mak.sync

import dev.retza.mak.data.repository.AcademicCalendarRecord
import dev.retza.mak.data.repository.ClassRecord
import dev.retza.mak.data.repository.OccurrenceChangeRecord
import dev.retza.mak.data.repository.OccurrenceNoteRecord
import dev.retza.mak.data.repository.SemesterRecord
import dev.retza.mak.data.repository.SemesterProgramRecord
import dev.retza.mak.data.repository.StudyProgramRecord
import dev.retza.mak.data.repository.WeekOverrideRecord
import dev.retza.mak.data.repository.FreeDayRecord
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.SemesterBackup
import dev.retza.mak.data.repository.withActiveSemester
import dev.retza.mak.export.ExportImporter
import dev.retza.mak.export.ExportSnapshot
import dev.retza.mak.export.ImportSnapshotResult

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
    picks: Map<PlanDifference, PlanSide>
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
                    val current = result.getValue(key) as ClassRecord
                    result[key] = difference.field.copy(from = driveRows.getValue(key) as ClassRecord, into = current)
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
    // Without a kept active semester the plan has none, and replaceAll picks the one covering today on save.
    val withActive = data.withActiveSemester(activeId?.takeIf { merged.containsKey(PlanRowKey(PlanRowKind.SEMESTER, it)) })
    return when (val checked = ExportImporter.prepare(ExportSnapshot.from(withActive))) {
        is ImportSnapshotResult.Ready -> PlanMergeResult.Ready(withActive)
        is ImportSnapshotResult.Invalid -> PlanMergeResult.Invalid(checked.errors)
    }
}

private data class Origin(val key: PlanRowKey, val side: PlanSide)

private fun Any.withId(id: Long): Any = when (this) {
    is StudyProgramRecord -> copy(id = id)
    is SemesterRecord -> copy(id = id)
    is AcademicCalendarRecord -> copy(id = id)
    is SemesterProgramRecord -> copy(id = id)
    is ClassRecord -> copy(id = id)
    is WeekOverrideRecord -> copy(id = id)
    is FreeDayRecord -> copy(id = id)
    is OccurrenceNoteRecord -> copy(id = id)
    is OccurrenceChangeRecord -> copy(id = id)
    else -> error("Unknown plan row ${this::class.simpleName}")
}

private fun Any.withReferences(map: (PlanRowKind, Long) -> Long): Any = when (this) {
    is StudyProgramRecord, is SemesterRecord -> this
    is AcademicCalendarRecord -> copy(semesterId = map(PlanRowKind.SEMESTER, semesterId))
    is SemesterProgramRecord -> copy(
        semesterId = map(PlanRowKind.SEMESTER, semesterId),
        studyProgramId = map(PlanRowKind.STUDY_PROGRAM, studyProgramId),
        academicCalendarId = map(PlanRowKind.CALENDAR, academicCalendarId)
    )
    is ClassRecord -> copy(
        semesterId = map(PlanRowKind.SEMESTER, semesterId),
        semesterProgramId = map(PlanRowKind.SEMESTER_PROGRAM, semesterProgramId)
    )
    is WeekOverrideRecord -> copy(
        semesterId = map(PlanRowKind.SEMESTER, semesterId),
        academicCalendarId = map(PlanRowKind.CALENDAR, academicCalendarId)
    )
    is FreeDayRecord -> copy(
        semesterId = map(PlanRowKind.SEMESTER, semesterId),
        academicCalendarId = map(PlanRowKind.CALENDAR, academicCalendarId)
    )
    is OccurrenceNoteRecord -> copy(semesterId = map(PlanRowKind.SEMESTER, semesterId), classId = map(PlanRowKind.CLASS, classId))
    is OccurrenceChangeRecord -> copy(semesterId = map(PlanRowKind.SEMESTER, semesterId), classId = map(PlanRowKind.CLASS, classId))
    else -> error("Unknown plan row ${this::class.simpleName}")
}

/** The rows this row needs in the plan, read from the same table as [withReferences]. */
internal fun Any.references(): List<PlanRowKey> = buildList {
    this@references.withReferences { kind, id ->
        add(PlanRowKey(kind, id))
        id
    }
}

private fun Map<PlanRowKey, Any>.toBackupData(): BackupData {
    val all = values
    return BackupData(
        studyPrograms = all.filterIsInstance<StudyProgramRecord>().sortedBy { it.id },
        semesters = all.filterIsInstance<SemesterRecord>().sortedBy { it.id }.map { semester ->
            SemesterBackup(
                semester = semester,
                calendars = all.filterIsInstance<AcademicCalendarRecord>().filter { it.semesterId == semester.id }.sortedBy { it.id },
                programs = all.filterIsInstance<SemesterProgramRecord>().filter { it.semesterId == semester.id }.sortedBy { it.id },
                classes = all.filterIsInstance<ClassRecord>().filter { it.semesterId == semester.id }.sortedBy { it.id },
                weekOverrides = all.filterIsInstance<WeekOverrideRecord>().filter { it.semesterId == semester.id }.sortedBy { it.id },
                freeDays = all.filterIsInstance<FreeDayRecord>().filter { it.semesterId == semester.id }.sortedBy { it.id },
                occurrenceNotes = all.filterIsInstance<OccurrenceNoteRecord>().filter { it.semesterId == semester.id }.sortedBy { it.id },
                occurrenceChanges = all.filterIsInstance<OccurrenceChangeRecord>().filter { it.semesterId == semester.id }.sortedBy { it.id }
            )
        }
    )
}
