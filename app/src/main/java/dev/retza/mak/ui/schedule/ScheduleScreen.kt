package dev.retza.mak.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.retza.mak.ui.components.CalendarDayUi
import dev.retza.mak.ui.components.CalendarLegendUi
import dev.retza.mak.ui.components.CalendarMarkerColor
import dev.retza.mak.ui.components.CalendarMarkerUi
import dev.retza.mak.ui.components.ClassCard
import dev.retza.mak.ui.components.ClassItemUi
import dev.retza.mak.ui.components.MakCheckbox
import dev.retza.mak.ui.components.MakChoiceRow
import dev.retza.mak.ui.components.MakDialog
import dev.retza.mak.ui.components.MakDot
import dev.retza.mak.ui.components.MakEmptyState
import dev.retza.mak.ui.components.MakExpandableSection
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakRoundButton
import dev.retza.mak.ui.components.MakRowTitle
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSelectField
import dev.retza.mak.ui.components.MakSpacing
import dev.retza.mak.ui.components.MakStateMessage
import dev.retza.mak.ui.components.MakViewSwitch
import dev.retza.mak.ui.components.ScreenStatus
import dev.retza.mak.ui.components.parseHexColor
import dev.retza.mak.ui.semester.WeekOverrideScopeUi
import dev.retza.mak.ui.semester.WeekTypeUi
import dev.retza.mak.ui.theme.MakOrangeMark
import dev.retza.mak.ui.theme.MakTeal

enum class ScheduleView {
    List,
    Calendar
}

data class ScheduleDayUi(
    val id: String,
    val shortLabel: String,
    val dateLabel: String,
    val accessibilityLabel: String,
    val isSelected: Boolean = false,
    val isEnabled: Boolean = true
)

data class ScheduleFilterUi(
    val id: String,
    val label: String,
    val isSelected: Boolean = false
)

data class ScheduleUiState(
    val view: ScheduleView = ScheduleView.List,
    val weekRangeLabel: String,
    val weekSubtitle: String,
    val weekTypeLabel: String,
    val weekSourceLabel: String,
    val weekType: WeekTypeUi? = null,
    val days: List<ScheduleDayUi> = emptyList(),
    val filters: List<ScheduleFilterUi> = emptyList(),
    val selectedDayLabel: String = "",
    val selectedDayCountLabel: String = "",
    val items: List<ClassItemUi> = emptyList(),
    val calendarMonthLabel: String = "",
    val calendarDays: List<CalendarDayUi> = emptyList(),
    val calendarLegend: List<CalendarLegendUi> = emptyList(),
    val calendarSelectedDayLabel: String = "",
    val calendarSelectedDayCountLabel: String = "",
    val calendarItems: List<ClassItemUi> = emptyList(),
    val showCancelled: Boolean = false,
    val hasOneWeekCorrection: Boolean = false,
    val hasFromWeekCorrection: Boolean = false,
    val status: ScreenStatus = ScreenStatus.Ready,
    val emptyMessage: String = "Brak zajęć w tym dniu dla wybranego kierunku."
)

@Composable
fun ScheduleScreen(
    state: ScheduleUiState,
    onViewChanged: (ScheduleView) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onDaySelected: (String) -> Unit,
    onFilterSelected: (String) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCalendarDaySelected: (String) -> Unit,
    onShowCancelledChanged: (Boolean) -> Unit,
    onAddOneOff: () -> Unit,
    onOpenClass: (String) -> Unit,
    onSaveWeekCorrection: (WeekTypeUi, WeekOverrideScopeUi) -> Unit,
    onClearWeekCorrection: (WeekOverrideScopeUi) -> Unit,
    onStartSetup: () -> Unit,
    onRetry: () -> Unit,
    requiresSetup: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showWeekDialog by remember { mutableStateOf(false) }
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        Text(
            text = "Plan zajęć",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = MakSpacing.xs, top = MakSpacing.xs, bottom = MakSpacing.sm)
        )
        MakViewSwitch(
            firstLabel = "Lista",
            secondLabel = "Kalendarz",
            firstSelected = state.view == ScheduleView.List,
            onFirst = { onViewChanged(ScheduleView.List) },
            onSecond = { onViewChanged(ScheduleView.Calendar) },
            modifier = Modifier.padding(bottom = 4.dp)
        )
        when {
            state.status != ScreenStatus.Ready ->
                MakStateMessage(status = state.status, onRetry = onRetry)

            requiresSetup -> Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
                MakEmptyState("Nie masz jeszcze aktywnego planu. Skonfiguruj semestr i kierunek.")
                MakPrimaryAction(text = "Skonfiguruj plan", onClick = onStartSetup)
            }

            state.view == ScheduleView.List -> ListView(
                state = state,
                onPreviousWeek = onPreviousWeek,
                onNextWeek = onNextWeek,
                onDaySelected = onDaySelected,
                onFilterSelected = onFilterSelected,
                onOpenClass = onOpenClass,
                onEditWeek = { showWeekDialog = true }
            )

            else -> CalendarView(
                state = state,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onCalendarDaySelected = onCalendarDaySelected,
                onShowCancelledChanged = onShowCancelledChanged,
                onAddOneOff = onAddOneOff,
                onOpenClass = onOpenClass
            )
        }
    }
    if (showWeekDialog) {
        WeekCorrectionDialog(
            currentType = state.weekType ?: WeekTypeUi.A,
            hasOneWeekCorrection = state.hasOneWeekCorrection,
            hasFromWeekCorrection = state.hasFromWeekCorrection,
            onDismiss = { showWeekDialog = false },
            onSave = { type, scope ->
                onSaveWeekCorrection(type, scope)
                showWeekDialog = false
            },
            onClear = { scope ->
                onClearWeekCorrection(scope)
                showWeekDialog = false
            }
        )
    }
}

@Composable
private fun ListView(
    state: ScheduleUiState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onDaySelected: (String) -> Unit,
    onFilterSelected: (String) -> Unit,
    onOpenClass: (String) -> Unit,
    onEditWeek: () -> Unit
) {
    Column {
        WeekNavigationHeader(
            state = state,
            onPreviousWeek = onPreviousWeek,
            onNextWeek = onNextWeek,
            onEditWeek = onEditWeek
        )
        DaySelector(days = state.days, onDaySelected = onDaySelected)
        if (state.filters.isNotEmpty()) {
            ScheduleFilterSection(filters = state.filters, onFilterSelected = onFilterSelected)
        }
        MakRowTitle(title = state.selectedDayLabel, meta = state.selectedDayCountLabel)
        if (state.items.isEmpty()) {
            MakEmptyState(state.emptyMessage)
        } else {
                Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
                state.items.forEach { item ->
                    ClassCard(item = item, onClick = { onOpenClass(item.id) })
                }
            }
        }
    }
}

@Composable
private fun WeekNavigationHeader(
    state: ScheduleUiState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onEditWeek: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MakSpacing.xs, bottom = MakSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MakRoundButton("Poprzedni tydzień", Icons.AutoMirrored.Outlined.ArrowBack, onPreviousWeek)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(state.weekRangeLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    state.weekSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            MakRoundButton("Następny tydzień", Icons.AutoMirrored.Outlined.ArrowForward, onNextWeek)
        }
        WeekTypeBadge(
            weekTypeLabel = state.weekTypeLabel,
            weekSourceLabel = state.weekSourceLabel,
            onClick = onEditWeek
        )
    }
}

@Composable
private fun WeekTypeBadge(
    weekTypeLabel: String,
    weekSourceLabel: String,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(shape)
            .border(
                width = 2.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape
            )
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .semantics {
                contentDescription =
                    "Zmień oznaczenie tygodnia, obecnie: $weekTypeLabel, źródło: $weekSourceLabel"
            }
            .padding(horizontal = MakSpacing.md, vertical = MakSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        Text(
            text = weekTypeLabel,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = weekSourceLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "Zmień",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ScheduleFilterSection(
    filters: List<ScheduleFilterUi>,
    onFilterSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedFilter = filters.firstOrNull { it.isSelected } ?: filters.first()
    val allSelected = selectedFilter.id == "all"
    val headerLabel = if (allSelected) "Filtry" else "Filtry: ${selectedFilter.label}"
    val stateLabel = if (expanded) "Rozwinięte" else "Zwinięte"
    val shape = RoundedCornerShape(10.dp)
    var focused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = MakSpacing.sm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(shape)
                .border(
                    width = 2.dp,
                    color = if (focused) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    shape = shape
                )
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(
                    role = Role.Button,
                    onClick = { expanded = !expanded }
                )
                .onFocusChanged { focused = it.isFocused }
                .focusable()
                .semantics {
                    contentDescription =
                        "$headerLabel, wybór: ${selectedFilter.label}, $stateLabel"
                }
                .padding(horizontal = MakSpacing.md, vertical = MakSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
        ) {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = headerLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (expanded) {
            MakSelectField(
                label = "Kierunek",
                value = selectedFilter.label,
                options = filters.map { it.label },
                onSelected = { label ->
                    filters.firstOrNull { it.label == label }?.let { filter ->
                        onFilterSelected(filter.id)
                        expanded = false
                    }
                },
                modifier = Modifier.padding(top = MakSpacing.sm)
            )
        }
    }
}

@Composable
private fun DaySelector(
    days: List<ScheduleDayUi>,
    onDaySelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 17.dp),
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.xs)
    ) {
        days.forEach { day ->
            val selected = day.isSelected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .selectable(
                        selected = selected,
                        enabled = day.isEnabled,
                        role = Role.Tab,
                        onClick = { onDaySelected(day.id) }
                    )
                    .heightIn(min = 48.dp)
                    .padding(vertical = MakSpacing.sm)
                    .semantics {
                        contentDescription = day.accessibilityLabel
                        this.selected = selected
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)
            ) {
                Text(
                    day.shortLabel,
                    fontSize = 12.sp,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    day.dateLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CalendarView(
    state: ScheduleUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCalendarDaySelected: (String) -> Unit,
    onShowCancelledChanged: (Boolean) -> Unit,
    onAddOneOff: () -> Unit,
    onOpenClass: (String) -> Unit
) {
    var showCalendarOptions by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MakRoundButton("Poprzedni miesiąc", Icons.AutoMirrored.Outlined.ArrowBack, onPreviousMonth)
            Text(state.calendarMonthLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            MakRoundButton("Następny miesiąc", Icons.AutoMirrored.Outlined.ArrowForward, onNextMonth)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        state.calendarDays.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { day -> CalendarDay(day, onCalendarDaySelected) }
                repeat(7 - week.size) {
                    Box(Modifier.weight(1f).height(45.dp))
                }
            }
        }
        CalendarLegend(state.calendarLegend)
        MakExpandableSection(
            label = "opcje kalendarza",
            expanded = showCalendarOptions,
            onExpandedChange = { showCalendarOptions = it },
            modifier = Modifier.padding(bottom = 14.dp)
        ) {
            MakCheckbox(
                label = "Pokaż odwołane",
                checked = state.showCancelled,
                onCheckedChange = onShowCancelledChanged
            )
            MakSecondaryAction(text = "Dodaj jednorazowe", onClick = onAddOneOff)
        }
        MakRowTitle(title = state.calendarSelectedDayLabel, meta = state.calendarSelectedDayCountLabel)
        if (state.calendarItems.isEmpty()) {
            MakEmptyState("Brak zajęć. Możesz dodać termin jednorazowy.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
                state.calendarItems.forEach { item ->
                    ClassCard(item = item, onClick = { onOpenClass(item.id) })
                }
            }
        }
    }
}

@Composable
private fun RowScope.CalendarDay(day: CalendarDayUi, onSelected: (String) -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .weight(1f)
            .height(45.dp)
            .clip(shape)
            .then(
                if (day.isToday && !day.isSelected) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape)
                } else Modifier
            )
            .background(if (day.isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .selectable(selected = day.isSelected, role = Role.Button, onClick = { onSelected(day.id) })
            .semantics {
                contentDescription = day.accessibilityLabel
                selected = day.isSelected
            }
            .padding(top = 5.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            day.dayLabel,
            fontSize = 13.sp,
            color = when {
                day.isSelected -> MaterialTheme.colorScheme.onPrimary
                day.isInCurrentMonth -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(6.dp)) {
            day.markers.take(3).forEach { marker ->
                MakDot(
                    color = if (day.isSelected) MaterialTheme.colorScheme.onPrimary else markerColor(marker),
                    size = 6.dp
                )
            }
        }
    }
}

@Composable
private fun markerColor(marker: CalendarMarkerUi): Color = when (marker.colorToken) {
    CalendarMarkerColor.Error -> MaterialTheme.colorScheme.error
    CalendarMarkerColor.Warning -> MaterialTheme.colorScheme.tertiary
    CalendarMarkerColor.Secondary -> MakOrangeMark
    CalendarMarkerColor.Primary -> marker.colorHex?.let(::parseHexColor) ?: MakTeal
}

@Composable
private fun CalendarLegend(items: List<CalendarLegendUi>) {
    if (items.isEmpty()) return
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MakSpacing.md, bottom = MakSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)
    ) {
        items.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MakDot(
                    color = if (item.isChange) {
                        MaterialTheme.colorScheme.error
                    } else {
                        item.colorHex?.let(::parseHexColor) ?: MakTeal
                    },
                    size = 8.dp
                )
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeekCorrectionDialog(
    currentType: WeekTypeUi,
    hasOneWeekCorrection: Boolean,
    hasFromWeekCorrection: Boolean,
    onDismiss: () -> Unit,
    onSave: (WeekTypeUi, WeekOverrideScopeUi) -> Unit,
    onClear: (WeekOverrideScopeUi) -> Unit
) {
    var type by remember { mutableStateOf(currentType) }
    var scope by remember { mutableStateOf(WeekOverrideScopeUi.ONE_WEEK) }
    MakDialog(
        title = "Zmień tydzień A/B",
        description = "Wybierz oznaczenie i zakres zmiany. W każdym momencie możesz wrócić do automatycznego planu.",
        onDismiss = onDismiss
    ) {
        MakSelectField(
            label = "Oznaczenie tygodnia",
            value = "Tydzień ${type.name}",
            options = listOf("Tydzień A", "Tydzień B"),
            onSelected = { type = if (it.endsWith("B")) WeekTypeUi.B else WeekTypeUi.A }
        )
        Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)) {
            WeekOverrideScopeUi.entries.forEach { item ->
                val label = if (item == WeekOverrideScopeUi.ONE_WEEK) {
                    "Tylko ten tydzień"
                } else {
                    "Od tego tygodnia w przyszłość"
                }
                MakChoiceRow(
                    label = label,
                    selected = scope == item,
                    onClick = { scope = item }
                )
            }
        }
        MakSecondaryAction(
            text = "Usuń korektę",
            onClick = { onClear(scope) },
            enabled = if (scope == WeekOverrideScopeUi.ONE_WEEK) hasOneWeekCorrection else hasFromWeekCorrection
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm), modifier = Modifier.fillMaxWidth()) {
            MakSecondaryAction(text = "Anuluj", onClick = onDismiss, modifier = Modifier.weight(1f))
            MakPrimaryAction(text = "Zapisz", onClick = { onSave(type, scope) }, modifier = Modifier.weight(1f))
        }
    }
}
