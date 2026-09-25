package dev.retza.mak.data.repository

import dev.retza.mak.data.entity.OccurrenceChangeKind
import dev.retza.mak.data.entity.Recurrence
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Plain rows used to move occurrence notes from the actual date of an occurrence to its
 * original planned date. Room migration and the import of older backups share this rule.
 */
data class NoteRow(
    val id: Long,
    val semesterId: Long,
    val classId: Long,
    val date: LocalDate,
    val body: String
)

data class ChangeRow(
    val classId: Long,
    val originalDate: LocalDate,
    val isModified: Boolean,
    val targetDate: LocalDate?
)

data class ClassRow(
    val id: Long,
    val dayOfWeek: DayOfWeek,
    val isOneOff: Boolean
)

data class NoteRemap(
    /** Notes whose date or body changed, with their final values. */
    val updated: List<NoteRow>,
    /** Notes merged into another note for the same occurrence. */
    val deletedIds: List<Long>
)

/**
 * Notes used to be stored under the actual date of an occurrence. They now belong to the
 * occurrence, identified by its original date.
 *
 * A note on date T of class C moves to date D only when exactly one modification moved an
 * occurrence of C from D to T and no regular occurrence of C can take place on T. When
 * several notes end up on the same occurrence their bodies are joined, so no text is lost.
 */
fun remapOccurrenceNotesToOriginalDates(
    notes: List<NoteRow>,
    changes: List<ChangeRow>,
    classes: List<ClassRow>
): NoteRemap {
    val classesById = classes.associateBy { it.id }
    val moved = notes.map { note -> note to originalDateFor(note, changes, classesById[note.classId]) }

    val updated = mutableListOf<NoteRow>()
    val deleted = mutableListOf<Long>()
    moved.groupBy { (note, date) -> note.classId to date }.forEach { (key, group) ->
        val ordered = group.sortedWith(compareBy({ it.first.date }, { it.first.id }))
        val keeper = ordered.minBy { it.first.id }.first
        val body = ordered.joinToString("\n\n") { it.first.body }
        val result = keeper.copy(date = key.second, body = body)
        if (result != keeper) updated += result
        deleted += ordered.map { it.first.id }.filter { it != keeper.id }
    }
    return NoteRemap(updated = updated.sortedBy { it.id }, deletedIds = deleted.sorted())
}

private fun originalDateFor(note: NoteRow, changes: List<ChangeRow>, classRow: ClassRow?): LocalDate {
    val movedHere = changes.filter {
        it.classId == note.classId && it.isModified && it.targetDate == note.date && it.originalDate != note.date
    }
    if (movedHere.size != 1 || classRow == null) return note.date
    val regularMayHappenHere = !classRow.isOneOff &&
        classRow.dayOfWeek == note.date.dayOfWeek &&
        changes.none { it.classId == note.classId && it.originalDate == note.date }
    return if (regularMayHappenHere) note.date else movedHere.single().originalDate
}

/** Applies [remapOccurrenceNotesToOriginalDates] to one semester of a backup. */
fun SemesterBackup.withOccurrenceNotesOnOriginalDates(): SemesterBackup {
    val remap = remapOccurrenceNotesToOriginalDates(
        notes = occurrenceNotes.map { NoteRow(it.id, it.semesterId, it.classId, it.occurrenceDate, it.body) },
        changes = occurrenceChanges.map {
            ChangeRow(it.classId, it.originalDate, it.kind == OccurrenceChangeKind.MODIFIED, it.targetDate)
        },
        classes = classes.map { ClassRow(it.id, it.dayOfWeek, it.recurrence == Recurrence.ONCE) }
    )
    val updatedById = remap.updated.associateBy { it.id }
    val notes = occurrenceNotes
        .filter { it.id !in remap.deletedIds }
        .map { note ->
            updatedById[note.id]?.let { note.copy(occurrenceDate = it.date, body = it.body) } ?: note
        }
    return copy(occurrenceNotes = notes)
}
