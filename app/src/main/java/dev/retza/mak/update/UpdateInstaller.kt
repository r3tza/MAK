package dev.retza.mak.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

sealed interface InstallEvent {
    data object Success : InstallEvent
    data object Cancelled : InstallEvent
    data class Failure(val message: String?) : InstallEvent
}

@Single
class InstallEventStore {
    private val mutableEvents = MutableSharedFlow<InstallEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<InstallEvent> = mutableEvents.asSharedFlow()
    fun emit(event: InstallEvent) { mutableEvents.tryEmit(event) }
}

interface UpdateInstaller {
    fun canInstallPackages(): Boolean
    fun permissionIntent(): Intent
    fun install(file: File)
}

@Single(binds = [UpdateInstaller::class])
class AndroidUpdateInstaller(private val context: Context) : UpdateInstaller {
    override fun canInstallPackages(): Boolean = context.packageManager.canRequestPackageInstalls()

    override fun permissionIntent(): Intent = Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:${context.packageName}")
    )

    override fun install(file: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            .apply { setAppPackageName(context.packageName) }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            file.inputStream().use { input ->
                session.openWrite("update.apk", 0, file.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }
            val intent = Intent(context, UpdateInstallReceiver::class.java)
                .setAction("${context.packageName}.UPDATE_INSTALL.$sessionId")
                .putExtra(EXTRA_APK_PATH, file.path)
            val pending = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            session.commit(pending.intentSender)
        }
    }
}

class UpdateInstallReceiver : BroadcastReceiver(), KoinComponent {
    private val eventStore: InstallEventStore by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirmation?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (confirmation != null) context.startActivity(confirmation)
                else eventStore.emit(InstallEvent.Failure(null))
            }
            PackageInstaller.STATUS_SUCCESS -> eventStore.emit(InstallEvent.Success)
            PackageInstaller.STATUS_FAILURE_ABORTED -> eventStore.emit(InstallEvent.Cancelled)
            else -> eventStore.emit(InstallEvent.Failure(intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)))
        }
        if (status != PackageInstaller.STATUS_PENDING_USER_ACTION) {
            intent.getStringExtra(EXTRA_APK_PATH)?.let(::File)?.delete()
        }
    }
}

private const val EXTRA_APK_PATH = "dev.retza.mak.extra.UPDATE_APK_PATH"
