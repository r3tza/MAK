package dev.retza.mak.ui.feedback

import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UiFeedbackTest {
    @Test
    fun successAndInfoUseShortDuration() {
        assertEquals(
            SnackbarDuration.Short,
            UiFeedback("Zapisano", UiFeedbackKind.Success).toVisuals().duration
        )
        assertEquals(
            SnackbarDuration.Short,
            UiFeedback("Gotowe", UiFeedbackKind.Info).toVisuals().duration
        )
    }

    @Test
    fun errorAndWarningUseLongDuration() {
        assertEquals(
            SnackbarDuration.Long,
            UiFeedback("Błąd", UiFeedbackKind.Error).toVisuals().duration
        )
        assertEquals(
            SnackbarDuration.Long,
            UiFeedback("Uwaga", UiFeedbackKind.Warning).toVisuals().duration
        )
    }

    @Test
    fun visualsKeepMessageAndKind() {
        val visuals = UiFeedback("Zapisano", UiFeedbackKind.Success).toVisuals()
        assertEquals("Zapisano", visuals.message)
        assertEquals(UiFeedbackKind.Success, visuals.kind)
    }

    @Test
    fun channelHandsEachMessageToExactlyOneCollector() = runTest {
        val channel = Channel<UiFeedback>(Channel.BUFFERED)
        val first = mutableListOf<UiFeedback>()
        val second = mutableListOf<UiFeedback>()
        val firstJob = launch { channel.receiveAsFlow().collect { first += it } }
        val secondJob = launch { channel.receiveAsFlow().collect { second += it } }

        repeat(4) { index -> channel.send(UiFeedback("m$index", UiFeedbackKind.Info)) }
        advanceUntilIdle()
        firstJob.cancel()
        secondJob.cancel()

        assertEquals(4, first.size + second.size)
        assertEquals(
            listOf("m0", "m1", "m2", "m3"),
            (first + second).map { it.message }.sorted()
        )
    }
}
