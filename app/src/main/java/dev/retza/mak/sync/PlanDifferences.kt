package dev.retza.mak.sync

import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.repository.BackupData
import kotlinx.serialization.Serializable

@Serializable
enum class PlanRowKind {
    STUDY_PROGRAM,
    SEMESTER,
    CALENDAR,
    SEMESTER_PROGRAM,
    CLASS,
    WEEK_OVERRIDE,
    OCCURRENCE_NOTE,
    OCCURRENCE_CHANGE
}

@Serializable
data class PlanRowKey(val kind: PlanRowKind, val id: Long)

enum class PlanSide { PHONE, DRIVE }

/**
 * One row that differs between the phone plan and the Drive plan. A side's key is null when the
 * row is missing there. [collision] marks two entries added independently under the same number:
 * each becomes its own difference, so one of the keys is always null.
 */
@Serializable
data class PlanDifference(
    val key: PlanRowKey,
    val phoneKey: PlanRowKey?,
    val driveKey: PlanRowKey?,
    val collision: Boolean = false
)

/**
 * Rows of [phone] and [drive] compared by number. [base] is the plan both sides had after the
 * last synchronization; it tells a changed row apart from two new rows that got the same number.
 */
fun planDifferences(phone: BackupData, drive: BackupData, base: BackupData): List<PlanDifference> {
    val phoneRows = phone.rows()
    val driveRows = drive.rows()
    val baseRows = base.rows()
    val differences = mutableListOf<PlanDifference>()
    (phoneRows.keys + driveRows.keys).forEach { key ->
        val onPhone = phoneRows[key]
        val onDrive = driveRows[key]
        when {
            onPhone == onDrive -> Unit
            onPhone != null && onDrive != null && key !in baseRows -> {
                differences += PlanDifference(key, phoneKey = key, driveKey = null, collision = true)
                differences += PlanDifference(key, phoneKey = null, driveKey = key, collision = true)
            }
            else -> differences += PlanDifference(
                key,
                phoneKey = key.takeIf { onPhone != null },
                driveKey = key.takeIf { onDrive != null }
            )
        }
    }
    val classes = (driveRows.values + phoneRows.values).filterIsInstance<ClassEntity>().associateBy { it.id }
    return differences.sortedWith(differenceOrder(phoneRows, driveRows, classes))
}

/** Every row of the plan by kind and number; the active semester is local, so it is left out. */
internal fun BackupData.rows(): Map<PlanRowKey, Any> {
    val rows = linkedMapOf<PlanRowKey, Any>()
    studyPrograms.forEach { rows[PlanRowKey(PlanRowKind.STUDY_PROGRAM, it.id)] = it }
    semesters.forEach { backup ->
        rows[PlanRowKey(PlanRowKind.SEMESTER, backup.semester.id)] = backup.semester.copy(isActive = false)
        backup.calendars.forEach { rows[PlanRowKey(PlanRowKind.CALENDAR, it.id)] = it }
        backup.programs.forEach { rows[PlanRowKey(PlanRowKind.SEMESTER_PROGRAM, it.id)] = it }
        backup.classes.forEach { rows[PlanRowKey(PlanRowKind.CLASS, it.id)] = it }
        backup.weekOverrides.forEach { rows[PlanRowKey(PlanRowKind.WEEK_OVERRIDE, it.id)] = it }
        backup.occurrenceNotes.forEach { rows[PlanRowKey(PlanRowKind.OCCURRENCE_NOTE, it.id)] = it }
        backup.occurrenceChanges.forEach { rows[PlanRowKey(PlanRowKind.OCCURRENCE_CHANGE, it.id)] = it }
    }
    return rows
}

/** Classes and their notes and changes first, by day and time; the plan structure after them. */
private fun differenceOrder(
    phoneRows: Map<PlanRowKey, Any>,
    driveRows: Map<PlanRowKey, Any>,
    classes: Map<Long, ClassEntity>
): Comparator<PlanDifference> {
    fun classOf(difference: PlanDifference): ClassEntity? {
        val row = phoneRows[difference.key] ?: driveRows[difference.key]
        return when (row) {
            is ClassEntity -> row
            is OccurrenceNoteEntity -> classes[row.classId]
            is OccurrenceChangeEntity -> classes[row.classId]
            else -> null
        }
    }
    return compareBy<PlanDifference> { if (classOf(it) == null) 1 else 0 }
        .thenBy { classOf(it)?.dayOfWeek }
        .thenBy { classOf(it)?.startTime }
        .thenBy { classOf(it)?.id }
        .thenBy { it.key.kind.ordinal }
        .thenBy { it.key.id }
        .thenBy { if (it.phoneKey != null) 0 else 1 }
}
