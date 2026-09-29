package dev.retza.mak.ui.feedback

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Runs a user-started write: cancellation stays silent, a failure in the current session runs
 * [onError] and publishes [errorMessage] once, and [onFinish] always runs at the end.
 */
internal fun CoroutineScope.launchUiOperation(
    feedbackSink: FeedbackSink,
    errorMessage: String?,
    onFinish: () -> Unit,
    onError: () -> Unit = {},
    isCurrent: () -> Boolean = { true },
    block: suspend () -> Unit
): Job = launch {
    try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        if (isCurrent()) {
            onError()
            errorMessage?.let { feedbackSink.publish(UiFeedback(it, UiFeedbackKind.Error)) }
        }
    } finally {
        onFinish()
    }
}
