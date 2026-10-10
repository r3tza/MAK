package dev.retza.mak.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import dev.retza.mak.data.PlanChanges
import java.io.IOException
import java.time.Clock
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val coordinator: SyncCoordinator
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = syncWorkResult { coordinator.synchronize() }
}

/**
 * Only network-side failures are retried; anything the user must act on ends the work as done. The
 * coordinator records every lasting error itself and lets only transient ones through.
 */
internal suspend fun syncWorkResult(run: suspend () -> SyncOutcome): ListenableWorker.Result = try {
    when (run()) {
        SyncOutcome.RetryLater -> ListenableWorker.Result.retry()
        // Choices, invalid files and consent need the app; closing an editor enqueues a new run.
        else -> ListenableWorker.Result.success()
    }
} catch (_: DriveHttpException) {
    ListenableWorker.Result.retry()
} catch (_: IOException) {
    ListenableWorker.Result.retry()
}

@Single
class SyncWorkerFactory(private val coordinator: SyncCoordinator) : WorkerFactory() {
    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? =
        if (workerClassName == SyncWorker::class.java.name) SyncWorker(appContext, workerParameters, coordinator) else null
}

@Single
class SyncWorkScheduler(
    private val workManager: WorkManager,
    private val coordinator: SyncCoordinator,
    private val backgroundDispatcher: CoroutineDispatcher
) {
    private val taskScope = CoroutineScope(SupervisorJob() + backgroundDispatcher)

    fun scheduleForAppOpen() {
        taskScope.launch {
            if (!hasActiveAccount()) {
                cancel()
                return@launch
            }
            enqueuePeriodicNow()
            enqueueImmediateNow()
        }
    }

    fun enqueueImmediate() {
        taskScope.launch { if (hasActiveAccount()) enqueueImmediateNow() }
    }

    private fun enqueueImmediateNow() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, INITIAL_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    fun schedulePeriodic() {
        taskScope.launch { if (hasActiveAccount()) enqueuePeriodicNow() }
    }

    private fun enqueuePeriodicNow() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIOD_MINUTES, TimeUnit.MINUTES)
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, INITIAL_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel() {
        workManager.cancelUniqueWork(ONE_TIME_WORK_NAME)
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    private fun networkConstraints() = androidx.work.Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private fun hasActiveAccount(): Boolean = coordinator.state.value.account != null

    companion object {
        const val ONE_TIME_WORK_NAME = "mak-google-sync-once"
        const val PERIODIC_WORK_NAME = "mak-google-sync-periodic"
        const val PERIOD_MINUTES = 60L
        const val INITIAL_BACKOFF_MILLIS = 30_000L
    }
}

/**
 * Starts a run after each plan change and after the last open editor closes if a run was waiting for it, and records when the
 * plan last changed, which the version choice shows.
 */
@Single
class SyncRoomChangeObserver(
    private val planChanges: PlanChanges,
    private val clock: Clock,
    private val editTracker: PlanEditTracker,
    private val scheduler: SyncWorkScheduler,
    private val coordinator: SyncCoordinator
) {
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    fun observe(scope: CoroutineScope) {
        scope.launch {
            planChanges.changes
                // The debounce keeps the time of the last write of a series, and the plan file is saved once per series.
                .map { clock.millis() }
                .debounce(1_000L)
                .collect { changedAt ->
                    try {
                        coordinator.recordLocalChange(changedAt)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        // Only a label in the version choice depends on it.
                    }
                    scheduler.enqueueImmediate()
                }
        }
        scope.launch {
            editTracker.editing
                .drop(1)
                .collect { editing ->
                    if (!editing && coordinator.waitsForEditor.value) scheduler.enqueueImmediate()
                }
        }
    }
}
