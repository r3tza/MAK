package dev.retza.mak.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.retza.mak.data.repository.ChangeRow
import dev.retza.mak.data.repository.ClassRow
import dev.retza.mak.data.repository.NoteRow
import dev.retza.mak.data.repository.remapOccurrenceNotesToOriginalDates
import java.time.DayOfWeek
import java.time.LocalDate

private val v1Tables = listOf(
    "occurrence_notes",
    "occurrence_changes",
    "week_overrides",
    "classes",
    "teachers",
    "courses",
    "semesters"
)

private val v2Statements = listOf(
    "CREATE TABLE IF NOT EXISTS `semesters` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `is_active` INTEGER NOT NULL)",
    "CREATE TABLE IF NOT EXISTS `study_programs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `color` TEXT NOT NULL)",
    "CREATE TABLE IF NOT EXISTS `academic_calendars` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `semester_id` INTEGER NOT NULL, `start_date` TEXT NOT NULL, `end_date` TEXT NOT NULL, `first_week_type` TEXT NOT NULL, FOREIGN KEY(`semester_id`) REFERENCES `semesters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
    "CREATE INDEX IF NOT EXISTS `index_academic_calendars_semester_id` ON `academic_calendars` (`semester_id`)",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_academic_calendars_id_semester_id` ON `academic_calendars` (`id`, `semester_id`)",
    "CREATE TABLE IF NOT EXISTS `semester_programs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `semester_id` INTEGER NOT NULL, `study_program_id` INTEGER NOT NULL, `academic_calendar_id` INTEGER NOT NULL, FOREIGN KEY(`semester_id`) REFERENCES `semesters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`study_program_id`) REFERENCES `study_programs`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`academic_calendar_id`, `semester_id`) REFERENCES `academic_calendars`(`id`, `semester_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
    "CREATE INDEX IF NOT EXISTS `index_semester_programs_semester_id` ON `semester_programs` (`semester_id`)",
    "CREATE INDEX IF NOT EXISTS `index_semester_programs_study_program_id` ON `semester_programs` (`study_program_id`)",
    "CREATE INDEX IF NOT EXISTS `index_semester_programs_academic_calendar_id` ON `semester_programs` (`academic_calendar_id`)",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_semester_programs_semester_id_study_program_id` ON `semester_programs` (`semester_id`, `study_program_id`)",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_semester_programs_id_semester_id` ON `semester_programs` (`id`, `semester_id`)",
    "CREATE INDEX IF NOT EXISTS `index_semester_programs_academic_calendar_id_semester_id` ON `semester_programs` (`academic_calendar_id`, `semester_id`)",
    "CREATE TABLE IF NOT EXISTS `classes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `semester_id` INTEGER NOT NULL, `semester_program_id` INTEGER NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `teacher_name` TEXT, `day_of_week` INTEGER NOT NULL, `start_time` TEXT NOT NULL, `end_time` TEXT NOT NULL, `room` TEXT, `building` TEXT, `group` TEXT, `recurrence` TEXT NOT NULL, `date` TEXT, `class_note` TEXT, FOREIGN KEY(`semester_id`) REFERENCES `semesters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`semester_program_id`, `semester_id`) REFERENCES `semester_programs`(`id`, `semester_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
    "CREATE INDEX IF NOT EXISTS `index_classes_semester_id` ON `classes` (`semester_id`)",
    "CREATE INDEX IF NOT EXISTS `index_classes_semester_program_id` ON `classes` (`semester_program_id`)",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_classes_id_semester_id` ON `classes` (`id`, `semester_id`)",
    "CREATE INDEX IF NOT EXISTS `index_classes_semester_program_id_semester_id` ON `classes` (`semester_program_id`, `semester_id`)",
    "CREATE TABLE IF NOT EXISTS `week_overrides` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `semester_id` INTEGER NOT NULL, `academic_calendar_id` INTEGER NOT NULL, `week_start_date` TEXT NOT NULL, `week_type` TEXT NOT NULL, `scope` TEXT NOT NULL, FOREIGN KEY(`semester_id`) REFERENCES `semesters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`academic_calendar_id`, `semester_id`) REFERENCES `academic_calendars`(`id`, `semester_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
    "CREATE INDEX IF NOT EXISTS `index_week_overrides_semester_id` ON `week_overrides` (`semester_id`)",
    "CREATE INDEX IF NOT EXISTS `index_week_overrides_academic_calendar_id` ON `week_overrides` (`academic_calendar_id`)",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_week_overrides_academic_calendar_id_week_start_date_scope` ON `week_overrides` (`academic_calendar_id`, `week_start_date`, `scope`)",
    "CREATE INDEX IF NOT EXISTS `index_week_overrides_academic_calendar_id_semester_id` ON `week_overrides` (`academic_calendar_id`, `semester_id`)",
    "CREATE TABLE IF NOT EXISTS `occurrence_notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `semester_id` INTEGER NOT NULL, `class_id` INTEGER NOT NULL, `occurrence_date` TEXT NOT NULL, `body` TEXT NOT NULL, FOREIGN KEY(`semester_id`) REFERENCES `semesters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`class_id`, `semester_id`) REFERENCES `classes`(`id`, `semester_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
    "CREATE INDEX IF NOT EXISTS `index_occurrence_notes_semester_id` ON `occurrence_notes` (`semester_id`)",
    "CREATE INDEX IF NOT EXISTS `index_occurrence_notes_class_id` ON `occurrence_notes` (`class_id`)",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_occurrence_notes_semester_id_class_id_occurrence_date` ON `occurrence_notes` (`semester_id`, `class_id`, `occurrence_date`)",
    "CREATE INDEX IF NOT EXISTS `index_occurrence_notes_class_id_semester_id` ON `occurrence_notes` (`class_id`, `semester_id`)",
    "CREATE TABLE IF NOT EXISTS `occurrence_changes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `semester_id` INTEGER NOT NULL, `class_id` INTEGER NOT NULL, `original_date` TEXT NOT NULL, `kind` TEXT NOT NULL, `target_date` TEXT, `new_start_time` TEXT, `new_end_time` TEXT, `new_room` TEXT, `new_building` TEXT, `new_teacher_name` TEXT, `new_note` TEXT, FOREIGN KEY(`semester_id`) REFERENCES `semesters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`class_id`, `semester_id`) REFERENCES `classes`(`id`, `semester_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
    "CREATE INDEX IF NOT EXISTS `index_occurrence_changes_semester_id` ON `occurrence_changes` (`semester_id`)",
    "CREATE INDEX IF NOT EXISTS `index_occurrence_changes_class_id` ON `occurrence_changes` (`class_id`)",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_occurrence_changes_semester_id_class_id_original_date` ON `occurrence_changes` (`semester_id`, `class_id`, `original_date`)",
    "CREATE INDEX IF NOT EXISTS `index_occurrence_changes_class_id_semester_id` ON `occurrence_changes` (`class_id`, `semester_id`)"
)

private val copyStatements = listOf(
    "INSERT INTO `semesters` (`id`, `name`, `is_active`) SELECT `id`, `name`, `is_active` FROM `v1_semesters`",
    "INSERT INTO `study_programs` (`id`, `name`, `color`) SELECT `id`, `name`, `color` FROM `v1_courses`",
    "INSERT INTO `academic_calendars` (`id`, `semester_id`, `start_date`, `end_date`, `first_week_type`) " +
        "SELECT `id`, `id`, `start_date`, `end_date`, `first_week_type` FROM `v1_semesters`",
    "INSERT INTO `semester_programs` (`id`, `semester_id`, `study_program_id`, `academic_calendar_id`) " +
        "SELECT `id`, `semester_id`, `id`, `semester_id` FROM `v1_courses`",
    "INSERT INTO `classes` (`id`, `semester_id`, `semester_program_id`, `name`, `type`, `teacher_name`, " +
        "`day_of_week`, `start_time`, `end_time`, `room`, `building`, `group`, `recurrence`, `date`, `class_note`) " +
        "SELECT c.`id`, c.`semester_id`, c.`course_id`, c.`name`, c.`type`, t.`name`, c.`day_of_week`, " +
        "c.`start_time`, c.`end_time`, c.`room`, c.`building`, c.`group`, c.`recurrence`, c.`date`, c.`class_note` " +
        "FROM `v1_classes` c LEFT JOIN `v1_teachers` t ON t.`id` = c.`teacher_id`",
    "INSERT INTO `week_overrides` (`id`, `semester_id`, `academic_calendar_id`, `week_start_date`, `week_type`, `scope`) " +
        "SELECT `id`, `semester_id`, `semester_id`, `week_start_date`, `week_type`, `scope` FROM `v1_week_overrides`",
    "INSERT INTO `occurrence_notes` (`id`, `semester_id`, `class_id`, `occurrence_date`, `body`) " +
        "SELECT `id`, `semester_id`, `class_id`, `occurrence_date`, `body` FROM `v1_occurrence_notes`",
    "INSERT INTO `occurrence_changes` (`id`, `semester_id`, `class_id`, `original_date`, `kind`, `target_date`, " +
        "`new_start_time`, `new_end_time`, `new_room`, `new_building`, `new_teacher_name`, `new_note`) " +
        "SELECT oc.`id`, oc.`semester_id`, oc.`class_id`, oc.`original_date`, oc.`kind`, oc.`target_date`, " +
        "oc.`new_start_time`, oc.`new_end_time`, oc.`new_room`, oc.`new_building`, t.`name`, oc.`new_note` " +
        "FROM `v1_occurrence_changes` oc LEFT JOIN `v1_teachers` t ON t.`id` = oc.`new_teacher_id`"
)

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TEMP TABLE `v1_semesters` AS SELECT * FROM `semesters`")
        db.execSQL("CREATE TEMP TABLE `v1_courses` AS SELECT * FROM `courses`")
        db.execSQL("CREATE TEMP TABLE `v1_teachers` AS SELECT * FROM `teachers`")
        db.execSQL("CREATE TEMP TABLE `v1_classes` AS SELECT * FROM `classes`")
        db.execSQL("CREATE TEMP TABLE `v1_week_overrides` AS SELECT * FROM `week_overrides`")
        db.execSQL("CREATE TEMP TABLE `v1_occurrence_notes` AS SELECT * FROM `occurrence_notes`")
        db.execSQL("CREATE TEMP TABLE `v1_occurrence_changes` AS SELECT * FROM `occurrence_changes`")

        v1Tables.forEach { db.execSQL("DROP TABLE IF EXISTS `$it`") }
        v2Statements.forEach(db::execSQL)
        copyStatements.forEach(db::execSQL)

        db.execSQL("DROP TABLE IF EXISTS `v1_occurrence_changes`")
        db.execSQL("DROP TABLE IF EXISTS `v1_occurrence_notes`")
        db.execSQL("DROP TABLE IF EXISTS `v1_week_overrides`")
        db.execSQL("DROP TABLE IF EXISTS `v1_classes`")
        db.execSQL("DROP TABLE IF EXISTS `v1_teachers`")
        db.execSQL("DROP TABLE IF EXISTS `v1_courses`")
        db.execSQL("DROP TABLE IF EXISTS `v1_semesters`")

        db.query("PRAGMA foreign_key_check").use { cursor ->
            check(!cursor.moveToFirst()) { "Room migration 1 to 2 left broken foreign keys" }
        }
    }
}

/**
 * Version 3 keeps the schema and moves occurrence notes from the actual date of a moved
 * occurrence to its original date, so a note follows the occurrence it was written for.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val notes = buildList {
            db.query("SELECT id, semester_id, class_id, occurrence_date, body FROM occurrence_notes").use { c ->
                while (c.moveToNext()) {
                    add(NoteRow(c.getLong(0), c.getLong(1), c.getLong(2), LocalDate.parse(c.getString(3)), c.getString(4)))
                }
            }
        }
        if (notes.isEmpty()) return
        val changes = buildList {
            db.query("SELECT class_id, original_date, kind, target_date FROM occurrence_changes").use { c ->
                while (c.moveToNext()) {
                    add(
                        ChangeRow(
                            classId = c.getLong(0),
                            originalDate = LocalDate.parse(c.getString(1)),
                            isModified = c.getString(2) == "MODIFIED",
                            targetDate = if (c.isNull(3)) null else LocalDate.parse(c.getString(3))
                        )
                    )
                }
            }
        }
        val classes = buildList {
            db.query("SELECT id, day_of_week, recurrence FROM classes").use { c ->
                while (c.moveToNext()) {
                    add(ClassRow(c.getLong(0), DayOfWeek.of(c.getInt(1)), c.getString(2) == "ONCE"))
                }
            }
        }

        val remap = remapOccurrenceNotesToOriginalDates(notes, changes, classes)
        // Delete every touched row first so re-inserted dates never clash with the unique index.
        (remap.deletedIds + remap.updated.map { it.id }).forEach { id ->
            db.execSQL("DELETE FROM occurrence_notes WHERE id = ?", arrayOf<Any>(id))
        }
        remap.updated.forEach { note ->
            db.execSQL(
                "INSERT INTO occurrence_notes (id, semester_id, class_id, occurrence_date, body) VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any>(note.id, note.semesterId, note.classId, note.date.toString(), note.body)
            )
        }
    }
}
