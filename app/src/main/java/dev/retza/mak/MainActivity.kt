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
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.retza.mak.ui.MakApp
import dev.retza.mak.ui.MakViewModel
import dev.retza.mak.ui.edit.ClassEditViewModel
import dev.retza.mak.ui.occurrence.OccurrenceViewModel
import dev.retza.mak.ui.theme.MAKTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var makViewModel: MakViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val application = application as MakApplication
            val occurrenceViewModel: OccurrenceViewModel = viewModel(
                factory = OccurrenceViewModel.Factory(application.repository, application.feedbackController)
            )
            val classEditViewModel: ClassEditViewModel = viewModel(
                factory = ClassEditViewModel.Factory(application.repository, application.feedbackController)
            )
            val viewModel: MakViewModel = viewModel(
                factory = MakViewModel.Factory(
                    application.repository,
                    application.feedbackController,
                    classEditViewModel
                )
            )
            makViewModel = viewModel
            LaunchedEffect(viewModel) {
                if (intent.getBooleanExtra(EXTRA_OPEN_TODAY, false)) {
                    viewModel.navigate(dev.retza.mak.ui.MakDestination.Today)
                    intent.removeExtra(EXTRA_OPEN_TODAY)
                }
            }
            val state = viewModel.uiState.collectAsStateWithLifecycle().value
            val systemDark = isSystemInDarkTheme()
            MAKTheme(
                darkTheme = when (state.themeId) {
                    "light" -> false
                    "dark" -> true
                    else -> systemDark
                }
            ) {
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshToday()
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                val scope = rememberCoroutineScope()
                val exportLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    if (uri != null) {
                        viewModel.exportJson { bytes ->
                            scope.launch {
                                contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                            }
                        }
                    }
                }
                MakApp(
                    viewModel = viewModel,
                    occurrenceViewModel = occurrenceViewModel,
                    classEditViewModel = classEditViewModel,
                    feedback = application.feedbackController.feedback,
                    onCreateExportDocument = { exportLauncher.launch("mak-plan.json") }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_TODAY, false)) {
            makViewModel?.navigate(dev.retza.mak.ui.MakDestination.Today)
            intent.removeExtra(EXTRA_OPEN_TODAY)
        }
    }

    companion object {
        const val EXTRA_OPEN_TODAY = "dev.retza.mak.extra.OPEN_TODAY"
    }
}
