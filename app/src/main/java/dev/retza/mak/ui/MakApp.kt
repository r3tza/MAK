package dev.retza.mak.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.retza.mak.ui.components.MakBrandMark
import dev.retza.mak.ui.components.MakIconButton
import dev.retza.mak.ui.components.MakNavBar
import dev.retza.mak.ui.edit.ClassEditScreen
import dev.retza.mak.ui.occurrence.OccurrenceDetailsScreen
import dev.retza.mak.ui.schedule.ScheduleScreen
import dev.retza.mak.ui.semester.SemesterScreen
import dev.retza.mak.ui.settings.SettingsScreen
import dev.retza.mak.ui.setup.SetupWizard
import dev.retza.mak.ui.today.TodayScreen

@Composable
private fun LegacyMakApp(viewModel: MakViewModel, onCreateExportDocument: () -> Unit) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val showNavigation = !state.requiresSetup && state.destination !in setOf(
        MakDestination.OccurrenceDetails,
        MakDestination.Semester
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MakBrandMark()
                    Text(
                        "MAK",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.8).sp
                    )
                }
                if (!state.requiresSetup) {
                    MakIconButton(
                        label = "Ustawienia i motyw",
                        symbol = "⚙",
                        onClick = {
                            if (state.destination == MakDestination.Settings) {
                                viewModel.navigate(MakDestination.Today)
                            } else {
                                viewModel.navigate(MakDestination.Settings)
                            }
                        }
                    )
                }
            }
        },
        bottomBar = {
            if (showNavigation) {
                MakNavBar(
                    todaySelected = state.destination == MakDestination.Today,
                    planSelected = state.destination == MakDestination.Schedule,
                    addSelected = state.destination == MakDestination.EditClass,
                    onToday = { viewModel.navigate(MakDestination.Today) },
                    onPlan = { viewModel.navigate(MakDestination.Schedule) },
                    onAdd = { viewModel.openNewClass() }
                )
            }
        }
    ) { padding ->
        val baseModifier = Modifier.fillMaxSize().padding(padding)
        when (state.destination) {
            MakDestination.Setup -> SetupWizard(
                state = state.setup,
                onSemesterNameChanged = { value -> viewModel.updateSetup { it.copy(semesterName = value) } },
                onStartDateChanged = { value -> viewModel.updateSetup { it.copy(startDate = value) } },
                onEndDateChanged = { value -> viewModel.updateSetup { it.copy(endDate = value) } },
                onFirstWeekChanged = { value -> viewModel.updateSetup { it.copy(firstWeekLabel = value) } },
                onCourseNameChanged = { value -> viewModel.updateSetup { it.copy(courseName = value) } },
                onCourseColorChanged = { value -> viewModel.updateSetup { it.copy(courseColor = value) } },
                onNext = viewModel::setupNext,
                onBack = viewModel::setupBack,
                onAddClass = { viewModel.finishSetup(); viewModel.openNewClass() },
                onFinish = viewModel::finishSetup,
                onReturnToSettings = viewModel::cancelSetup,
                showReturnToSettings = state.settings.semesters.isNotEmpty(),
                onRetry = {},
                modifier = baseModifier
            )

            MakDestination.Today -> TodayScreen(
                state = state.today,
                onOpenPlan = { viewModel.navigate(MakDestination.Schedule) },
                onOpenClass = viewModel::openOccurrence,
                onRetry = {},
                modifier = baseModifier
            )

            MakDestination.Schedule -> ScheduleScreen(
                state = state.schedule,
                onViewChanged = viewModel::selectScheduleView,
                onPreviousWeek = { viewModel.changeWeek(-1) },
                onNextWeek = { viewModel.changeWeek(1) },
                onDaySelected = viewModel::selectScheduleDay,
                onFilterSelected = viewModel::selectCourseFilter,
                onPreviousMonth = { viewModel.changeMonth(-1) },
                onNextMonth = { viewModel.changeMonth(1) },
                onCalendarDaySelected = viewModel::selectCalendarDay,
                onShowCancelledChanged = viewModel::setShowCancelled,
                onAddOneOff = viewModel::openNewClassForSelectedCalendarDay,
                onOpenClass = viewModel::openOccurrence,
                onSaveWeekCorrection = viewModel::saveVisibleWeekOverride,
                onClearWeekCorrection = viewModel::clearVisibleWeekOverride,
                onRetry = {},
                modifier = baseModifier
            )

            MakDestination.EditClass -> ClassEditScreen(
                state = state.editor,
                onNameChanged = { value -> viewModel.updateEditor { it.copy(name = value) } },
                onCourseChanged = { value -> viewModel.updateEditor { it.copy(courseName = value) } },
                onTypeChanged = { value -> viewModel.updateEditor { it.copy(type = value) } },
                onDayChanged = { value -> viewModel.updateEditor { it.copy(dayLabel = value) } },
                onStartTimeChanged = { value -> viewModel.updateEditor { it.copy(startTime = value) } },
                onEndTimeChanged = { value -> viewModel.updateEditor { it.copy(endTime = value) } },
                onRecurrenceChanged = { value ->
                    viewModel.updateEditor {
                        it.copy(recurrenceId = value, recurrenceLabel = recurrenceLabelForUi(value))
                    }
                },
                onOccurrenceDateChanged = { value -> viewModel.updateEditor { it.copy(occurrenceDate = value) } },
                onRoomChanged = { value -> viewModel.updateEditor { it.copy(room = value) } },
                onBuildingChanged = { value -> viewModel.updateEditor { it.copy(building = value) } },
                onGroupChanged = { value -> viewModel.updateEditor { it.copy(group = value) } },
                onTeacherChanged = { value -> viewModel.updateEditor { it.copy(teacher = value) } },
                onNoteChanged = { value -> viewModel.updateEditor { it.copy(note = value) } },
                onSave = viewModel::saveClass,
                onCancel = { viewModel.navigate(MakDestination.Today) },
                onRetry = {},
                modifier = baseModifier
            )

            MakDestination.OccurrenceDetails -> OccurrenceDetailsScreen(
                state = state.occurrence,
                onDeleteBaseClass = viewModel::deleteSelectedClass,
                onDismissDeleteConfirmation = viewModel::cancelClassDeletion,
                onChangeOccurrence = viewModel::changeSelectedOccurrence,
                onMoveOccurrence = viewModel::moveSelectedOccurrence,
                onRestoreOccurrence = viewModel::restoreSelectedOccurrence,
                onOccurrenceNoteDraftChanged = { value -> viewModel.updateOccurrence { it.copy(occurrenceNoteDraft = value) } },
                onTargetDateDraftChanged = { value -> viewModel.updateOccurrence { it.copy(targetDateDraft = value) } },
                onStartTimeDraftChanged = { value -> viewModel.updateOccurrence { it.copy(startTimeDraft = value) } },
                onEndTimeDraftChanged = { value -> viewModel.updateOccurrence { it.copy(endTimeDraft = value) } },
                onRoomDraftChanged = { value -> viewModel.updateOccurrence { it.copy(roomDraft = value) } },
                onSaveOccurrenceNote = viewModel::saveOccurrenceNote,
                onDeleteOccurrenceNote = viewModel::deleteOccurrenceNote,
                onBack = { viewModel.navigate(MakDestination.Schedule) },
                modifier = baseModifier
            )

            MakDestination.Semester -> SemesterScreen(
                state = state.semester,
                onSemesterNameChanged = { value -> viewModel.updateSemester { it.copy(semester = it.semester.copy(name = value)) } },
                onSemesterStartDateChanged = { value -> viewModel.updateSemester { it.copy(semester = it.semester.copy(startDate = value)) } },
                onSemesterEndDateChanged = { value -> viewModel.updateSemester { it.copy(semester = it.semester.copy(endDate = value)) } },
                onSemesterFirstWeekChanged = { value -> viewModel.updateSemester { it.copy(semester = it.semester.copy(firstWeek = value)) } },
                onSaveSemester = viewModel::saveSemesterConfiguration,
                onOpenCourses = {},
                onOpenOverrides = {},
                onBack = { viewModel.navigate(MakDestination.Settings) },
                onRetry = {},
                modifier = baseModifier
            )

            MakDestination.Settings -> SettingsScreen(
                state = state.settings,
                onSemesterSelected = viewModel::selectSemester,
                onAddSemester = viewModel::startSemesterSetup,
                onConfigureSemester = viewModel::openSemesterConfiguration,
                onDeleteSemester = viewModel::requestSemesterDeletion,
                onConfirmDelete = viewModel::confirmSemesterDeletion,
                onCancelDelete = viewModel::cancelSemesterDeletion,
                onThemeSelected = viewModel::selectTheme,
                onExport = onCreateExportDocument,
                onRetry = {},
                modifier = baseModifier
            )
        }
    }
}

private fun recurrenceLabelForUi(id: String): String = when (id) {
    "a_week" -> "Tydzień A"
    "b_week" -> "Tydzień B"
    "once" -> "Jednorazowo"
    else -> "Co tydzień"
}
