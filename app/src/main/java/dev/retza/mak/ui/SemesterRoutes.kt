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
import dev.retza.mak.ui.semester.SemesterCoursesScreen
import dev.retza.mak.ui.semester.SemesterEffect
import dev.retza.mak.ui.semester.SemesterScreen
import dev.retza.mak.ui.semester.SemesterViewModel
import dev.retza.mak.ui.semester.SemesterWeekOverridesScreen

internal fun NavGraphBuilder.semesterRoutes(
    semesterViewModel: SemesterViewModel,
    onBack: () -> Unit,
    onOpenCourses: (String) -> Unit,
    onOpenOverrides: (String) -> Unit
) {
    composable(
        route = MakRoutes.Semester,
        arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
    ) { entry ->
        val semesterId = entry.arguments?.getString("semesterId")
        LaunchedEffect(semesterId) {
            semesterId?.let(semesterViewModel::open)
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
            onOpenCourses = { semesterId?.let(onOpenCourses) },
            onOpenOverrides = { semesterId?.let(onOpenOverrides) },
            onBack = onBack,
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
            state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
            onCourseNameChanged = semesterViewModel::updateCourseName,
            onCourseColorChanged = semesterViewModel::updateCourseColor,
            onAddCourse = semesterViewModel::addCourse,
            onDeleteCourse = semesterViewModel::deleteCourse,
            onBack = onBack,
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
            state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
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
            onBack = onBack,
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
