package dev.retza.mak

import android.app.Application
import android.content.pm.ApplicationInfo
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.data.repository.ScheduleRepository
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.data.repository.seedDemoDataIfEmpty
import dev.retza.mak.notifications.CollisionAlarmScheduler
import dev.retza.mak.notifications.ensureCollisionChannel
import dev.retza.mak.ui.settings.SettingsPreferences
import dev.retza.mak.widget.GlanceWidgetRefreshRequester
import dev.retza.mak.widget.registerMakWidgetRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinApplication
import org.koin.plugin.module.dsl.startKoin

private const val NOTIFICATION_REFRESH_DEBOUNCE_MILLIS = 1_000L

@OptIn(ExperimentalCoroutinesApi::class)
@KoinApplication
class MakApplication : Application() {
    private val scheduleRepository: ScheduleRepository by inject()

    private val semesterRepository: SemesterRepository by inject()

    private val database: AppDatabase by inject()

    private val scheduler: CollisionAlarmScheduler by inject()

    private val preferences: SettingsPreferences by inject()

    private val initializationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        startKoin<MakApplication> {
            androidContext(this@MakApplication)
        }
        ensureCollisionChannel(this)
        registerMakWidgetRefresh(
            database = database,
            requester = GlanceWidgetRefreshRequester(this, initializationScope)
        )
        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebuggable) {
            initializationScope.launch {
                seedDemoDataIfEmpty(semesterRepository, scheduleRepository)
            }
        }
        initializationScope.launch {
            semesterRepository.observeActiveSemester()
                .flatMapLatest { semester ->
                    if (semester == null) flowOf(null) else scheduleRepository.observeActivePlanData(semester.id)
                }
                .combine(preferences.collisionNotifications) { data, settings -> data to settings }
                .debounce(NOTIFICATION_REFRESH_DEBOUNCE_MILLIS)
                .collect { scheduler.refresh() }
        }
    }
}
