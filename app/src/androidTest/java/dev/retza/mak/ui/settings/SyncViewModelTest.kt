package dev.retza.mak.ui.settings

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.os.StrictMode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.sync.AuthorizationAttempt
import dev.retza.mak.sync.ArchiveSource
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
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    private val archiveDirectory = File(context.cacheDir, "sync-vm-test-${System.nanoTime()}")
    private val archive = SyncArchive(archiveDirectory)
    private val feedback = RecordingFeedback()
    private val coordinator = SyncCoordinator(
        gateway = EmptyPlan(),
        transport = drive,
        store = store,
        archive = archive,
        editTracker = PlanEditTracker(CoroutineScope(Dispatchers.Default)),
        clock = Clock.systemUTC(),
        ioDispatcher = Dispatchers.IO
    )
    private val viewModel = runBlocking(Dispatchers.Main) {
        SyncViewModel(coordinator, authorization, GlobalContext.get().get<SyncWorkScheduler>(), feedback)
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

        awaitState { !it.isWorking && it.accountEmail != null && drive.uploads == 1 }
        assertEquals(1, authorization.completed)
        assertEquals(1, drive.uploads)
    }

    @Test
    fun archiveIsWrittenOnIoAndSuccessWaitsForStreamClose() = runBlocking {
        val bytes = "archived plan".toByteArray()
        archive.add(bytes, ArchiveSource.PHONE, System.currentTimeMillis())
        val archiveId = archive.list().single().id
        val stored = java.io.ByteArrayOutputStream()
        var closed = false
        var ranOnMain = true
        val violations = AtomicInteger()
        val priorPolicy = onMain {
            StrictMode.getThreadPolicy().also {
                StrictMode.setThreadPolicy(
                    StrictMode.ThreadPolicy.Builder().detectDiskReads()
                        .penaltyListener(Executor { command -> command.run() }) { violations.incrementAndGet() }
                        .build()
                )
            }
        }
        try {
            val job = onMain {
                viewModel.exportArchivedPlan(archiveId) {
                    ranOnMain = Looper.myLooper() == Looper.getMainLooper()
                    object : OutputStream() {
                        override fun write(value: Int) = stored.write(value)
                        override fun write(bytes: ByteArray, offset: Int, length: Int) = stored.write(bytes, offset, length)
                        override fun close() {
                            closed = true
                        }
                    }
                }!!
            }
            job.join()
        } finally {
            onMain { StrictMode.setThreadPolicy(priorPolicy) }
        }

        assertEquals(bytes.toList(), stored.toByteArray().toList())
        assertEquals(true, closed)
        assertEquals(false, ranOnMain)
        assertEquals(0, violations.get())
        assertEquals("Zapisano poprzednią wersję planu", feedback.records.single().message)
    }

    @Test
    fun missingArchiveReportsFailureWithoutOpeningDestination() = runBlocking {
        var opened = false

        onMain { viewModel.exportArchivedPlan("missing") { opened = true; null } }

        awaitFeedbackCount(1)
        assertEquals(false, opened)
        assertEquals("Nie udało się zapisać pliku.", feedback.records.single().message)
        assertEquals(dev.retza.mak.ui.feedback.UiFeedbackKind.Error, feedback.records.single().kind)
    }

    @Test
    fun archiveReadExceptionDoesNotOpenDestination() = runBlocking {
        archive.add("archived plan".toByteArray(), ArchiveSource.DRIVE, System.currentTimeMillis())
        val id = archive.list().single().id
        val archivedFile = File(archiveDirectory, "$id.json")
        assertEquals(true, archivedFile.setReadable(false, false))
        try {
            assertTrue(runCatching { archive.read(id) }.exceptionOrNull() is IOException)
            var opened = false

            onMain { viewModel.exportArchivedPlan(id) { opened = true; null } }

            awaitFeedbackCount(1)
            assertEquals(false, opened)
            assertEquals(dev.retza.mak.ui.feedback.UiFeedbackKind.Error, feedback.records.single().kind)
        } finally {
            archivedFile.setReadable(true, false)
        }
    }

    @Test
    fun failedWriteReportsNoSuccess() = runBlocking {
        archive.add("archived plan".toByteArray(), ArchiveSource.DRIVE, System.currentTimeMillis())

        onMain {
            viewModel.exportArchivedPlan(archive.list().single().id) {
                object : OutputStream() {
                    override fun write(value: Int) = throw IOException("write failed")
                }
            }
        }

        awaitFeedbackCount(1)
        assertEquals(dev.retza.mak.ui.feedback.UiFeedbackKind.Error, feedback.records.single().kind)
    }

    @Test
    fun failedCloseReportsNoSuccess() = runBlocking {
        archive.add("archived plan".toByteArray(), ArchiveSource.DRIVE, System.currentTimeMillis())

        onMain {
            viewModel.exportArchivedPlan(archive.list().single().id) {
                object : OutputStream() {
                    override fun write(value: Int) = Unit
                    override fun close() = throw IOException("close failed")
                }
            }
        }

        awaitFeedbackCount(1)
        assertEquals(dev.retza.mak.ui.feedback.UiFeedbackKind.Error, feedback.records.single().kind)
    }

    @Test
    fun overlappingArchiveExportsAreIgnored() = runBlocking {
        archive.add("archived plan".toByteArray(), ArchiveSource.PHONE, System.currentTimeMillis())
        val opened = CompletableDeferred<Unit>()
        val releaseWrite = CountDownLatch(1)
        var secondOutputOpened = false

        onMain {
            viewModel.exportArchivedPlan(archive.list().single().id) {
                opened.complete(Unit)
                object : OutputStream() {
                    override fun write(value: Int) = waitForRelease()
                    override fun write(bytes: ByteArray, offset: Int, length: Int) = waitForRelease()
                    private fun waitForRelease() {
                        if (!releaseWrite.await(5, TimeUnit.SECONDS)) throw IOException("test timed out")
                    }
                }
            }
        }
        opened.await()
        assertEquals(true, onMain { viewModel.isWorkingNow() })
        onMain { viewModel.exportArchivedPlan(archive.list().single().id) { secondOutputOpened = true; null } }
        releaseWrite.countDown()

        awaitFeedbackCount(1)
        assertEquals(false, onMain { viewModel.isWorkingNow() })
        assertEquals(false, secondOutputOpened)
        assertEquals(1, feedback.records.size)
    }

    @Test
    fun cancelledExportDoesNotReportSuccessOrFailure() = runBlocking {
        archive.add("archived plan".toByteArray(), ArchiveSource.PHONE, System.currentTimeMillis())
        val writing = CompletableDeferred<Unit>()
        val releaseWrite = CountDownLatch(1)

        val exportJob = onMain {
            viewModel.exportArchivedPlan(archive.list().single().id) {
                writing.complete(Unit)
                object : OutputStream() {
                    override fun write(value: Int) {
                        if (!releaseWrite.await(5, TimeUnit.SECONDS)) throw IOException("test timed out")
                        throw IOException("write failed after cancellation")
                    }
                }
            }!!
        }

        writing.await()
        exportJob.cancel()
        releaseWrite.countDown()
        exportJob.join()
        assertEquals(emptyList<UiFeedback>(), feedback.records)
    }

    private suspend fun awaitState(condition: (SyncUiState) -> Boolean) {
        withTimeout(5_000) { viewModel.sync.first(condition) }
    }

    private suspend fun awaitFeedbackCount(count: Int) {
        withTimeout(5_000) { repeat(count) { feedback.published.receive() } }
    }

    private suspend fun <T> onMain(block: () -> T): T = kotlinx.coroutines.withContext(Dispatchers.Main) { block() }

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

    private class RecordingFeedback : FeedbackSink {
        val records = mutableListOf<UiFeedback>()
        val published = Channel<Unit>(Channel.UNLIMITED)
        override fun publish(feedback: UiFeedback) {
            records += feedback
            published.trySend(Unit)
        }
    }
}
