package dev.retza.mak.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.koin.compose.viewmodel.koinViewModel
import dev.retza.mak.ui.freedays.FreeDayDialog
import dev.retza.mak.ui.freedays.FreeDayEditorViewModel
import dev.retza.mak.ui.schedule.ScheduleEffect
import dev.retza.mak.ui.schedule.ScheduleScreen
import dev.retza.mak.ui.schedule.ScheduleViewModel
import dev.retza.mak.ui.settings.SyncAttentionUi
import dev.retza.mak.ui.today.TodayScreen
import dev.retza.mak.ui.today.TodayViewModel
import dev.retza.mak.update.UpdateViewModel
import kotlinx.coroutines.flow.StateFlow

internal fun NavGraphBuilder.todayRoute(
    appState: AppUiState,
    updateViewModel: UpdateViewModel,
    navController: NavController,
    onOpenOccurrence: (String) -> Unit,
    startSetup: () -> Unit,
    openSync: () -> Unit,
    syncAttention: StateFlow<SyncAttentionUi?>
) {
    composable(MakRoutes.Today) {
        val todayViewModel: TodayViewModel = koinViewModel()
        LifecycleResumeEffect(todayViewModel) {
            todayViewModel.refreshToday()
            onPauseOrDispose { }
        }
        val todayState = todayViewModel.today.collectAsStateWithLifecycle().value
        val updateState = updateViewModel.state.collectAsStateWithLifecycle().value
        val attention = syncAttention.collectAsStateWithLifecycle().value
        TodayScreen(
            state = todayState,
            onOpenClass = onOpenOccurrence,
            onStartSetup = startSetup,
            onOpenSync = openSync,
            requiresSetup = appState.requiresSetup,
            availableUpdateVersion = updateState.availableUpdate?.versionName.takeIf { updateState.showUpdateBanner },
            onViewUpdate = { navController.navigate(MakRoutes.SettingsUpdate) },
            onDismissUpdate = updateViewModel::dismissAvailableUpdate,
            modifier = Modifier.fillMaxSize(),
            twoColumns = LocalMakWidthClass.current == MakWidthClass.Expanded,
            syncAttention = attention,
            onOpenPrograms = { navController.navigate(MakRoutes.StudyPrograms) }
        )
    }
}

internal fun NavGraphBuilder.scheduleRoute(
    appState: AppUiState,
    navController: NavController,
    // Read while composing: the graph keeps the arguments it was built with, so a plain value would stay stale.
    pendingPlanDate: () -> String?,
    onPlanDateShown: () -> Unit,
    onOpenOccurrence: (String) -> Unit,
    startSetup: () -> Unit,
    openSync: () -> Unit,
    openPrograms: () -> Unit
) {
    composable(MakRoutes.Schedule) {
        val scheduleViewModel: ScheduleViewModel = koinViewModel()
        LifecycleResumeEffect(scheduleViewModel) {
            scheduleViewModel.refreshToday()
            onPauseOrDispose { }
        }
        val planDate = pendingPlanDate()
        LaunchedEffect(planDate) {
            planDate?.let { date ->
                scheduleViewModel.showDate(date)
                onPlanDateShown()
            }
        }
        val freeDayEditor: FreeDayEditorViewModel = koinViewModel()
        ScheduleEffects(scheduleViewModel, navController, freeDayEditor)
        val scheduleState = scheduleViewModel.schedule.collectAsStateWithLifecycle().value
        val freeDayState = freeDayEditor.editor.collectAsStateWithLifecycle().value
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
            onMarkFree = scheduleViewModel::markSelectedCalendarDayFree,
            onOpenClass = onOpenOccurrence,
            onSaveWeekCorrection = scheduleViewModel::saveVisibleWeekOverride,
            onClearWeekCorrection = scheduleViewModel::clearVisibleWeekOverride,
            onStartSetup = startSetup,
            onOpenSync = openSync,
            onOpenPrograms = openPrograms,
            requiresSetup = appState.requiresSetup,
            modifier = Modifier.fillMaxSize()
        )
        FreeDayDialog(
            state = freeDayState,
            onRangeChange = freeDayEditor::updateRange,
            onNameChange = freeDayEditor::updateName,
            onToggleCalendar = freeDayEditor::toggleCalendar,
            onSave = freeDayEditor::save,
            onCancel = freeDayEditor::cancel
        )
    }
}

@Composable
internal fun ScheduleEffects(
    scheduleViewModel: ScheduleViewModel,
    navController: NavController,
    freeDayEditor: FreeDayEditorViewModel
) {
    LaunchedEffect(scheduleViewModel, navController, freeDayEditor) {
        scheduleViewModel.effects.collect { effect ->
            when (effect) {
                is ScheduleEffect.OpenNewClassEditor -> {
                    if (navController.currentBackStackEntry?.destination?.route == MakRoutes.Schedule) {
                        openClassEditor(navController, effect.date)
                    }
                }
                is ScheduleEffect.OpenFreeDayEditor -> freeDayEditor.startNew(effect.semesterId, effect.date)
            }
        }
    }
}
