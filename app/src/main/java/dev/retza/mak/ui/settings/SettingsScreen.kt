package dev.retza.mak.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.retza.mak.ui.components.MakChoiceRow
import dev.retza.mak.ui.components.MakDialog
import dev.retza.mak.ui.components.MakEmptyState
import dev.retza.mak.ui.components.MakExpandableSection
import dev.retza.mak.ui.components.MakNoteBanner
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSectionHeader
import dev.retza.mak.ui.components.MakSelectField
import dev.retza.mak.ui.components.MakSpacing
import dev.retza.mak.ui.components.MakStateMessage
import dev.retza.mak.ui.components.SemesterUi
import dev.retza.mak.ui.components.ScreenStatus

data class ThemeOptionUi(
    val id: String,
    val label: String,
    val isSelected: Boolean = false
)

data class NotificationOptionUi(
    val id: String,
    val label: String,
    val isSelected: Boolean = false
)

data class NotificationSettingsUi(
    val enabled: Boolean = false,
    val eveningEnabled: Boolean = true,
    val beforeClassEnabled: Boolean = true,
    val eveningHourOptions: List<NotificationOptionUi> = emptyList(),
    val leadOptions: List<NotificationOptionUi> = emptyList()
)

data class ImportPreviewUi(
    val semesterCount: Int,
    val programCount: Int,
    val classCount: Int,
    val overrideCount: Int,
    val noteCount: Int,
    val changeCount: Int,
    val activeSemesterName: String?
)

data class SettingsUiState(
    val semesters: List<SemesterUi> = emptyList(),
    val activeSemesterId: String? = null,
    val themeOptions: List<ThemeOptionUi> = emptyList(),
    val notificationsLabel: String = "Obsługiwane przez aplikację, treść i moment wysyłki do ustalenia",
    val notificationsDetails: String = "Treść i moment wysyłki zostaną ustalone.",
    val semesterToDeleteId: String? = null,
    val isDeletingSemester: Boolean = false,
    val importPreview: ImportPreviewUi? = null,
    val importErrorMessage: String? = null,
    val isPreparingImport: Boolean = false,
    val isReplacingData: Boolean = false,
    val notifications: NotificationSettingsUi = NotificationSettingsUi(),
    val status: ScreenStatus = ScreenStatus.Ready
)

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onSemesterSelected: (String) -> Unit,
    onAddSemester: () -> Unit,
    onConfigureSemester: (String) -> Unit,
    onDeleteSemester: (String) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onThemeSelected: (String) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onDismissImportError: () -> Unit,
    onNotificationsEnabled: (Boolean) -> Unit,
    onEveningNotificationsEnabled: (Boolean) -> Unit,
    onBeforeClassNotificationsEnabled: (Boolean) -> Unit,
    onEveningHourSelected: (String) -> Unit,
    onBeforeClassLeadSelected: (String) -> Unit,
    notificationsBlocked: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = state.semesters.firstOrNull { it.id == state.activeSemesterId }
    var showData by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        MakSectionHeader(
            eyebrow = "Ustawienia",
            title = "Semestry i wygląd",
            subtitle = "Wybierz plan, który ma być aktywny na ekranie i w powiadomieniach."
        )
        when (state.status) {
            ScreenStatus.Ready -> Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
                if (state.semesters.isEmpty()) {
                    MakEmptyState("Dodaj semestr, aby rozpocząć pracę z planem.")
                } else {
                    MakSelectField(
                        label = "Aktywny semestr",
                        value = active?.name.orEmpty(),
                        options = state.semesters.map { it.name },
                        onSelected = { name ->
                            state.semesters.firstOrNull { it.name == name }?.id?.let(onSemesterSelected)
                        }
                    )
                    if (active != null) {
                        MakNoteBanner(
                            title = active.name,
                            subtitle = "${active.dateRangeLabel}, ${active.firstWeekLabel.lowercase()}"
                        )
                    }
                    MakSecondaryAction(
                        text = "Konfiguruj semestr",
                        onClick = { active?.id?.let(onConfigureSemester) },
                        enabled = active != null
                    )
                    MakPrimaryAction(text = "Dodaj semestr", onClick = onAddSemester)
                }
                MakSelectField(
                    label = "Motyw",
                    value = state.themeOptions.firstOrNull { it.isSelected }?.label.orEmpty(),
                    options = state.themeOptions.map { it.label },
                    onSelected = { label ->
                        state.themeOptions.firstOrNull { it.label == label }?.id?.let(onThemeSelected)
                    }
                )
                MakExpandableSection(
                    label = "dane",
                    expanded = showData,
                    onExpandedChange = { showData = it }
                ) {
                    MakSecondaryAction(
                        text = "Eksportuj plan do JSON",
                        onClick = onExport
                    )
                    MakSecondaryAction(
                        text = "Importuj plan z JSON",
                        onClick = onImport,
                        enabled = !state.isPreparingImport
                    )
                    MakSecondaryAction(
                        text = "Usuń wybrany semestr",
                        onClick = { active?.id?.let(onDeleteSemester) },
                        enabled = active != null,
                        destructive = true
                    )
                }
                MakExpandableSection(
                    label = "powiadomienia",
                    expanded = showNotifications,
                    onExpandedChange = { showNotifications = it }
                ) {
                    val notifications = state.notifications
                    MakNoteBanner(
                        title = "Powiadomienia o kolizjach",
                        subtitle = if (notifications.enabled) "Włączone" else "Wyłączone"
                    )
                    if (notifications.enabled && notificationsBlocked) {
                        Text("Zablokowane przez system", color = MaterialTheme.colorScheme.error)
                        MakSecondaryAction(
                            text = "Otwórz ustawienia aplikacji",
                            onClick = onOpenAppSettings
                        )
                    }
                    NotificationToggle(
                        title = "Powiadomienia",
                        enabled = notifications.enabled,
                        onEnabledChange = { enabled ->
                            onNotificationsEnabled(enabled)
                            if (enabled && notificationsBlocked) onRequestNotificationPermission()
                        }
                    )
                    if (notifications.enabled) {
                        NotificationToggle(
                            title = "Powiadomienie wieczorne",
                            enabled = notifications.eveningEnabled,
                            onEnabledChange = onEveningNotificationsEnabled
                        )
                        MakSelectField(
                            label = "Godzina wieczorna",
                            value = notifications.eveningHourOptions
                                .firstOrNull { it.isSelected }
                                ?.label
                                .orEmpty(),
                            options = notifications.eveningHourOptions,
                            onSelected = { onEveningHourSelected(it.id) },
                            optionLabel = { it.label }
                        )
                        NotificationToggle(
                            title = "Powiadomienie przed zajęciami",
                            enabled = notifications.beforeClassEnabled,
                            onEnabledChange = onBeforeClassNotificationsEnabled
                        )
                        MakSelectField(
                            label = "Wyprzedzenie przed zajęciami",
                            value = notifications.leadOptions
                                .firstOrNull { it.isSelected }
                                ?.label
                                .orEmpty(),
                            options = notifications.leadOptions,
                            onSelected = { onBeforeClassLeadSelected(it.id) },
                            optionLabel = { it.label }
                        )
                    }
                }
            }

            else -> MakStateMessage(status = state.status, onRetry = onRetry)
        }
    }
    if (state.semesterToDeleteId != null) {
        MakDialog(
            title = "Usuń semestr",
            description = "Usunięcie semestru usunie jego plan i dane. Tej operacji nie można cofnąć.",
            onDismiss = onCancelDelete
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                MakSecondaryAction(
                    text = "Anuluj",
                    onClick = onCancelDelete,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isDeletingSemester
                )
                MakSecondaryAction(
                    text = "Usuń",
                    onClick = onConfirmDelete,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isDeletingSemester,
                    destructive = true
                )
            }
        }
    }
    if (state.importErrorMessage != null) {
        MakDialog(
            title = "Nie udało się zaimportować",
            description = state.importErrorMessage,
            onDismiss = onDismissImportError
        ) {
            MakSecondaryAction(text = "Zamknij", onClick = onDismissImportError)
        }
    }
}

@Composable
private fun NotificationToggle(
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MakChoiceRow(
                    label = "Włączone",
                    selected = enabled,
                    onClick = { onEnabledChange(true) }
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                MakChoiceRow(
                    label = "Wyłączone",
                    selected = !enabled,
                    onClick = { onEnabledChange(false) }
                )
            }
        }
    }
}

@Composable
fun ImportPreviewScreen(
    state: SettingsUiState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preview = state.importPreview
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        MakSectionHeader(
            eyebrow = "Import",
            title = "Podgląd kopii",
            subtitle = "Sprawdź zawartość pliku przed zastąpieniem danych."
        )
        if (preview == null) {
            MakEmptyState("Nie wybrano poprawnej kopii do importu.")
        } else {
            MakNoteBanner(
                title = "Zastąpisz wszystkie lokalne dane",
                subtitle = "Import zastępuje cały plan. Tej operacji nie można cofnąć."
            )
            ImportStatRow("Semestry", preview.semesterCount)
            ImportStatRow("Kierunki", preview.programCount)
            ImportStatRow("Zajęcia", preview.classCount)
            ImportStatRow("Korekty tygodni", preview.overrideCount)
            ImportStatRow("Notatki", preview.noteCount)
            ImportStatRow("Zmiany wystąpień", preview.changeCount)
            Text(
                text = "Aktywny semestr: ${preview.activeSemesterName ?: "brak"}",
                style = MaterialTheme.typography.bodyMedium
            )
            state.importErrorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                MakSecondaryAction(
                    text = "Anuluj",
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isReplacingData
                )
                MakPrimaryAction(
                    text = "Zastąp dane",
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isReplacingData
                )
            }
        }
    }
}

@Composable
private fun ImportStatRow(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value.toString(), fontWeight = FontWeight.SemiBold)
    }
}
