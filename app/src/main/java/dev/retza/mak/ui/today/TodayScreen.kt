package dev.retza.mak.ui.today

import androidx.compose.foundation.layout.Arrangement
import dev.retza.mak.ui.AllProgramsHiddenNote
import dev.retza.mak.ui.HiddenProgramsHint
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
import dev.retza.mak.ui.components.MakBannerAction
import dev.retza.mak.ui.components.MakNoteBanner
import dev.retza.mak.ui.components.MakNoteRole
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.ScreenStatus
import dev.retza.mak.ui.settings.SyncAttentionUi
import dev.retza.mak.ui.settings.SyncIssueFix

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
    val emptyMessage: String = "Nie masz dziś zajęć.",
    // Study programs of the active semester hidden on this phone; the hint explains the shorter plan.
    val hiddenProgramNames: List<String> = emptyList(),
    val allProgramsHidden: Boolean = false
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
    twoColumns: Boolean = false,
    onOpenSync: () -> Unit = {},
    syncAttention: SyncAttentionUi? = null,
    onOpenPrograms: () -> Unit = {}
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
            ).joinToString(", ").ifBlank { null },
            supportingContent = if (state.hiddenProgramNames.isNotEmpty() && !state.allProgramsHidden) {
                { HiddenProgramsHint(state.hiddenProgramNames) }
            } else {
                null
            }
        )
        if (availableUpdateVersion != null || syncAttention != null) {
            // Banners are one section, so the summary card keeps the 16 dp section gap below them.
            Column(
                modifier = Modifier.padding(bottom = MakSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
            ) {
                if (syncAttention != null) {
                    MakNoteBanner(
                        title = syncAttention.text,
                        subtitle = null,
                        role = MakNoteRole.Warning,
                        action = MakBannerAction(
                            syncAttention.action,
                            if (syncAttention.fix == SyncIssueFix.UPDATE) onViewUpdate else onOpenSync
                        )
                    )
                }
                if (availableUpdateVersion != null) {
                    MakNoteBanner(
                        title = "Dostępna aktualizacja",
                        subtitle = "Wersja $availableUpdateVersion jest gotowa do pobrania.",
                        role = MakNoteRole.Neutral,
                        action = MakBannerAction("Zobacz", onViewUpdate),
                        dismissAction = MakBannerAction("Nie teraz", onDismissUpdate)
                    )
                }
            }
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(MakSpacing.xl)) {
                TodaySummary(state, Modifier.width(TodaySummaryColumnWidth))
                Column(modifier = Modifier.weight(1f)) {
                    TodayClasses(state, requiresSetup, onOpenClass, onStartSetup, onOpenSync, onOpenPrograms, onRetry)
                }
            }
        } else {
            if (state.hasActiveSemester) {
                TodaySummary(state, Modifier.padding(bottom = MakSpacing.xl))
            }
            TodayClasses(state, requiresSetup, onOpenClass, onStartSetup, onOpenSync, onOpenPrograms, onRetry)
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
    onOpenSync: () -> Unit,
    onOpenPrograms: () -> Unit,
    onRetry: () -> Unit
) {
    MakRowTitle(title = "Zajęcia")
    when (state.status) {
        ScreenStatus.Ready -> if (state.allProgramsHidden) {
            AllProgramsHiddenNote(onOpenPrograms)
        } else if (state.items.isEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
                MakStateMessage(
                    status = state.status,
                    emptyMessage = state.emptyMessage
                )
                if (requiresSetup) {
                    MakPrimaryAction(text = "Skonfiguruj plan", onClick = onStartSetup)
                    MakSecondaryAction(text = "Pobierz plan z konta Google", onClick = onOpenSync)
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
