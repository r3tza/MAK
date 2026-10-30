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
import dev.retza.mak.data.repository.AcademicCalendarRecord
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.SemesterRecord
import dev.retza.mak.data.repository.StudyProgramRecord
import dev.retza.mak.data.repository.WeekOverrideRecord
import dev.retza.mak.data.repository.OccurrenceChangeRecord
import dev.retza.mak.data.repository.OccurrenceNoteRecord
import dev.retza.mak.data.repository.RoomPlanBackupGateway
import dev.retza.mak.data.repository.RoomScheduleRepository
import dev.retza.mak.data.repository.RoomSemesterRepository
import dev.retza.mak.data.repository.SemesterBackup
import dev.retza.mak.data.repository.toRecord
import dev.retza.mak.domain.Holiday
import dev.retza.mak.domain.OccurrenceChangeKind as ChangeKind
import dev.retza.mak.domain.WeekOverrideScope as OverrideScope
import dev.retza.mak.domain.WeekType as DomainWeekType
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    // An ordinary Monday of the test plan without its own note or change.
    private val changedDate = LocalDate.of(2026, 11, 9)
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-10-10T10:00:00Z"), ZoneId.of("Europe/Warsaw"))

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
    fun wholePlanIsReadBackUnchangedAfterOpeningDatabaseAgain() = runBlocking {
        val plan = RoomTestPlan.data()
        database = openDatabase()
        backup().replaceAll(plan)
        database!!.close()

        database = openDatabase()
        val stored = backup().snapshot()

        assertEquals(plan.sortedById(), stored.sortedById())
    }

    @Test
    fun deletingSemesterRemovesItsWholePlanButKeepsStudyPrograms() = runBlocking {
        openWithTestPlan()

        repository().deleteSemesterAndSelectFallback(RoomTestPlan.ACTIVE_SEMESTER)

        val stored = backup().snapshot()
        assertEquals(listOf(RoomTestPlan.OTHER_SEMESTER), stored.semesters.map { it.semester.id })
        assertEquals(
            listOf(RoomTestPlan.PROGRAM, RoomTestPlan.UNASSIGNED_PROGRAM),
            stored.studyPrograms.map { it.id }.sorted()
        )
        assertNull(database!!.classDao().findById(RoomTestPlan.WEEKLY_CLASS))
        assertNull(database!!.occurrenceNoteDao().findById(RoomTestPlan.NOTE))
        assertNull(database!!.occurrenceChangeDao().findById(RoomTestPlan.CANCELLED_CHANGE))
        assertTrue(database!!.weekOverrideDao().getForCalendar(RoomTestPlan.CALENDAR).isEmpty())
        assertTrue(database!!.freeDayDao().getForCalendar(RoomTestPlan.CALENDAR).isEmpty())
    }

    @Test
    fun deletingCourseDeletesItsClassesAndItsUnusedCalendar() = runBlocking {
        openWithTestPlan()

        repository().deleteSemesterProgram(RoomTestPlan.ASSIGNMENT)

        val semester = backup().snapshot().semesters.single { it.semester.id == RoomTestPlan.ACTIVE_SEMESTER }
        assertTrue(semester.programs.isEmpty())
        assertTrue(semester.classes.isEmpty())
        assertEquals(listOf(RoomTestPlan.UNUSED_CALENDAR), semester.calendars.map { it.id })
        assertNotNull(database!!.studyProgramDao().findById(RoomTestPlan.PROGRAM))
    }

    @Test
    fun onlyOneSemesterIsActiveAfterSavingOrSelectingAnother() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val first = repository.saveSemester(SemesterRecord(name = "Zimowy", isActive = true))

        val second = repository.saveSemester(SemesterRecord(name = "Letni", isActive = true))

        assertEquals(listOf(second), repository.observeSemesters().first().filter { it.isActive }.map { it.id })

        repository.setActiveSemester(first)

        assertEquals(listOf(first), repository.observeSemesters().first().filter { it.isActive }.map { it.id })
    }

    @Test
    fun savingWeekOverrideForTheSameWeekAndScopeReplacesItsType() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val semesterId = database!!.semesterDao().insert(semester("Semestr", true))
        val calendarId = insertCalendar(semesterId)
        val monday = LocalDate.of(2026, 10, 5)
        fun override(type: DomainWeekType, scope: OverrideScope) =
            WeekOverrideRecord(
                semesterId = semesterId,
                academicCalendarId = calendarId,
                weekStartDate = monday,
                weekType = type,
                scope = scope
            )

        val firstId = repository.saveWeekOverride(override(DomainWeekType.B, OverrideScope.ONE_WEEK))
        val secondId = repository.saveWeekOverride(override(DomainWeekType.A, OverrideScope.ONE_WEEK))
        repository.saveWeekOverride(override(DomainWeekType.B, OverrideScope.FROM_WEEK))

        assertEquals(firstId, secondId)
        val stored = database!!.weekOverrideDao().getForCalendar(calendarId).associate { it.scope to it.weekType }
        assertEquals(mapOf(WeekOverrideScope.ONE_WEEK to WeekType.A, WeekOverrideScope.FROM_WEEK to WeekType.B), stored)
    }

    @Test
    fun deleteByNaturalKeyRemovesOnlyThatRow() = runBlocking {
        database = openDatabase()
        val semesterId = database!!.semesterDao().insert(SemesterEntity(name = "Semestr zimowy", isActive = true))
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
                classNote = null
            )
        )
        val monday = LocalDate.of(2026, 10, 5)
        val nextMonday = monday.plusWeeks(1)
        listOf(monday, nextMonday).forEach { date ->
            database!!.occurrenceChangeDao().insert(
                OccurrenceChangeEntity(
                    semesterId = semesterId,
                    classId = classId,
                    originalDate = date,
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
            database!!.occurrenceNoteDao().insert(
                OccurrenceNoteEntity(semesterId = semesterId, classId = classId, occurrenceDate = date, body = "Kolokwium")
            )
        }
        listOf(WeekOverrideScope.ONE_WEEK, WeekOverrideScope.FROM_WEEK).forEach { scope ->
            database!!.weekOverrideDao().insert(
                WeekOverrideEntity(
                    semesterId = semesterId,
                    academicCalendarId = calendarId,
                    weekStartDate = monday,
                    weekType = WeekType.B,
                    scope = scope
                )
            )
        }

        database!!.occurrenceChangeDao().deleteForOccurrence(classId, monday)
        database!!.occurrenceNoteDao().deleteForOccurrence(classId, monday)
        database!!.weekOverrideDao().deleteForWeek(calendarId, monday, WeekOverrideScope.ONE_WEEK)

        assertNull(database!!.occurrenceChangeDao().findForOccurrence(classId, monday))
        assertNotNull(database!!.occurrenceChangeDao().findForOccurrence(classId, nextMonday))
        assertNull(database!!.occurrenceNoteDao().findForOccurrence(classId, monday))
        assertNotNull(database!!.occurrenceNoteDao().findForOccurrence(classId, nextMonday))
        assertEquals(
            listOf(WeekOverrideScope.FROM_WEEK),
            database!!.weekOverrideDao().getForCalendar(calendarId).map { it.scope }
        )
    }

    @Test
    fun setupConfigurationRollsBackSemesterWhenProgramFails() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val nextCalendarId = 1000L
        val before = database!!.semesterDao().observeAll().first()

        try {
            repository.saveSetupConfiguration(
                SemesterRecord(name = "Nowy semestr", isActive = true),
                StudyProgramRecord(id = 999L, name = "Kierunek", color = "#445566"),
                AcademicCalendarRecord(
                    id = nextCalendarId,
                    semesterId = 0L,
                    startDate = LocalDate.of(2026, 10, 1),
                    endDate = LocalDate.of(2027, 2, 28),
                    firstWeekType = dev.retza.mak.domain.WeekType.A
                ),
                activate = true
            )
            fail("Expected the setup transaction to fail")
        } catch (_: IllegalArgumentException) {
        }

        val after = database!!.semesterDao().observeAll().first()
        assertEquals(before, after)
        assertNull(after.firstOrNull { it.name == "Nowy semestr" })
    }

    @Test
    fun setupConfigurationWithoutActivationKeepsActiveSemester() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val current = database!!.semesterDao().insert(semester("Bieżący", true))
        database!!.semesterDao().markActive(current)

        val ids = repository.saveSetupConfiguration(
            SemesterRecord(name = "Letni", isActive = false),
            StudyProgramRecord(name = "Kierunek", color = "#445566"),
            AcademicCalendarRecord(
                semesterId = 0L,
                startDate = LocalDate.of(2027, 2, 22),
                endDate = LocalDate.of(2027, 6, 30),
                firstWeekType = dev.retza.mak.domain.WeekType.A
            ),
            activate = false
        )

        val semesters = database!!.semesterDao().observeAll().first()
        assertEquals(false, ids.semesterIsActive)
        assertEquals(listOf(current), semesters.filter { it.isActive }.map { it.id })
        assertEquals(false, semesters.single { it.id == ids.semesterId }.isActive)
    }

    @Test
    fun sharedCalendarsMapLowestCalendarPerSemester() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val first = database!!.semesterDao().insert(semester("Pierwszy", true))
        val second = database!!.semesterDao().insert(semester("Drugi", false))
        val firstCalendar = database!!.academicCalendarDao().insert(
            AcademicCalendarEntity(
                semesterId = first,
                startDate = LocalDate.of(2026, 9, 7),
                endDate = LocalDate.of(2027, 2, 14),
                firstWeekType = WeekType.A
            )
        )
        val secondCalendar = database!!.academicCalendarDao().insert(
            AcademicCalendarEntity(
                semesterId = second,
                startDate = LocalDate.of(2027, 2, 22),
                endDate = LocalDate.of(2027, 6, 15),
                firstWeekType = WeekType.A
            )
        )
        database!!.academicCalendarDao().insert(
            AcademicCalendarEntity(
                semesterId = first,
                startDate = LocalDate.of(2026, 10, 1),
                endDate = LocalDate.of(2027, 1, 31),
                firstWeekType = WeekType.B
            )
        )

        val shared = repository.observeSharedCalendars().first()

        assertEquals(mapOf(first to firstCalendar, second to secondCalendar), shared.mapValues { it.value.id })
    }

    @Test
    fun deletingInactiveSemesterKeepsActive() = runBlocking {
        database = openDatabase()
        val repository = repository()
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
        val repository = repository()
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
        val repository = repository()
        val only = database!!.semesterDao().insert(semester("Jedyny", true))
        database!!.semesterDao().markActive(only)

        val result = repository.deleteSemesterAndSelectFallback(only)

        assertNull(result.activeSemesterId)
        assertNull(repository.observeActiveSemester().first())
        assertTrue(repository.observeSemesters().first().isEmpty())
    }

    @Test
    fun deletingActiveSemesterActivatesTheOneCoveringToday() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val earlier = database!!.semesterDao().insert(semester("Wcześniejszy", false))
        val covering = database!!.semesterDao().insert(semester("Bieżący", false))
        val last = database!!.semesterDao().insert(semester("Następny", false))
        database!!.semesterDao().markActive(last)
        insertCalendar(earlier, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 6, 30))
        insertCalendar(covering, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31))
        insertCalendar(last, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 30))

        val result = repository.deleteSemesterAndSelectFallback(last)

        assertEquals(covering, result.activeSemesterId)
        assertEquals(covering, repository.observeActiveSemester().first()?.id)
    }

    @Test
    fun importWithoutActiveSemesterActivatesOne() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val data = BackupData(
            studyPrograms = emptyList(),
            semesters = listOf(
                semesterBackup(601, "Wcześniejszy", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 6, 30)),
                semesterBackup(602, "Bieżący", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31))
            )
        )

        val activeId = RoomPlanBackupGateway(database!!, fixedClock).replaceAll(data)

        assertEquals(602L, activeId)
        assertEquals(602L, repository.observeActiveSemester().first()?.id)
    }

    @Test
    fun deleteTransactionRollsBackWhenFallbackWriteFails() = runBlocking {
        database = openDatabase()
        val repository = repository()
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
        val repository = repository()
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
        assertEquals(listOf("Semestr"), repository.observeStudyProgramSemesters(programId).first())
    }

    @Test
    fun replaceAllDataSwapsWholeBackup() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val oldSemesterId = database!!.semesterDao().insert(semester("Stary", true))
        database!!.studyProgramDao().insert(StudyProgramEntity(name = "Stary kierunek", color = "#000000"))

        val newSemester = SemesterEntity(id = 500, name = "Nowy", isActive = true)
        val newProgram = StudyProgramEntity(id = 500, name = "Nowy kierunek", color = "#111111")
        val newCalendar = AcademicCalendarEntity(
            id = 501,
            semesterId = 500,
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2027, 2, 28),
            firstWeekType = WeekType.A
        )
        val newAssignment = SemesterProgramEntity(
            id = 502,
            semesterId = 500,
            studyProgramId = 500,
            academicCalendarId = 501
        )
        val newClass = ClassEntity(
            id = 503,
            semesterId = 500,
            semesterProgramId = 502,
            name = "Nowe zajęcia",
            type = "Wykład",
            teacherName = null,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(9, 30),
            room = null,
            building = null,
            group = null,
            recurrence = Recurrence.EVERY_WEEK,
            date = null,
            classNote = null
        )
        val data = BackupData(
            studyPrograms = listOf(newProgram.toRecord()),
            semesters = listOf(
                SemesterBackup(
                    semester = newSemester.toRecord(),
                    calendars = listOf(newCalendar.toRecord()),
                    programs = listOf(newAssignment.toRecord()),
                    classes = listOf(newClass.toRecord()),
                    weekOverrides = emptyList(),
                    freeDays = emptyList(),
                    occurrenceNotes = emptyList(),
                    occurrenceChanges = emptyList()
                )
            )
        )

        val activeId = RoomPlanBackupGateway(database!!, fixedClock).replaceAll(data)

        assertEquals(500L, activeId)
        assertNull(database!!.semesterDao().findById(oldSemesterId))
        assertEquals(listOf(500L), repository.observeSemesters().first().map { it.id })
        assertEquals(listOf(500L), repository.observeStudyPrograms().first().map { it.id })
        assertEquals(1, RoomScheduleRepository(database!!).observeActivePlanData(500).first()!!.classes.size)
    }

    @Test
    fun replaceAllDataRollsBackOnFailure() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val existingId = database!!.semesterDao().insert(semester("Istniejący", true))
        database!!.studyProgramDao().insert(StudyProgramEntity(name = "Istniejący kierunek", color = "#000000"))
        database!!.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_program_insert BEFORE INSERT ON study_programs " +
                "BEGIN SELECT RAISE(ABORT, 'insert failed'); END"
        )
        val data = BackupData(
            studyPrograms = listOf(StudyProgramEntity(id = 900, name = "Nowy", color = "#111111").toRecord()),
            semesters = listOf(
                SemesterBackup(
                    semester = SemesterEntity(id = 900, name = "Nowy", isActive = true).toRecord(),
                    calendars = emptyList(),
                    programs = emptyList(),
                    classes = emptyList(),
                    weekOverrides = emptyList(),
                    freeDays = emptyList(),
                    occurrenceNotes = emptyList(),
                    occurrenceChanges = emptyList()
                )
            )
        )

        try {
            RoomPlanBackupGateway(database!!, fixedClock).replaceAll(data)
            fail("Expected replaceAllData to fail")
        } catch (_: Exception) {
        }

        assertEquals(listOf(existingId), repository.observeSemesters().first().map { it.id })
        assertEquals(existingId, repository.observeActiveSemester().first()?.id)
        assertEquals(1, repository.observeStudyPrograms().first().size)
    }

    @Test
    fun separatingCalendarCopiesOverridesAndFreeDaysAndKeepsSource() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val semesterId = database!!.semesterDao().insert(semester("Semestr", true))
        val sourceCalendarId = insertCalendar(semesterId)
        val firstProgramId = insertProgram("Informatyka")
        val secondProgramId = insertProgram("Matematyka")
        val firstAssignmentId = insertAssignment(semesterId, firstProgramId, sourceCalendarId)
        val secondAssignmentId = insertAssignment(semesterId, secondProgramId, sourceCalendarId)
        val monday = LocalDate.of(2026, 10, 5)
        database!!.weekOverrideDao().insert(
            WeekOverrideEntity(
                semesterId = semesterId,
                academicCalendarId = sourceCalendarId,
                weekStartDate = monday,
                weekType = WeekType.B,
                scope = WeekOverrideScope.ONE_WEEK
            )
        )
        val holiday = LocalDate.of(2026, 11, 11)
        repository.saveFreeDayGroup(
            semesterId = semesterId,
            replacedIds = emptyList(),
            startDate = holiday,
            endDate = holiday,
            name = "Święto",
            calendarIds = listOf(sourceCalendarId)
        )

        val result = repository.separateSemesterProgramCalendar(firstAssignmentId)

        val separated = database!!.academicCalendarDao().findById(result.academicCalendarId)
        assertEquals(LocalDate.of(2026, 10, 1), separated?.startDate)
        assertEquals(LocalDate.of(2027, 2, 28), separated?.endDate)
        assertEquals(WeekType.A, separated?.firstWeekType)
        assertEquals(
            result.academicCalendarId,
            database!!.semesterProgramDao().findById(firstAssignmentId)?.academicCalendarId
        )
        assertEquals(
            sourceCalendarId,
            database!!.semesterProgramDao().findById(secondAssignmentId)?.academicCalendarId
        )
        val copied = database!!.weekOverrideDao().getForCalendar(result.academicCalendarId)
        assertEquals(1, copied.size)
        assertEquals(monday, copied.single().weekStartDate)
        assertEquals(WeekType.B, copied.single().weekType)
        assertEquals(WeekOverrideScope.ONE_WEEK, copied.single().scope)
        assertEquals(1, database!!.weekOverrideDao().getForCalendar(sourceCalendarId).size)
        assertEquals(
            listOf(holiday to "Święto"),
            database!!.freeDayDao().getForCalendar(result.academicCalendarId).map { it.startDate to it.name }
        )
        assertEquals(1, database!!.freeDayDao().getForCalendar(sourceCalendarId).size)
        assertEquals(sourceCalendarId, database!!.academicCalendarDao().findById(sourceCalendarId)?.id)
    }

    @Test
    fun addingSeparatedProgramCopiesSourceCalendarAndOverrides() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val semesterId = database!!.semesterDao().insert(semester("Semestr", true))
        val sourceCalendarId = insertCalendar(semesterId)
        database!!.weekOverrideDao().insert(
            WeekOverrideEntity(
                semesterId = semesterId,
                academicCalendarId = sourceCalendarId,
                weekStartDate = LocalDate.of(2026, 10, 5),
                weekType = WeekType.B,
                scope = WeekOverrideScope.ONE_WEEK
            )
        )

        val result = repository.addSeparatedSemesterProgram(
            semesterId = semesterId,
            studyProgram = StudyProgramRecord(name = "Informatyka", color = "#112233"),
            sourceCalendarId = sourceCalendarId
        )

        val separated = database!!.academicCalendarDao().findById(result.academicCalendarId)
        assertEquals(LocalDate.of(2026, 10, 1), separated?.startDate)
        assertEquals(
            result.academicCalendarId,
            database!!.semesterProgramDao().findById(result.semesterProgramId)?.academicCalendarId
        )
        assertEquals(1, database!!.weekOverrideDao().getForCalendar(result.academicCalendarId).size)
        assertEquals(1, database!!.weekOverrideDao().getForCalendar(sourceCalendarId).size)
        assertEquals(sourceCalendarId, database!!.academicCalendarDao().findById(sourceCalendarId)?.id)
    }

    @Test
    fun assigningExistingProgramDoesNotChangeItsData() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val semesterId = database!!.semesterDao().insert(semester("Semestr", true))
        val calendarId = insertCalendar(semesterId)
        val programId = insertProgram("Informatyka")

        repository.saveStudyProgramAssignment(
            semesterId = semesterId,
            studyProgram = StudyProgramRecord(id = programId, name = "Zmieniona", color = "#FFFFFF"),
            academicCalendarId = calendarId
        )

        val stored = database!!.studyProgramDao().findById(programId)
        assertEquals("Informatyka", stored?.name)
        assertEquals("#112233", stored?.color)
    }

    @Test
    fun separatedAssignmentDoesNotChangeExistingProgram() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val semesterId = database!!.semesterDao().insert(semester("Semestr", true))
        val calendarId = insertCalendar(semesterId)
        val programId = insertProgram("Informatyka")

        repository.addSeparatedSemesterProgram(
            semesterId = semesterId,
            studyProgram = StudyProgramRecord(id = programId, name = "Zmieniona", color = "#FFFFFF"),
            sourceCalendarId = calendarId
        )

        val stored = database!!.studyProgramDao().findById(programId)
        assertEquals("Informatyka", stored?.name)
        assertEquals("#112233", stored?.color)
    }

    @Test
    fun reconnectingDeletesOrphanCalendarWithOverrides() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val semesterId = database!!.semesterDao().insert(semester("Semestr", true))
        val sourceCalendarId = insertCalendar(semesterId)
        val targetCalendarId = insertCalendar(semesterId)
        val firstProgramId = insertProgram("Informatyka")
        val secondProgramId = insertProgram("Matematyka")
        val firstAssignmentId = insertAssignment(semesterId, firstProgramId, sourceCalendarId)
        insertAssignment(semesterId, secondProgramId, targetCalendarId)
        database!!.weekOverrideDao().insert(
            WeekOverrideEntity(
                semesterId = semesterId,
                academicCalendarId = sourceCalendarId,
                weekStartDate = LocalDate.of(2026, 10, 5),
                weekType = WeekType.B,
                scope = WeekOverrideScope.ONE_WEEK
            )
        )

        repository.reconnectSemesterProgram(firstAssignmentId, targetCalendarId)

        assertNull(database!!.academicCalendarDao().findById(sourceCalendarId))
        assertTrue(database!!.weekOverrideDao().getForCalendar(sourceCalendarId).isEmpty())
        assertEquals(
            targetCalendarId,
            database!!.semesterProgramDao().findById(firstAssignmentId)?.academicCalendarId
        )
    }

    @Test
    fun reassigningProgramDeletesOrphanCalendarWithOverrides() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val semesterId = database!!.semesterDao().insert(semester("Semestr", true))
        val sourceCalendarId = insertCalendar(semesterId)
        val targetCalendarId = insertCalendar(semesterId)
        val programId = insertProgram("Informatyka")
        val assignmentId = insertAssignment(semesterId, programId, sourceCalendarId)
        database!!.weekOverrideDao().insert(
            WeekOverrideEntity(
                semesterId = semesterId,
                academicCalendarId = sourceCalendarId,
                weekStartDate = LocalDate.of(2026, 10, 5),
                weekType = WeekType.B,
                scope = WeekOverrideScope.ONE_WEEK
            )
        )

        repository.saveStudyProgramAssignment(
            semesterId,
            StudyProgramRecord(id = programId, name = "Informatyka", color = "#112233"),
            targetCalendarId
        )

        assertNull(database!!.academicCalendarDao().findById(sourceCalendarId))
        assertTrue(database!!.weekOverrideDao().getForCalendar(sourceCalendarId).isEmpty())
        assertEquals(targetCalendarId, database!!.semesterProgramDao().findById(assignmentId)?.academicCalendarId)
    }

    @Test
    fun reassigningProgramKeepsCalendarStillUsedByAnotherProgram() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val semesterId = database!!.semesterDao().insert(semester("Semestr", true))
        val sourceCalendarId = insertCalendar(semesterId)
        val targetCalendarId = insertCalendar(semesterId)
        val firstProgramId = insertProgram("Informatyka")
        val secondProgramId = insertProgram("Matematyka")
        insertAssignment(semesterId, firstProgramId, sourceCalendarId)
        insertAssignment(semesterId, secondProgramId, sourceCalendarId)

        repository.saveStudyProgramAssignment(
            semesterId,
            StudyProgramRecord(id = firstProgramId, name = "Informatyka", color = "#112233"),
            targetCalendarId
        )

        assertEquals(sourceCalendarId, database!!.academicCalendarDao().findById(sourceCalendarId)?.id)
    }

    @Test
    fun reconnectingKeepsSourceCalendarWhenStillUsed() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val semesterId = database!!.semesterDao().insert(semester("Semestr", true))
        val sourceCalendarId = insertCalendar(semesterId)
        val targetCalendarId = insertCalendar(semesterId)
        val firstProgramId = insertProgram("Informatyka")
        val secondProgramId = insertProgram("Matematyka")
        val firstAssignmentId = insertAssignment(semesterId, firstProgramId, sourceCalendarId)
        insertAssignment(semesterId, secondProgramId, sourceCalendarId)

        repository.reconnectSemesterProgram(firstAssignmentId, targetCalendarId)

        assertEquals(sourceCalendarId, database!!.academicCalendarDao().findById(sourceCalendarId)?.id)
        assertEquals(
            targetCalendarId,
            database!!.semesterProgramDao().findById(firstAssignmentId)?.academicCalendarId
        )
    }

    @Test
    fun deletingUsedCalendarIsBlocked() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val semesterId = database!!.semesterDao().insert(semester("Semestr", true))
        val calendarId = insertCalendar(semesterId)
        val programId = insertProgram("Informatyka")
        insertAssignment(semesterId, programId, calendarId)

        try {
            repository.deleteCalendar(calendarId)
            fail("Expected deletion of a used calendar to fail")
        } catch (_: IllegalArgumentException) {
        }

        assertEquals(calendarId, database!!.academicCalendarDao().findById(calendarId)?.id)
    }

    @Test
    fun semesterConfigurationEmitsAfterAWrite() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val semesterId = database!!.semesterDao().insert(semester("Semestr", true))
        val calendarId = insertCalendar(semesterId)
        val monday = LocalDate.of(2026, 10, 5)

        val configurations = repository.observeSemesterConfiguration(semesterId)
        assertTrue(configurations.first()?.overrides?.isEmpty() == true)

        repository.saveWeekOverride(
            WeekOverrideRecord(
                semesterId = semesterId,
                academicCalendarId = calendarId,
                weekStartDate = monday,
                weekType = dev.retza.mak.domain.WeekType.B,
                scope = dev.retza.mak.domain.WeekOverrideScope.ONE_WEEK
            )
        )

        val updated = configurations.first { it?.overrides?.isNotEmpty() == true }
        assertEquals(monday, updated?.overrides?.single()?.weekStartDate)
        assertEquals(dev.retza.mak.domain.WeekType.B, updated?.overrides?.single()?.weekType)
    }

    @Test
    fun saveFreeDayGroupReplacesGroupInEachCalendar() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val semesterId = database!!.semesterDao().insert(semester("Semestr", true))
        val firstCalendarId = insertCalendar(semesterId)
        val secondCalendarId = insertCalendar(semesterId)
        val holiday = LocalDate.of(2026, 11, 11)
        repository.saveFreeDayGroup(
            semesterId = semesterId,
            replacedIds = emptyList(),
            startDate = holiday,
            endDate = holiday,
            name = "Święto",
            calendarIds = listOf(firstCalendarId, secondCalendarId)
        )
        val groupIds = database!!.freeDayDao().getForSemester(semesterId).map { it.id }
        assertEquals(2, groupIds.size)

        repository.saveFreeDayGroup(
            semesterId = semesterId,
            replacedIds = groupIds,
            startDate = LocalDate.of(2026, 11, 12),
            endDate = LocalDate.of(2026, 11, 13),
            name = "  ",
            calendarIds = listOf(firstCalendarId)
        )

        val days = repository.observeFreeDays(semesterId).first()
        assertEquals(1, days.size)
        assertEquals(firstCalendarId, days.single().academicCalendarId)
        assertEquals(LocalDate.of(2026, 11, 12), days.single().startDate)
        assertEquals(LocalDate.of(2026, 11, 13), days.single().endDate)
        assertEquals(null, days.single().name)
    }

    @Test
    fun saveFreeDayGroupWithWeekAfterBreakAddsFromWeekOverride() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val semesterId = database!!.semesterDao().insert(semester("Semestr", true))
        val calendarId = insertCalendar(semesterId)
        repository.saveFreeDayGroup(
            semesterId = semesterId,
            replacedIds = emptyList(),
            startDate = LocalDate.of(2026, 11, 2),
            endDate = LocalDate.of(2026, 11, 15),
            name = "Przerwa",
            calendarIds = listOf(calendarId),
            weekAfterBreak = dev.retza.mak.domain.WeekType.B
        )

        val overrides = repository.observeWeekOverrides(semesterId).first()
        assertEquals(1, overrides.size)
        assertEquals(LocalDate.of(2026, 11, 16), overrides.single().weekStartDate)
        assertEquals("B", overrides.single().weekType.name)
        assertEquals("FROM_WEEK", overrides.single().scope.name)
    }

    @Test
    fun addHolidaysAddsOneDayFreeDaysInEachCalendar() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val semesterId = database!!.semesterDao().insert(semester("Semestr", true))
        val firstCalendarId = insertCalendar(semesterId)
        val secondCalendarId = insertCalendar(semesterId)
        val holidays = listOf(
            Holiday(LocalDate.of(2026, 11, 11), "Święto Niepodległości"),
            Holiday(LocalDate.of(2026, 12, 25), "Boże Narodzenie")
        )
        repository.addHolidays(
            semesterId = semesterId,
            holidays = holidays,
            calendarIds = listOf(firstCalendarId, secondCalendarId)
        )

        val days = repository.observeFreeDays(semesterId).first()
        assertEquals(4, days.size)
        assertTrue(days.all { it.startDate == it.endDate })
        assertEquals(
            listOf(firstCalendarId, firstCalendarId, secondCalendarId, secondCalendarId),
            days.sortedBy { it.id }.map { it.academicCalendarId }
        )
        assertEquals(
            listOf("Święto Niepodległości", "Boże Narodzenie"),
            days.filter { it.academicCalendarId == firstCalendarId }.map { it.name }
        )
    }

    @Test
    fun addHolidaysSavesTheChristmasBreakAsOneRangePerCalendar() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val semesterId = database!!.semesterDao().insert(semester("Semestr", true))
        val firstCalendarId = insertCalendar(semesterId)
        val secondCalendarId = insertCalendar(semesterId)

        repository.addHolidays(
            semesterId = semesterId,
            holidays = listOf(Holiday(LocalDate.of(2026, 12, 24), "Przerwa świąteczna", LocalDate.of(2027, 1, 2))),
            calendarIds = listOf(firstCalendarId, secondCalendarId)
        )

        val days = repository.observeFreeDays(semesterId).first()
        assertEquals(2, days.size)
        assertTrue(days.all { it.startDate == LocalDate.of(2026, 12, 24) && it.endDate == LocalDate.of(2027, 1, 2) })
        assertEquals(setOf(firstCalendarId, secondCalendarId), days.map { it.academicCalendarId }.toSet())
    }

    @Test
    fun deletingCalendarDeletesItsFreeDays() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val semesterId = database!!.semesterDao().insert(semester("Semestr", true))
        val firstCalendarId = insertCalendar(semesterId)
        val secondCalendarId = insertCalendar(semesterId)
        val holiday = LocalDate.of(2026, 11, 11)
        repository.saveFreeDayGroup(
            semesterId = semesterId,
            replacedIds = emptyList(),
            startDate = holiday,
            endDate = holiday,
            name = null,
            calendarIds = listOf(firstCalendarId, secondCalendarId)
        )

        repository.deleteCalendar(secondCalendarId)

        val days = repository.observeFreeDays(semesterId).first()
        assertEquals(listOf(firstCalendarId), days.map { it.academicCalendarId })
    }

    @Test
    fun saveStudyProgramInActiveSemesterAssignsSharedCalendar() = runBlocking {
        database = openDatabase()
        val repository = repository()
        val semesterId = database!!.semesterDao().insert(semester("Semestr", true))
        val sharedCalendarId = insertCalendar(semesterId)
        insertCalendar(semesterId)

        val result = repository.saveStudyProgramInActiveSemester(
            StudyProgramRecord(name = "Fizyka", color = "#112233")
        )

        assertEquals(semesterId, result?.id)
        val assignments = repository.observeSemesterPrograms(semesterId).first()
        assertEquals(listOf(sharedCalendarId), assignments.map { it.academicCalendarId })
    }

    @Test
    fun saveStudyProgramInActiveSemesterWithoutSemesterSavesProgramOnly() = runBlocking {
        database = openDatabase()
        val repository = repository()

        val result = repository.saveStudyProgramInActiveSemester(
            StudyProgramRecord(name = "Fizyka", color = "#112233")
        )

        assertNull(result)
        val programs = repository.observeStudyPrograms().first()
        assertEquals(listOf("Fizyka"), programs.map { it.name })
        assertTrue(repository.observeStudyProgramSemesters(programs.single().id).first().isEmpty())
    }

    @Test
    fun savingNoteAgainForTheSameOccurrenceReplacesItsText() = runBlocking {
        openWithTestPlan()
        val firstId = scheduleRepository().saveOccurrenceNote(note("Kolokwium"))

        val secondId = scheduleRepository().saveOccurrenceNote(note("Kolokwium przełożone"))

        assertEquals(firstId, secondId)
        assertEquals(
            listOf("Kolokwium przełożone"),
            planData().occurrenceNotes.filter { it.occurrenceDate == changedDate }.map { it.body }
        )
    }

    @Test
    fun savingChangeAgainForTheSameOccurrenceReplacesIt() = runBlocking {
        openWithTestPlan()
        val firstId = scheduleRepository().saveOccurrenceChange(
            change(ChangeKind.MODIFIED, start = LocalTime.of(10, 0), end = LocalTime.of(11, 30))
        )

        val secondId = scheduleRepository().saveOccurrenceChange(change(ChangeKind.CANCELLED))

        assertEquals(firstId, secondId)
        val stored = planData().occurrenceChanges.single { it.originalDate == changedDate }
        assertEquals(ChangeKind.CANCELLED, stored.kind)
        assertNull(stored.startTime)
        assertNull(stored.endTime)
    }

    @Test
    fun deletingClassDeletesItsNotesAndChanges() = runBlocking {
        openWithTestPlan()
        scheduleRepository().saveOccurrenceNote(note("Kolokwium"))
        scheduleRepository().saveOccurrenceChange(change(ChangeKind.CANCELLED))

        scheduleRepository().deleteClass(RoomTestPlan.WEEKLY_CLASS)

        val plan = planData()
        assertTrue(plan.classes.none { it.id == RoomTestPlan.WEEKLY_CLASS })
        assertTrue(plan.occurrenceNotes.none { it.classId == RoomTestPlan.WEEKLY_CLASS })
        assertTrue(plan.occurrenceChanges.none { it.classId == RoomTestPlan.WEEKLY_CLASS })
    }

    @Test
    fun recordsOfAClassCannotBeSavedInAnotherSemester() = runBlocking {
        openWithTestPlan()
        val otherAssignment = insertAssignment(RoomTestPlan.OTHER_SEMESTER, RoomTestPlan.UNASSIGNED_PROGRAM, RoomTestPlan.OTHER_SEMESTER_CALENDAR)
        val before = backup().snapshot()
        val weekly = before.semesters.first().classes.single { it.id == RoomTestPlan.WEEKLY_CLASS }

        assertRejected { scheduleRepository().saveClass(weekly.copy(id = 0, semesterProgramId = otherAssignment)) }
        assertRejected { scheduleRepository().saveClass(weekly.copy(semesterId = RoomTestPlan.OTHER_SEMESTER, semesterProgramId = otherAssignment)) }
        assertRejected { scheduleRepository().saveOccurrenceNote(note("Kolokwium").copy(semesterId = RoomTestPlan.OTHER_SEMESTER)) }
        assertRejected { scheduleRepository().saveOccurrenceChange(change(ChangeKind.CANCELLED).copy(semesterId = RoomTestPlan.OTHER_SEMESTER)) }

        assertEquals(before.sortedById(), backup().snapshot().sortedById())
    }

    private suspend fun planData() = scheduleRepository().observeActivePlanData(RoomTestPlan.ACTIVE_SEMESTER).first()!!

    private fun note(body: String) = OccurrenceNoteRecord(
        semesterId = RoomTestPlan.ACTIVE_SEMESTER,
        classId = RoomTestPlan.WEEKLY_CLASS,
        occurrenceDate = changedDate,
        body = body
    )

    private fun change(kind: ChangeKind, start: LocalTime? = null, end: LocalTime? = null) =
        OccurrenceChangeRecord(
            semesterId = RoomTestPlan.ACTIVE_SEMESTER,
            classId = RoomTestPlan.WEEKLY_CLASS,
            originalDate = changedDate,
            kind = kind,
            targetDate = null,
            startTime = start,
            endTime = end,
            room = null,
            building = null,
            teacherName = null,
            note = null
        )

    private suspend fun assertRejected(write: suspend () -> Unit) {
        try {
            write()
            fail("Expected the write to be rejected")
        } catch (_: IllegalArgumentException) {
        }
    }

    private suspend fun openWithTestPlan() {
        database = openDatabase()
        backup().replaceAll(RoomTestPlan.data())
    }

    private fun backup() = RoomPlanBackupGateway(database!!, fixedClock)

    private fun scheduleRepository() = RoomScheduleRepository(database!!)

    private suspend fun insertCalendar(semesterId: Long): Long =
        database!!.academicCalendarDao().insert(
            AcademicCalendarEntity(
                semesterId = semesterId,
                startDate = LocalDate.of(2026, 10, 1),
                endDate = LocalDate.of(2027, 2, 28),
                firstWeekType = WeekType.A
            )
        )

    private suspend fun insertProgram(name: String): Long =
        database!!.studyProgramDao().insert(StudyProgramEntity(name = name, color = "#112233"))

    private suspend fun insertAssignment(
        semesterId: Long,
        studyProgramId: Long,
        calendarId: Long
    ): Long = database!!.semesterProgramDao().insert(
        SemesterProgramEntity(
            semesterId = semesterId,
            studyProgramId = studyProgramId,
            academicCalendarId = calendarId
        )
    )

    private fun repository(): RoomSemesterRepository = RoomSemesterRepository(database!!, fixedClock)

    private suspend fun insertCalendar(semesterId: Long, startDate: LocalDate, endDate: LocalDate) {
        database!!.academicCalendarDao().insert(
            AcademicCalendarEntity(
                semesterId = semesterId,
                startDate = startDate,
                endDate = endDate,
                firstWeekType = WeekType.A
            )
        )
    }

    private fun semesterBackup(id: Long, name: String, startDate: LocalDate, endDate: LocalDate) = SemesterBackup(
        semester = SemesterEntity(id = id, name = name, isActive = false).toRecord(),
        calendars = listOf(
            AcademicCalendarEntity(
                id = id * 10,
                semesterId = id,
                startDate = startDate,
                endDate = endDate,
                firstWeekType = WeekType.A
            ).toRecord()
        ),
        programs = emptyList(),
        classes = emptyList(),
        weekOverrides = emptyList(),
        freeDays = emptyList(),
        occurrenceNotes = emptyList(),
        occurrenceChanges = emptyList()
    )

    private fun semester(name: String, active: Boolean) = SemesterEntity(
        name = name,
        isActive = active
    )

    private fun openDatabase(): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, databaseFileName).build()
}
