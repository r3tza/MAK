package dev.retza.mak.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import dev.retza.mak.sync.PlanEditTracker
import dev.retza.mak.ui.semester.SemesterCalendarsScreen
import dev.retza.mak.ui.semester.SemesterCourseAddScreen
import dev.retza.mak.ui.semester.SemesterCourseEditScreen
import dev.retza.mak.ui.semester.SemesterCoursesScreen
import dev.retza.mak.ui.semester.SemesterEffect
import dev.retza.mak.ui.semester.SemesterScreen
import dev.retza.mak.ui.semester.SemesterViewModel
import dev.retza.mak.ui.semester.hasPlanDraft
import dev.retza.mak.ui.semester.SemesterWeekOverridesScreen
import dev.retza.mak.ui.programs.StudyProgramEditOpenState
import dev.retza.mak.ui.programs.StudyProgramEditorUi
import dev.retza.mak.ui.programs.StudyProgramsViewModel

internal fun openSemesterConfiguration(
    navController: NavController,
    id: String
) {
    navController.navigate(semesterGraphRoute(id))
}

internal fun NavGraphBuilder.semesterRoutes(
    planEditTracker: PlanEditTracker,
    navController: NavController
) {
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
            val editing = TrackPlanEditing(
                planEditTracker,
                active = semesterViewModel.semester.collectAsStateWithLifecycle().value.hasPlanDraft(),
                isActiveNow = { semesterViewModel.semester.value.hasPlanDraft() }
            )
            val semesterId = entry.arguments?.getString("semesterId")
            SemesterScreen(
                state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
                onSemesterNameChanged = editing.callback(
                    refreshBeforeFirstEdit = { semesterViewModel.refreshForEdit() }
                ) { value -> semesterViewModel.update { it.copy(semester = it.semester.copy(name = value)) } },
                onSemesterStartDateChanged = editing.callback(
                    refreshBeforeFirstEdit = { semesterViewModel.refreshForEdit() }
                ) { value -> semesterViewModel.update { it.copy(semester = it.semester.copy(startDate = value)) } },
                onSemesterEndDateChanged = editing.callback(
                    refreshBeforeFirstEdit = { semesterViewModel.refreshForEdit() }
                ) { value -> semesterViewModel.update { it.copy(semester = it.semester.copy(endDate = value)) } },
                onSemesterFirstWeekChanged = editing.callback(
                    refreshBeforeFirstEdit = { semesterViewModel.refreshForEdit() }
                ) { value -> semesterViewModel.update { it.copy(semester = it.semester.copy(firstWeek = value)) } },
                onSaveSemester = semesterViewModel::saveSemester,
                onOpenCourses = { semesterId?.let { navController.navigate(semesterCoursesRoute(it)) } },
                onOpenOverrides = { semesterId?.let { navController.navigate(semesterOverridesRoute(it)) } },
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
            val editing = TrackPlanEditing(
                planEditTracker,
                active = semesterViewModel.semester.collectAsStateWithLifecycle().value.hasPlanDraft(),
                isActiveNow = { semesterViewModel.semester.value.hasPlanDraft() }
            )
            val semesterId = entry.arguments?.getString("semesterId")
            SemesterCoursesScreen(
                state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
                onAddCourse = {
                    editing.run(
                        refreshBeforeFirstEdit = { semesterViewModel.refreshForEdit() },
                        holdIfInactive = true
                    ) {
                        semesterId?.let {
                        semesterViewModel.resetCourseDraft()
                        navController.navigate(semesterCourseAddRoute(it))
                        }
                    }
                },
                onEditCourse = { assignmentId ->
                    editing.run(
                        refreshBeforeFirstEdit = { semesterViewModel.refreshForEdit() },
                        holdIfInactive = true
                    ) {
                        semesterId?.let {
                        navController.navigate(semesterCourseEditRoute(it, assignmentId))
                        }
                    }
                },
                onDeleteCourse = editing.callback(
                    refreshBeforeFirstEdit = { semesterViewModel.refreshForEdit() },
                    action = semesterViewModel::requestCourseDeletion
                ),
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
            val editing = TrackPlanEditing(planEditTracker)
            if (!editing.ready) {
                PlanEditingWait()
                return@composable
            }
            val opened by produceState<Boolean?>(null, entry.id) {
                value = semesterViewModel.refreshForCourseForm()
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
                    semesterViewModel.resetCourseDraft()
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
            val editing = TrackPlanEditing(planEditTracker)
            if (!editing.ready) {
                PlanEditingWait()
                return@composable
            }
            val assignmentId = entry.arguments?.getString("assignmentId")
            val opened by produceState<Boolean?>(null, entry.id) {
                val semesterLoaded = semesterViewModel.refreshForCourseForm()
                val programId = semesterViewModel.semester.value.courseItems
                    .firstOrNull { it.assignmentId == assignmentId }
                    ?.programId
                    ?.toLongOrNull()
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
                ?.toLongOrNull()
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
                onSeparateCourse = editing.callback(
                    refreshBeforeFirstEdit = { semesterViewModel.refreshForEdit() },
                    action = semesterViewModel::separateCourseCalendar
                ),
                onRequestReconnect = { assignmentId, calendarId ->
                    editing.run(
                        refreshBeforeFirstEdit = { semesterViewModel.refreshForEdit() },
                        holdIfInactive = true
                    ) {
                        semesterViewModel.requestReconnect(assignmentId, calendarId)
                    }
                },
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
            val editing = TrackPlanEditing(
                planEditTracker,
                active = semesterViewModel.semester.collectAsStateWithLifecycle().value.hasPlanDraft(),
                isActiveNow = { semesterViewModel.semester.value.hasPlanDraft() }
            )
            SemesterWeekOverridesScreen(
                state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
                onCalendarSelected = semesterViewModel::selectCalendar,
                onWeekStartDateChanged = editing.callback(
                    refreshBeforeFirstEdit = { semesterViewModel.refreshForEdit() },
                    action = semesterViewModel::updateWeekStartDate
                ),
                onWeekTypeChanged = editing.callback(
                    refreshBeforeFirstEdit = { semesterViewModel.refreshForEdit() }
                ) { value ->
                    semesterViewModel.update { it.copy(overrideForm = it.overrideForm.copy(weekType = value)) }
                },
                onScopeChanged = editing.callback(
                    refreshBeforeFirstEdit = { semesterViewModel.refreshForEdit() }
                ) { value ->
                    semesterViewModel.update { it.copy(overrideForm = it.overrideForm.copy(scope = value)) }
                },
                onNewOverride = editing.callback(
                    refreshBeforeFirstEdit = { semesterViewModel.refreshForEdit() },
                    action = semesterViewModel::newWeekOverride
                ),
                onEditOverride = editing.callback(
                    refreshBeforeFirstEdit = { semesterViewModel.refreshForEdit() },
                    action = semesterViewModel::editWeekOverride
                ),
                onSaveOverride = semesterViewModel::saveWeekOverride,
                onDeleteOverride = editing.callback(
                    refreshBeforeFirstEdit = { semesterViewModel.refreshForEdit() },
                    action = semesterViewModel::requestWeekOverrideDeletion
                ),
                onConfirmOverrideDeletion = semesterViewModel::confirmWeekOverrideDeletion,
                onCancelOverrideDeletion = semesterViewModel::cancelWeekOverrideDeletion,
                onCancelOverrideEdit = semesterViewModel::cancelWeekOverrideEdit,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(
            route = MakRoutes.SemesterCalendars,
            arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
        ) { entry ->
            val semesterViewModel = semesterGraphViewModel(navController, entry)
            SemesterEffects(semesterViewModel, navController)
            val editing = TrackPlanEditing(
                planEditTracker,
                active = semesterViewModel.semester.collectAsStateWithLifecycle().value.hasPlanDraft(),
                isActiveNow = { semesterViewModel.semester.value.hasPlanDraft() }
            )
            SemesterCalendarsScreen(
                state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
                onCalendarSelected = semesterViewModel::selectCalendar,
                onStartDateChanged = editing.callback(
                    refreshBeforeFirstEdit = { semesterViewModel.refreshForEdit() }
                ) { value ->
                    semesterViewModel.update { it.copy(semester = it.semester.copy(startDate = value)) }
                },
                onEndDateChanged = editing.callback(
                    refreshBeforeFirstEdit = { semesterViewModel.refreshForEdit() }
                ) { value ->
                    semesterViewModel.update { it.copy(semester = it.semester.copy(endDate = value)) }
                },
                onFirstWeekChanged = editing.callback(
                    refreshBeforeFirstEdit = { semesterViewModel.refreshForEdit() }
                ) { value ->
                    semesterViewModel.update { it.copy(semester = it.semester.copy(firstWeek = value)) }
                },
                onSaveCalendar = semesterViewModel::saveCalendar,
                onDeleteCalendar = editing.callback(
                    refreshBeforeFirstEdit = { semesterViewModel.refreshForEdit() },
                    action = semesterViewModel::requestCalendarDeletion
                ),
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
