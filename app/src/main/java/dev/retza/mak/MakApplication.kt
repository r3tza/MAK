package dev.retza.mak

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.work.Configuration
import androidx.work.WorkerFactory
import dev.retza.mak.data.PlanChanges
import dev.retza.mak.data.repository.ScheduleRepository
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.data.repository.seedDemoDataIfEmpty
import dev.retza.mak.notifications.CollisionAlarmScheduler
import dev.retza.mak.notifications.ensureCollisionChannel
import dev.retza.mak.ui.ActivePlanSource
import dev.retza.mak.ui.settings.SettingsPreferences
import dev.retza.mak.ui.settings.retainExistingHiddenPrograms
import dev.retza.mak.widget.GlanceWidgetRefreshRequester
import dev.retza.mak.widget.registerMakWidgetRefresh
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinApplication
import org.koin.core.context.GlobalContext
import org.koin.plugin.module.dsl.startKoin

private const val NOTIFICATION_REFRESH_DEBOUNCE_MILLIS = 1_000L

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@KoinApplication
class MakApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(GlobalContext.get().get<WorkerFactory>())
            .build()

    private val scheduleRepository: ScheduleRepository by inject()

    private val semesterRepository: SemesterRepository by inject()

    private val planChanges: PlanChanges by inject()

    private val scheduler: CollisionAlarmScheduler by inject()

    private val preferences: SettingsPreferences by inject()

    private val activePlanSource: ActivePlanSource by inject()

    private val syncRoomChangeObserver: dev.retza.mak.sync.SyncRoomChangeObserver by inject()

    private val initializationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        startKoin<MakApplication> {
            androidContext(this@MakApplication)
        }
        syncRoomChangeObserver.observe(initializationScope)
        ensureCollisionChannel(this)
        registerMakWidgetRefresh(
            planChanges = planChanges.changes,
            planDisplay = preferences.planDisplay,
            scope = initializationScope,
            requester = GlanceWidgetRefreshRequester(this, initializationScope)
        )
        initializationScope.launch {
            try {
                // Data from earlier versions may have no active semester; the rule lives in the repository.
                semesterRepository.ensureActiveSemester()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // The app still starts; the next start retries.
            }
        }
        initializationScope.launch {
            try {
                retainExistingHiddenPrograms(semesterRepository.observeStudyPrograms(), preferences)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // A stale hidden number only hides a program that does not exist; the next start retries.
            }
        }
        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebuggable) {
            initializationScope.launch {
                try {
                    seedDemoDataIfEmpty(semesterRepository, scheduleRepository)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    // Demo data is a debug convenience; the app works without it.
                }
            }
        }
        initializationScope.launch {
            preferences.collisionNotifications
                .flatMapLatest { settings ->
                    if (!settings.enabled) flowOf(settings to null)
                    else activePlanSource.observeActive().map { inputs -> settings to inputs }
                }
                .debounce(NOTIFICATION_REFRESH_DEBOUNCE_MILLIS)
                .collect { (settings, inputs) ->
                    try {
                        scheduler.refresh(settings, inputs)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        // Keep collecting; the next data or settings change retries the refresh.
                    }
                }
        }
    }
}
