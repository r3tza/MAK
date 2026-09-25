package dev.retza.mak.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.data.database.MIGRATION_1_2
import dev.retza.mak.data.database.MIGRATION_2_3
import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrationFromV1ToLatestPreservesData() {
        helper.createDatabase(DATABASE_NAME, 1).use { db ->
            db.execSQL(
                "INSERT INTO semesters (id, name, start_date, end_date, first_week_type, is_active) " +
                    "VALUES (1, 'Semestr', '2026-10-01', '2027-02-28', 'A', 1)"
            )
            db.execSQL(
                "INSERT INTO courses (id, semester_id, name, color) " +
                    "VALUES (1, 1, 'Informatyka', '#112233')"
            )
            db.execSQL(
                "INSERT INTO courses (id, semester_id, name, color) " +
                    "VALUES (2, 1, 'Informatyka', '#445566')"
            )
            db.execSQL(
                "INSERT INTO teachers (id, semester_id, name) VALUES (1, 1, 'Jan Kowalski')"
            )
            db.execSQL(
                "INSERT INTO classes (id, semester_id, name, type, course_id, teacher_id, day_of_week, " +
                    "start_time, end_time, room, building, `group`, recurrence, date, class_note) " +
                    "VALUES (1, 1, 'Programowanie', 'Wykład', 1, 1, 1, '08:00', '09:30', NULL, NULL, NULL, " +
                    "'EVERY_WEEK', NULL, NULL)"
            )
            db.execSQL(
                "INSERT INTO week_overrides (id, semester_id, week_start_date, week_type, scope) " +
                    "VALUES (1, 1, '2026-10-05', 'B', 'ONE_WEEK')"
            )
            db.execSQL(
                "INSERT INTO occurrence_notes (id, semester_id, class_id, occurrence_date, body) " +
                    "VALUES (1, 1, 1, '2026-10-05', 'Kolokwium')"
            )
            db.execSQL(
                "INSERT INTO occurrence_changes (id, semester_id, class_id, original_date, kind, target_date, " +
                    "new_start_time, new_end_time, new_room, new_building, new_teacher_id, new_note) " +
                    "VALUES (1, 1, 1, '2026-10-05', 'MODIFIED', NULL, NULL, NULL, NULL, NULL, 1, NULL)"
            )
            db.execSQL(
                "INSERT INTO semesters (id, name, start_date, end_date, first_week_type, is_active) " +
                    "VALUES (2, 'Semestr letni', '2027-02-01', '2027-06-30', 'B', 0)"
            )
            db.execSQL(
                "INSERT INTO courses (id, semester_id, name, color) VALUES (3, 2, 'Matematyka', '#778899')"
            )
            db.execSQL(
                "INSERT INTO classes (id, semester_id, name, type, course_id, teacher_id, day_of_week, " +
                    "start_time, end_time, room, building, `group`, recurrence, date, class_note) " +
                    "VALUES (2, 2, 'Algebra', 'Ćwiczenia', 3, NULL, 3, '10:00', '11:30', NULL, NULL, NULL, " +
                    "'EVERY_WEEK', NULL, NULL)"
            )
        }

        helper.runMigrationsAndValidate(DATABASE_NAME, 3, true, MIGRATION_1_2, MIGRATION_2_3).use { db ->
            db.query("SELECT name, is_active FROM semesters WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Semestr", cursor.getString(0))
                assertEquals(1, cursor.getInt(1))
            }
            db.query("SELECT id, name FROM study_programs ORDER BY id").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1L, cursor.getLong(0))
                assertEquals("Informatyka", cursor.getString(1))
                assertTrue(cursor.moveToNext())
                assertEquals(2L, cursor.getLong(0))
            }
            db.query("SELECT COUNT(*) FROM semesters").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(2, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM semester_programs").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(3, cursor.getInt(0))
            }
            db.query("SELECT name FROM study_programs WHERE id = 3").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Matematyka", cursor.getString(0))
            }
            db.query("SELECT COUNT(*) FROM classes").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(2, cursor.getInt(0))
            }
            db.query("SELECT new_teacher_name FROM occurrence_changes WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Jan Kowalski", cursor.getString(0))
            }
            db.query("SELECT teacher_name, semester_program_id FROM classes WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Jan Kowalski", cursor.getString(0))
                assertEquals(1L, cursor.getLong(1))
            }
            db.query("SELECT academic_calendar_id FROM week_overrides WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1L, cursor.getLong(0))
            }
            db.query("SELECT COUNT(*) FROM occurrence_notes").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM occurrence_changes").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrationFromV2MovesNotesToOriginalDatesWithoutLosingText() {
        helper.createDatabase(DATABASE_NAME, 2).use { db ->
            db.execSQL("INSERT INTO semesters (id, name, is_active) VALUES (1, 'Semestr', 1)")
            db.execSQL("INSERT INTO study_programs (id, name, color) VALUES (1, 'Informatyka', '#112233')")
            db.execSQL(
                "INSERT INTO academic_calendars (id, semester_id, start_date, end_date, first_week_type) " +
                    "VALUES (1, 1, '2026-10-01', '2027-02-28', 'A')"
            )
            db.execSQL(
                "INSERT INTO semester_programs (id, semester_id, study_program_id, academic_calendar_id) " +
                    "VALUES (1, 1, 1, 1)"
            )
            db.execSQL(
                "INSERT INTO classes (id, semester_id, semester_program_id, name, type, teacher_name, day_of_week, " +
                    "start_time, end_time, room, building, `group`, recurrence, date, class_note) " +
                    "VALUES (1, 1, 1, 'Programowanie', 'Wykład', NULL, 1, '08:00', '09:30', NULL, NULL, NULL, " +
                    "'EVERY_WEEK', NULL, NULL)"
            )
            // Monday 5 Oct moved to Wednesday 7 Oct, with a note left on each date.
            insertMove(db, id = 1, from = "2026-10-05", to = "2026-10-07")
            insertNote(db, id = 1, date = "2026-10-05", body = "Przed przeniesieniem")
            insertNote(db, id = 2, date = "2026-10-07", body = "Po przeniesieniu")
            // Moved onto another Monday, where a regular occurrence may also take place.
            insertMove(db, id = 2, from = "2026-10-12", to = "2026-10-19")
            insertNote(db, id = 3, date = "2026-10-19", body = "Niejednoznaczna")
            // Plain move with a note only on the new date.
            insertMove(db, id = 3, from = "2026-10-26", to = "2026-10-28")
            insertNote(db, id = 4, date = "2026-10-28", body = "Przeniesiona")
        }

        helper.runMigrationsAndValidate(DATABASE_NAME, 3, true, MIGRATION_2_3).use { db ->
            db.query("SELECT id, occurrence_date, body FROM occurrence_notes ORDER BY id").use { cursor ->
                assertTrue(cursor.moveToNext())
                assertEquals(1L, cursor.getLong(0))
                assertEquals("2026-10-05", cursor.getString(1))
                assertEquals("Przed przeniesieniem\n\nPo przeniesieniu", cursor.getString(2))
                assertTrue(cursor.moveToNext())
                assertEquals(3L, cursor.getLong(0))
                assertEquals("2026-10-19", cursor.getString(1))
                assertTrue(cursor.moveToNext())
                assertEquals(4L, cursor.getLong(0))
                assertEquals("2026-10-26", cursor.getString(1))
                assertEquals("Przeniesiona", cursor.getString(2))
                assertFalse(cursor.moveToNext())
            }
        }
    }

    private fun insertMove(db: SupportSQLiteDatabase, id: Long, from: String, to: String) {
        db.execSQL(
            "INSERT INTO occurrence_changes (id, semester_id, class_id, original_date, kind, target_date, " +
                "new_start_time, new_end_time, new_room, new_building, new_teacher_name, new_note) " +
                "VALUES ($id, 1, 1, '$from', 'MODIFIED', '$to', NULL, NULL, NULL, NULL, NULL, NULL)"
        )
    }

    private fun insertNote(db: SupportSQLiteDatabase, id: Long, date: String, body: String) {
        db.execSQL(
            "INSERT INTO occurrence_notes (id, semester_id, class_id, occurrence_date, body) " +
                "VALUES ($id, 1, 1, '$date', '$body')"
        )
    }

    private companion object {
        const val DATABASE_NAME = "mak-migration-test.db"
    }
}
