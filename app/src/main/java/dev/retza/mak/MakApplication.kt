package dev.retza.mak

import android.app.Application
import android.content.pm.ApplicationInfo
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.data.repository.seedDemoDataIfEmpty
import dev.retza.mak.widget.GlanceWidgetRefreshRequester
import dev.retza.mak.widget.registerMakWidgetRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinApplication
import org.koin.plugin.module.dsl.startKoin

@KoinApplication
class MakApplication : Application() {
    val repository: MakRepository by inject()

    private val database: AppDatabase by inject()

    private val initializationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        startKoin<MakApplication> {
            androidContext(this@MakApplication)
        }
        registerMakWidgetRefresh(
            database = database,
            requester = GlanceWidgetRefreshRequester(this, initializationScope)
        )
        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebuggable) {
            initializationScope.launch {
                repository.seedDemoDataIfEmpty()
            }
        }
    }
}
