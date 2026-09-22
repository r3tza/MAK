package dev.retza.mak.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.retza.mak.ui.settings.ImportPreviewScreen
import dev.retza.mak.ui.settings.SettingsDataScreen
import dev.retza.mak.ui.settings.SettingsEffect
import dev.retza.mak.ui.settings.SettingsNotificationsScreen
import dev.retza.mak.ui.settings.SettingsScreen
import dev.retza.mak.ui.settings.SettingsSemestersScreen
import dev.retza.mak.ui.settings.SettingsViewModel

internal fun openSettings(navController: NavController) {
    navController.navigate(MakRoutes.Settings)
}

internal fun NavGraphBuilder.settingsRoute(
    settingsViewModel: SettingsViewModel,
    navController: NavController,
    onAddSemester: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    notificationsBlocked: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    composable(MakRoutes.Settings) {
        val settingsState by settingsViewModel.settings.collectAsStateWithLifecycle()
        SettingsScreen(
            state = settingsState,
            onOpenSemesters = { navController.navigate(MakRoutes.SettingsSemesters) },
            onOpenNotifications = { navController.navigate(MakRoutes.SettingsNotifications) },
            onOpenData = { navController.navigate(MakRoutes.SettingsData) },
            onSemesterSelected = settingsViewModel::selectSemester,
            onAddSemester = onAddSemester,
            onThemeSelected = settingsViewModel::selectTheme,
            onGapThresholdSelected = settingsViewModel::setGapThresholdMinutes,
            notificationsBlocked = notificationsBlocked,
            onRetry = {},
            modifier = Modifier.fillMaxSize()
        )
    }

    composable(MakRoutes.SettingsSemesters) {
        val settingsState by settingsViewModel.settings.collectAsStateWithLifecycle()
        SettingsSemestersScreen(
            state = settingsState,
            onSemesterSelected = settingsViewModel::selectSemester,
            onAddSemester = onAddSemester,
            onConfigureSemester = { id -> openSemesterConfiguration(navController, id) },
            onDeleteSemester = settingsViewModel::requestSemesterDeletion,
            onConfirmDelete = settingsViewModel::confirmSemesterDeletion,
            onCancelDelete = settingsViewModel::cancelSemesterDeletion,
            modifier = Modifier.fillMaxSize()
        )
    }

    composable(MakRoutes.SettingsNotifications) {
        val settingsState by settingsViewModel.settings.collectAsStateWithLifecycle()
        SettingsNotificationsScreen(
            state = settingsState,
            notificationsBlocked = notificationsBlocked,
            onNotificationsEnabled = settingsViewModel::setNotificationsEnabled,
            onEveningNotificationsEnabled = settingsViewModel::setEveningNotificationsEnabled,
            onBeforeClassNotificationsEnabled = settingsViewModel::setBeforeClassNotificationsEnabled,
            onEveningHourSelected = settingsViewModel::setEveningHour,
            onBeforeClassLeadSelected = settingsViewModel::setBeforeClassLeadMinutes,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onOpenAppSettings = onOpenAppSettings,
            modifier = Modifier.fillMaxSize()
        )
    }

    composable(MakRoutes.SettingsData) {
        val settingsState by settingsViewModel.settings.collectAsStateWithLifecycle()
        SettingsDataScreen(
            state = settingsState,
            onExport = onExport,
            onImport = onImport,
            onDismissImportError = settingsViewModel::dismissImportError,
            modifier = Modifier.fillMaxSize()
        )
    }

    composable(MakRoutes.ImportPreview) {
        val settingsState by settingsViewModel.settings.collectAsStateWithLifecycle()
        ImportPreviewScreen(
            state = settingsState,
            onConfirm = settingsViewModel::confirmImport,
            onCancel = settingsViewModel::cancelImport,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
internal fun SettingsEffects(
    settingsViewModel: SettingsViewModel,
    navController: NavController
) {
    LaunchedEffect(settingsViewModel, navController) {
        settingsViewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.OpenImportPreview -> navController.navigate(MakRoutes.ImportPreview)
                SettingsEffect.CloseImportPreview -> {
                    if (shouldCloseImportPreview(navController.currentBackStackEntry?.destination?.route)) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}
