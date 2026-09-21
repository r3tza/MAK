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
import dev.retza.mak.ui.edit.ClassEditEffect
import dev.retza.mak.ui.edit.ClassEditScreen
import dev.retza.mak.ui.edit.ClassEditViewModel
import dev.retza.mak.ui.feedback.MakSnackbarHost
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.occurrence.OccurrenceDetailsScreen
import dev.retza.mak.ui.occurrence.OccurrenceEffect
import dev.retza.mak.ui.occurrence.OccurrenceViewModel
import dev.retza.mak.ui.schedule.ScheduleEffect
import dev.retza.mak.ui.schedule.ScheduleScreen
import dev.retza.mak.ui.schedule.ScheduleViewModel
import dev.retza.mak.ui.semester.SemesterCoursesScreen
import dev.retza.mak.ui.semester.SemesterEffect
import dev.retza.mak.ui.semester.SemesterScreen
import dev.retza.mak.ui.semester.SemesterViewModel
import dev.retza.mak.ui.semester.SemesterWeekOverridesScreen
import dev.retza.mak.ui.settings.SettingsScreen
import dev.retza.mak.ui.settings.SettingsViewModel
import dev.retza.mak.ui.setup.SetupEffect
import dev.retza.mak.ui.setup.SetupViewModel
import dev.retza.mak.ui.setup.SetupWizard
import dev.retza.mak.ui.today.TodayScreen
import dev.retza.mak.ui.today.TodayViewModel
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakApp(
    viewModel: AppViewModel,
    occurrenceViewModel: OccurrenceViewModel,
    classEditViewModel: ClassEditViewModel,
    semesterViewModel: SemesterViewModel,
    setupViewModel: SetupViewModel,
    settingsViewModel: SettingsViewModel,
    scheduleViewModel: ScheduleViewModel,
    todayViewModel: TodayViewModel,
    feedback: Flow<UiFeedback>,
    onCreateExportDocument: () -> Unit
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val occurrenceDetails = occurrenceViewModel.details.collectAsStateWithLifecycle().value
    val semesterState = semesterViewModel.semester.collectAsStateWithLifecycle().value
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value
        ?.destination
        ?.route
    val isRoot = currentRoute == MakRoutes.Today || currentRoute == MakRoutes.Schedule
    val showBack = currentRoute != null &&
        currentRoute != MakRoutes.Today &&
        currentRoute != MakRoutes.Schedule &&
        currentRoute != MakRoutes.Setup

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

    fun navigateToSetup() {
        viewModel.navigate(MakDestination.Setup)
        navController.navigate(MakRoutes.Setup) {
            popUpTo(MakRoutes.Today) { saveState = true }
            launchSingleTop = true
        }
    }

    val startSetup: () -> Unit = {
        if (state.hasLoadedData) {
            setupViewModel.start(state.setupResume)
            navigateToSetup()
        }
    }

    LaunchedEffect(occurrenceViewModel, navController) {
        occurrenceViewModel.effects.collect { effect ->
            when (effect) {
                OccurrenceEffect.CloseDetails -> {
                    if (shouldCloseOccurrenceDetails(navController.currentBackStackEntry?.destination?.route)) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }

    LaunchedEffect(classEditViewModel, navController) {
        classEditViewModel.effects.collect { effect ->
            when (effect) {
                ClassEditEffect.CloseEditor -> {
                    if (shouldCloseClassEditor(navController.currentBackStackEntry?.destination?.route)) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }

    LaunchedEffect(semesterViewModel, navController) {
        semesterViewModel.effects.collect { effect ->
            when (effect) {
                SemesterEffect.CloseConfiguration -> {
                    if (shouldCloseSemesterConfiguration(navController.currentBackStackEntry?.destination?.route)) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }

    LaunchedEffect(setupViewModel, navController) {
        setupViewModel.effects.collect { effect ->
            val route = navController.currentBackStackEntry?.destination?.route
            if (!shouldHandleSetupEffect(route)) return@collect
            when (effect) {
                SetupEffect.FinishToToday -> openRoot(MakDestination.Today, MakRoutes.Today)
                SetupEffect.ReturnToSettings -> {
                    viewModel.navigate(MakDestination.Settings)
                    if (navController.previousBackStackEntry?.destination?.route == MakRoutes.Settings) {
                        navController.popBackStack()
                    } else {
                        openChild(MakDestination.Settings, MakRoutes.Settings)
                    }
                }

                SetupEffect.OpenNewClassEditor -> {
                    classEditViewModel.openNew()
                    navController.navigate(MakRoutes.Edit)
                }
            }
        }
    }

    LaunchedEffect(scheduleViewModel, navController) {
        scheduleViewModel.effects.collect { effect ->
            when (effect) {
                is ScheduleEffect.OpenNewClassEditor -> {
                    if (navController.currentBackStackEntry?.destination?.route == MakRoutes.Schedule) {
                        classEditViewModel.openNew(effect.date)
                        navController.navigate(MakRoutes.Edit)
                    }
                }
            }
        }
    }

    LaunchedEffect(state.destination, currentRoute) {
        if (currentRoute != MakRoutes.Occurrence &&
            currentRoute != MakRoutes.Edit &&
            currentRoute != MakRoutes.Semester &&
            currentRoute != MakRoutes.SemesterCourses &&
            currentRoute != MakRoutes.SemesterOverrides &&
            !destinationMatchesRoute(state.destination, currentRoute)
        ) {
            when (state.destination) {
                MakDestination.Today -> openRoot(MakDestination.Today, MakRoutes.Today)
                MakDestination.Schedule -> openRoot(MakDestination.Schedule, MakRoutes.Schedule)
                MakDestination.EditClass -> Unit
                MakDestination.OccurrenceDetails -> Unit
                MakDestination.Semester -> Unit
                MakDestination.Settings -> openChild(MakDestination.Settings, MakRoutes.Settings)
                MakDestination.Setup -> Unit
            }
        }
    }

    BackHandler(enabled = showBack) { navigateBack() }

    val occurrenceActions: List<Pair<String, () -> Unit>> = if (currentRoute == MakRoutes.Occurrence) {
        listOfNotNull(
            if (occurrenceDetails.canCancelOccurrence) {
                "Odwołaj termin" to occurrenceViewModel::cancelOccurrence
            } else null,
            if (occurrenceDetails.canEditBaseClass) {
                "Edytuj bazowe zajęcia" to {
                    occurrenceViewModel.selectedClassId.value?.let { id ->
                        classEditViewModel.openEdit("$id:${occurrenceDetails.targetDateDraft}")
                        navController.navigate(editRoute(id, occurrenceDetails.targetDateDraft))
                    }
                }
            } else null,
            if (occurrenceDetails.canDeleteBaseClass) {
                "Usuń zajęcia" to occurrenceViewModel::requestClassDeletion
            } else null
        )
    } else {
        emptyList()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            MakSnackbarHost(feedback = feedback)
        },
        topBar = {
            MakTopBar(
                title = titleForRoute(currentRoute),
                showBack = showBack,
                showSettings = isRoot,
                onBack = ::navigateBack,
                onSettings = {
                    openChild(MakDestination.Settings, MakRoutes.Settings)
                },
                actions = if (occurrenceActions.isEmpty()) {
                    null
                } else {
                    {
                        MakActionMenu(
                            actions = occurrenceActions,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (isRoot) {
                MakNavBar(
                    todaySelected = currentRoute == MakRoutes.Today,
                    planSelected = currentRoute == MakRoutes.Schedule,
                    addSelected = false,
                    onToday = { openRoot(MakDestination.Today, MakRoutes.Today) },
                    onPlan = { openRoot(MakDestination.Schedule, MakRoutes.Schedule) },
                    onAdd = {
                        when (addAction(state.hasLoadedData, state.requiresSetup)) {
                            AddAction.None -> Unit
                            AddAction.Setup -> startSetup()
                            AddAction.Editor -> {
                                classEditViewModel.openNew()
                                navController.navigate(MakRoutes.Edit)
                            }
                        }
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
                val todayState = todayViewModel.today.collectAsStateWithLifecycle().value
                TodayScreen(
                    state = todayState,
                    onOpenPlan = { openRoot(MakDestination.Schedule, MakRoutes.Schedule) },
                    onOpenClass = { occurrenceId ->
                        occurrenceViewModel.open(occurrenceId)
                        navController.navigate(occurrenceRoute(occurrenceId))
                    },
                    onStartSetup = startSetup,
                    onRetry = {},
                    requiresSetup = state.requiresSetup,
                    modifier = Modifier.fillMaxSize()
                )
            }

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
                    onOpenClass = { occurrenceId ->
                        occurrenceViewModel.open(occurrenceId)
                        navController.navigate(occurrenceRoute(occurrenceId))
                    },
                    onSaveWeekCorrection = scheduleViewModel::saveVisibleWeekOverride,
                    onClearWeekCorrection = scheduleViewModel::clearVisibleWeekOverride,
                    onStartSetup = startSetup,
                    onRetry = {},
                    requiresSetup = state.requiresSetup,
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
                        classEditViewModel.openEdit("$classId:$date")
                    }
                }
                ClassEditScreen(
                    state = classEditViewModel.editor.collectAsStateWithLifecycle().value,
                    onNameChanged = { value -> classEditViewModel.update { it.copy(name = value) } },
                    onCourseChanged = { value -> classEditViewModel.update { it.copy(courseName = value) } },
                    onTypeChanged = { value -> classEditViewModel.update { it.copy(type = value) } },
                    onDayChanged = { value -> classEditViewModel.update { it.copy(dayLabel = value) } },
                    onStartTimeChanged = { value -> classEditViewModel.update { it.copy(startTime = value) } },
                    onEndTimeChanged = { value -> classEditViewModel.update { it.copy(endTime = value) } },
                    onRecurrenceChanged = { value ->
                        classEditViewModel.update {
                            it.copy(
                                recurrenceId = value,
                                recurrenceLabel = recurrenceLabelForUi(value)
                            )
                        }
                    },
                    onOccurrenceDateChanged = { value -> classEditViewModel.update { it.copy(occurrenceDate = value) } },
                    onRoomChanged = { value -> classEditViewModel.update { it.copy(room = value) } },
                    onBuildingChanged = { value -> classEditViewModel.update { it.copy(building = value) } },
                    onGroupChanged = { value -> classEditViewModel.update { it.copy(group = value) } },
                    onTeacherChanged = { value -> classEditViewModel.update { it.copy(teacher = value) } },
                    onNoteChanged = { value -> classEditViewModel.update { it.copy(note = value) } },
                    onSave = classEditViewModel::save,
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
                        occurrenceViewModel.open("$classId:$date")
                    }
                }
                OccurrenceDetailsScreen(
                    state = occurrenceDetails,
                    onDeleteBaseClass = occurrenceViewModel::deleteSelectedClass,
                    onDismissDeleteConfirmation = occurrenceViewModel::cancelClassDeletion,
                    onOpenOccurrenceEdit = occurrenceViewModel::openEditDialog,
                    onDismissOccurrenceEdit = occurrenceViewModel::dismissEditDialog,
                    onSaveOccurrenceChange = occurrenceViewModel::saveOccurrenceChange,
                    onRestoreOccurrence = occurrenceViewModel::restoreOccurrence,
                    onOccurrenceNoteDraftChanged = occurrenceViewModel::updateOccurrenceNoteDraft,
                    onSharedNoteDraftChanged = occurrenceViewModel::updateSharedNoteDraft,
                    onTargetDateDraftChanged = { value ->
                        occurrenceViewModel.updateDraft { it.copy(targetDateDraft = value) }
                    },
                    onStartTimeDraftChanged = { value ->
                        occurrenceViewModel.updateDraft { it.copy(startTimeDraft = value) }
                    },
                    onEndTimeDraftChanged = { value ->
                        occurrenceViewModel.updateDraft { it.copy(endTimeDraft = value) }
                    },
                    onRoomDraftChanged = { value ->
                        occurrenceViewModel.updateDraft { it.copy(roomDraft = value) }
                    },
                    onSaveSharedNote = occurrenceViewModel::saveSharedNote,
                    onSaveOccurrenceNote = occurrenceViewModel::saveOccurrenceNote,
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
                    semesterId?.let(semesterViewModel::open)
                }
                SemesterScreen(
                    state = semesterState,
                    onSemesterNameChanged = { value ->
                        semesterViewModel.update { it.copy(semester = it.semester.copy(name = value)) }
                    },
                    onSemesterStartDateChanged = { value ->
                        semesterViewModel.update { it.copy(semester = it.semester.copy(startDate = value)) }
                    },
                    onSemesterEndDateChanged = { value ->
                        semesterViewModel.update { it.copy(semester = it.semester.copy(endDate = value)) }
                    },
                    onSemesterFirstWeekChanged = { value ->
                        semesterViewModel.update { it.copy(semester = it.semester.copy(firstWeek = value)) }
                    },
                    onSaveSemester = semesterViewModel::saveSemester,
                    onOpenCourses = {
                        semesterId?.let { navController.navigate(semesterCoursesRoute(it)) }
                    },
                    onOpenOverrides = {
                        semesterId?.let { navController.navigate(semesterOverridesRoute(it)) }
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
                    semesterId?.let(semesterViewModel::open)
                }
                SemesterCoursesScreen(
                    state = semesterState,
                    onCourseNameChanged = semesterViewModel::updateCourseName,
                    onCourseColorChanged = semesterViewModel::updateCourseColor,
                    onAddCourse = semesterViewModel::addCourse,
                    onDeleteCourse = semesterViewModel::deleteCourse,
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
                    semesterId?.let(semesterViewModel::open)
                }
                SemesterWeekOverridesScreen(
                    state = semesterState,
                    onWeekStartDateChanged = { value ->
                        semesterViewModel.update { it.copy(overrideForm = it.overrideForm.copy(weekStartDate = value)) }
                    },
                    onWeekTypeChanged = { value ->
                        semesterViewModel.update { it.copy(overrideForm = it.overrideForm.copy(weekType = value)) }
                    },
                    onScopeChanged = { value ->
                        semesterViewModel.update { it.copy(overrideForm = it.overrideForm.copy(scope = value)) }
                    },
                    onNewOverride = semesterViewModel::newWeekOverride,
                    onEditOverride = semesterViewModel::editWeekOverride,
                    onSaveOverride = semesterViewModel::saveWeekOverride,
                    onDeleteOverride = semesterViewModel::deleteWeekOverride,
                    onCancelOverrideEdit = semesterViewModel::cancelWeekOverrideEdit,
                    onBack = ::navigateBack,
                    modifier = Modifier.fillMaxSize()
                )
            }

            composable(MakRoutes.Settings) {
                val settingsState = settingsViewModel.settings.collectAsStateWithLifecycle().value
                SettingsScreen(
                    state = settingsState,
                    onSemesterSelected = settingsViewModel::selectSemester,
                    onAddSemester = {
                        setupViewModel.start()
                        navigateToSetup()
                    },
                    onConfigureSemester = { id ->
                        navController.navigate(semesterRoute(id))
                    },
                    onDeleteSemester = settingsViewModel::requestSemesterDeletion,
                    onConfirmDelete = settingsViewModel::confirmSemesterDeletion,
                    onCancelDelete = settingsViewModel::cancelSemesterDeletion,
                    onThemeSelected = settingsViewModel::selectTheme,
                    onExport = onCreateExportDocument,
                    onRetry = {},
                    modifier = Modifier.fillMaxSize()
                )
            }

            composable(MakRoutes.Setup) {
                val setupState = setupViewModel.setup.collectAsStateWithLifecycle().value
                val settingsState = settingsViewModel.settings.collectAsStateWithLifecycle().value
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

internal fun shouldCloseOccurrenceDetails(currentRoute: String?): Boolean =
    currentRoute == MakRoutes.Occurrence

internal fun shouldCloseClassEditor(currentRoute: String?): Boolean =
    currentRoute == MakRoutes.Edit

internal fun shouldCloseSemesterConfiguration(currentRoute: String?): Boolean =
    currentRoute == MakRoutes.Semester

internal fun shouldHandleSetupEffect(currentRoute: String?): Boolean =
    currentRoute == MakRoutes.Setup

internal enum class AddAction {
    None,
    Setup,
    Editor
}

internal fun addAction(hasLoadedData: Boolean, requiresSetup: Boolean): AddAction = when {
    !hasLoadedData -> AddAction.None
    requiresSetup -> AddAction.Setup
    else -> AddAction.Editor
}

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
