package dev.retza.mak.widget

import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.data.repository.ScheduleRepository
import dev.retza.mak.data.repository.toActivePlanData
import dev.retza.mak.domain.ActivePlanProvider
import java.lang.reflect.Proxy
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPlanLoaderTest {
    @Test
    fun loaderUsesProviderAndReturnsTheSameOccurrenceIdsAsActivePlan() = runTest {
        val date = LocalDate.of(2026, 9, 21)
        val semester = SemesterEntity(id = 1, name = "Semestr", isActive = true)
        val calendar = AcademicCalendarEntity(
            id = 1,
            semesterId = semester.id,
            startDate = date,
            endDate = date.plusDays(7),
            firstWeekType = dev.retza.mak.data.entity.WeekType.A
        )
        val studyProgram = StudyProgramEntity(id = 2, name = "Informatyka", color = "#137B71")
        val assignment = SemesterProgramEntity(
            id = 1,
            semesterId = semester.id,
            studyProgramId = studyProgram.id,
            academicCalendarId = calendar.id
        )
        val classEntity = ClassEntity(
            id = 3,
            semesterId = semester.id,
            semesterProgramId = assignment.id,
            name = "Programowanie",
            type = "Wykład",
            teacherName = null,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            room = "L204",
            building = null,
            group = null,
            recurrence = dev.retza.mak.data.entity.Recurrence.EVERY_WEEK,
            date = null,
            classNote = null
        )
        val data = SemesterWithData(
            semester = semester,
            semesterPrograms = listOf(assignment),
            academicCalendars = listOf(calendar),
            studyPrograms = listOf(studyProgram),
            classes = listOf(classEntity),
            weekOverrides = emptyList(),
            occurrenceNotes = emptyList(),
            occurrenceChanges = emptyList()
        )
        val repository = repositoryReturning(semester, data)
        val clock = Clock.fixed(
            Instant.parse("2026-09-21T08:00:00Z"),
            ZoneId.of("UTC")
        )

        val state = WidgetPlanLoader(
            repository = repository,
            scheduleRepository = repository as ScheduleRepository,
            activePlanProvider = ActivePlanProvider(),
            clock = clock
        ).load()

        assertTrue(state is WidgetUiState.Ready)
        assertEquals(listOf("3:2026-09-21"), (state as WidgetUiState.Ready).items.map { it.id })
    }

    @Test
    fun loaderReturnsNoActiveSemesterState() = runTest {
        val repository = repositoryReturning(null, null)
        val clock = Clock.fixed(
            Instant.parse("2026-09-21T08:00:00Z"),
            ZoneId.of("UTC")
        )

        val state = WidgetPlanLoader(
            repository = repository,
            scheduleRepository = repository as ScheduleRepository,
            activePlanProvider = ActivePlanProvider(),
            clock = clock
        ).load()

        assertTrue(state is WidgetUiState.NoActiveSemester)
    }

    @Suppress("UNCHECKED_CAST")
    private fun repositoryReturning(
        semester: SemesterEntity?,
        data: SemesterWithData?
    ): MakRepository = Proxy.newProxyInstance(
        MakRepository::class.java.classLoader,
        arrayOf(MakRepository::class.java, ScheduleRepository::class.java)
    ) { _, method, _ ->
        when (method.name) {
            "observeActiveSemester" -> flowOf(semester)
            "observeActivePlanData" -> flowOf(data?.toActivePlanData())
            else -> error("Unexpected repository call: ${method.name}")
        }
    } as MakRepository
}
