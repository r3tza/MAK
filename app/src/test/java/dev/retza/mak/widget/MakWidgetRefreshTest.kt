package dev.retza.mak.widget

import dev.retza.mak.domain.PlanDisplaySettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MakWidgetRefreshTest {
    @Test
    fun burstOfPlanChangesIsOneRequestAfterOneSecondOfQuiet() = runTest {
        val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
        val requester = CountingRequester()
        registerMakWidgetRefresh(changes, MutableStateFlow(PlanDisplaySettings(minimumBreakMinutes = 10)), backgroundScope, requester)
        runCurrent()

        repeat(3) {
            changes.emit(Unit)
            advanceTimeBy(100)
        }
        // The last change was at 200 ms; the request comes 1 s after it, at 1200 ms.
        advanceTimeBy(899)
        runCurrent()
        assertEquals(0, requester.count)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(1, requester.count)
    }

    @Test
    fun changedDisplaySettingsRequestOneRefreshButTheStoredValueDoesNot() = runTest {
        val display = MutableStateFlow(PlanDisplaySettings(minimumBreakMinutes = 10))
        val requester = CountingRequester()
        registerMakWidgetRefresh(MutableSharedFlow(), display, backgroundScope, requester)
        runCurrent()
        advanceTimeBy(1_000)
        assertEquals(0, requester.count)

        display.value = PlanDisplaySettings(minimumBreakMinutes = 20)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(1, requester.count)
    }
}

private class CountingRequester : WidgetRefreshRequester {
    var count = 0

    override fun request() {
        count++
    }
}
