package dev.retza.mak.ui.feedback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedbackControllerTest {
    @Test
    fun handsEachMessageToExactlyOneCollector() = runTest {
        val controller = FeedbackController()
        val first = mutableListOf<UiFeedback>()
        val second = mutableListOf<UiFeedback>()
        val firstJob = launch { controller.feedback.collect { first += it } }
        val secondJob = launch { controller.feedback.collect { second += it } }

        repeat(4) { index -> controller.publish(UiFeedback("m$index", UiFeedbackKind.Info)) }
        advanceUntilIdle()
        firstJob.cancel()
        secondJob.cancel()

        assertEquals(4, first.size + second.size)
        assertEquals(
            listOf("m0", "m1", "m2", "m3"),
            (first + second).map { it.message }.sorted()
        )
    }

    @Test
    fun keepsQueueOrderForSingleCollector() = runTest {
        val controller = FeedbackController()
        val received = mutableListOf<String>()
        val job = launch { controller.feedback.collect { received += it.message } }

        controller.publish(UiFeedback("Pierwszy", UiFeedbackKind.Success))
        controller.publish(UiFeedback("Drugi", UiFeedbackKind.Error))
        controller.publish(UiFeedback("Trzeci", UiFeedbackKind.Info))
        advanceUntilIdle()
        job.cancel()

        assertEquals(listOf("Pierwszy", "Drugi", "Trzeci"), received)
    }
}
