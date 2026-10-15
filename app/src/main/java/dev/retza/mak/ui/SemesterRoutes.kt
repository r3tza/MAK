package dev.retza.mak.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.navigation
import dev.retza.mak.ui.freedays.FreeDayDialog
import dev.retza.mak.ui.freedays.FreeDayEditorViewModel
import dev.retza.mak.ui.freedays.FreeDaysScreen
import dev.retza.mak.ui.freedays.FreeDaysViewModel
import dev.retza.mak.ui.semester.SemesterCalendarsScreen
import dev.retza.mak.ui.semester.SemesterCourseAddScreen
import dev.retza.mak.ui.semester.SemesterCourseEditScreen
import dev.retza.mak.ui.semester.SemesterCoursesScreen
import dev.retza.mak.ui.semester.SemesterEffect
import dev.retza.mak.ui.semester.SemesterScreen
import dev.retza.mak.ui.semester.SemesterViewModel
import dev.retza.mak.ui.semester.SemesterWeekOverridesScreen
import dev.retza.mak.ui.programs.StudyProgramEditOpenState
import dev.retza.mak.ui.programs.StudyProgramEditorUi
import dev.retza.mak.ui.programs.StudyProgramsViewModel

internal fun openSemesterConfiguration(
    navController: NavController,
    id: Long
) {
    navController.navigate(semesterGraphRoute(id.toString()))
}

internal fun NavGraphBuilder.semesterRoutes(navController: NavController) {
    navigation(
        route = MakRoutes.SemesterGraph,
        startDestination = MakRoutes.Semester,
        arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
    ) {
        composable(
            route = MakRoutes.Semester,
            arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
        ) { entry ->
            val semesterViewModel = semesterGraphViewModel(navController, entry)
            SemesterEffects(semesterViewModel, navController)
            val semesterId = entry.arguments?.getString("semesterId")
            SemesterScreen(
                state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
                onSemesterNameChanged = semesterViewModel::updateSemesterName,
                onSemesterStartDateChanged = semesterViewModel::updateSemesterStartDate,
                onSemesterEndDateChanged = semesterViewModel::updateSemesterEndDate,
                onSemesterFirstWeekChanged = semesterViewModel::updateSemesterFirstWeek,
                onSaveSemester = semesterViewModel::saveSemester,
                onOpenCourses = { semesterId?.let { navController.navigate(semesterCoursesRoute(it)) } },
                onOpenOverrides = { semesterId?.let { navController.navigate(semesterOverridesRoute(it)) } },
                onOpenFreeDays = { semesterId?.let { navController.navigate(semesterFreeDaysRoute(it)) } },
                onOpenCalendars = { semesterId?.let { navController.navigate(semesterCalendarsRoute(it)) } },
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(
            route = MakRoutes.SemesterCourses,
            arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
        ) { entry ->
            val semesterViewModel = semesterGraphViewModel(navController, entry)
            SemesterEffects(semesterViewModel, navController)
            val semesterId = entry.arguments?.getString("semesterId")
            SemesterCoursesScreen(
                state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
                onAddCourse = {
                    semesterViewModel.openAddCourse {
                        semesterId?.let { navController.navigate(semesterCourseAddRoute(it)) }
                    }
                },
                onEditCourse = { assignmentId ->
                    semesterViewModel.openCourseEdit {
                        semesterId?.let { navController.navigate(semesterCourseEditRoute(it, assignmentId)) }
                    }
                },
                onDeleteCourse = semesterViewModel::requestCourseDeletion,
                onConfirmCourseDeletion = semesterViewModel::confirmCourseDeletion,
                onCancelCourseDeletion = semesterViewModel::cancelCourseDeletion,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(
            route = MakRoutes.SemesterCourseAdd,
            arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
        ) { entry ->
            val semesterViewModel = semesterGraphViewModel(navController, entry)
            SemesterEffects(semesterViewModel, navController)
            // Leaving the form by any way (cancel, saved, back) closes it and releases plan edits.
            DisposableEffect(entry.id) { onDispose { semesterViewModel.endCourseForm() } }
            val opened by produceState<Boolean?>(null, entry.id) {
                value = semesterViewModel.openCourseFormOnce()
            }
            if (opened != true) {
                LaunchedEffect(opened) {
                    if (opened == false) navController.popBackStack()
                }
                PlanEditingWait()
                return@composable
            }
            SemesterCourseAddScreen(
                state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
                onCourseNameChanged = semesterViewModel::updateCourseName,
                onCourseColorChanged = semesterViewModel::updateCourseColor,
                onProgramModeChanged = semesterViewModel::setCourseProgramMode,
                onSelectProgram = semesterViewModel::selectCourseProgram,
                onCourseModeChanged = semesterViewModel::setCourseCalendarMode,
                onCourseCalendarChanged = semesterViewModel::selectCourseCalendar,
                onAddCourse = semesterViewModel::addCourse,
                onCancel = {
                    semesterViewModel.cancelCourseForm()
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(
            route = MakRoutes.SemesterCourseEdit,
            arguments = listOf(
                navArgument("semesterId") { type = NavType.StringType },
                navArgument("assignmentId") { type = NavType.StringType }
            )
        ) { entry ->
            val semesterViewModel = semesterGraphViewModel(navController, entry)
            SemesterEffects(semesterViewModel, navController)
            val studyProgramsViewModel: StudyProgramsViewModel = koinViewModel()
            // Saving, cancelling and back all leave this screen, which closes the course form.
            DisposableEffect(entry.id) { onDispose { semesterViewModel.endCourseForm() } }
            // A route id that is not a number matches no course, so the screen leaves instead of using id 0.
            val assignmentId = entry.arguments?.getString("assignmentId")?.toLongOrNull()
            val opened by produceState<Boolean?>(null, entry.id) {
                val semesterLoaded = semesterViewModel.openCourseFormOnce()
                val programId = semesterViewModel.semester.value.courseItems
                    .firstOrNull { it.assignmentId == assignmentId }
                    ?.programId
                programId?.let { studyProgramsViewModel.openOnce(it) }
                val programLoaded = programId != null &&
                    studyProgramsViewModel.opened.value == StudyProgramEditOpenState.Open
                value = semesterLoaded && programLoaded
            }
            if (opened != true) {
                LaunchedEffect(opened) {
                    if (opened == false) navController.popBackStack()
                }
                PlanEditingWait()
                return@composable
            }
            StudyProgramEffects(studyProgramsViewModel, navController)
            val semesterState = semesterViewModel.semester.collectAsStateWithLifecycle().value
            val programId = semesterState.courseItems
                .firstOrNull { it.assignmentId == assignmentId }
                ?.programId
            val programsState = studyProgramsViewModel.programs.collectAsStateWithLifecycle().value
            val editor = if (programId != null && programsState.editor.id == programId) {
                programsState.editor
            } else {
                StudyProgramEditorUi()
            }
            SemesterCourseEditScreen(
                state = semesterState,
                editor = editor,
                onNameChanged = studyProgramsViewModel::updateName,
                onColorChanged = studyProgramsViewModel::updateColor,
                onSave = studyProgramsViewModel::save,
                onCancel = {
                    studyProgramsViewModel.closeEditor()
                    navController.popBackStack()
                },
                onSeparateCourse = semesterViewModel::separateCourseCalendar,
                onRequestReconnect = semesterViewModel::requestReconnect,
                onConfirmReconnect = semesterViewModel::confirmReconnect,
                onCancelReconnect = semesterViewModel::cancelReconnect,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(
            route = MakRoutes.SemesterOverrides,
            arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
        ) { entry ->
            val semesterViewModel = semesterGraphViewModel(navController, entry)
            SemesterEffects(semesterViewModel, navController)
            SemesterWeekOverridesScreen(
                state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
                onCalendarSelected = semesterViewModel::selectCalendar,
                onWeekStartDateChanged = semesterViewModel::updateWeekStartDate,
                onWeekTypeChanged = semesterViewModel::updateOverrideWeekType,
                onScopeChanged = semesterViewModel::updateOverrideScope,
                onNewOverride = semesterViewModel::newWeekOverride,
                onEditOverride = semesterViewModel::editWeekOverride,
                onSaveOverride = semesterViewModel::saveWeekOverride,
                onDeleteOverride = semesterViewModel::requestWeekOverrideDeletion,
                onConfirmOverrideDeletion = semesterViewModel::confirmWeekOverrideDeletion,
                onCancelOverrideDeletion = semesterViewModel::cancelWeekOverrideDeletion,
                onCancelOverrideEdit = semesterViewModel::cancelWeekOverrideEdit,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(
            route = MakRoutes.SemesterFreeDays,
            arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
        ) { entry ->
            val semesterViewModel = semesterGraphViewModel(navController, entry)
            SemesterEffects(semesterViewModel, navController)
            val viewModel: FreeDaysViewModel = koinViewModel()
            val editor: FreeDayEditorViewModel = koinViewModel()
            val state = viewModel.freeDays.collectAsStateWithLifecycle().value
            val editorState = editor.editor.collectAsStateWithLifecycle().value
            FreeDaysScreen(
                state = state,
                onAdd = { state.semesterId?.let { editor.startNew(it, null) } },
                onEdit = { ids -> state.semesterId?.let { editor.startEdit(it, ids) } },
                onDelete = viewModel::requestDeletion,
                onConfirmDeletion = viewModel::confirmDeletion,
                onCancelDeletion = viewModel::cancelDeletion,
                onOpenHolidays = viewModel::openHolidays,
                onToggleHoliday = viewModel::toggleHoliday,
                onAddHolidays = viewModel::addHolidays,
                onCancelHolidays = viewModel::cancelHolidays,
                modifier = Modifier.fillMaxSize()
            )
            FreeDayDialog(
                state = editorState,
                onRangeChange = editor::updateRange,
                onNameChange = editor::updateName,
                onToggleCalendar = editor::toggleCalendar,
                onWeekAfterBreakChange = editor::updateWeekAfterBreak,
                onSave = editor::save,
                onCancel = editor::cancel
            )
        }

        composable(
            route = MakRoutes.SemesterCalendars,
            arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
        ) { entry ->
            val semesterViewModel = semesterGraphViewModel(navController, entry)
            SemesterEffects(semesterViewModel, navController)
            SemesterCalendarsScreen(
                state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
                onCalendarSelected = semesterViewModel::selectCalendar,
                onStartDateChanged = semesterViewModel::updateSemesterStartDate,
                onEndDateChanged = semesterViewModel::updateSemesterEndDate,
                onFirstWeekChanged = semesterViewModel::updateSemesterFirstWeek,
                onSaveCalendar = semesterViewModel::saveCalendar,
                onDeleteCalendar = semesterViewModel::requestCalendarDeletion,
                onConfirmCalendarDeletion = semesterViewModel::confirmCalendarDeletion,
                onCancelCalendarDeletion = semesterViewModel::cancelCalendarDeletion,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** The semester screens share one view model, kept by the semester navigation graph entry. */
@Composable
private fun semesterGraphViewModel(navController: NavController, entry: NavBackStackEntry): SemesterViewModel {
    val graphEntry = remember(entry) { navController.getBackStackEntry(MakRoutes.SemesterGraph) }
    return koinViewModel(viewModelStoreOwner = graphEntry)
}

@Composable
internal fun SemesterEffects(
    semesterViewModel: SemesterViewModel,
    navController: NavController
) {
    LaunchedEffect(semesterViewModel, navController) {
        semesterViewModel.effects.collect { effect ->
            when (effect) {
                SemesterEffect.CloseConfiguration -> {
                    if (shouldCloseSemesterConfiguration(navController.currentBackStackEntry?.destination?.route)) {
                        navController.popBackStack()
                    }
                }
                SemesterEffect.CourseAdded -> {
                    if (navController.currentBackStackEntry?.destination?.route == MakRoutes.SemesterCourseAdd) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}
