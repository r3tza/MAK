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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.retza.mak.ui.components.MakBrandMark
import dev.retza.mak.ui.components.MakActionMenu
import dev.retza.mak.ui.components.MakIconButton
import dev.retza.mak.ui.components.MakNavBar
import dev.retza.mak.ui.edit.ClassEditViewModel
import dev.retza.mak.ui.feedback.MakSnackbarHost
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.occurrence.OccurrenceViewModel
import dev.retza.mak.ui.schedule.ScheduleViewModel
import dev.retza.mak.ui.semester.SemesterViewModel
import dev.retza.mak.ui.settings.SettingsViewModel
import dev.retza.mak.ui.setup.SetupViewModel
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

    fun openOccurrence(occurrenceId: String) {
        occurrenceViewModel.open(occurrenceId)
        navController.navigate(occurrenceRoute(occurrenceId))
    }

    OccurrenceEffects(occurrenceViewModel, navController)
    ClassEditEffects(classEditViewModel, navController)
    SemesterEffects(semesterViewModel, navController)
    SetupEffects(setupViewModel, classEditViewModel, navController, viewModel::navigate)
    ScheduleEffects(scheduleViewModel, classEditViewModel, navController)

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

    val occurrenceActions = if (currentRoute == MakRoutes.Occurrence) {
        occurrenceTopBarActions(occurrenceViewModel, classEditViewModel, navController)
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
            todayRoute(
                appState = state,
                todayViewModel = todayViewModel,
                onOpenPlan = { openRoot(MakDestination.Schedule, MakRoutes.Schedule) },
                onOpenOccurrence = ::openOccurrence,
                startSetup = startSetup
            )
            scheduleRoute(
                appState = state,
                scheduleViewModel = scheduleViewModel,
                onOpenOccurrence = ::openOccurrence,
                startSetup = startSetup
            )
            classEditRoute(classEditViewModel = classEditViewModel, onBack = ::navigateBack)
            occurrenceDetailsRoute(occurrenceViewModel = occurrenceViewModel, onBack = ::navigateBack)
            semesterRoutes(
                semesterViewModel = semesterViewModel,
                onBack = ::navigateBack,
                onOpenCourses = { navController.navigate(semesterCoursesRoute(it)) },
                onOpenOverrides = { navController.navigate(semesterOverridesRoute(it)) }
            )
            settingsRoute(
                settingsViewModel = settingsViewModel,
                onAddSemester = {
                    setupViewModel.start()
                    navigateToSetup()
                },
                onConfigureSemester = { navController.navigate(semesterRoute(it)) },
                onExport = onCreateExportDocument
            )
            setupRoute(setupViewModel = setupViewModel, settingsViewModel = settingsViewModel)
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
