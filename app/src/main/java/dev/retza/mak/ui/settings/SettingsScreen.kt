package dev.retza.mak.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.retza.mak.ui.components.MakChoiceRow
import dev.retza.mak.ui.components.MakDialog
import dev.retza.mak.ui.components.MakEmptyState
import dev.retza.mak.ui.components.MakNoteBanner
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSelectField
import dev.retza.mak.ui.components.MakSpacing
import dev.retza.mak.ui.components.MakStateMessage
import dev.retza.mak.ui.components.MakTextAction
import dev.retza.mak.ui.components.SemesterUi
import dev.retza.mak.ui.components.ScreenStatus
import dev.retza.mak.ui.components.distinctLabels

@Composable
private fun ActiveSemesterField(
    semesters: List<SemesterUi>,
    activeSemesterId: String?,
    onSemesterSelected: (String) -> Unit
) {
    val labels = distinctLabels(semesters.map { it.name })
    val labelById = semesters.map { it.id }.zip(labels).toMap()
    MakSelectField(
        label = "Aktywny semestr",
        value = activeSemesterId?.let { labelById[it] }.orEmpty(),
        options = semesters,
        onSelected = { onSemesterSelected(it.id) },
        optionLabel = { labelById[it.id].orEmpty() }
    )
}

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

data class GapThresholdOptionUi(
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
    val gapThresholdOptions: List<GapThresholdOptionUi> = emptyList(),
    val notificationsDetails: String =
        "Android może opóźnić powiadomienie o kilkanaście minut, aby oszczędzać baterię.",
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
    onOpenSemesters: () -> Unit,
    onOpenPrograms: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenData: () -> Unit,
    onSemesterSelected: (String) -> Unit,
    onAddSemester: () -> Unit,
    onThemeSelected: (String) -> Unit,
    onGapThresholdSelected: (String) -> Unit,
    notificationsBlocked: Boolean,
    onRetry: () -> Unit,
    updates: UpdateSettingsUi = UpdateSettingsUi(),
    onCheckUpdates: () -> Unit = {},
    onOpenUpdate: () -> Unit = {},
    onAutomaticChecksChanged: (Boolean) -> Unit = {},
    onOpenAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val active = state.semesters.firstOrNull { it.id == state.activeSemesterId }
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        when (state.status) {
            ScreenStatus.Ready -> Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.lg)) {
                SettingsListSection("Plan") {
                    if (state.semesters.isEmpty()) {
                        SettingsFieldItem {
                            MakEmptyState("Dodaj semestr, aby rozpocząć pracę z planem.")
                            MakPrimaryAction(text = "Dodaj semestr", onClick = onAddSemester)
                        }
                    } else {
                        SettingsFieldItem {
                            ActiveSemesterField(
                                semesters = state.semesters,
                                activeSemesterId = state.activeSemesterId,
                                onSemesterSelected = onSemesterSelected
                            )
                        }
                        SettingsNavigationRow(
                            title = "Zarządzaj semestrami",
                            value = active
                                ?.let { "${it.dateRangeLabel}, ${it.firstWeekLabel.replaceFirstChar { char -> char.lowercase() }}" }
                                .orEmpty(),
                            onClick = onOpenSemesters
                        )
                        SettingsRowDivider()
                    }
                    SettingsNavigationRow(
                        title = "Kierunki",
                        value = "Nazwy i kolory kierunków",
                        onClick = onOpenPrograms
                    )
                    SettingsFieldItem {
                        MakSelectField(
                            label = "Próg okienka",
                            value = state.gapThresholdOptions.firstOrNull { it.isSelected }?.label.orEmpty(),
                            options = state.gapThresholdOptions,
                            onSelected = { onGapThresholdSelected(it.id) },
                            optionLabel = { it.label }
                        )
                    }
                }
                SettingsListSection("Wygląd") {
                    SettingsFieldItem {
                        MakSelectField(
                            label = "Motyw",
                            value = state.themeOptions.firstOrNull { it.isSelected }?.label.orEmpty(),
                            options = state.themeOptions.map { it.label },
                            onSelected = { label ->
                                state.themeOptions.firstOrNull { it.label == label }?.id?.let(onThemeSelected)
                            }
                        )
                    }
                }
                SettingsListSection("Powiadomienia") {
                    val notificationsStatus = notificationsStatus(state.notifications, notificationsBlocked)
                    SettingsNavigationRow(
                        title = "Powiadomienia o kolizjach",
                        value = notificationsStatus.first,
                        details = notificationsStatus.second,
                        onClick = onOpenNotifications
                    )
                }
                SettingsListSection("Dane") {
                    SettingsNavigationRow(
                        title = "Kopia zapasowa i import",
                        value = "Eksport i import pliku JSON",
                        onClick = onOpenData
                    )
                }
                SettingsListSection("Aktualizacje") {
                    SettingsActionRow(
                        title = "Sprawdź aktualizacje",
                        value = updates.checkSummary,
                        enabled = updates.canCheck,
                        onClick = onCheckUpdates
                    )
                    updates.pendingVersion?.let { version ->
                        SettingsRowDivider()
                        SettingsNavigationRow(
                            title = "Aktualizacja do $version",
                            value = "Pobieranie i instalacja",
                            onClick = onOpenUpdate
                        )
                    }
                    SettingsRowDivider()
                    SettingsSwitchRow(
                        title = "Sprawdzaj przy uruchomieniu",
                        details = if (updates.automaticChecks) {
                            "Najwyżej raz na 24 godziny. Aplikacja łączy się tylko z GitHubem i nie wysyła planu. GitHub widzi adres IP."
                        } else {
                            "Nowe wersje nie pojawią się same."
                        },
                        checked = updates.automaticChecks,
                        onCheckedChange = onAutomaticChecksChanged
                    )
                }
                SettingsListSection("O aplikacji") {
                    SettingsNavigationRow(
                        title = "Wersja",
                        value = updates.installedVersion,
                        onClick = onOpenAbout
                    )
                }
            }

            else -> MakStateMessage(status = state.status, onRetry = onRetry)
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(MakSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(MakSpacing.md),
            content = content
        )
    }
}

/** Section whose rows reach the card edges, so the pressed state covers the whole row. */
@Composable
private fun SettingsListSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SettingsCardShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, SettingsCardShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(vertical = MakSpacing.xs),
            content = content
        )
    }
}

@Composable
private fun SettingsFieldItem(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MakSpacing.lg, vertical = MakSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.md),
        content = content
    )
}

@Composable
private fun SettingsRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = MakSpacing.lg),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

private val SettingsCardShape = RoundedCornerShape(14.dp)
private val SettingsRowFocusShape = RoundedCornerShape(12.dp)

/** Keyboard focus ring for full-width rows; the chain must place it before the click modifier. */
@Composable
private fun Modifier.settingsRowFocus(): Modifier {
    var focused by remember { mutableStateOf(false) }
    val ring = if (focused) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, SettingsRowFocusShape)
    } else {
        Modifier
    }
    return this.then(ring).onFocusChanged { focused = it.isFocused }
}
private val SettingsRowPadding = PaddingValues(horizontal = MakSpacing.lg, vertical = MakSpacing.md)

@Composable
private fun SettingsRowText(title: String, lines: List<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        lines.filter { it.isNotBlank() }.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsNavigationRow(
    title: String,
    value: String,
    details: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .settingsRowFocus()
            .clickable(onClick = onClick)
            .padding(SettingsRowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        SettingsRowText(title, listOfNotNull(value, details), Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = "Otwórz $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .settingsRowFocus()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(SettingsRowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        SettingsRowText(title, listOf(value), Modifier.weight(1f).alpha(if (enabled) 1f else 0.6f))
        if (enabled) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    details: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .settingsRowFocus()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(SettingsRowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        SettingsRowText(title, listOf(details), Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SettingsInfoRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun notificationsStatus(
    notifications: NotificationSettingsUi,
    blocked: Boolean
): Pair<String, String?> {
    if (!notifications.enabled) return "Wyłączone" to null
    if (blocked) return "Włączone" to "Zablokowane przez system"
    val hour = notifications.eveningHourOptions.firstOrNull { it.isSelected }?.label.orEmpty()
    val lead = notifications.leadOptions.firstOrNull { it.isSelected }?.label.orEmpty()
    return "Włączone" to "$hour dzień wcześniej, $lead przed zajęciami"
}

@Composable
fun SettingsSemestersScreen(
    state: SettingsUiState,
    onSemesterSelected: (String) -> Unit,
    onAddSemester: () -> Unit,
    onConfigureSemester: (String) -> Unit,
    onDeleteSemester: (String) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    MakScreenContent(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = MakSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        if (state.semesters.isEmpty()) {
            MakEmptyState("Dodaj semestr, aby rozpocząć pracę z planem.")
        } else {
            ActiveSemesterField(
                semesters = state.semesters,
                activeSemesterId = state.activeSemesterId,
                onSemesterSelected = onSemesterSelected
            )
        }
        MakNoteBanner(
            title = "Semestry",
            subtitle = "Konfiguracja przypisań, kalendarzy i korekt należy do wybranego semestru."
        )
        state.semesters.forEach { semester ->
            SemesterRow(
                semester = semester,
                onConfigure = { onConfigureSemester(semester.id) },
                onDelete = { onDeleteSemester(semester.id) }
            )
        }
        MakPrimaryAction(text = "Dodaj semestr", onClick = onAddSemester)
    }
    if (state.semesterToDeleteId != null) {
        DeleteSemesterDialog(
            isDeleting = state.isDeletingSemester,
            onConfirm = onConfirmDelete,
            onCancel = onCancelDelete
        )
    }
}

@Composable
private fun SemesterRow(
    semester: SemesterUi,
    onConfigure: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(MakSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
        ) {
            Text(semester.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            if (semester.isActive) {
                StatusText(text = "Aktywny")
            }
        }
        Text(
            text = "${semester.dateRangeLabel}, ${semester.firstWeekLabel}, " +
                "${semester.courseCountLabel}, ${semester.classCountLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
            MakTextAction(text = "Konfiguruj", onClick = onConfigure)
            MakTextAction(text = "Usuń", onClick = onDelete, destructive = true)
        }
    }
}

@Composable
private fun DeleteSemesterDialog(
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    MakDialog(
        title = "Usuń semestr",
        description = "Usunięcie semestru usunie jego plan i dane. Tej operacji nie można cofnąć.",
        onDismiss = onCancel
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm), modifier = Modifier.fillMaxWidth()) {
            MakSecondaryAction(
                text = "Anuluj",
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !isDeleting
            )
            MakSecondaryAction(
                text = "Usuń",
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                enabled = !isDeleting,
                destructive = true
            )
        }
    }
}

@Composable
fun SettingsNotificationsScreen(
    state: SettingsUiState,
    notificationsBlocked: Boolean,
    onNotificationsEnabled: (Boolean) -> Unit,
    onEveningNotificationsEnabled: (Boolean) -> Unit,
    onBeforeClassNotificationsEnabled: (Boolean) -> Unit,
    onEveningHourSelected: (String) -> Unit,
    onBeforeClassLeadSelected: (String) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notifications = state.notifications
    MakScreenContent(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = MakSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.lg)
    ) {
        if (notifications.enabled && notificationsBlocked) {
            Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
                MakNoteBanner(
                    title = "Zablokowane przez system",
                    subtitle = "Aplikacja nie może wyświetlać powiadomień, dopóki nie włączysz ich w ustawieniach systemowych."
                )
                MakSecondaryAction(text = "Otwórz ustawienia aplikacji", onClick = onOpenAppSettings)
            }
        }
        SettingsSection("Kolizje w planie") {
            NotificationToggle(
                title = "Powiadomienia",
                enabled = notifications.enabled,
                onEnabledChange = { enabled ->
                    onNotificationsEnabled(enabled)
                    if (enabled && notificationsBlocked) onRequestNotificationPermission()
                }
            )
        }
        if (notifications.enabled) {
            SettingsSection("Dzień wcześniej") {
                NotificationToggle(
                    title = "Powiadomienie wieczorne",
                    enabled = notifications.eveningEnabled,
                    onEnabledChange = onEveningNotificationsEnabled
                )
                if (notifications.eveningEnabled) {
                    MakSelectField(
                        label = "Godzina wieczorna",
                        value = notifications.eveningHourOptions.firstOrNull { it.isSelected }?.label.orEmpty(),
                        options = notifications.eveningHourOptions,
                        onSelected = { onEveningHourSelected(it.id) },
                        optionLabel = { it.label }
                    )
                }
            }
            SettingsSection("Przed zajęciami") {
                NotificationToggle(
                    title = "Powiadomienie przed zajęciami",
                    enabled = notifications.beforeClassEnabled,
                    onEnabledChange = onBeforeClassNotificationsEnabled
                )
                if (notifications.beforeClassEnabled) {
                    MakSelectField(
                        label = "Wyprzedzenie przed zajęciami",
                        value = notifications.leadOptions.firstOrNull { it.isSelected }?.label.orEmpty(),
                        options = notifications.leadOptions,
                        onSelected = { onBeforeClassLeadSelected(it.id) },
                        optionLabel = { it.label }
                    )
                }
            }
        }
        MakNoteBanner(title = "Czas dostarczenia", subtitle = state.notificationsDetails)
    }
}

@Composable
private fun NotificationToggle(
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MakChoiceRow(label = "Włączone", selected = enabled, onClick = { onEnabledChange(true) })
            }
            Column(modifier = Modifier.weight(1f)) {
                MakChoiceRow(label = "Wyłączone", selected = !enabled, onClick = { onEnabledChange(false) })
            }
        }
    }
}

@Composable
fun SettingsDataScreen(
    state: SettingsUiState,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onDismissImportError: () -> Unit,
    modifier: Modifier = Modifier
) {
    MakScreenContent(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = MakSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        MakNoteBanner(
            title = "Kopia zapasowa i import",
            subtitle = "Import zastępuje wszystkie lokalne dane. Tej operacji nie można cofnąć."
        )
        MakSecondaryAction(text = "Eksportuj plan do JSON", onClick = onExport)
        MakSecondaryAction(
            text = "Importuj plan z JSON",
            onClick = onImport,
            enabled = !state.isPreparingImport
        )
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
fun ImportPreviewScreen(
    state: SettingsUiState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preview = state.importPreview
    MakScreenContent(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = MakSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        if (preview == null) {
            MakEmptyState("Nie wybrano poprawnej kopii do importu.")
        } else {
            MakNoteBanner(
                title = "Zastąpisz wszystkie lokalne dane",
                subtitle = "Import zastępuje cały plan. Tej operacji nie można cofnąć."
            )
            Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)) {
                ImportStatRow("Semestry", preview.semesterCount)
                ImportStatRow("Kierunki", preview.programCount)
                ImportStatRow("Zajęcia", preview.classCount)
                ImportStatRow("Korekty tygodni", preview.overrideCount)
                ImportStatRow("Notatki", preview.noteCount)
                ImportStatRow("Zmiany wystąpień", preview.changeCount)
            }
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
