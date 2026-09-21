package dev.retza.mak.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.data.database.AppDatabase
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
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.data.repository.RoomMakRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomPersistenceTest {
    private lateinit var context: Context
    private lateinit var databaseFileName: String
    private var database: AppDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        databaseFileName = "mak-room-persistence-${System.nanoTime()}.db"
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(databaseFileName)
    }

    @Test
    fun planRecordsAreReadableAfterOpeningDatabaseAgain() = runBlocking {
        val expected = SemesterEntity(name = "Semestr zimowy", isActive = true)

        database = openDatabase()
        val semesterId = database!!.semesterDao().insert(expected)
        val calendarId = database!!.academicCalendarDao().insert(
            AcademicCalendarEntity(
                semesterId = semesterId,
                startDate = LocalDate.of(2026, 10, 1),
                endDate = LocalDate.of(2027, 2, 28),
                firstWeekType = WeekType.A
            )
        )
        val programId = database!!.studyProgramDao().insert(
            StudyProgramEntity(name = "Informatyka", color = "#112233")
        )
        val assignmentId = database!!.semesterProgramDao().insert(
            SemesterProgramEntity(
                semesterId = semesterId,
                studyProgramId = programId,
                academicCalendarId = calendarId
            )
        )
        val classId = database!!.classDao().insert(
            ClassEntity(
                semesterId = semesterId,
                semesterProgramId = assignmentId,
                name = "Programowanie",
                type = "Wykład",
                teacherName = "Jan Kowalski",
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(9, 30),
                room = null,
                building = null,
                group = null,
                recurrence = Recurrence.EVERY_WEEK,
                date = null,
                classNote = "Notatka wspólna"
            )
        )
        val monday = LocalDate.of(2026, 10, 5)
        val overrideId = database!!.weekOverrideDao().insert(
            WeekOverrideEntity(
                semesterId = semesterId,
                academicCalendarId = calendarId,
                weekStartDate = monday,
                weekType = WeekType.B,
                scope = WeekOverrideScope.ONE_WEEK
            )
        )
        val changeId = database!!.occurrenceChangeDao().insert(
            OccurrenceChangeEntity(
                semesterId = semesterId,
                classId = classId,
                originalDate = monday,
                kind = OccurrenceChangeKind.CANCELLED,
                targetDate = null,
                newStartTime = null,
                newEndTime = null,
                newRoom = null,
                newBuilding = null,
                newTeacherName = null,
                newNote = null
            )
        )
        val noteId = database!!.occurrenceNoteDao().insert(
            OccurrenceNoteEntity(semesterId = semesterId, classId = classId, occurrenceDate = monday, body = "Kolokwium")
        )
        database!!.close()

        database = openDatabase()
        val actual = database!!.semesterDao().findById(semesterId)

        assertEquals(expected.copy(id = semesterId), actual)
        assertEquals(overrideId, database!!.weekOverrideDao().findById(overrideId)?.id)
        assertEquals(changeId, database!!.occurrenceChangeDao().findById(changeId)?.id)
        assertEquals(noteId, database!!.occurrenceNoteDao().findById(noteId)?.id)
    }

    @Test
    fun setupConfigurationRollsBackSemesterWhenProgramFails() = runBlocking {
        database = openDatabase()
        val repository = RoomMakRepository(database!!)
        val nextCalendarId = 1000L
        val before = database!!.semesterDao().observeAll().first()

        try {
            repository.saveSetupConfiguration(
                SemesterEntity(name = "Nowy semestr", isActive = true),
                StudyProgramEntity(id = 999L, name = "Kierunek", color = "#445566"),
                AcademicCalendarEntity(
                    id = nextCalendarId,
                    semesterId = 0L,
                    startDate = LocalDate.of(2026, 10, 1),
                    endDate = LocalDate.of(2027, 2, 28),
                    firstWeekType = WeekType.A
                )
            )
            fail("Expected the setup transaction to fail")
        } catch (_: IllegalArgumentException) {
        }

        val after = database!!.semesterDao().observeAll().first()
        assertEquals(before, after)
        assertNull(after.firstOrNull { it.name == "Nowy semestr" })
    }

    @Test
    fun deletingInactiveSemesterKeepsActive() = runBlocking {
        database = openDatabase()
        val repository = RoomMakRepository(database!!)
        val first = database!!.semesterDao().insert(semester("Pierwszy", true))
        val second = database!!.semesterDao().insert(semester("Drugi", true))
        database!!.semesterDao().markActive(second)

        val result = repository.deleteSemesterAndSelectFallback(first)

        assertEquals(second, result.activeSemesterId)
        assertEquals(second, repository.observeActiveSemester().first()?.id)
        assertEquals(listOf(second), repository.observeSemesters().first().map { it.id })
    }

    @Test
    fun deletingActiveSemesterSelectsDeterministicFallback() = runBlocking {
        database = openDatabase()
        val repository = RoomMakRepository(database!!)
        val first = database!!.semesterDao().insert(semester("Pierwszy", false))
        val second = database!!.semesterDao().insert(semester("Drugi", false))
        val third = database!!.semesterDao().insert(semester("Trzeci", false))
        database!!.semesterDao().markActive(second)

        val result = repository.deleteSemesterAndSelectFallback(second)

        assertEquals(first, result.activeSemesterId)
        assertEquals(first, repository.observeActiveSemester().first()?.id)
        assertEquals(listOf(first, third), repository.observeSemesters().first().map { it.id })
    }

    @Test
    fun deletingLastSemesterClearsActive() = runBlocking {
        database = openDatabase()
        val repository = RoomMakRepository(database!!)
        val only = database!!.semesterDao().insert(semester("Jedyny", true))
        database!!.semesterDao().markActive(only)

        val result = repository.deleteSemesterAndSelectFallback(only)

        assertNull(result.activeSemesterId)
        assertNull(repository.observeActiveSemester().first())
        assertTrue(repository.observeSemesters().first().isEmpty())
    }

    @Test
    fun deleteTransactionRollsBackWhenFallbackWriteFails() = runBlocking {
        database = openDatabase()
        val repository = RoomMakRepository(database!!)
        val first = database!!.semesterDao().insert(semester("Pierwszy", false))
        val second = database!!.semesterDao().insert(semester("Drugi", false))
        database!!.semesterDao().markActive(second)
        database!!.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_mark_active BEFORE UPDATE OF is_active ON semesters " +
                "WHEN NEW.is_active = 1 BEGIN SELECT RAISE(ABORT, 'mark active failed'); END"
        )

        try {
            repository.deleteSemesterAndSelectFallback(second)
            fail("Expected the delete transaction to fail")
        } catch (_: Exception) {
        }

        assertEquals(listOf(first, second), repository.observeSemesters().first().map { it.id })
        assertEquals(second, repository.observeActiveSemester().first()?.id)
    }

    @Test
    fun deletingAssignedStudyProgramIsBlocked() = runBlocking {
        database = openDatabase()
        val repository = RoomMakRepository(database!!)
        val semesterId = database!!.semesterDao().insert(semester("Semestr", true))
        val calendarId = database!!.academicCalendarDao().insert(
            AcademicCalendarEntity(
                semesterId = semesterId,
                startDate = LocalDate.of(2026, 10, 1),
                endDate = LocalDate.of(2027, 2, 28),
                firstWeekType = WeekType.A
            )
        )
        val programId = database!!.studyProgramDao().insert(
            StudyProgramEntity(name = "Informatyka", color = "#112233")
        )
        database!!.semesterProgramDao().insert(
            SemesterProgramEntity(
                semesterId = semesterId,
                studyProgramId = programId,
                academicCalendarId = calendarId
            )
        )

        try {
            repository.deleteStudyProgram(programId)
            fail("Expected deletion of an assigned study program to fail")
        } catch (_: IllegalArgumentException) {
        }

        assertEquals(programId, database!!.studyProgramDao().findById(programId)?.id)
    }

    private fun semester(name: String, active: Boolean) = SemesterEntity(
        name = name,
        isActive = active
    )

    private fun openDatabase(): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, databaseFileName).build()
}
