package dev.retza.mak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.enableEdgeToEdge
import dev.retza.mak.ui.AppViewModel
import dev.retza.mak.ui.MakApp
import dev.retza.mak.ui.MakDestination
import dev.retza.mak.ui.edit.ClassEditViewModel
import dev.retza.mak.ui.feedback.FeedbackController
import dev.retza.mak.ui.occurrence.OccurrenceViewModel
import dev.retza.mak.ui.schedule.ScheduleViewModel
import dev.retza.mak.ui.semester.SemesterViewModel
import dev.retza.mak.ui.settings.SettingsViewModel
import dev.retza.mak.ui.settings.ThemeMode
import dev.retza.mak.ui.setup.SetupViewModel
import dev.retza.mak.ui.theme.MAKTheme
import dev.retza.mak.ui.today.TodayViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModel()
    private val occurrenceViewModel: OccurrenceViewModel by viewModel()
    private val classEditViewModel: ClassEditViewModel by viewModel()
    private val semesterViewModel: SemesterViewModel by viewModel()
    private val setupViewModel: SetupViewModel by viewModel()
    private val settingsViewModel: SettingsViewModel by viewModel()
    private val scheduleViewModel: ScheduleViewModel by viewModel()
    private val todayViewModel: TodayViewModel by viewModel()
    private val feedbackController: FeedbackController by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LaunchedEffect(appViewModel) {
                if (intent.getBooleanExtra(EXTRA_OPEN_TODAY, false)) {
                    appViewModel.navigate(MakDestination.Today)
                    intent.removeExtra(EXTRA_OPEN_TODAY)
                }
            }
            val themeMode = settingsViewModel.themeMode.collectAsStateWithLifecycle().value
            val systemDark = isSystemInDarkTheme()
            MAKTheme(
                darkTheme = when (themeMode) {
                    ThemeMode.Light -> false
                    ThemeMode.Dark -> true
                    ThemeMode.System -> systemDark
                }
            ) {
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) todayViewModel.refreshToday()
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                val scope = rememberCoroutineScope()
                val exportLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    if (uri != null) {
                        settingsViewModel.exportJson { bytes ->
                            scope.launch {
                                val success = runCatching {
                                    contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                                        ?: error("No output stream")
                                }.isSuccess
                                settingsViewModel.reportExportFinished(success)
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
                    scheduleViewModel = scheduleViewModel,
                    todayViewModel = todayViewModel,
                    feedback = feedbackController.feedback,
                    onCreateExportDocument = { exportLauncher.launch("mak-plan.json") }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_TODAY, false)) {
            appViewModel.navigate(MakDestination.Today)
            intent.removeExtra(EXTRA_OPEN_TODAY)
        }
    }

    companion object {
        const val EXTRA_OPEN_TODAY = "dev.retza.mak.extra.OPEN_TODAY"
    }
}
