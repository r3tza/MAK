package dev.retza.mak.ui.feedback

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiOperationTest {
    private val sink = RecordingFeedbackSink()
    private val calls = mutableListOf<String>()

    @Test
    fun successPublishesNothingAndFinishes() = runTest {
        launchUiOperation(sink, "Błąd", onFinish = { calls += "finish" }, onError = { calls += "error" }) {
            calls += "block"
        }
        advanceUntilIdle()

        assertEquals(listOf("block", "finish"), calls)
        assertTrue(sink.published.isEmpty())
    }

    @Test
    fun failurePublishesOneErrorAfterOnErrorAndFinishes() = runTest {
        launchUiOperation(
            sink,
            "Nie udało się zapisać.",
            onFinish = { calls += "finish" },
            onError = { calls += "error:${sink.published.size}" }
        ) {
            throw IllegalStateException("write failed")
        }
        advanceUntilIdle()

        // onError runs before the message is published, then the flag is cleared.
        assertEquals(listOf("error:0", "finish"), calls)
        assertEquals(listOf(UiFeedback("Nie udało się zapisać.", UiFeedbackKind.Error)), sink.published)
    }

    @Test
    fun failureWithoutMessageOnlyRunsOnError() = runTest {
        launchUiOperation(sink, null, onFinish = { calls += "finish" }, onError = { calls += "error" }) {
            throw IllegalStateException("read failed")
        }
        advanceUntilIdle()

        assertEquals(listOf("error", "finish"), calls)
        assertTrue(sink.published.isEmpty())
    }

    @Test
    fun cancellationPublishesNothingAndFinishes() = runTest {
        val gate = CompletableDeferred<Unit>()
        val job = launchUiOperation(sink, "Błąd", onFinish = { calls += "finish" }, onError = { calls += "error" }) {
            gate.await()
        }
        advanceUntilIdle()

        job.cancel()
        advanceUntilIdle()

        assertEquals(listOf("finish"), calls)
        assertTrue(sink.published.isEmpty())
    }

    @Test
    fun staleFailureSkipsErrorHandlingButFinishes() = runTest {
        launchUiOperation(
            sink,
            "Błąd",
            onFinish = { calls += "finish" },
            onError = { calls += "error" },
            isCurrent = { false }
        ) {
            throw IllegalStateException("write failed")
        }
        advanceUntilIdle()

        assertEquals(listOf("finish"), calls)
        assertTrue(sink.published.isEmpty())
    }
}

private class RecordingFeedbackSink : FeedbackSink {
    val published = mutableListOf<UiFeedback>()

    override fun publish(feedback: UiFeedback) {
        published += feedback
    }
}
