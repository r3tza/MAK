package dev.retza.mak.sync

import androidx.work.ListenableWorker
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncWorkResultTest {
    @Test
    fun networkSideFailuresAreRetried() = runTest {
        assertTrue(syncWorkResult { throw IOException("offline") } is ListenableWorker.Result.Retry)
        assertTrue(syncWorkResult { throw DriveHttpException(503, "unavailable") } is ListenableWorker.Result.Retry)
        assertTrue(syncWorkResult { SyncOutcome.RetryLater } is ListenableWorker.Result.Retry)
    }

    @Test
    fun outcomesThatNeedTheUserEndTheWork() = runTest {
        // Retrying these would only repeat the same question or error in the background.
        listOf(SyncOutcome.NeedsAttention, SyncOutcome.ChoiceRequired, SyncOutcome.WaitingForEditor).forEach { outcome ->
            assertTrue(outcome.toString(), syncWorkResult { outcome } is ListenableWorker.Result.Success)
        }
    }
}
