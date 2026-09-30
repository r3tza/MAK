package dev.retza.mak.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
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
import dev.retza.mak.ui.components.MakNavRail
import dev.retza.mak.ui.edit.ClassEditViewModel
import dev.retza.mak.ui.feedback.MakSnackbarHost
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.occurrence.OccurrenceViewModel
import dev.retza.mak.ui.schedule.ScheduleViewModel
import dev.retza.mak.ui.programs.StudyProgramsViewModel
import dev.retza.mak.ui.semester.SemesterViewModel
import dev.retza.mak.ui.settings.SettingsViewModel
import dev.retza.mak.ui.settings.SyncViewModel
import dev.retza.mak.ui.setup.SetupViewModel
import dev.retza.mak.ui.today.TodayViewModel
import dev.retza.mak.update.UpdateViewModel
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
    syncViewModel: SyncViewModel,
    studyProgramsViewModel: StudyProgramsViewModel,
    scheduleViewModel: ScheduleViewModel,
    todayViewModel: TodayViewModel,
    updateViewModel: UpdateViewModel,
    feedback: Flow<UiFeedback>,
    openTodayRequests: Flow<Unit>,
    openPlanRequests: Flow<String>,
    onCreateExportDocument: () -> Unit,
    onImportPlan: () -> Unit,
    notificationsBlocked: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onGrantInstallPermission: () -> Unit = {}
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val syncState = syncViewModel.sync.collectAsStateWithLifecycle().value
    val editor = classEditViewModel.editor.collectAsStateWithLifecycle().value
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value
        ?.destination
        ?.route
    val isRoot = currentRoute == MakRoutes.Today || currentRoute == MakRoutes.Schedule
    val showBack = currentRoute != null && !isRoot && !isSetupRoute(currentRoute)
    val navigationLayout = makNavigationLayout(LocalMakWidthClass.current, isRoot)

    fun openRoot(route: String) {
        navController.navigate(route) {
            popUpTo(MakRoutes.Today) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateBack() {
        if (!navController.popBackStack()) {
            openRoot(MakRoutes.Today)
        }
    }

    val startSetup: () -> Unit = {
        if (state.hasLoadedData) {
            setupViewModel.start(state.setupResume)
            openSetup(navController)
        }
    }

    val onAddClass: () -> Unit = {
        when (addAction(state.hasLoadedData, state.requiresSetup)) {
            AddAction.None -> Unit
            AddAction.Setup -> startSetup()
            AddAction.Editor -> openClassEditor(classEditViewModel, navController)
        }
    }

    val openOccurrenceById: (String) -> Unit = { id ->
        openOccurrence(occurrenceViewModel, navController, id)
    }

    LaunchedEffect(openTodayRequests, navController) {
        openTodayRequests.collect { openRoot(MakRoutes.Today) }
    }

    LaunchedEffect(openPlanRequests, navController) {
        openPlanRequests.collect { date ->
            scheduleViewModel.showDate(date)
            openRoot(MakRoutes.Schedule)
        }
    }

    OccurrenceEffects(occurrenceViewModel, navController)
    ClassEditEffects(classEditViewModel, navController)
    SemesterEffects(semesterViewModel, navController)
    SettingsEffects(settingsViewModel, navController)
    StudyProgramEffects(studyProgramsViewModel, navController)
    SetupEffects(setupViewModel, classEditViewModel, navController)
    ScheduleEffects(scheduleViewModel, classEditViewModel, navController)

    BackHandler(enabled = showBack) { navigateBack() }

    val occurrenceActions = occurrenceTopBarActions(occurrenceViewModel, classEditViewModel, navController)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            MakSnackbarHost(feedback = feedback)
        },
        topBar = {
            MakTopBar(
                title = titleForRoute(currentRoute, editor.title),
                showBack = showBack,
                showSettings = isRoot,
                onBack = ::navigateBack,
                onSettings = { openSettings(navController) },
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
            if (navigationLayout == MakNavigationLayout.BottomBar) {
                MakNavBar(
                    todaySelected = currentRoute == MakRoutes.Today,
                    planSelected = currentRoute == MakRoutes.Schedule,
                    addSelected = false,
                    onToday = { openRoot(MakRoutes.Today) },
                    onPlan = { openRoot(MakRoutes.Schedule) },
                    onAdd = onAddClass
                )
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (navigationLayout == MakNavigationLayout.Rail) {
                MakNavRail(
                    todaySelected = currentRoute == MakRoutes.Today,
                    planSelected = currentRoute == MakRoutes.Schedule,
                    onToday = { openRoot(MakRoutes.Today) },
                    onPlan = { openRoot(MakRoutes.Schedule) },
                    onAdd = onAddClass
                )
            }
            NavHost(
                navController = navController,
                startDestination = MakRoutes.Today,
                modifier = if (navigationLayout == MakNavigationLayout.Rail) {
                    Modifier.weight(1f).fillMaxHeight()
                } else {
                    Modifier.fillMaxSize()
                }
            ) {
                todayRoute(
                    appState = state,
                    todayViewModel = todayViewModel,
                    updateViewModel = updateViewModel,
                    navController = navController,
                    onOpenOccurrence = openOccurrenceById,
                    startSetup = startSetup,
                    openSync = { navController.navigate(MakRoutes.SettingsSync) },
                    syncAttention = { syncState.attention }
                )
                scheduleRoute(
                    appState = state,
                    scheduleViewModel = scheduleViewModel,
                    onOpenOccurrence = openOccurrenceById,
                    startSetup = startSetup,
                    openSync = { navController.navigate(MakRoutes.SettingsSync) }
                )
                classEditRoute(classEditViewModel = classEditViewModel, onBack = ::navigateBack)
                occurrenceDetailsRoute(occurrenceViewModel = occurrenceViewModel)
                semesterRoutes(
                    semesterViewModel = semesterViewModel,
                    navController = navController,
                    studyProgramsViewModel = studyProgramsViewModel
                )
                settingsRoute(
                    settingsViewModel = settingsViewModel,
                    syncViewModel = syncViewModel,
                    updateViewModel = updateViewModel,
                    navController = navController,
                    onAddSemester = {
                        setupViewModel.start()
                        openSetup(navController)
                    },
                    onConfigureSemester = { id ->
                        openSemesterConfiguration(semesterViewModel, navController, id)
                    },
                    onExport = onCreateExportDocument,
                    onImport = onImportPlan,
                    notificationsBlocked = notificationsBlocked,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onOpenAppSettings = onOpenAppSettings,
                    onGrantInstallPermission = onGrantInstallPermission
                )
                studyProgramRoutes(
                    studyProgramsViewModel = studyProgramsViewModel,
                    navController = navController,
                    onBack = ::navigateBack
                )
                setupRoute(setupViewModel = setupViewModel, settingsViewModel = settingsViewModel)
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
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null
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
