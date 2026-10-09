package dev.retza.mak.widget

import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.Recurrence
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.data.repository.ScheduleRepository
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.data.repository.toActivePlanData
import dev.retza.mak.data.repository.toRecord
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.domain.PlanDisplaySettings
import dev.retza.mak.ui.ActivePlanSource
import dev.retza.mak.ui.settings.InMemorySettingsPreferences
import java.lang.reflect.Proxy
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPlanLoaderTest {
    private val date = LocalDate.of(2026, 9, 21)
    private val semester = SemesterEntity(id = 1, name = "Semestr", isActive = true)
    private val programming = ClassEntity(
        id = 3,
        semesterId = semester.id,
        semesterProgramId = 1,
        name = "Programowanie",
        type = "Wykład",
        teacherName = null,
        dayOfWeek = DayOfWeek.MONDAY,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 0),
        room = "L204",
        building = null,
        group = null,
        recurrence = Recurrence.EVERY_WEEK,
        date = null,
        classNote = null
    )

    @Test
    fun loaderUsesProviderAndReturnsTheSameOccurrenceIdsAsActivePlan() = runTest {
        val state = loader(repositoryReturning(semester, planData(programming))).load()

        assertTrue(state is WidgetUiState.Ready)
        assertEquals(listOf("3:2026-09-21"), (state as WidgetUiState.Ready).items.map { it.id })
    }

    @Test
    fun loaderUsesTheStoredMinimumBreak() = runTest {
        val analysis = programming.copy(id = 4, name = "Analiza", startTime = LocalTime.of(10, 5), endTime = LocalTime.of(11, 0))

        val state = loader(
            repositoryReturning(semester, planData(programming, analysis)),
            InMemorySettingsPreferences(initialPlanDisplay = PlanDisplaySettings(minimumBreakMinutes = 5))
        ).load()

        assertEquals(1, (state as WidgetUiState.Ready).collisionCount)
    }

    @Test
    fun loaderReturnsNoActiveSemesterState() = runTest {
        val state = loader(repositoryReturning(null, null)).load()

        assertTrue(state is WidgetUiState.NoActiveSemester)
    }

    @Test
    fun loaderMapsReadFailureToErrorState() = runTest {
        val repository = repositoryFailingWith(IllegalStateException("database closed"))

        val state = loader(repository).load()

        assertTrue(state is WidgetUiState.Error)
    }

    @Test
    fun loaderPropagatesCancellation() = runTest {
        val repository = repositoryFailingWith(CancellationException("widget update cancelled"))

        val cancelled = try {
            loader(repository).load()
            false
        } catch (error: CancellationException) {
            true
        }

        assertTrue(cancelled)
    }

    private fun planData(vararg classes: ClassEntity): SemesterWithData {
        val calendar = AcademicCalendarEntity(
            id = 1,
            semesterId = semester.id,
            startDate = date,
            endDate = date.plusDays(7),
            firstWeekType = WeekType.A
        )
        val studyProgram = StudyProgramEntity(id = 2, name = "Informatyka", color = "#137B71")
        return SemesterWithData(
            semester = semester,
            semesterPrograms = listOf(
                SemesterProgramEntity(
                    id = 1,
                    semesterId = semester.id,
                    studyProgramId = studyProgram.id,
                    academicCalendarId = calendar.id
                )
            ),
            academicCalendars = listOf(calendar),
            studyPrograms = listOf(studyProgram),
            classes = classes.toList(),
            weekOverrides = emptyList(),
            occurrenceNotes = emptyList(),
            occurrenceChanges = emptyList()
        )
    }

    private fun loader(
        repository: Any,
        preferences: InMemorySettingsPreferences = InMemorySettingsPreferences()
    ) = WidgetPlanLoader(
        semesterRepository = repository as SemesterRepository,
        activePlanSource = ActivePlanSource(repository, repository as ScheduleRepository, preferences),
        activePlanProvider = ActivePlanProvider(),
        clock = Clock.fixed(Instant.parse("2026-09-21T08:00:00Z"), ZoneId.of("UTC"))
    )

    private fun repositoryFailingWith(error: Throwable): Any = Proxy.newProxyInstance(
        SemesterRepository::class.java.classLoader,
        arrayOf(SemesterRepository::class.java, ScheduleRepository::class.java)
    ) { _, method, _ ->
        when (method.name) {
            "observeActiveSemester" -> flow<Any?> { throw error }
            else -> error("Unexpected repository call: ${method.name}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun repositoryReturning(
        semester: SemesterEntity?,
        data: SemesterWithData?
    ): Any = Proxy.newProxyInstance(
        SemesterRepository::class.java.classLoader,
        arrayOf(SemesterRepository::class.java, ScheduleRepository::class.java)
    ) { _, method, _ ->
        when (method.name) {
            "observeActiveSemester" -> flowOf(semester?.toRecord())
            "observeActivePlanData" -> flowOf(data?.toActivePlanData())
            else -> error("Unexpected repository call: ${method.name}")
        }
    }
}
