package dev.retza.mak.data.database

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekOverrideEntity

data class SemesterWithData(
    @Embedded
    val semester: SemesterEntity,
    @Relation(parentColumn = "id", entityColumn = "semester_id")
    val semesterPrograms: List<SemesterProgramEntity>,
    @Relation(parentColumn = "id", entityColumn = "semester_id")
    val academicCalendars: List<AcademicCalendarEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SemesterProgramEntity::class,
            parentColumn = "semester_id",
            entityColumn = "study_program_id"
        )
    )
    val studyPrograms: List<StudyProgramEntity>,
    @Relation(parentColumn = "id", entityColumn = "semester_id")
    val classes: List<ClassEntity>,
    @Relation(parentColumn = "id", entityColumn = "semester_id")
    val weekOverrides: List<WeekOverrideEntity>,
    @Relation(parentColumn = "id", entityColumn = "semester_id")
    val occurrenceNotes: List<OccurrenceNoteEntity>,
    @Relation(parentColumn = "id", entityColumn = "semester_id")
    val occurrenceChanges: List<OccurrenceChangeEntity>
)

data class ClassWithDetails(
    @Embedded
    val classEntity: ClassEntity,
    @Relation(parentColumn = "id", entityColumn = "class_id")
    val occurrenceNotes: List<OccurrenceNoteEntity>,
    @Relation(parentColumn = "id", entityColumn = "class_id")
    val occurrenceChanges: List<OccurrenceChangeEntity>
)
