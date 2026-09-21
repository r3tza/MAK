package dev.retza.mak.ui.feedback

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.core.annotation.Single

interface FeedbackSink {
    fun publish(feedback: UiFeedback)
}

@Single(binds = [FeedbackSink::class])
class FeedbackController : FeedbackSink {
    private val channel = Channel<UiFeedback>(Channel.BUFFERED)

    val feedback: Flow<UiFeedback> = channel.receiveAsFlow()

    override fun publish(feedback: UiFeedback) {
        channel.trySend(feedback)
    }
}
