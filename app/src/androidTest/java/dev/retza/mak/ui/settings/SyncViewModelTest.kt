package dev.retza.mak.ui.settings

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.sync.AuthorizationAttempt
import dev.retza.mak.sync.PlanEditTracker
import dev.retza.mak.sync.PlanFileTransport
import dev.retza.mak.sync.PlanSyncGateway
import dev.retza.mak.sync.RemotePlanFile
import dev.retza.mak.sync.SyncAccount
import dev.retza.mak.sync.SyncArchive
import dev.retza.mak.sync.SyncAuthorization
import dev.retza.mak.sync.SyncCoordinator
import dev.retza.mak.sync.SyncState
import dev.retza.mak.sync.SyncStateStore
import dev.retza.mak.sync.SyncWorkScheduler
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import java.io.File
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/** Consent results arrive from another activity; only the answer to the current request may act. */
@RunWith(AndroidJUnit4::class)
class SyncViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val connected = SyncAccount("sub-1", "ala@example.com", "ala@example.com")
    private val authorization = FakeAuthorization()
    private val drive = EmptyDrive()
    private val store = MemoryStore()
    private val coordinator = SyncCoordinator(
        gateway = EmptyPlan(),
        transport = drive,
        store = store,
        archive = SyncArchive(File(context.cacheDir, "sync-vm-test-${System.nanoTime()}")),
        editTracker = PlanEditTracker(CoroutineScope(Dispatchers.Default)),
        clock = Clock.systemUTC(),
        ioDispatcher = Dispatchers.IO
    )
    private val viewModel = runBlocking(Dispatchers.Main) {
        SyncViewModel(coordinator, authorization, GlobalContext.get().get<SyncWorkScheduler>(), NoFeedback())
    }

    @Test
    fun resultWithoutAPendingRequestIsIgnored() = runBlocking {
        onMain { viewModel.onAuthorizationResult(succeeded = true, data = Intent()) }

        assertEquals(0, authorization.completed)
        assertNull(coordinator.state.value.account)
    }

    @Test
    fun cancelledConsentKeepsTheAccountAndUnlocksActions() = runBlocking {
        coordinator.connect(connected)
        authorization.next = AuthorizationAttempt.NeedsResolution(pendingIntent())
        onMain { viewModel.reconnect() }
        awaitState { it.isWorking }

        onMain { viewModel.onAuthorizationResult(succeeded = false, data = null) }

        awaitState { !it.isWorking }
        assertEquals(connected, coordinator.state.value.account)
    }

    @Test
    fun grantedConsentConnectsAndSynchronizes() = runBlocking {
        authorization.next = AuthorizationAttempt.NeedsResolution(pendingIntent())
        onMain { viewModel.connect() }
        awaitState { it.isWorking }
        authorization.next = AuthorizationAttempt.Granted(SyncAccount("sub-1", "ala@example.com", "ala@example.com"))

        onMain { viewModel.onAuthorizationResult(succeeded = true, data = Intent()) }

        awaitState { !it.isWorking && it.accountEmail != null }
        assertEquals(1, authorization.completed)
        assertEquals(1, drive.uploads)
    }

    private suspend fun awaitState(condition: (SyncUiState) -> Boolean) {
        withTimeout(5_000) { viewModel.sync.first(condition) }
    }

    private suspend fun onMain(block: () -> Unit) = kotlinx.coroutines.withContext(Dispatchers.Main) { block() }

    private fun pendingIntent(): PendingIntent =
        PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)

    private class FakeAuthorization : SyncAuthorization {
        var next: AuthorizationAttempt? = null
        var completed = 0

        override suspend fun beginAccountSelection() = requireNotNull(next)
        override suspend fun refreshPinnedAccount(account: SyncAccount) = requireNotNull(next)
        override suspend fun completeAccountSelection(resultIntent: Intent): AuthorizationAttempt {
            completed += 1
            return requireNotNull(next)
        }
        override suspend fun completePinnedAuthorization(resultIntent: Intent, account: SyncAccount): AuthorizationAttempt {
            completed += 1
            return requireNotNull(next)
        }
    }

    private class EmptyPlan : PlanSyncGateway {
        override suspend fun snapshot() = BackupData(emptyList(), emptyList())
        override suspend fun replaceIfUnchanged(
            expectedFingerprint: String,
            data: BackupData,
            keepLocalActive: Boolean,
            fallbackActiveId: Long?
        ) = true
    }

    private class EmptyDrive : PlanFileTransport {
        var uploads = 0
        private var file: RemotePlanFile? = null

        override suspend fun find(account: SyncAccount) = file
        override suspend fun download(account: SyncAccount, file: RemotePlanFile) = ByteArray(0)
        override suspend fun upload(account: SyncAccount, existing: RemotePlanFile?, bytes: ByteArray): RemotePlanFile {
            uploads += 1
            return RemotePlanFile("file-1", "md5-$uploads").also { file = it }
        }
        override suspend fun deleteAll(account: SyncAccount) {
            file = null
        }
    }

    private class MemoryStore : SyncStateStore {
        private var state = SyncState()
        override fun load() = state
        override fun save(state: SyncState) {
            this.state = state
        }
    }

    private class NoFeedback : FeedbackSink {
        override fun publish(feedback: UiFeedback) = Unit
    }
}
