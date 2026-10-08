package dev.retza.mak.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.retza.mak.ui.edit.ClassEditViewModel
import dev.retza.mak.ui.settings.SettingsViewModel
import dev.retza.mak.ui.setup.SetupEffect
import dev.retza.mak.ui.setup.SetupViewModel
import dev.retza.mak.ui.setup.SetupWizard

internal fun isSetupRoute(route: String?): Boolean =
    route == MakRoutes.Setup || route == MakRoutes.SetupRoute

internal fun openSetup(navController: NavController, resumeExisting: Boolean = false) {
    navController.navigate(setupRoute(resumeExisting)) {
        popUpTo(MakRoutes.Today) { saveState = true }
        launchSingleTop = true
    }
}

internal fun NavGraphBuilder.setupRoute(
    setupViewModel: SetupViewModel,
    settingsViewModel: SettingsViewModel
) {
    composable(
        route = MakRoutes.SetupRoute,
        arguments = listOf(navArgument("resume") {
            type = NavType.BoolType
            defaultValue = false
        })
    ) { entry ->
        val editing = TrackPlanEditing()
        if (!editing.ready) {
            PlanEditingWait()
            return@composable
        }
        var opened by rememberSaveable { mutableStateOf(false) }
        val resumeExisting = entry.arguments?.getBoolean("resume") ?: false
        LaunchedEffect(editing.ready, opened) {
            if (!opened) {
                setupViewModel.startFromRoom(resumeExisting)
                opened = true
            }
        }
        if (!opened) {
            PlanEditingWait()
            return@composable
        }
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
            onProgramModeChanged = setupViewModel::selectProgramMode,
            onProgramSelected = setupViewModel::selectProgram,
            onNext = setupViewModel::next,
            onBack = setupViewModel::back,
            onAddClass = setupViewModel::addClass,
            onActivateAndAddClass = setupViewModel::activateAndAddClass,
            onFinish = setupViewModel::finish,
            onReturnToSettings = setupViewModel::returnToSettings,
            showReturnToSettings = settingsState.semesters.isNotEmpty(),
            onRetry = {},
            onAddAnotherProgram = setupViewModel::startAnotherProgram,
            onCalendarModeChanged = setupViewModel::selectCalendarMode,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
internal fun SetupEffects(
    setupViewModel: SetupViewModel,
    navController: NavController
) {
    LaunchedEffect(setupViewModel, navController) {
        setupViewModel.effects.collect { effect ->
            val route = navController.currentBackStackEntry?.destination?.route
            if (!shouldHandleSetupEffect(route)) return@collect
            when (effect) {
                SetupEffect.FinishToToday -> {
                    navController.navigate(MakRoutes.Today) {
                        popUpTo(MakRoutes.Today) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                SetupEffect.ReturnToSettings -> {
                    if (navController.previousBackStackEntry?.destination?.route == MakRoutes.Settings) {
                        navController.popBackStack()
                    } else {
                        openSettings(navController)
                    }
                }

                SetupEffect.OpenNewClassEditor -> openClassEditor(navController)
            }
        }
    }
}
