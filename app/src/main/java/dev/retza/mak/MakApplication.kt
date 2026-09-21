package dev.retza.mak

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.datastore.preferences.preferencesDataStore
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.data.repository.RoomMakRepository
import dev.retza.mak.data.repository.seedDemoDataIfEmpty
import dev.retza.mak.ui.feedback.FeedbackController
import dev.retza.mak.ui.settings.DataStoreSettingsPreferences
import dev.retza.mak.ui.settings.SettingsPreferences
import dev.retza.mak.widget.GlanceWidgetRefreshRequester
import dev.retza.mak.widget.registerMakWidgetRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val Context.settingsDataStore by preferencesDataStore(name = "mak_settings")

class MakApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val repository: MakRepository by lazy { RoomMakRepository(database) }

    val feedbackController: FeedbackController by lazy { FeedbackController() }

    val settingsPreferences: SettingsPreferences by lazy {
        DataStoreSettingsPreferences(settingsDataStore)
    }

    private val initializationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
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
