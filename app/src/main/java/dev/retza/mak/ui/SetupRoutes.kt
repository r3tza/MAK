package dev.retza.mak.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.retza.mak.ui.edit.ClassEditViewModel
import dev.retza.mak.ui.settings.SettingsViewModel
import dev.retza.mak.ui.setup.SetupEffect
import dev.retza.mak.ui.setup.SetupViewModel
import dev.retza.mak.ui.setup.SetupWizard

internal fun NavGraphBuilder.setupRoute(
    setupViewModel: SetupViewModel,
    settingsViewModel: SettingsViewModel
) {
    composable(MakRoutes.Setup) {
        val setupState by setupViewModel.setup.collectAsStateWithLifecycle()
        val settingsState by settingsViewModel.settings.collectAsStateWithLifecycle()
        SetupWizard(
            state = setupState,
            onSemesterNameChanged = { value -> setupViewModel.update { it.copy(semesterName = value) } },
            onStartDateChanged = { value -> setupViewModel.update { it.copy(startDate = value) } },
            onEndDateChanged = { value -> setupViewModel.update { it.copy(endDate = value) } },
            onFirstWeekChanged = { value -> setupViewModel.update { it.copy(firstWeekLabel = value) } },
            onCourseNameChanged = { value -> setupViewModel.update { it.copy(courseName = value) } },
            onCourseColorChanged = { value -> setupViewModel.update { it.copy(courseColor = value) } },
            onNext = setupViewModel::next,
            onBack = setupViewModel::back,
            onAddClass = setupViewModel::addClass,
            onFinish = setupViewModel::finish,
            onReturnToSettings = setupViewModel::returnToSettings,
            showReturnToSettings = settingsState.semesters.isNotEmpty(),
            onRetry = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
internal fun SetupEffects(
    setupViewModel: SetupViewModel,
    classEditViewModel: ClassEditViewModel,
    navController: NavController,
    onNavigate: (MakDestination) -> Unit
) {
    LaunchedEffect(setupViewModel, navController) {
        setupViewModel.effects.collect { effect ->
            val route = navController.currentBackStackEntry?.destination?.route
            if (!shouldHandleSetupEffect(route)) return@collect
            when (effect) {
                SetupEffect.FinishToToday -> {
                    onNavigate(MakDestination.Today)
                    navController.navigate(MakRoutes.Today) {
                        popUpTo(MakRoutes.Today) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                SetupEffect.ReturnToSettings -> {
                    onNavigate(MakDestination.Settings)
                    if (navController.previousBackStackEntry?.destination?.route == MakRoutes.Settings) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(MakRoutes.Settings)
                    }
                }

                SetupEffect.OpenNewClassEditor -> {
                    classEditViewModel.openNew()
                    navController.navigate(MakRoutes.Edit)
                }
            }
        }
    }
}
