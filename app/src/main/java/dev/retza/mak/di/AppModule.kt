package dev.retza.mak.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dev.retza.mak.data.database.AppDatabase
import java.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

private val Context.settingsDataStore by preferencesDataStore(name = "mak_settings")

@Module
@Configuration
@ComponentScan("dev.retza.mak")
class AppModule {
    @Single
    fun provideDatabase(context: Context): AppDatabase = AppDatabase.getInstance(context)

    @Single
    fun provideSettingsDataStore(context: Context): DataStore<Preferences> = context.settingsDataStore

    @Single
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Single
    fun provideBackgroundDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
