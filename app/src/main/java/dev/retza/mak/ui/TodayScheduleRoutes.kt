package dev.retza.mak.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.retza.mak.ui.edit.ClassEditViewModel
import dev.retza.mak.ui.schedule.ScheduleEffect
import dev.retza.mak.ui.schedule.ScheduleScreen
import dev.retza.mak.ui.schedule.ScheduleViewModel
import dev.retza.mak.ui.today.TodayScreen
import dev.retza.mak.ui.today.TodayViewModel
import dev.retza.mak.update.UpdateViewModel

internal fun NavGraphBuilder.todayRoute(
    appState: AppUiState,
    todayViewModel: TodayViewModel,
    updateViewModel: UpdateViewModel,
    navController: NavController,
    onOpenOccurrence: (String) -> Unit,
    startSetup: () -> Unit,
    openSync: () -> Unit,
    syncAttention: () -> String?
) {
    composable(MakRoutes.Today) {
        val todayState = todayViewModel.today.collectAsStateWithLifecycle().value
        val updateState = updateViewModel.state.collectAsStateWithLifecycle().value
        TodayScreen(
            state = todayState,
            onOpenClass = onOpenOccurrence,
            onStartSetup = startSetup,
            onOpenSync = openSync,
            onRetry = {},
            requiresSetup = appState.requiresSetup,
            availableUpdateVersion = updateState.availableUpdate?.versionName.takeIf { updateState.showUpdateBanner },
            onViewUpdate = { navController.navigate(MakRoutes.SettingsUpdate) },
            onDismissUpdate = updateViewModel::dismissAvailableUpdate,
            modifier = Modifier.fillMaxSize(),
            twoColumns = LocalMakWidthClass.current == MakWidthClass.Expanded,
            syncAttention = syncAttention()
        )
    }
}

internal fun NavGraphBuilder.scheduleRoute(
    appState: AppUiState,
    scheduleViewModel: ScheduleViewModel,
    onOpenOccurrence: (String) -> Unit,
    startSetup: () -> Unit,
    openSync: () -> Unit
) {
    composable(MakRoutes.Schedule) {
        val scheduleState = scheduleViewModel.schedule.collectAsStateWithLifecycle().value
        ScheduleScreen(
            state = scheduleState,
            onViewChanged = scheduleViewModel::selectView,
            onPreviousWeek = { scheduleViewModel.changeWeek(-1) },
            onNextWeek = { scheduleViewModel.changeWeek(1) },
            onDaySelected = scheduleViewModel::selectDay,
            onFilterSelected = scheduleViewModel::selectCourseFilter,
            onPreviousMonth = { scheduleViewModel.changeMonth(-1) },
            onNextMonth = { scheduleViewModel.changeMonth(1) },
            onCalendarDaySelected = scheduleViewModel::selectCalendarDay,
            onShowCancelledChanged = scheduleViewModel::setShowCancelled,
            onAddOneOff = scheduleViewModel::openNewClassForSelectedCalendarDay,
            onOpenClass = onOpenOccurrence,
            onSaveWeekCorrection = scheduleViewModel::saveVisibleWeekOverride,
            onClearWeekCorrection = scheduleViewModel::clearVisibleWeekOverride,
            onStartSetup = startSetup,
            onOpenSync = openSync,
            onRetry = {},
            requiresSetup = appState.requiresSetup,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
internal fun ScheduleEffects(
    scheduleViewModel: ScheduleViewModel,
    navController: NavController
) {
    LaunchedEffect(scheduleViewModel, navController) {
        scheduleViewModel.effects.collect { effect ->
            when (effect) {
                is ScheduleEffect.OpenNewClassEditor -> {
                    if (navController.currentBackStackEntry?.destination?.route == MakRoutes.Schedule) {
                        openClassEditor(navController, effect.date)
                    }
                }
            }
        }
    }
}
