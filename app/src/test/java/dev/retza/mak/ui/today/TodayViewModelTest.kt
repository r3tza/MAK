package dev.retza.mak.ui.today

import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.Recurrence
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.ui.FakeRepository
import dev.retza.mak.ui.FakeSemesterRepository
import dev.retza.mak.ui.MainDispatcherRule
import dev.retza.mak.ui.components.ScreenStatus
import dev.retza.mak.ui.settings.InMemorySettingsPreferences
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TodayViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()
    private val zone = ZoneId.of("Europe/Warsaw")

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    @Test
    fun todayStateMapsActiveSemesterPlan() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = TodayViewModel(FakeSemesterRepository(repository), repository, InMemorySettingsPreferences(), Clock.fixed(Instant.parse("2026-09-21T08:00:00Z"), zone), ActivePlanProvider())
        backgroundScope.launch { viewModel.today.collect {} }
        advanceUntilIdle()

        val state = viewModel.today.value
        assertEquals("Poniedziałek, 21 września", state.dateLabel)
        assertEquals("Semestr", state.semesterLabel)
        assertTrue(state.weekLabel.startsWith("Tydzień"))
        assertEquals(1, state.classCount)
        assertEquals(0, state.collisionCount)
        assertEquals(0, state.gapCount)
        assertEquals("Programowanie", state.items.single().name)
    }

    @Test
    fun refreshTodayUpdatesTheDate() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val clock = MutableClock(Instant.parse("2026-09-21T08:00:00Z"), zone)
        val viewModel = TodayViewModel(FakeSemesterRepository(repository), repository, InMemorySettingsPreferences(), clock, ActivePlanProvider())
        backgroundScope.launch { viewModel.today.collect {} }
        advanceUntilIdle()
        assertEquals("Poniedziałek, 21 września", viewModel.today.value.dateLabel)

        clock.advanceTo(Instant.parse("2026-09-22T08:00:00Z"))
        viewModel.refreshToday()
        advanceUntilIdle()

        assertEquals("Wtorek, 22 września", viewModel.today.value.dateLabel)
    }

    @Test
    fun gapCountingUsesThreshold() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.classes += ClassEntity(
            id = 2L,
            semesterId = 1L,
            semesterProgramId = 1L,
            name = "Analiza",
            type = "Wykład",
            teacherName = null,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(11, 1),
            endTime = LocalTime.of(12, 0),
            room = null,
            building = null,
            group = null,
            recurrence = Recurrence.EVERY_WEEK,
            date = null,
            classNote = null
        )
        val preferences = InMemorySettingsPreferences(initialGapThresholdMinutes = 30)
        val viewModel = TodayViewModel(
            FakeSemesterRepository(repository),
            repository,
            preferences,
            Clock.fixed(Instant.parse("2026-09-21T08:00:00Z"), zone),
            ActivePlanProvider()
        )
        backgroundScope.launch { viewModel.today.collect {} }
        advanceUntilIdle()

        val state = viewModel.today.value
        assertEquals(2, state.classCount)
        assertEquals(1, state.gapCount)
    }

    @Test
    fun startsInLoadingStateInsteadOfEmptyState() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.clearActiveSemester()
        val viewModel = TodayViewModel(FakeSemesterRepository(repository), repository, InMemorySettingsPreferences(), Clock.fixed(Instant.parse("2026-09-21T08:00:00Z"), zone), ActivePlanProvider())

        assertEquals(ScreenStatus.Loading, viewModel.today.value.status)
        assertEquals("", viewModel.today.value.dateLabel)

        backgroundScope.launch { viewModel.today.collect {} }
        advanceUntilIdle()

        assertEquals(ScreenStatus.Ready, viewModel.today.value.status)
        assertEquals("Brak aktywnego semestru", viewModel.today.value.dateLabel)
    }

    @Test
    fun withoutActiveSemesterShowsEmptyState() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.clearActiveSemester()
        val viewModel = TodayViewModel(FakeSemesterRepository(repository), repository, InMemorySettingsPreferences(), Clock.fixed(Instant.parse("2026-09-21T08:00:00Z"), zone), ActivePlanProvider())
        backgroundScope.launch { viewModel.today.collect {} }
        advanceUntilIdle()

        assertEquals("Brak aktywnego semestru", viewModel.today.value.dateLabel)
        assertTrue(viewModel.today.value.items.isEmpty())
        assertFalse(viewModel.today.value.hasActiveSemester)
    }
}

private class MutableClock(
    private var current: Instant,
    private val zone: ZoneId
) : Clock() {
    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)

    override fun instant(): Instant = current

    fun advanceTo(instant: Instant) {
        current = instant
    }
}
