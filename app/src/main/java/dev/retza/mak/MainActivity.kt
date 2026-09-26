package dev.retza.mak

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.retza.mak.export.BackupFileRead
import dev.retza.mak.export.readBackupFile
import dev.retza.mak.ui.AppViewModel
import dev.retza.mak.ui.MakApp
import dev.retza.mak.ui.edit.ClassEditViewModel
import dev.retza.mak.ui.feedback.FeedbackController
import dev.retza.mak.ui.occurrence.OccurrenceViewModel
import dev.retza.mak.ui.schedule.ScheduleViewModel
import dev.retza.mak.ui.programs.StudyProgramsViewModel
import dev.retza.mak.ui.semester.SemesterViewModel
import dev.retza.mak.ui.settings.SettingsViewModel
import dev.retza.mak.ui.settings.ThemeMode
import dev.retza.mak.ui.setup.SetupViewModel
import dev.retza.mak.ui.theme.MAKTheme
import dev.retza.mak.ui.today.TodayViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModel()
    private val occurrenceViewModel: OccurrenceViewModel by viewModel()
    private val classEditViewModel: ClassEditViewModel by viewModel()
    private val semesterViewModel: SemesterViewModel by viewModel()
    private val setupViewModel: SetupViewModel by viewModel()
    private val settingsViewModel: SettingsViewModel by viewModel()
    private val studyProgramsViewModel: StudyProgramsViewModel by viewModel()
    private val scheduleViewModel: ScheduleViewModel by viewModel()
    private val todayViewModel: TodayViewModel by viewModel()
    private val feedbackController: FeedbackController by inject()
    private val openTodayRequests = Channel<Unit>(Channel.CONFLATED)
    private val openPlanRequests = Channel<String>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent.getBooleanExtra(EXTRA_OPEN_TODAY, false)) {
            openTodayRequests.trySend(Unit)
            intent.removeExtra(EXTRA_OPEN_TODAY)
        }
        intent.getStringExtra(EXTRA_OPEN_PLAN_DATE)?.let { date ->
            openPlanRequests.trySend(date)
            intent.removeExtra(EXTRA_OPEN_PLAN_DATE)
        }
        setContent {
            val themeMode = settingsViewModel.themeMode.collectAsStateWithLifecycle().value
            val systemDark = isSystemInDarkTheme()
            MAKTheme(
                darkTheme = when (themeMode) {
                    ThemeMode.Light -> false
                    ThemeMode.Dark -> true
                    ThemeMode.System -> systemDark
                }
            ) {
                var notificationsBlocked by remember {
                    mutableStateOf(!NotificationManagerCompat.from(this@MainActivity).areNotificationsEnabled())
                }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            todayViewModel.refreshToday()
                            scheduleViewModel.refreshToday()
                            notificationsBlocked =
                                !NotificationManagerCompat.from(this@MainActivity).areNotificationsEnabled()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    notificationsBlocked = !granted ||
                        !NotificationManagerCompat.from(this@MainActivity).areNotificationsEnabled()
                }
                val scope = rememberCoroutineScope()
                val exportLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    if (uri != null) {
                        settingsViewModel.exportJson { bytes ->
                            scope.launch {
                                val success = withContext(Dispatchers.IO) {
                                    runCatching {
                                        // "wt" truncates an existing document; plain "w" may leave old bytes at the end.
                                        contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
                                            ?: error("No output stream")
                                    }.isSuccess
                                }
                                settingsViewModel.reportExportFinished(success)
                            }
                        }
                    }
                }
                val importLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        scope.launch {
                            val read = withContext(Dispatchers.IO) {
                                runCatching {
                                    contentResolver.openInputStream(uri)?.use { readBackupFile(it) }
                                }.getOrNull()
                            }
                            when (read) {
                                null -> settingsViewModel.reportImportReadError()
                                BackupFileRead.TooLarge -> settingsViewModel.reportImportTooLarge()
                                is BackupFileRead.Loaded -> settingsViewModel.prepareImport(read.bytes)
                            }
                        }
                    }
                }
                MakApp(
                    viewModel = appViewModel,
                    occurrenceViewModel = occurrenceViewModel,
                    classEditViewModel = classEditViewModel,
                    semesterViewModel = semesterViewModel,
                    setupViewModel = setupViewModel,
                    settingsViewModel = settingsViewModel,
                    studyProgramsViewModel = studyProgramsViewModel,
                    scheduleViewModel = scheduleViewModel,
                    todayViewModel = todayViewModel,
                    feedback = feedbackController.feedback,
                    openTodayRequests = openTodayRequests.receiveAsFlow(),
                    openPlanRequests = openPlanRequests.receiveAsFlow(),
                    onCreateExportDocument = { exportLauncher.launch("mak-plan.json") },
                    onImportPlan = { importLauncher.launch(arrayOf("application/json")) },
                    notificationsBlocked = notificationsBlocked,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            // Android 12 has no runtime permission; notifications are switched on
                            // in the app's system settings, and onResume refreshes the blocked state.
                            startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                            )
                        }
                    },
                    onOpenAppSettings = {
                        startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", packageName, null)
                            )
                        )
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_TODAY, false)) {
            openTodayRequests.trySend(Unit)
            intent.removeExtra(EXTRA_OPEN_TODAY)
        }
        intent.getStringExtra(EXTRA_OPEN_PLAN_DATE)?.let { date ->
            openPlanRequests.trySend(date)
            intent.removeExtra(EXTRA_OPEN_PLAN_DATE)
        }
    }

    companion object {
        const val EXTRA_OPEN_TODAY = "dev.retza.mak.extra.OPEN_TODAY"
        const val EXTRA_OPEN_PLAN_DATE = "dev.retza.mak.extra.OPEN_PLAN_DATE"
    }
}
