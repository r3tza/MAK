package dev.retza.mak.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
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
import dev.retza.mak.ui.components.MakActionMenu
import dev.retza.mak.ui.components.MakIconButton
import dev.retza.mak.ui.components.MakNavBar
import dev.retza.mak.ui.edit.ClassEditScreen
import dev.retza.mak.ui.occurrence.OccurrenceDetailsScreen
import dev.retza.mak.ui.schedule.ScheduleScreen
import dev.retza.mak.ui.semester.SemesterCoursesScreen
import dev.retza.mak.ui.semester.SemesterScreen
import dev.retza.mak.ui.semester.SemesterWeekOverridesScreen
import dev.retza.mak.ui.settings.SettingsScreen
import dev.retza.mak.ui.setup.SetupWizard
import dev.retza.mak.ui.today.TodayScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakApp(viewModel: MakViewModel, onCreateExportDocument: () -> Unit) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value
        ?.destination
        ?.route
    val isRoot = currentRoute == MakRoutes.Today || currentRoute == MakRoutes.Schedule
    val showBack = currentRoute != null &&
        currentRoute != MakRoutes.Today &&
        currentRoute != MakRoutes.Schedule &&
        (!state.requiresSetup || currentRoute != MakRoutes.Setup)

    fun openRoot(destination: MakDestination, route: String) {
        viewModel.navigate(destination)
        navController.navigate(route) {
            popUpTo(MakRoutes.Today) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openChild(destination: MakDestination, route: String) {
        viewModel.navigate(destination)
        navController.navigate(route)
    }

    fun navigateBack() {
        val previousRoute = navController.previousBackStackEntry?.destination?.route
        if (navController.popBackStack()) {
            viewModel.navigate(destinationForRoute(previousRoute))
        } else {
            openRoot(MakDestination.Today, MakRoutes.Today)
        }
    }

    LaunchedEffect(state.requiresSetup, state.destination, currentRoute) {
        when {
            state.requiresSetup && currentRoute != MakRoutes.Setup -> {
                navController.navigate(MakRoutes.Setup) {
                    popUpTo(MakRoutes.Today) { saveState = true }
                }
            }

            !state.requiresSetup && currentRoute == MakRoutes.Setup -> {
                openRoot(MakDestination.Today, MakRoutes.Today)
            }

            !state.requiresSetup &&
                !destinationMatchesRoute(state.destination, currentRoute) -> {
                when (state.destination) {
                    MakDestination.Today -> openRoot(MakDestination.Today, MakRoutes.Today)
                    MakDestination.Schedule -> openRoot(MakDestination.Schedule, MakRoutes.Schedule)
                    MakDestination.EditClass -> openChild(MakDestination.EditClass, MakRoutes.Edit)
                    MakDestination.OccurrenceDetails -> {
                        state.selectedClassId?.let { classId ->
                            val date = state.occurrence.targetDateDraft
                            navController.navigate(occurrenceRoute("$classId:$date"))
                        }
                    }
                    MakDestination.Semester -> {
                        state.settings.activeSemesterId?.let {
                            navController.navigate(semesterRoute(it))
                        }
                    }
                    MakDestination.Settings -> openChild(MakDestination.Settings, MakRoutes.Settings)
                    MakDestination.Setup -> Unit
                }
            }
        }
    }

    BackHandler(enabled = showBack) { navigateBack() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            MakTopBar(
                title = titleForRoute(currentRoute),
                showBack = showBack,
                showSettings = !state.requiresSetup && isRoot,
                onBack = ::navigateBack,
                onSettings = {
                    openChild(MakDestination.Settings, MakRoutes.Settings)
                },
                actions = if (currentRoute == MakRoutes.Occurrence) {
                    {
                        MakActionMenu(
                            actions = listOfNotNull(
                                if (state.occurrence.canCancelOccurrence) {
                                    "Odwołaj termin" to viewModel::cancelSelectedOccurrence
                                } else null,
                                if (state.occurrence.canEditBaseClass) {
                                    "Edytuj bazowe zajęcia" to {
                                        state.selectedClassId?.let { id ->
                                            viewModel.openEditClass("$id:${state.occurrence.targetDateDraft}")
                                            navController.navigate(editRoute(id, state.occurrence.targetDateDraft))
                                        }
                                    }
                                } else null,
                                if (state.occurrence.canDeleteBaseClass) {
                                    "Usuń zajęcia" to viewModel::requestClassDeletion
                                } else null
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                } else null
            )
        },
        bottomBar = {
            if (!state.requiresSetup && isRoot) {
                MakNavBar(
                    todaySelected = currentRoute == MakRoutes.Today,
                    planSelected = currentRoute == MakRoutes.Schedule,
                    addSelected = false,
                    onToday = { openRoot(MakDestination.Today, MakRoutes.Today) },
                    onPlan = { openRoot(MakDestination.Schedule, MakRoutes.Schedule) },
                    onAdd = {
                        viewModel.openNewClass()
                        navController.navigate(MakRoutes.Edit)
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = MakRoutes.Today,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(MakRoutes.Today) {
                TodayScreen(
                    state = state.today,
                    onOpenPlan = { openRoot(MakDestination.Schedule, MakRoutes.Schedule) },
                    onOpenClass = { occurrenceId ->
                        viewModel.openOccurrence(occurrenceId)
                        navController.navigate(occurrenceRoute(occurrenceId))
                    },
                    onRetry = {},
                    modifier = Modifier.fillMaxSize()
                )
            }

            composable(MakRoutes.Schedule) {
                ScheduleScreen(
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
                    onAddOneOff = {
                        viewModel.openNewClassForSelectedCalendarDay()
                        navController.navigate(MakRoutes.Edit)
                    },
                    onOpenClass = { occurrenceId ->
                        viewModel.openOccurrence(occurrenceId)
                        navController.navigate(occurrenceRoute(occurrenceId))
                    },
                    onSaveWeekCorrection = viewModel::saveVisibleWeekOverride,
                    onClearWeekCorrection = viewModel::clearVisibleWeekOverride,
                    onRetry = {},
                    modifier = Modifier.fillMaxSize()
                )
            }

            composable(
                route = MakRoutes.Edit,
                arguments = listOf(
                    navArgument("classId") {
                        type = NavType.StringType
                        nullable = true
                    },
                    navArgument("date") {
                        type = NavType.StringType
                        nullable = true
                    }
                )
            ) { entry ->
                val classId = entry.arguments?.getString("classId")
                val date = entry.arguments?.getString("date")
                LaunchedEffect(classId, date) {
                    if (classId != null && date != null) {
                        viewModel.openEditClass("$classId:$date")
                    }
                }
                ClassEditScreen(
                    state = state.editor,
                    onNameChanged = { value -> viewModel.updateEditor { it.copy(name = value) } },
                    onCourseChanged = { value -> viewModel.updateEditor { it.copy(courseName = value) } },
                    onTypeChanged = { value -> viewModel.updateEditor { it.copy(type = value) } },
                    onDayChanged = { value -> viewModel.updateEditor { it.copy(dayLabel = value) } },
                    onStartTimeChanged = { value -> viewModel.updateEditor { it.copy(startTime = value) } },
                    onEndTimeChanged = { value -> viewModel.updateEditor { it.copy(endTime = value) } },
                    onRecurrenceChanged = { value ->
                        viewModel.updateEditor {
                            it.copy(
                                recurrenceId = value,
                                recurrenceLabel = recurrenceLabelForUi(value)
                            )
                        }
                    },
                    onOccurrenceDateChanged = { value -> viewModel.updateEditor { it.copy(occurrenceDate = value) } },
                    onRoomChanged = { value -> viewModel.updateEditor { it.copy(room = value) } },
                    onBuildingChanged = { value -> viewModel.updateEditor { it.copy(building = value) } },
                    onGroupChanged = { value -> viewModel.updateEditor { it.copy(group = value) } },
                    onTeacherChanged = { value -> viewModel.updateEditor { it.copy(teacher = value) } },
                    onNoteChanged = { value -> viewModel.updateEditor { it.copy(note = value) } },
                    onSave = viewModel::saveClass,
                    onCancel = ::navigateBack,
                    onRetry = {},
                    modifier = Modifier.fillMaxSize()
                )
            }

            composable(
                route = MakRoutes.Occurrence,
                arguments = listOf(
                    navArgument("classId") { type = NavType.StringType },
                    navArgument("date") { type = NavType.StringType }
                )
            ) { entry ->
                val classId = entry.arguments?.getString("classId")
                val date = entry.arguments?.getString("date")
                LaunchedEffect(classId, date) {
                    if (classId != null && date != null) {
                        viewModel.openOccurrence("$classId:$date")
                    }
                }
                OccurrenceDetailsScreen(
                    state = state.occurrence,
                    onDeleteBaseClass = viewModel::deleteSelectedClass,
                    onDismissDeleteConfirmation = viewModel::cancelClassDeletion,
                    onChangeOccurrence = viewModel::changeSelectedOccurrence,
                    onMoveOccurrence = viewModel::moveSelectedOccurrence,
                    onRestoreOccurrence = viewModel::restoreSelectedOccurrence,
                    onOccurrenceNoteDraftChanged = { value ->
                        viewModel.updateOccurrence { it.copy(occurrenceNoteDraft = value) }
                    },
                    onTargetDateDraftChanged = { value ->
                        viewModel.updateOccurrence { it.copy(targetDateDraft = value) }
                    },
                    onStartTimeDraftChanged = { value ->
                        viewModel.updateOccurrence { it.copy(startTimeDraft = value) }
                    },
                    onEndTimeDraftChanged = { value ->
                        viewModel.updateOccurrence { it.copy(endTimeDraft = value) }
                    },
                    onRoomDraftChanged = { value ->
                        viewModel.updateOccurrence { it.copy(roomDraft = value) }
                    },
                    onSaveOccurrenceNote = viewModel::saveOccurrenceNote,
                    onDeleteOccurrenceNote = viewModel::deleteOccurrenceNote,
                    onBack = ::navigateBack,
                    modifier = Modifier.fillMaxSize()
                )
            }

            composable(
                route = MakRoutes.Semester,
                arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
            ) { entry ->
                val semesterId = entry.arguments?.getString("semesterId")
                LaunchedEffect(semesterId) {
                    semesterId?.let(viewModel::openSemesterConfiguration)
                }
                SemesterScreen(
                    state = state.semester,
                    onSemesterNameChanged = { value ->
                        viewModel.updateSemester { it.copy(semester = it.semester.copy(name = value)) }
                    },
                    onSemesterStartDateChanged = { value ->
                        viewModel.updateSemester { it.copy(semester = it.semester.copy(startDate = value)) }
                    },
                    onSemesterEndDateChanged = { value ->
                        viewModel.updateSemester { it.copy(semester = it.semester.copy(endDate = value)) }
                    },
                    onSemesterFirstWeekChanged = { value ->
                        viewModel.updateSemester { it.copy(semester = it.semester.copy(firstWeek = value)) }
                    },
                    onSaveSemester = viewModel::saveSemesterConfiguration,
                    onOverrideWeekStartDateChanged = { value ->
                        viewModel.updateSemester { it.copy(overrideForm = it.overrideForm.copy(weekStartDate = value)) }
                    },
                    onOverrideWeekTypeChanged = { value ->
                        viewModel.updateSemester { it.copy(overrideForm = it.overrideForm.copy(weekType = value)) }
                    },
                    onOverrideScopeChanged = { value ->
                        viewModel.updateSemester { it.copy(overrideForm = it.overrideForm.copy(scope = value)) }
                    },
                    onNewOverride = viewModel::newWeekOverride,
                    onEditOverride = viewModel::editWeekOverride,
                    onSaveOverride = viewModel::saveWeekOverride,
                    onDeleteOverride = viewModel::deleteWeekOverride,
                    onCancelOverrideEdit = viewModel::cancelWeekOverrideEdit,
                    onCourseNameChanged = { value ->
                        viewModel.updateSemester { it.copy(courseNameDraft = value) }
                    },
                    onCourseColorChanged = { value ->
                        viewModel.updateSemester { it.copy(courseColorDraft = value) }
                    },
                    onAddCourse = viewModel::addCourse,
                    onDeleteCourse = viewModel::deleteCourse,
                    onOpenCourses = {
                        state.settings.activeSemesterId?.let { id ->
                            navController.navigate(semesterCoursesRoute(id))
                        }
                    },
                    onOpenOverrides = {
                        state.settings.activeSemesterId?.let { id ->
                            navController.navigate(semesterOverridesRoute(id))
                        }
                    },
                    onBack = ::navigateBack,
                    onRetry = {},
                    modifier = Modifier.fillMaxSize()
                )
            }

            composable(
                route = MakRoutes.SemesterCourses,
                arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
            ) { entry ->
                val semesterId = entry.arguments?.getString("semesterId")
                LaunchedEffect(semesterId) {
                    semesterId?.let(viewModel::openSemesterConfiguration)
                }
                SemesterCoursesScreen(
                    state = state.semester,
                    onCourseNameChanged = { value ->
                        viewModel.updateSemester { it.copy(courseNameDraft = value) }
                    },
                    onCourseColorChanged = { value ->
                        viewModel.updateSemester { it.copy(courseColorDraft = value) }
                    },
                    onAddCourse = viewModel::addCourse,
                    onDeleteCourse = viewModel::deleteCourse,
                    onBack = ::navigateBack,
                    modifier = Modifier.fillMaxSize()
                )
            }

            composable(
                route = MakRoutes.SemesterOverrides,
                arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
            ) { entry ->
                val semesterId = entry.arguments?.getString("semesterId")
                LaunchedEffect(semesterId) {
                    semesterId?.let(viewModel::openSemesterConfiguration)
                }
                SemesterWeekOverridesScreen(
                    state = state.semester,
                    onWeekStartDateChanged = { value ->
                        viewModel.updateSemester { it.copy(overrideForm = it.overrideForm.copy(weekStartDate = value)) }
                    },
                    onWeekTypeChanged = { value ->
                        viewModel.updateSemester { it.copy(overrideForm = it.overrideForm.copy(weekType = value)) }
                    },
                    onScopeChanged = { value ->
                        viewModel.updateSemester { it.copy(overrideForm = it.overrideForm.copy(scope = value)) }
                    },
                    onNewOverride = viewModel::newWeekOverride,
                    onEditOverride = viewModel::editWeekOverride,
                    onSaveOverride = viewModel::saveWeekOverride,
                    onDeleteOverride = viewModel::deleteWeekOverride,
                    onCancelOverrideEdit = viewModel::cancelWeekOverrideEdit,
                    onBack = ::navigateBack,
                    modifier = Modifier.fillMaxSize()
                )
            }

            composable(MakRoutes.Settings) {
                SettingsScreen(
                    state = state.settings,
                    onSemesterSelected = viewModel::selectSemester,
                    onAddSemester = {
                        viewModel.startSemesterSetup()
                        navController.navigate(MakRoutes.Setup)
                    },
                    onConfigureSemester = { id ->
                        viewModel.openSemesterConfiguration(id)
                        navController.navigate(semesterRoute(id))
                    },
                    onDeleteSemester = viewModel::requestSemesterDeletion,
                    onConfirmDelete = viewModel::confirmSemesterDeletion,
                    onCancelDelete = viewModel::cancelSemesterDeletion,
                    onThemeSelected = viewModel::selectTheme,
                    onExport = onCreateExportDocument,
                    onRetry = {},
                    modifier = Modifier.fillMaxSize()
                )
            }

            composable(MakRoutes.Setup) {
                SetupWizard(
                    state = state.setup,
                    onSemesterNameChanged = { value -> viewModel.updateSetup { it.copy(semesterName = value) } },
                    onStartDateChanged = { value -> viewModel.updateSetup { it.copy(startDate = value) } },
                    onEndDateChanged = { value -> viewModel.updateSetup { it.copy(endDate = value) } },
                    onFirstWeekChanged = { value -> viewModel.updateSetup { it.copy(firstWeekLabel = value) } },
                    onCourseNameChanged = { value -> viewModel.updateSetup { it.copy(courseName = value) } },
                    onCourseColorChanged = { value -> viewModel.updateSetup { it.copy(courseColor = value) } },
                    onNext = viewModel::setupNext,
                    onBack = viewModel::setupBack,
                    onAddClass = {
                        viewModel.finishSetup()
                        viewModel.openNewClass()
                        navController.navigate(MakRoutes.Edit)
                    },
                    onFinish = {
                        viewModel.finishSetup()
                        openRoot(MakDestination.Today, MakRoutes.Today)
                    },
                    onReturnToSettings = {
                        viewModel.cancelSetup()
                        navigateBack()
                    },
                    showReturnToSettings = state.settings.semesters.isNotEmpty(),
                    onRetry = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakTopBar(
    title: String,
    showBack: Boolean,
    showSettings: Boolean,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    actions: (@Composable RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            if (showBack) {
                Text(title)
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MakBrandMark()
                    Text(
                        text = "MAK",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.8).sp
                    )
                }
            }
        },
        navigationIcon = {
            if (showBack) {
                MakIconButton(
                    label = "Wstecz",
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    onClick = onBack,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        },
        actions = {
            if (actions != null) {
                actions.invoke(this)
            } else if (showSettings) {
                MakIconButton(
                    label = "Ustawienia i motyw",
                    icon = Icons.Outlined.Settings,
                    onClick = onSettings,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
    )
}

object MakRoutes {
    const val Today = "today"
    const val Schedule = "schedule"
    const val Edit = "edit?classId={classId}&date={date}"
    const val Occurrence = "occurrence/{classId}/{date}"
    const val Semester = "semester/{semesterId}"
    const val SemesterCourses = "semester/{semesterId}/courses"
    const val SemesterOverrides = "semester/{semesterId}/week-overrides"
    const val Settings = "settings"
    const val Setup = "setup"
}

fun occurrenceRoute(occurrenceId: String): String {
    val parts = occurrenceId.split(":", limit = 2)
    return "occurrence/${parts[0]}/${parts.getOrElse(1) { "" }}"
}

fun editRoute(classId: Long, date: String): String =
    "edit?classId=$classId&date=$date"

fun semesterRoute(id: String): String = "semester/$id"

fun semesterCoursesRoute(id: String): String = "semester/$id/courses"

fun semesterOverridesRoute(id: String): String = "semester/$id/week-overrides"

fun destinationForRoute(route: String?): MakDestination = when (route) {
    MakRoutes.Schedule -> MakDestination.Schedule
    MakRoutes.Settings -> MakDestination.Settings
    MakRoutes.Setup -> MakDestination.Setup
    MakRoutes.Occurrence -> MakDestination.OccurrenceDetails
    MakRoutes.Semester,
    MakRoutes.SemesterCourses,
    MakRoutes.SemesterOverrides -> MakDestination.Semester
    MakRoutes.Edit -> MakDestination.EditClass
    else -> MakDestination.Today
}

fun destinationMatchesRoute(destination: MakDestination, route: String?): Boolean = when (destination) {
    MakDestination.Today -> route == MakRoutes.Today
    MakDestination.Schedule -> route == MakRoutes.Schedule
    MakDestination.Settings -> route == MakRoutes.Settings
    MakDestination.Setup -> route == MakRoutes.Setup
    MakDestination.OccurrenceDetails -> route == MakRoutes.Occurrence
    MakDestination.Semester -> route == MakRoutes.Semester ||
        route == MakRoutes.SemesterCourses ||
        route == MakRoutes.SemesterOverrides
    MakDestination.EditClass -> route == MakRoutes.Edit
}

private fun titleForRoute(route: String?): String = when (route) {
    MakRoutes.Schedule -> "Plan"
    MakRoutes.Edit -> "Zajęcia"
    MakRoutes.Occurrence -> "Termin"
    MakRoutes.Semester -> "Semestr"
    MakRoutes.SemesterCourses -> "Kierunki"
    MakRoutes.SemesterOverrides -> "Korekty tygodni"
    MakRoutes.Settings -> "Ustawienia"
    MakRoutes.Setup -> "Konfiguracja"
    else -> "Dzisiaj"
}

private fun recurrenceLabelForUi(id: String): String = when (id) {
    "a_week" -> "Tydzień A"
    "b_week" -> "Tydzień B"
    "once" -> "Jednorazowo"
    else -> "Co tydzień"
}
