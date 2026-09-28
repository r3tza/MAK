package dev.retza.mak.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.update.InstalledAppInfoProvider
import dev.retza.mak.update.UpdateCheckService
import dev.retza.mak.update.UpdateChecker
import java.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/** Also named in `res/xml/data_extraction_rules.xml`, which allows it into Android backup. */
internal const val SETTINGS_DATASTORE_NAME = "mak_settings"

private val Context.settingsDataStore by preferencesDataStore(name = SETTINGS_DATASTORE_NAME)

@Module
@Configuration
@ComponentScan("dev.retza.mak")
class AppModule {
    @Single
    fun provideDatabase(context: Context): AppDatabase = AppDatabase.getInstance(context)

    @Single
    fun provideSettingsDataStore(context: Context): DataStore<Preferences> = context.settingsDataStore

    @Single
    fun provideClock(): Clock = SystemZoneClock()

    @Single
    fun provideBackgroundDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Single
    fun provideUpdateChecker(appInfoProvider: InstalledAppInfoProvider): UpdateCheckService {
        val info = appInfoProvider.get()
        return UpdateChecker.production(
            installedVersionCode = info.versionCode.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            deviceSdk = info.deviceSdk
        )
    }
}
