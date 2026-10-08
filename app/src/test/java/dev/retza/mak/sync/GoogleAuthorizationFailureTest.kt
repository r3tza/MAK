package dev.retza.mak.sync

import androidx.work.ListenableWorker
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import dev.retza.mak.data.repository.BackupData
import java.io.IOException
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GoogleAuthorizationFailureTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun retryableAuthorizationFailureRetriesWithoutSavingPermanentFailure() = runTest {
        val account = SyncAccount("sub-1", "ala@example.com", "ala@example.com")
        val store = MemoryStore(SyncState(account = account))
        val transport = DrivePlanTransport(
            http = DriveHttpClient { _, _, _, _ -> error("No HTTP request should be made") },
            tokens = object : DriveAccessTokenProvider {
                override suspend fun tokenFor(account: SyncAccount): String = throw mapGoogleAuthorizationException(
                    ApiException(Status(CommonStatusCodes.NETWORK_ERROR))
                )

                override suspend fun invalidate(token: String) = Unit
            }
        )
        val coordinator = SyncCoordinator(
            gateway = EmptyPlan(),
            transport = transport,
            store = store,
            archive = SyncArchive(folder.newFolder("archive")),
            editTracker = PlanEditTracker(CoroutineScope(Dispatchers.Unconfined)),
            clock = Clock.systemUTC(),
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = syncWorkResult { coordinator.synchronize() }

        assertTrue(result is ListenableWorker.Result.Retry)
        assertNull(coordinator.state.value.issue)
        assertTrue(coordinator.state.value.account != null)
    }

    @Test
    fun knownTransientGoogleStatusesBecomeRetryableIoFailures() {
        listOf(
            CommonStatusCodes.NETWORK_ERROR,
            CommonStatusCodes.INTERNAL_ERROR,
            CommonStatusCodes.TIMEOUT,
            CommonStatusCodes.CONNECTION_SUSPENDED_DURING_CALL
        ).forEach { status ->
            assertTrue(
                "status $status",
                mapGoogleAuthorizationException(ApiException(Status(status))) is IOException
            )
        }
    }

    @Test
    fun developerErrorRemainsAConfigurationFailure() {
        val failure = ApiException(Status(CommonStatusCodes.DEVELOPER_ERROR))

        assertTrue(mapGoogleAuthorizationException(failure) === failure)
    }

    @Test
    fun nonTransientAuthorizationStatusesRemainUnchanged() {
        listOf(CommonStatusCodes.DEVELOPER_ERROR, CommonStatusCodes.SIGN_IN_REQUIRED, CommonStatusCodes.RESOLUTION_REQUIRED).forEach { status ->
            val error = ApiException(Status(status))
            assertTrue("status $status", mapGoogleAuthorizationException(error) === error)
        }
    }

    @Test
    fun cancellationRemainsCancellation() {
        val cancelled = CancellationException("already cancelled")
        val sdkCancelled = mapGoogleAuthorizationException(ApiException(Status(CommonStatusCodes.CANCELED)))

        assertTrue(mapGoogleAuthorizationException(cancelled) === cancelled)
        assertTrue(sdkCancelled is CancellationException)
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

    private class MemoryStore(private var state: SyncState) : SyncStateStore {
        override fun load() = state
        override fun save(state: SyncState) {
            this.state = state
        }
    }
}
