package dev.retza.mak.ui.settings

import dev.retza.mak.settings.ThemeMode
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
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.School
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.retza.mak.ui.components.MakConfirmDeletionDialog
import dev.retza.mak.ui.components.MakDialog
import dev.retza.mak.ui.components.MakDialogAction
import dev.retza.mak.ui.components.MakChoiceRow
import dev.retza.mak.ui.components.MakEmptyState
import dev.retza.mak.ui.components.MakHelperText
import dev.retza.mak.ui.components.MakNoteBanner
import dev.retza.mak.ui.components.MakNoteRole
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSelectField
import dev.retza.mak.ui.components.MakSpacing
import dev.retza.mak.ui.components.MakTextAction
import dev.retza.mak.ui.components.SemesterUi
import dev.retza.mak.ui.components.distinctLabels
import dev.retza.mak.ui.components.makRowFocus
import dev.retza.mak.export.ImportSummary

@Composable
private fun ActiveSemesterField(
    semesters: List<SemesterUi>,
    activeSemesterId: Long?,
    onSemesterSelected: (Long) -> Unit
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

/** One choice of a settings select field: theme, gap threshold, minimum break or notification time. */
data class SettingsOptionUi(
    val id: String,
    val label: String,
    val isSelected: Boolean = false
)

data class NotificationSettingsUi(
    val enabled: Boolean = false,
    val eveningEnabled: Boolean = true,
    val beforeClassEnabled: Boolean = true,
    val gapEveningEnabled: Boolean = false,
    val gapBeforeClassEnabled: Boolean = false,
    val eveningHourOptions: List<SettingsOptionUi> = emptyList(),
    val leadOptions: List<SettingsOptionUi> = emptyList(),
    val gapEveningHourOptions: List<SettingsOptionUi> = emptyList(),
    val gapLeadOptions: List<SettingsOptionUi> = emptyList()
) {
    // One entry per notification kind; the settings summary counts the enabled ones.
    val kindStates: List<Boolean>
        get() = listOf(eveningEnabled, beforeClassEnabled, gapEveningEnabled, gapBeforeClassEnabled)
}

data class SettingsUiState(
    val semesters: List<SemesterUi> = emptyList(),
    val activeSemesterId: Long? = null,
    val theme: ThemeMode = ThemeMode.System,
    val gapThresholdOptions: List<SettingsOptionUi> = emptyList(),
    val minimumBreakOptions: List<SettingsOptionUi> = emptyList(),
    val semesterToDeleteId: Long? = null,
    val isDeletingSemester: Boolean = false,
    val importPreview: ImportSummary? = null,
    val importErrorMessage: String? = null,
    val isPreparingImport: Boolean = false,
    val isReplacingData: Boolean = false,
    val notifications: NotificationSettingsUi = NotificationSettingsUi(),
)

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onOpenSemesters: () -> Unit,
    onOpenPrograms: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenData: () -> Unit,
    onSemesterSelected: (Long) -> Unit,
    onAddSemester: () -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onGapThresholdSelected: (String) -> Unit,
    onMinimumBreakSelected: (String) -> Unit,
    notificationsBlocked: Boolean,
    updates: UpdateSettingsUi = UpdateSettingsUi(),
    onCheckUpdates: () -> Unit = {},
    onOpenUpdate: () -> Unit = {},
    onAutomaticChecksChanged: (Boolean) -> Unit = {},
    onOpenAbout: () -> Unit = {},
    syncSummary: String = "",
    onOpenSync: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val active = state.semesters.firstOrNull { it.id == state.activeSemesterId }
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.lg)) {
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
                        value = active?.dateLabel.orEmpty(),
                        onClick = onOpenSemesters
                    )
                    SettingsRowDivider()
                }
                SettingsNavigationRow(
                    title = "Kierunki",
                    value = "",
                    onClick = onOpenPrograms
                )
                SettingsRowDivider()
                SettingsFieldItem {
                    MakSelectField(
                        label = "Próg okienka",
                        value = state.gapThresholdOptions.firstOrNull { it.isSelected }?.label.orEmpty(),
                        options = state.gapThresholdOptions,
                        onSelected = { onGapThresholdSelected(it.id) },
                        optionLabel = { it.label }
                    )
                }
                SettingsFieldItem {
                    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
                        MakSelectField(
                            label = "Minimalna przerwa",
                            value = state.minimumBreakOptions.firstOrNull { it.isSelected }?.label.orEmpty(),
                            options = state.minimumBreakOptions,
                            onSelected = { onMinimumBreakSelected(it.id) },
                            optionLabel = { it.label }
                        )
                        MakHelperText("Zajęcia z krótszą lub równą przerwą są kolizją.")
                    }
                }
            }
            SettingsListSection("Wygląd") {
                SettingsFieldItem {
                    MakSelectField(
                        label = "Motyw",
                        value = state.theme.label(),
                        options = ThemeMode.entries,
                        onSelected = onThemeSelected,
                        optionLabel = { it.label() }
                    )
                }
            }
            SettingsListSection("Powiadomienia") {
                val notificationsStatus = notificationsStatus(state.notifications, notificationsBlocked)
                SettingsNavigationRow(
                    title = "Powiadomienia",
                    value = notificationsStatus.first,
                    details = notificationsStatus.second,
                    onClick = onOpenNotifications
                )
            }
            SettingsListSection("Dane") {
                SettingsNavigationRow(
                    title = "Synchronizacja Google",
                    value = syncSummary,
                    onClick = onOpenSync
                )
                SettingsRowDivider()
                SettingsNavigationRow(
                    title = "Kopia zapasowa i import",
                    value = "",
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
                    // How the check reaches GitHub belongs to the privacy policy, not to this row.
                    details = if (updates.automaticChecks) "Sprawdzanie raz dziennie." else "Nowe wersje nie pojawią się same.",
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
    }
}

/** Section whose rows reach the card edges, so the pressed state covers the whole row. */
@Composable
internal fun SettingsListSection(
    title: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
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
internal fun SettingsFieldItem(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MakSpacing.lg, vertical = MakSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.md),
        content = content
    )
}

@Composable
internal fun SettingsRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = MakSpacing.lg),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

private val SettingsCardShape = RoundedCornerShape(14.dp)
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
internal fun SettingsNavigationRow(
    title: String,
    value: String,
    details: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .makRowFocus()
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
            .makRowFocus()
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
    details: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .makRowFocus()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(SettingsRowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        SettingsRowText(title, listOfNotNull(details), Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

private fun notificationsStatus(
    notifications: NotificationSettingsUi,
    blocked: Boolean
): Pair<String, String?> {
    val kinds = notifications.kindStates
    val enabledCount = kinds.count { it }
    val value = when (enabledCount) {
        0 -> return "Wyłączone" to null
        kinds.size -> "Wszystkie włączone"
        else -> "Włączone: $enabledCount z ${kinds.size}"
    }
    return value to if (blocked) "Zablokowane przez system" else null
}

@Composable
fun SettingsSemestersScreen(
    state: SettingsUiState,
    onSemesterSelected: (Long) -> Unit,
    onAddSemester: () -> Unit,
    onConfigureSemester: (Long) -> Unit,
    onDeleteSemester: (Long) -> Unit,
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
        if (state.semesters.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
                ActiveSemesterField(
                    semesters = state.semesters,
                    activeSemesterId = state.activeSemesterId,
                    onSemesterSelected = onSemesterSelected
                )
                MakHelperText("Konfiguracja przypisań, kalendarzy i korekt należy do wybranego semestru.")
            }
        }
        Text(
            text = "Lista semestrów",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() }
        )
        if (state.semesters.isNotEmpty()) {
            MakSecondaryAction(
                text = "Dodaj semestr",
                onClick = onAddSemester,
                icon = Icons.Outlined.Add
            )
            state.semesters.forEach { semester ->
                SemesterRow(
                    semester = semester,
                    onConfigure = { onConfigureSemester(semester.id) },
                    onDelete = { onDeleteSemester(semester.id) }
                )
            }
        } else {
            MakEmptyState("Dodaj semestr, aby rozpocząć pracę z planem.")
            MakSecondaryAction(text = "Dodaj semestr", onClick = onAddSemester, icon = Icons.Outlined.Add)
        }
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
        val details = listOf(
            Icons.Outlined.CalendarMonth to semester.dateLabel,
            Icons.Outlined.School to semester.programsLabel,
            Icons.AutoMirrored.Outlined.FormatListBulleted to semester.classCountLabel
        ).filter { (_, text) -> text.isNotBlank() }
        if (details.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                details.forEach { (icon, text) -> SemesterDetailRow(icon = icon, text = text) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
            MakTextAction(text = "Konfiguruj", onClick = onConfigure)
            MakTextAction(text = "Usuń", onClick = onDelete, destructive = true)
        }
    }
}

@Composable
private fun SemesterDetailRow(icon: ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp).padding(top = 1.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DeleteSemesterDialog(
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    MakConfirmDeletionDialog(
        title = "Usuń semestr",
        text = "Usunięcie semestru usunie jego plan i dane. Tej operacji nie można cofnąć.",
        isDeleting = isDeleting,
        onConfirm = onConfirm,
        onCancel = onCancel
    )
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
    onGapEveningNotificationsEnabled: (Boolean) -> Unit,
    onGapBeforeClassNotificationsEnabled: (Boolean) -> Unit,
    onGapEveningHourSelected: (String) -> Unit,
    onGapLeadSelected: (String) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notifications = state.notifications
    // "evening", "lead", "gapEvening" or "gapLead" while a choice dialog is open.
    var openDialog by rememberSaveable { mutableStateOf<String?>(null) }
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
                    subtitle = "Aplikacja nie może wyświetlać powiadomień, dopóki nie włączysz ich w ustawieniach systemowych.",
                    role = MakNoteRole.Warning
                )
                MakSecondaryAction(text = "Otwórz ustawienia aplikacji", onClick = onOpenAppSettings)
            }
        }
        SettingsListSection(title = null) {
            SettingsSwitchRow(
                title = "Wszystkie powiadomienia",
                checked = notifications.enabled,
                onCheckedChange = { enabled ->
                    onNotificationsEnabled(enabled)
                    if (enabled && notificationsBlocked) onRequestNotificationPermission()
                }
            )
        }
        NotificationTopicSection(
            title = "Kolizje",
            eveningValue = "O " + notifications.eveningHourOptions.firstOrNull { it.isSelected }?.label.orEmpty(),
            eveningChecked = notifications.eveningEnabled,
            onEveningCheckedChange = { enabled ->
                onEveningNotificationsEnabled(enabled)
                if (enabled && notificationsBlocked) onRequestNotificationPermission()
            },
            onOpenEvening = { openDialog = "evening" },
            leadValue = notifications.leadOptions.firstOrNull { it.isSelected }?.label.orEmpty() + " wcześniej",
            leadChecked = notifications.beforeClassEnabled,
            onLeadCheckedChange = { enabled ->
                onBeforeClassNotificationsEnabled(enabled)
                if (enabled && notificationsBlocked) onRequestNotificationPermission()
            },
            onOpenLead = { openDialog = "lead" }
        )
        NotificationTopicSection(
            title = "Okienka",
            eveningValue = "O " + notifications.gapEveningHourOptions.firstOrNull { it.isSelected }?.label.orEmpty(),
            eveningChecked = notifications.gapEveningEnabled,
            onEveningCheckedChange = { enabled ->
                onGapEveningNotificationsEnabled(enabled)
                if (enabled && notificationsBlocked) onRequestNotificationPermission()
            },
            onOpenEvening = { openDialog = "gapEvening" },
            leadValue = notifications.gapLeadOptions.firstOrNull { it.isSelected }?.label.orEmpty() + " wcześniej",
            leadChecked = notifications.gapBeforeClassEnabled,
            onLeadCheckedChange = { enabled ->
                onGapBeforeClassNotificationsEnabled(enabled)
                if (enabled && notificationsBlocked) onRequestNotificationPermission()
            },
            onOpenLead = { openDialog = "gapLead" }
        )
        MakHelperText(NOTIFICATION_DELAY_NOTE)
        when (openDialog) {
            "evening" -> NotificationChoiceDialog(
                title = "Kolizje: dzień wcześniej",
                options = notifications.eveningHourOptions,
                optionLabel = { "O ${it.label}" },
                onSave = onEveningHourSelected,
                onDismiss = { openDialog = null }
            )
            "lead" -> NotificationChoiceDialog(
                title = "Kolizje: przed zajęciami",
                options = notifications.leadOptions,
                optionLabel = { "${it.label} wcześniej" },
                onSave = onBeforeClassLeadSelected,
                onDismiss = { openDialog = null }
            )
            "gapEvening" -> NotificationChoiceDialog(
                title = "Okienka: dzień wcześniej",
                options = notifications.gapEveningHourOptions,
                optionLabel = { "O ${it.label}" },
                onSave = onGapEveningHourSelected,
                onDismiss = { openDialog = null }
            )
            "gapLead" -> NotificationChoiceDialog(
                title = "Okienka: przed zajęciami",
                options = notifications.gapLeadOptions,
                optionLabel = { "${it.label} wcześniej" },
                onSave = onGapLeadSelected,
                onDismiss = { openDialog = null }
            )
        }
    }
}

/** One topic of notification kinds (the evening and the before-class kind) in its own list section. */
@Composable
private fun NotificationTopicSection(
    title: String,
    eveningValue: String,
    eveningChecked: Boolean,
    onEveningCheckedChange: (Boolean) -> Unit,
    onOpenEvening: () -> Unit,
    leadValue: String,
    leadChecked: Boolean,
    onLeadCheckedChange: (Boolean) -> Unit,
    onOpenLead: () -> Unit
) {
    SettingsListSection(title) {
        NotificationKindRow(
            sectionTitle = title,
            title = "Dzień wcześniej",
            value = eveningValue,
            checked = eveningChecked,
            onCheckedChange = onEveningCheckedChange,
            onOpenValue = onOpenEvening
        )
        SettingsRowDivider()
        NotificationKindRow(
            sectionTitle = title,
            title = "Przed zajęciami",
            value = leadValue,
            checked = leadChecked,
            onCheckedChange = onLeadCheckedChange,
            onOpenValue = onOpenLead
        )
    }
}

/** One notification kind: the title and its switch, with the value below that opens the choice dialog. */
@Composable
private fun NotificationKindRow(
    sectionTitle: String,
    title: String,
    value: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpenValue: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(SettingsRowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onOpenValue)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "$sectionTitle $title, $value, zmień"
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MakSpacing.xs)
            ) {
                Text(
                    text = value,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = "$sectionTitle: $title" }
        )
    }
}

/** Single choice from [options]; the picked id survives rotation and is saved only on „Zapisz”. */
@Composable
private fun NotificationChoiceDialog(
    title: String,
    options: List<SettingsOptionUi>,
    optionLabel: (SettingsOptionUi) -> String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var picked by rememberSaveable { mutableStateOf(options.firstOrNull { it.isSelected }?.id) }
    MakDialog(
        title = title,
        onDismiss = onDismiss,
        dismissAction = MakDialogAction("Anuluj", onDismiss),
        confirmAction = MakDialogAction("Zapisz", {
            picked?.let(onSave)
            onDismiss()
        })
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)) {
            options.forEach { option ->
                MakChoiceRow(
                    label = optionLabel(option),
                    selected = option.id == picked,
                    onClick = { picked = option.id }
                )
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
    modifier: Modifier = Modifier,
    syncConnected: Boolean = false
) {
    MakScreenContent(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = MakSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        MakSecondaryAction(text = "Eksportuj plan do JSON", onClick = onExport)
        MakSecondaryAction(
            text = "Importuj plan z JSON",
            onClick = onImport,
            enabled = !state.isPreparingImport
        )
        MakNoteBanner(
            title = null,
            subtitle = "Import zastępuje wszystkie lokalne dane. Tej operacji nie można cofnąć." +
                if (syncConnected) " $SYNC_NOTE" else "",
            role = MakNoteRole.Warning
        )
    }
    if (state.importErrorMessage != null) {
        MakDialog(
            title = "Nie udało się zaimportować",
            description = state.importErrorMessage,
            onDismiss = onDismissImportError,
            dismissAction = MakDialogAction("Zamknij", onDismissImportError)
        )
    }
}

private const val SYNC_NOTE = "Zaimportowany plan trafi też na Dysk Google i na pozostałe połączone telefony."

@Composable
fun ImportPreviewScreen(
    state: SettingsUiState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    syncConnected: Boolean = false
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
                subtitle = "Tej operacji nie można cofnąć." + if (syncConnected) " $SYNC_NOTE" else "",
                role = MakNoteRole.Warning
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

private const val NOTIFICATION_DELAY_NOTE =
    "Android może opóźnić powiadomienie o kilkanaście minut, aby oszczędzać baterię."
