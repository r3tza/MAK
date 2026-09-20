package dev.retza.mak.ui.semester

import dev.retza.mak.data.entity.WeekOverrideEntity
import dev.retza.mak.data.entity.WeekOverrideScope
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.ui.FakeMakRepository
import dev.retza.mak.ui.MainDispatcherRule
import dev.retza.mak.ui.feedback.FeedbackController
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SemesterViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    private fun viewModel(repository: FakeMakRepository) =
        SemesterViewModel(repository, FeedbackController())

    @Test
    fun openMapsFormCoursesAndOverrides() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.weekOverrides += WeekOverrideEntity(
            id = 5L,
            semesterId = 1L,
            weekStartDate = LocalDate.of(2026, 10, 5),
            weekType = WeekType.B,
            scope = WeekOverrideScope.ONE_WEEK
        )
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.open("1")
        advanceUntilIdle()

        val state = viewModel.semester.value
        assertEquals("Semestr", state.semester.name)
        assertEquals("2026-09-01", state.semester.startDate)
        assertEquals("2026-10-31", state.semester.endDate)
        assertEquals(WeekTypeUi.A, state.semester.firstWeek)
        assertEquals(listOf("1" to "Informatyka"), state.courses)
        assertEquals(1, state.overrides.size)
        assertEquals("2026-10-05", state.overrides.single().weekStartDate)
        assertEquals(WeekTypeUi.B, state.overrides.single().weekType)
        assertEquals(WeekOverrideScopeUi.ONE_WEEK, state.overrides.single().scope)
        assertEquals(1L, viewModel.semesterId.value)
    }

    @Test
    fun openWaitsForFirstDataEmission() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.occurrenceDataGate = CompletableDeferred()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.open("1")
        advanceUntilIdle()
        assertEquals("", viewModel.semester.value.semester.name)
        assertEquals(null, viewModel.semesterId.value)

        repository.occurrenceDataGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals("Semestr", viewModel.semester.value.semester.name)
        assertEquals(1L, viewModel.semesterId.value)
    }

    @Test
    fun lastOpenWinsWhenDataArrivesLate() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.occurrenceDataGate = CompletableDeferred()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.open("1")
        viewModel.open("2")
        advanceUntilIdle()
        assertEquals("", viewModel.semester.value.semester.name)

        repository.occurrenceDataGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals("Semestr drugi", viewModel.semester.value.semester.name)
        assertEquals(2L, viewModel.semesterId.value)
    }

    @Test
    fun invalidIdDoesNotKeepPreviousSemester() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()
        assertEquals("Semestr", viewModel.semester.value.semester.name)

        viewModel.open("99")
        advanceUntilIdle()

        assertEquals("", viewModel.semester.value.semester.name)
        assertEquals(null, viewModel.semesterId.value)
    }
}
