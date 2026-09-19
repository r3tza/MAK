package dev.retza.mak

import android.app.Application
import android.content.pm.ApplicationInfo
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.data.repository.RoomMakRepository
import dev.retza.mak.data.repository.seedDemoDataIfEmpty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MakApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val repository: MakRepository by lazy { RoomMakRepository(database) }

    private val initializationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebuggable) {
            initializationScope.launch {
                repository.seedDemoDataIfEmpty()
            }
        }
    }
}
