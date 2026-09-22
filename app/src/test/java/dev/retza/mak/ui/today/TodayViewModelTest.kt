package dev.retza.mak.ui.today

import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.ui.FakeMakRepository
import dev.retza.mak.ui.FakeSemesterRepository
import dev.retza.mak.ui.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
        val repository = FakeMakRepository()
        val viewModel = TodayViewModel(FakeSemesterRepository(repository), repository, Clock.fixed(Instant.parse("2026-09-21T08:00:00Z"), zone), ActivePlanProvider())
        backgroundScope.launch { viewModel.today.collect {} }
        advanceUntilIdle()

        val state = viewModel.today.value
        assertEquals("Poniedziałek, 21 września", state.dateLabel)
        assertEquals("Semestr", state.semesterLabel)
        assertTrue(state.weekLabel.startsWith("Tydzień"))
        assertEquals("1 zajęcie", state.summaryLabel)
        assertEquals("Programowanie", state.items.single().name)
    }

    @Test
    fun refreshTodayUpdatesTheDate() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val clock = MutableClock(Instant.parse("2026-09-21T08:00:00Z"), zone)
        val viewModel = TodayViewModel(FakeSemesterRepository(repository), repository, clock, ActivePlanProvider())
        backgroundScope.launch { viewModel.today.collect {} }
        advanceUntilIdle()
        assertEquals("Poniedziałek, 21 września", viewModel.today.value.dateLabel)

        clock.advanceTo(Instant.parse("2026-09-22T08:00:00Z"))
        viewModel.refreshToday()
        advanceUntilIdle()

        assertEquals("Wtorek, 22 września", viewModel.today.value.dateLabel)
    }

    @Test
    fun withoutActiveSemesterShowsEmptyState() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.clearActiveSemester()
        val viewModel = TodayViewModel(FakeSemesterRepository(repository), repository, Clock.fixed(Instant.parse("2026-09-21T08:00:00Z"), zone), ActivePlanProvider())
        backgroundScope.launch { viewModel.today.collect {} }
        advanceUntilIdle()

        assertEquals("Brak aktywnego semestru", viewModel.today.value.dateLabel)
        assertTrue(viewModel.today.value.items.isEmpty())
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
