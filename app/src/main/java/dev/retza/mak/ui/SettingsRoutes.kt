package dev.retza.mak.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.retza.mak.ui.settings.ImportPreviewScreen
import dev.retza.mak.ui.settings.AboutScreen
import dev.retza.mak.ui.settings.SettingsDataScreen
import dev.retza.mak.ui.settings.SettingsEffect
import dev.retza.mak.ui.settings.SettingsNotificationsScreen
import dev.retza.mak.ui.settings.SettingsScreen
import dev.retza.mak.ui.settings.SettingsSemestersScreen
import dev.retza.mak.ui.settings.SettingsViewModel
import dev.retza.mak.ui.settings.SyncScreen
import dev.retza.mak.ui.settings.SyncViewModel
import dev.retza.mak.ui.settings.settingsSummary
import dev.retza.mak.ui.settings.UpdateScreen
import dev.retza.mak.ui.settings.toSettingsUi
import dev.retza.mak.update.UpdateViewModel

internal fun openSettings(navController: NavController) {
    navController.navigate(MakRoutes.Settings)
}

internal fun NavGraphBuilder.settingsRoute(
    settingsViewModel: SettingsViewModel,
    syncViewModel: SyncViewModel,
    updateViewModel: UpdateViewModel,
    navController: NavController,
    onAddSemester: () -> Unit,
    onConfigureSemester: (String) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    notificationsBlocked: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onGrantInstallPermission: () -> Unit
) {
    composable(MakRoutes.Settings) {
        val settingsState by settingsViewModel.settings.collectAsStateWithLifecycle()
        val updateState by updateViewModel.state.collectAsStateWithLifecycle()
        val syncState by syncViewModel.sync.collectAsStateWithLifecycle()
        SettingsScreen(
            state = settingsState,
            onOpenSemesters = { navController.navigate(MakRoutes.SettingsSemesters) },
            onOpenPrograms = { navController.navigate(MakRoutes.StudyPrograms) },
            onOpenNotifications = { navController.navigate(MakRoutes.SettingsNotifications) },
            onOpenData = { navController.navigate(MakRoutes.SettingsData) },
            onOpenAbout = { navController.navigate(MakRoutes.SettingsAbout) },
            syncSummary = syncState.settingsSummary(),
            onOpenSync = { navController.navigate(MakRoutes.SettingsSync) },
            onSemesterSelected = settingsViewModel::selectSemester,
            onAddSemester = onAddSemester,
            onThemeSelected = settingsViewModel::selectTheme,
            onGapThresholdSelected = settingsViewModel::setGapThresholdMinutes,
            notificationsBlocked = notificationsBlocked,
            onRetry = {},
            updates = updateState.toSettingsUi(),
            onCheckUpdates = updateViewModel::checkNow,
            onOpenUpdate = { navController.navigate(MakRoutes.SettingsUpdate) },
            onAutomaticChecksChanged = updateViewModel::setAutomaticChecks,
            modifier = Modifier.fillMaxSize()
        )
    }

    composable(MakRoutes.SettingsAbout) {
        val updateState by updateViewModel.state.collectAsStateWithLifecycle()
        AboutScreen(
            installedVersionName = updateState.installedVersionName,
            releaseHistory = updateState.releaseHistory,
            modifier = Modifier.fillMaxSize()
        )
    }

    composable(MakRoutes.SettingsUpdate) {
        val updateState by updateViewModel.state.collectAsStateWithLifecycle()
        UpdateScreen(
            state = updateState,
            onCheckNow = updateViewModel::checkNow,
            onDownload = updateViewModel::downloadUpdate,
            onCancelDownload = updateViewModel::cancelDownload,
            onInstall = updateViewModel::installUpdate,
            onGrantInstallPermission = onGrantInstallPermission,
            modifier = Modifier.fillMaxSize()
        )
    }

    composable(MakRoutes.SettingsSemesters) {
        val settingsState by settingsViewModel.settings.collectAsStateWithLifecycle()
        SettingsSemestersScreen(
            state = settingsState,
            onSemesterSelected = settingsViewModel::selectSemester,
            onAddSemester = onAddSemester,
            onConfigureSemester = onConfigureSemester,
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
        val syncState by syncViewModel.sync.collectAsStateWithLifecycle()
        SettingsDataScreen(
            state = settingsState,
            syncConnected = syncState.accountEmail != null,
            onExport = onExport,
            onImport = onImport,
            onDismissImportError = settingsViewModel::dismissImportError,
            modifier = Modifier.fillMaxSize()
        )
    }

    composable(MakRoutes.SettingsSync) {
        SyncRoute(syncViewModel)
    }

    composable(MakRoutes.ImportPreview) {
        val settingsState by settingsViewModel.settings.collectAsStateWithLifecycle()
        val syncState by syncViewModel.sync.collectAsStateWithLifecycle()
        ImportPreviewScreen(
            state = settingsState,
            syncConnected = syncState.accountEmail != null,
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

@Composable
private fun SyncRoute(syncViewModel: SyncViewModel) {
    val state by syncViewModel.sync.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var exportId by rememberSaveable { mutableStateOf<String?>(null) }
    val authorizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result -> syncViewModel.onAuthorizationResult(result.resultCode == Activity.RESULT_OK, result.data) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val id = exportId
        exportId = null
        // Cancelling the file picker is not an error and shows nothing.
        if (uri == null || id == null) return@rememberLauncherForActivityResult
        val bytes = syncViewModel.archivedPlan(id)
        val written = bytes != null && runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null
        }.getOrDefault(false)
        syncViewModel.reportArchiveExport(written)
    }
    LaunchedEffect(syncViewModel) {
        syncViewModel.authorizationRequests.collect { pendingIntent ->
            authorizationLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
        }
    }
    SyncScreen(
        state = state,
        onConnect = syncViewModel::connect,
        onReconnect = syncViewModel::reconnect,
        onSyncNow = syncViewModel::syncNow,
        onOpenChoice = syncViewModel::openChoice,
        onChoose = syncViewModel::choose,
        onDismissChoice = syncViewModel::dismissChoice,
        onRequestDisconnect = syncViewModel::requestDisconnect,
        onDisconnect = syncViewModel::disconnect,
        onDismissDisconnect = syncViewModel::dismissDisconnect,
        onExportArchived = { id ->
            state.archive.firstOrNull { it.id == id }?.let { item ->
                exportId = item.id
                exportLauncher.launch(item.fileName)
            }
        },
        onDismissError = syncViewModel::dismissError,
        modifier = Modifier.fillMaxSize()
    )
}
