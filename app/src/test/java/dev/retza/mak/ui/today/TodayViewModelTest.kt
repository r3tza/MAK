package dev.retza.mak.ui.today

import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.ui.FakeRepository
import dev.retza.mak.ui.MainDispatcherRule
import dev.retza.mak.ui.activePlanSource
import dev.retza.mak.ui.components.ScreenStatus
import dev.retza.mak.ui.settings.InMemorySettingsPreferences
import java.time.Clock
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
        val viewModel = todayViewModel(repository)
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
        val viewModel = todayViewModel(repository, clock = clock)
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
        repository.classes += repository.classes.single().copy(
            id = 2L,
            name = "Analiza",
            startTime = LocalTime.of(11, 1),
            endTime = LocalTime.of(12, 0)
        )
        val preferences = InMemorySettingsPreferences(initialGapThresholdMinutes = 30)
        val viewModel = todayViewModel(repository, preferences = preferences)
        backgroundScope.launch { viewModel.today.collect {} }
        advanceUntilIdle()

        val state = viewModel.today.value
        assertEquals(2, state.classCount)
        assertEquals(1, state.gapCount)
    }

    @Test
    fun minimumBreakTurnsShortBreakIntoCollision() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.classes += repository.classes.single().copy(
            id = 2L,
            name = "Analiza",
            startTime = LocalTime.of(10, 35),
            endTime = LocalTime.of(12, 0)
        )
        val preferences = InMemorySettingsPreferences()
        val viewModel = todayViewModel(repository, preferences = preferences)
        backgroundScope.launch { viewModel.today.collect {} }
        advanceUntilIdle()
        assertEquals(0, viewModel.today.value.collisionCount)

        preferences.setMinimumBreakMinutes(5)
        advanceUntilIdle()

        assertEquals(1, viewModel.today.value.collisionCount)
    }

    @Test
    fun startsInLoadingStateInsteadOfEmptyState() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.clearActiveSemester()
        val viewModel = todayViewModel(repository)

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
        val viewModel = todayViewModel(repository)
        backgroundScope.launch { viewModel.today.collect {} }
        advanceUntilIdle()

        assertEquals("Brak aktywnego semestru", viewModel.today.value.dateLabel)
        assertTrue(viewModel.today.value.items.isEmpty())
        assertFalse(viewModel.today.value.hasActiveSemester)
    }

    private fun todayViewModel(
        repository: FakeRepository,
        clock: Clock = Clock.fixed(Instant.parse("2026-09-21T08:00:00Z"), zone),
        preferences: InMemorySettingsPreferences = InMemorySettingsPreferences()
    ) = TodayViewModel(
        activePlanSource(repository, preferences),
        preferences,
        clock,
        ActivePlanProvider()
    )
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
