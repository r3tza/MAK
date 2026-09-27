package dev.retza.mak.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.retza.mak.ui.semester.SemesterCalendarsScreen
import dev.retza.mak.ui.semester.SemesterCoursesScreen
import dev.retza.mak.ui.semester.SemesterEffect
import dev.retza.mak.ui.semester.SemesterScreen
import dev.retza.mak.ui.semester.SemesterViewModel
import dev.retza.mak.ui.semester.SemesterWeekOverridesScreen

internal fun openSemesterConfiguration(
    semesterViewModel: SemesterViewModel,
    navController: NavController,
    id: String
) {
    semesterViewModel.open(id)
    navController.navigate(semesterRoute(id))
}

internal fun NavGraphBuilder.semesterRoutes(
    semesterViewModel: SemesterViewModel,
    navController: NavController
) {
    composable(
        route = MakRoutes.Semester,
        arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
    ) { entry ->
        val semesterId = entry.arguments?.getString("semesterId")
        LaunchedEffect(semesterId) {
            semesterId?.let(semesterViewModel::openIfNeeded)
        }
        SemesterScreen(
            state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
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
            onOpenCourses = { semesterId?.let { navController.navigate(semesterCoursesRoute(it)) } },
            onOpenOverrides = { semesterId?.let { navController.navigate(semesterOverridesRoute(it)) } },
            onOpenCalendars = { semesterId?.let { navController.navigate(semesterCalendarsRoute(it)) } },
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
            semesterId?.let(semesterViewModel::openIfNeeded)
        }
        SemesterCoursesScreen(
            state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
            onCourseNameChanged = semesterViewModel::updateCourseName,
            onCourseColorChanged = semesterViewModel::updateCourseColor,
            onProgramModeChanged = semesterViewModel::setCourseProgramMode,
            onSelectProgram = semesterViewModel::selectCourseProgram,
            onCourseModeChanged = semesterViewModel::setCourseCalendarMode,
            onCourseCalendarChanged = semesterViewModel::selectCourseCalendar,
            onAddCourse = semesterViewModel::addCourse,
            onDeleteCourse = semesterViewModel::requestCourseDeletion,
            onSeparateCourse = semesterViewModel::separateCourseCalendar,
            onRequestReconnect = semesterViewModel::requestReconnect,
            onConfirmReconnect = semesterViewModel::confirmReconnect,
            onCancelReconnect = semesterViewModel::cancelReconnect,
            onConfirmCourseDeletion = semesterViewModel::confirmCourseDeletion,
            onCancelCourseDeletion = semesterViewModel::cancelCourseDeletion,
            modifier = Modifier.fillMaxSize()
        )
    }

    composable(
        route = MakRoutes.SemesterOverrides,
        arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
    ) { entry ->
        val semesterId = entry.arguments?.getString("semesterId")
        LaunchedEffect(semesterId) {
            semesterId?.let(semesterViewModel::openIfNeeded)
        }
        SemesterWeekOverridesScreen(
            state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
            onCalendarSelected = semesterViewModel::selectCalendar,
            onWeekStartDateChanged = semesterViewModel::updateWeekStartDate,
            onWeekTypeChanged = { value ->
                semesterViewModel.update { it.copy(overrideForm = it.overrideForm.copy(weekType = value)) }
            },
            onScopeChanged = { value ->
                semesterViewModel.update { it.copy(overrideForm = it.overrideForm.copy(scope = value)) }
            },
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
        route = MakRoutes.SemesterCalendars,
        arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
    ) { entry ->
        val semesterId = entry.arguments?.getString("semesterId")
        LaunchedEffect(semesterId) {
            semesterId?.let(semesterViewModel::openIfNeeded)
        }
        SemesterCalendarsScreen(
            state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
            onCalendarSelected = semesterViewModel::selectCalendar,
            onStartDateChanged = { value ->
                semesterViewModel.update { it.copy(semester = it.semester.copy(startDate = value)) }
            },
            onEndDateChanged = { value ->
                semesterViewModel.update { it.copy(semester = it.semester.copy(endDate = value)) }
            },
            onFirstWeekChanged = { value ->
                semesterViewModel.update { it.copy(semester = it.semester.copy(firstWeek = value)) }
            },
            onSaveCalendar = semesterViewModel::saveCalendar,
            onDeleteCalendar = semesterViewModel::requestCalendarDeletion,
            onConfirmCalendarDeletion = semesterViewModel::confirmCalendarDeletion,
            onCancelCalendarDeletion = semesterViewModel::cancelCalendarDeletion,
            modifier = Modifier.fillMaxSize()
        )
    }
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
            }
        }
    }
}
