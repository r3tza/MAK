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
import dev.retza.mak.data.ActivePlanSource
import dev.retza.mak.settings.InMemorySettingsPreferences
import java.lang.reflect.Proxy
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
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
        val state = loader(repositoryReturning(semester, planData(programming))).states().first()

        assertTrue(state is WidgetUiState.Ready)
        assertEquals(listOf("3:2026-09-21"), (state as WidgetUiState.Ready).items.map { it.id })
    }

    @Test
    fun loaderUsesTheStoredMinimumBreak() = runTest {
        val analysis = programming.copy(id = 4, name = "Analiza", startTime = LocalTime.of(10, 5), endTime = LocalTime.of(11, 0))

        val state = loader(
            repositoryReturning(semester, planData(programming, analysis)),
            InMemorySettingsPreferences(initialPlanDisplay = PlanDisplaySettings(minimumBreakMinutes = 5))
        ).states().first()

        assertEquals(1, (state as WidgetUiState.Ready).collisionCount)
    }

    @Test
    fun loaderSaysWhenEveryStudyProgramIsHidden() = runTest {
        val state = loader(
            repositoryReturning(semester, planData(programming)),
            InMemorySettingsPreferences(initialPlanDisplay = PlanDisplaySettings.DEFAULT.copy(hiddenProgramIds = setOf(2L, 3L)))
        ).states().first()

        assertTrue(state is WidgetUiState.AllProgramsHidden)
    }

    @Test
    fun loaderReturnsNoActiveSemesterState() = runTest {
        val state = loader(repositoryReturning(null, null)).states().first()

        assertTrue(state is WidgetUiState.NoActiveSemester)
    }

    @Test
    fun loaderShowsTodayUntilTwoHoursAfterLastClassThenTomorrow() = runTest {
        val calls = mutableListOf<LocalDateTime>()
        val repository = repositoryReturning(semester, planData(programming))

        val beforeSwitch = loader(
            repository,
            clock = clockAt("2026-09-21T08:00:00Z"),
            daySwitchScheduler = WidgetDaySwitchScheduler { calls += it }
        ).states().first()

        assertTrue(beforeSwitch is WidgetUiState.Ready)
        assertEquals(listOf(LocalDateTime.of(2026, 9, 21, 9, 0)), calls)

        calls.clear()
        val afterSwitch = loader(
            repository,
            clock = clockAt("2026-09-21T12:30:00Z"),
            daySwitchScheduler = WidgetDaySwitchScheduler { calls += it }
        ).states().first()

        assertTrue(afterSwitch is WidgetUiState.EmptyDay)
        assertTrue((afterSwitch as WidgetUiState.EmptyDay).isTomorrow)
        assertTrue(afterSwitch.dateLabel.startsWith("Jutro, "))
        assertEquals(listOf(LocalDateTime.of(2026, 9, 22, 0, 0)), calls)
    }

    @Test
    fun loaderPlansRefreshAtClassEndThenAtSwitchToTomorrow() = runTest {
        val calls = mutableListOf<LocalDateTime>()
        val repository = repositoryReturning(semester, planData(programming))

        for (time in listOf("2026-09-21T09:30:00Z", "2026-09-21T10:30:00Z")) {
            loader(repository, clock = clockAt(time), daySwitchScheduler = WidgetDaySwitchScheduler { calls += it })
                .states().first()
        }

        assertEquals(listOf(LocalDateTime.of(2026, 9, 21, 10, 0), LocalDateTime.of(2026, 9, 21, 12, 0)), calls)
    }

    @Test
    fun loaderKeepsTodayWhenTomorrowSwitchIsTurnedOff() = runTest {
        val calls = mutableListOf<LocalDateTime>()

        val state = loader(
            repositoryReturning(semester, planData(programming)),
            InMemorySettingsPreferences(initialPlanDisplay = PlanDisplaySettings.DEFAULT.copy(showTomorrowInEvening = false)),
            clock = clockAt("2026-09-21T12:30:00Z"),
            daySwitchScheduler = WidgetDaySwitchScheduler { calls += it }
        ).states().first()

        assertTrue(state is WidgetUiState.Ready)
        assertEquals(listOf(LocalDateTime.of(2026, 9, 22, 0, 0)), calls)
    }

    @Test
    fun loaderPlansMidnightRefreshWithoutActiveSemester() = runTest {
        val calls = mutableListOf<LocalDateTime>()

        loader(
            repositoryReturning(null, null),
            daySwitchScheduler = WidgetDaySwitchScheduler { calls += it }
        ).states().first()

        assertEquals(listOf(LocalDateTime.of(2026, 9, 22, 0, 0)), calls)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun loaderRecalculatesPlanWhenTimeTickChanges() = runTest {
        val clock = MutableClock(Instant.parse("2026-09-21T08:00:00Z"), ZoneId.of("UTC"))
        val timeTicks = MutableStateFlow(0L)
        val states = mutableListOf<WidgetUiState>()
        val widgetStates = loader(
            repositoryReturning(semester, planData(programming)),
            clock = clock,
            timeTicks = timeTicks
        ).states()

        backgroundScope.launch { widgetStates.collect { states += it } }
        runCurrent()
        assertTrue(states.last() is WidgetUiState.Ready)

        clock.instant = Instant.parse("2026-09-21T12:30:00Z")
        timeTicks.value += 1
        runCurrent()

        assertTrue(states.last() is WidgetUiState.EmptyDay)
        assertTrue((states.last() as WidgetUiState.EmptyDay).isTomorrow)
    }

    @Test
    fun loaderMapsReadFailureToErrorState() = runTest {
        val repository = repositoryFailingWith(IllegalStateException("database closed"))

        val state = loader(repository).states().first()

        assertTrue(state is WidgetUiState.Error)
    }

    @Test
    fun loaderPropagatesCancellation() = runTest {
        val repository = repositoryFailingWith(CancellationException("widget update cancelled"))

        val cancelled = try {
            loader(repository).states().first()
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
        val informatics = StudyProgramEntity(id = 2, name = "Informatyka", color = "#137B71")
        val management = StudyProgramEntity(id = 3, name = "Zarządzanie", color = "#7B1371")
        return SemesterWithData(
            semester = semester,
            semesterPrograms = listOf(
                SemesterProgramEntity(id = 1, semesterId = semester.id, studyProgramId = informatics.id, academicCalendarId = calendar.id),
                SemesterProgramEntity(id = 2, semesterId = semester.id, studyProgramId = management.id, academicCalendarId = calendar.id)
            ),
            academicCalendars = listOf(calendar),
            studyPrograms = listOf(informatics, management),
            classes = classes.toList(),
            weekOverrides = emptyList(),
            occurrenceNotes = emptyList(),
            occurrenceChanges = emptyList(),
            freeDays = emptyList()
        )
    }

    private fun TestScope.loader(
        repository: Any,
        preferences: InMemorySettingsPreferences = InMemorySettingsPreferences(),
        clock: Clock = Clock.fixed(Instant.parse("2026-09-21T08:00:00Z"), ZoneId.of("UTC")),
        daySwitchScheduler: WidgetDaySwitchScheduler = WidgetDaySwitchScheduler {},
        timeTicks: Flow<Any?> = flowOf(Unit)
    ) = WidgetPlanLoader(
        semesterRepository = repository as SemesterRepository,
        activePlanSource = ActivePlanSource(repository, repository as ScheduleRepository, preferences, backgroundScope),
        activePlanProvider = ActivePlanProvider(),
        clock = clock,
        daySwitchScheduler = daySwitchScheduler,
        timeTicks = timeTicks
    )

    private class MutableClock(
        var instant: Instant,
        private val clockZone: ZoneId
    ) : Clock() {
        override fun getZone(): ZoneId = clockZone

        override fun withZone(zone: ZoneId): Clock = MutableClock(instant, zone)

        override fun instant(): Instant = instant
    }

    private fun clockAt(moment: String) = Clock.fixed(Instant.parse(moment), ZoneId.of("UTC"))

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
