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
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val coordinator: SyncCoordinator
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        when (coordinator.synchronize()) {
            SyncOutcome.RetryLater -> Result.retry()
            // Choices, invalid files and consent need the app; closing an editor enqueues a new run.
            else -> Result.success()
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: DriveHttpException) {
        if (error.statusCode == 0 || error.statusCode == 429 || error.statusCode in 500..599) Result.retry() else Result.failure()
    } catch (_: IOException) {
        Result.retry()
    } catch (_: Exception) {
        Result.failure()
    }
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

/** Starts a run after each plan change and after the last open editor closes. */
@Single
class SyncRoomChangeObserver(
    private val database: dev.retza.mak.data.database.AppDatabase,
    private val editTracker: PlanEditTracker,
    private val scheduler: SyncWorkScheduler
) {
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    fun observe(scope: CoroutineScope) {
        scope.launch {
            database.invalidationTracker.createFlow(*SYNC_PLAN_TABLES, emitInitialState = false)
                .debounce(1_000L)
                .collect { scheduler.enqueueImmediate() }
        }
        scope.launch {
            editTracker.editorCount
                .map { it > 0 }
                .distinctUntilChanged()
                .drop(1)
                .collect { editing -> if (!editing) scheduler.enqueueImmediate() }
        }
    }
}

private val SYNC_PLAN_TABLES = arrayOf(
    "semesters", "study_programs", "academic_calendars", "semester_programs", "classes",
    "week_overrides", "occurrence_notes", "occurrence_changes"
)
