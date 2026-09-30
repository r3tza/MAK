package dev.retza.mak.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.WorkManager
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.update.InstalledAppInfoProvider
import dev.retza.mak.update.UpdateCheckService
import dev.retza.mak.update.UpdateChecker
import dev.retza.mak.sync.DriveAccessTokenProvider
import dev.retza.mak.sync.DrivePlanTransport
import dev.retza.mak.sync.FileSyncStateStore
import dev.retza.mak.sync.PlanEditTracker
import dev.retza.mak.sync.PlanSyncGateway
import dev.retza.mak.sync.SyncArchive
import dev.retza.mak.sync.SyncCoordinator
import dev.retza.mak.sync.UrlConnectionDriveHttpClient
import java.io.File
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
    fun provideWorkManager(context: Context): WorkManager = WorkManager.getInstance(context)

    @Single
    fun provideSettingsDataStore(context: Context): DataStore<Preferences> = context.settingsDataStore

    @Single
    fun provideClock(): Clock = SystemZoneClock()

    @Single
    fun provideBackgroundDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Single
    fun provideSyncCoordinator(
        context: Context,
        gateway: PlanSyncGateway,
        tokens: DriveAccessTokenProvider,
        editTracker: PlanEditTracker,
        clock: Clock
    ): SyncCoordinator = SyncCoordinator(
        gateway = gateway,
        transport = DrivePlanTransport(UrlConnectionDriveHttpClient(Dispatchers.IO), tokens),
        store = FileSyncStateStore(File(context.noBackupFilesDir, "google-sync/state.json")),
        archive = SyncArchive(File(context.noBackupFilesDir, "google-sync/archive")),
        editTracker = editTracker,
        clock = clock
    )

    @Single
    fun provideUpdateChecker(appInfoProvider: InstalledAppInfoProvider): UpdateCheckService {
        val info = appInfoProvider.get()
        return UpdateChecker.production(
            installedVersionCode = info.versionCode.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            deviceSdk = info.deviceSdk
        )
    }
}
