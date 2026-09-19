package dev.retza.mak.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.retza.mak.ui.components.CalendarDayUi
import dev.retza.mak.ui.components.CalendarMarkerColor
import dev.retza.mak.ui.components.ClassCard
import dev.retza.mak.ui.components.ClassItemUi
import dev.retza.mak.ui.components.MakActionMenu
import dev.retza.mak.ui.components.MakCheckbox
import dev.retza.mak.ui.components.MakChoiceRow
import dev.retza.mak.ui.components.MakDialog
import dev.retza.mak.ui.components.MakDot
import dev.retza.mak.ui.components.MakEmptyState
import dev.retza.mak.ui.components.MakExpandableSection
import dev.retza.mak.ui.components.MakNoteBanner
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakRoundButton
import dev.retza.mak.ui.components.MakRowTitle
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSectionHeader
import dev.retza.mak.ui.components.MakSelectField
import dev.retza.mak.ui.components.MakSpacing
import dev.retza.mak.ui.components.MakStateMessage
import dev.retza.mak.ui.components.MakTextAction
import dev.retza.mak.ui.components.MakViewSwitch
import dev.retza.mak.ui.components.ScreenStatus
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
    val days: List<ScheduleDayUi> = emptyList(),
    val filters: List<ScheduleFilterUi> = emptyList(),
    val selectedDayLabel: String = "",
    val selectedDayCountLabel: String = "",
    val items: List<ClassItemUi> = emptyList(),
    val calendarMonthLabel: String = "",
    val calendarDays: List<CalendarDayUi> = emptyList(),
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
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showWeekDialog by remember { mutableStateOf(false) }
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        MakSectionHeader(eyebrow = "Plan", title = "Twoje zajęcia")
        MakViewSwitch(
            firstLabel = "Lista",
            secondLabel = "Kalendarz",
            firstSelected = state.view == ScheduleView.List,
            onFirst = { onViewChanged(ScheduleView.List) },
            onSecond = { onViewChanged(ScheduleView.Calendar) },
            modifier = Modifier.padding(bottom = 4.dp)
        )
        when (state.status) {
            ScreenStatus.Ready -> when (state.view) {
                ScheduleView.List -> ListView(
                    state = state,
                    onPreviousWeek = onPreviousWeek,
                    onNextWeek = onNextWeek,
                    onDaySelected = onDaySelected,
                    onFilterSelected = onFilterSelected,
                    onOpenClass = onOpenClass,
                    onEditWeek = { showWeekDialog = true }
                )

                ScheduleView.Calendar -> CalendarView(
                    state = state,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onCalendarDaySelected = onCalendarDaySelected,
                    onShowCancelledChanged = onShowCancelledChanged,
                    onAddOneOff = onAddOneOff,
                    onOpenClass = onOpenClass
                )
            }

            else -> MakStateMessage(status = state.status, onRetry = onRetry)
        }
    }
    if (showWeekDialog) {
        WeekCorrectionDialog(
            currentType = if (state.weekTypeLabel.contains("B")) WeekTypeUi.B else WeekTypeUi.A,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MakRoundButton("Poprzedni tydzień", Icons.AutoMirrored.Outlined.ArrowBack, onPreviousWeek)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(state.weekRangeLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(state.weekSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            MakRoundButton("Następny tydzień", Icons.AutoMirrored.Outlined.ArrowForward, onNextWeek)
        }
        MakNoteBanner(
            title = state.weekTypeLabel,
            subtitle = state.weekSourceLabel,
            modifier = Modifier.padding(bottom = 13.dp)
        )
        DaySelector(days = state.days, onDaySelected = onDaySelected)
        if (state.filters.isNotEmpty()) {
            var showFilters by remember { mutableStateOf(false) }
            MakExpandableSection(
                label = "filtry",
                expanded = showFilters,
                onExpandedChange = { showFilters = it }
            ) {
                MakSelectField(
                    label = "Kierunek",
                    value = state.filters.firstOrNull { it.isSelected }?.label.orEmpty(),
                    options = state.filters.map { it.label },
                    onSelected = { label ->
                        state.filters.firstOrNull { it.label == label }?.id?.let(onFilterSelected)
                    }
                )
            }
        }
        MakActionMenu(
            actions = listOf("Zmień A/B" to onEditWeek),
            modifier = Modifier.fillMaxWidth()
        )
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
                    fontSize = 10.sp,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    day.dateLabel,
                    fontSize = 13.sp,
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
            Text(state.calendarMonthLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            MakRoundButton("Następny miesiąc", Icons.AutoMirrored.Outlined.ArrowForward, onNextMonth)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    fontSize = 9.sp,
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
            fontSize = 10.sp,
            color = when {
                day.isSelected -> MaterialTheme.colorScheme.onPrimary
                day.isInCurrentMonth -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(5.dp)) {
            day.markers.take(3).forEach { marker ->
                MakDot(
                    color = if (day.isSelected) MaterialTheme.colorScheme.onPrimary else markerColor(marker.colorToken)
                )
            }
        }
    }
}

@Composable
private fun markerColor(color: CalendarMarkerColor) = when (color) {
    CalendarMarkerColor.Primary -> MakTeal
    CalendarMarkerColor.Secondary -> MakOrangeMark
    CalendarMarkerColor.Warning -> MaterialTheme.colorScheme.tertiary
    CalendarMarkerColor.Error -> MaterialTheme.colorScheme.error
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
