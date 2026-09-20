package dev.retza.mak.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.CourseEntity
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.Recurrence
import dev.retza.mak.data.entity.WeekOverrideEntity
import dev.retza.mak.data.entity.WeekOverrideScope
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceChangeKind
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.repository.RoomMakRepository
import java.time.DayOfWeek
import java.time.LocalTime
import dev.retza.mak.data.entity.WeekType
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        val expected = SemesterEntity(
            name = "Semestr zimowy",
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2027, 2, 28),
            firstWeekType = WeekType.A,
            isActive = true
        )

        database = openDatabase()
        val insertedId = database!!.semesterDao().insert(expected)
        val courseId = database!!.courseDao().insert(CourseEntity(semesterId = insertedId, name = "Informatyka", color = "#112233"))
        val classId = database!!.classDao().insert(
            ClassEntity(
                semesterId = insertedId,
                name = "Programowanie",
                type = "Wykład",
                courseId = courseId,
                teacherId = null,
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
            WeekOverrideEntity(semesterId = insertedId, weekStartDate = monday, weekType = WeekType.B, scope = WeekOverrideScope.ONE_WEEK)
        )
        val changeId = database!!.occurrenceChangeDao().insert(
            OccurrenceChangeEntity(
                semesterId = insertedId,
                classId = classId,
                originalDate = monday,
                kind = OccurrenceChangeKind.CANCELLED,
                targetDate = null,
                newStartTime = null,
                newEndTime = null,
                newRoom = null,
                newBuilding = null,
                newTeacherId = null,
                newNote = null
            )
        )
        val noteId = database!!.occurrenceNoteDao().insert(
            OccurrenceNoteEntity(semesterId = insertedId, classId = classId, occurrenceDate = monday, body = "Kolokwium")
        )
        database!!.close()

        database = openDatabase()
        val actual = database!!.semesterDao().findById(insertedId)

        assertEquals(expected.copy(id = insertedId), actual)
        assertEquals(overrideId, database!!.weekOverrideDao().findById(overrideId)?.id)
        assertEquals(changeId, database!!.occurrenceChangeDao().findById(changeId)?.id)
        assertEquals(noteId, database!!.occurrenceNoteDao().findById(noteId)?.id)
    }

    @Test
    fun setupConfigurationRollsBackSemesterWhenCourseFails() = runBlocking {
        database = openDatabase()
        val repository = RoomMakRepository(database!!)
        val otherSemesterId = database!!.semesterDao().insert(
            SemesterEntity(
                name = "Inny semestr",
                startDate = LocalDate.of(2026, 10, 1),
                endDate = LocalDate.of(2027, 2, 28),
                firstWeekType = WeekType.B,
                isActive = true
            )
        )
        val otherCourseId = database!!.courseDao().insert(
            CourseEntity(semesterId = otherSemesterId, name = "Inny kierunek", color = "#112233")
        )
        val before = database!!.semesterDao().observeAll().first()

        try {
            repository.saveSetupConfiguration(
                SemesterEntity(
                    name = "Nowy semestr",
                    startDate = LocalDate.of(2026, 10, 1),
                    endDate = LocalDate.of(2027, 2, 28),
                    firstWeekType = WeekType.A,
                    isActive = true
                ),
                CourseEntity(id = otherCourseId, semesterId = 0L, name = "Kierunek", color = "#445566")
            )
            fail("Expected the setup transaction to fail")
        } catch (_: IllegalArgumentException) {
        }

        val after = database!!.semesterDao().observeAll().first()
        assertEquals(before, after)
        assertNull(after.firstOrNull { it.name == "Nowy semestr" })
    }

    private fun openDatabase(): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, databaseFileName).build()
}
