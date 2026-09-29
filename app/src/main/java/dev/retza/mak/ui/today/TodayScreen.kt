package dev.retza.mak.ui.today

import androidx.compose.foundation.layout.Arrangement
import dev.retza.mak.ui.MakTwoColumnMaxWidth
import dev.retza.mak.ui.MakContentMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.retza.mak.ui.components.ClassCard
import dev.retza.mak.ui.components.ClassItemUi
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakRowTitle
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSectionHeader
import dev.retza.mak.ui.components.MakSpacing
import dev.retza.mak.ui.components.MakStateMessage
import dev.retza.mak.ui.components.MakSummaryCard
import dev.retza.mak.ui.components.MakNoteBanner
import dev.retza.mak.ui.components.MakNoteRole
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakTextAction
import dev.retza.mak.ui.components.ScreenStatus

data class TodayUiState(
    val dateLabel: String,
    val semesterLabel: String,
    val weekLabel: String,
    val hasActiveSemester: Boolean = false,
    val classCount: Int = 0,
    val collisionCount: Int = 0,
    val gapCount: Int = 0,
    val items: List<ClassItemUi> = emptyList(),
    val status: ScreenStatus = ScreenStatus.Ready,
    val emptyMessage: String = "Nie masz dziś zajęć."
)

@Composable
fun TodayScreen(
    state: TodayUiState,
    onOpenClass: (String) -> Unit,
    onStartSetup: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    requiresSetup: Boolean = false,
    availableUpdateVersion: String? = null,
    onViewUpdate: () -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    twoColumns: Boolean = false
) {
    // Wide windows put the summary beside the classes; without a semester there is no summary.
    val wide = twoColumns && state.hasActiveSemester
    MakScreenContent(
        modifier = modifier.verticalScroll(rememberScrollState()),
        maxWidth = if (wide) MakTwoColumnMaxWidth else MakContentMaxWidth
    ) {
        MakSectionHeader(
            title = state.dateLabel,
            subtitle = listOfNotNull(
                state.weekLabel.takeIf { it.isNotBlank() },
                state.semesterLabel.takeIf { it.isNotBlank() }
            ).joinToString(", ").ifBlank { null }
        )
        if (availableUpdateVersion != null) {
            Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)) {
                MakNoteBanner(
                    title = "Dostępna aktualizacja",
                    subtitle = "Wersja $availableUpdateVersion jest gotowa do pobrania.",
                    role = MakNoteRole.Neutral,
                    actions = {
                        MakTextAction("Nie teraz", onDismissUpdate)
                        MakTextAction("Zobacz", onViewUpdate)
                    }
                )
            }
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(MakSpacing.xl)) {
                TodaySummary(state, Modifier.width(TodaySummaryColumnWidth))
                Column(modifier = Modifier.weight(1f)) {
                    TodayClasses(state, requiresSetup, onOpenClass, onStartSetup, onRetry)
                }
            }
        } else {
            if (state.hasActiveSemester) {
                TodaySummary(state, Modifier.padding(bottom = MakSpacing.xl))
            }
            TodayClasses(state, requiresSetup, onOpenClass, onStartSetup, onRetry)
        }
    }
}

private val TodaySummaryColumnWidth = 360.dp

@Composable
private fun TodaySummary(state: TodayUiState, modifier: Modifier) {
    MakSummaryCard(
        title = if (state.classCount == 0) "Dziś bez zajęć" else "Twój plan na dziś",
        classCount = state.classCount,
        collisionCount = state.collisionCount,
        gapCount = state.gapCount,
        modifier = modifier
    )
}

@Composable
private fun ColumnScope.TodayClasses(
    state: TodayUiState,
    requiresSetup: Boolean,
    onOpenClass: (String) -> Unit,
    onStartSetup: () -> Unit,
    onRetry: () -> Unit
) {
    MakRowTitle(title = "Zajęcia")
    when (state.status) {
        ScreenStatus.Ready -> if (state.items.isEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
                MakStateMessage(
                    status = state.status,
                    emptyMessage = state.emptyMessage
                )
                if (requiresSetup) {
                    MakPrimaryAction(text = "Skonfiguruj plan", onClick = onStartSetup)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
                state.items.forEach { item ->
                    ClassCard(item = item, onClick = { onOpenClass(item.id) })
                }
            }
        }

        else -> MakStateMessage(status = state.status, onRetry = onRetry)
    }
}
