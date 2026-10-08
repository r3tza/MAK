package dev.retza.mak.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.retza.mak.sync.PlanSide
import dev.retza.mak.ui.components.MakNoteBanner
import dev.retza.mak.ui.components.MakNoteRole
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakScreenIntro
import dev.retza.mak.ui.components.MakSpacing

/** Picks one version for each difference; saving waits until every difference has a pick. */
@Composable
fun SyncChangesScreen(
    state: SyncChangesUi,
    onPick: (index: Int, side: PlanSide) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        MakScreenContent(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = MakSpacing.md, bottom = MakSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
        ) {
            MakScreenIntro("Przy każdej różnicy wybierz wersję, którą chcesz zachować. Reszta planu zostaje bez zmian.")
            state.problem?.let { MakNoteBanner(title = null, subtitle = it, role = MakNoteRole.Error) }
            state.differences.forEachIndexed { index, difference ->
                DifferenceChoice(
                    difference = difference,
                    picked = state.picks[index],
                    flagged = index in state.flagged,
                    onPick = { side -> onPick(index, side) }
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        MakScreenContent(
            modifier = Modifier.padding(vertical = MakSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)
        ) {
            Text(
                "Wybrano ${state.chosenCount} z ${state.differences.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MakPrimaryAction(text = "Zapisz plan", onClick = onSave, enabled = state.canSave)
        }
    }
}

@Composable
private fun DifferenceChoice(
    difference: SyncDifferenceUi,
    picked: PlanSide?,
    flagged: Boolean,
    onPick: (PlanSide) -> Unit
) {
    val outline = if (flagged) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .border(1.dp, outline, RoundedCornerShape(13.dp))
            .padding(MakSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        DifferenceHeading(difference)
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
        ) {
            SideOption("Telefon", difference.phone, picked == PlanSide.PHONE, Modifier.weight(1f)) { onPick(PlanSide.PHONE) }
            SideOption("Dysk", difference.drive, picked == PlanSide.DRIVE, Modifier.weight(1f)) { onPick(PlanSide.DRIVE) }
        }
    }
}

@Composable
private fun SideOption(
    side: String,
    value: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(if (selected) colors.primaryContainer else colors.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outline, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = MakSpacing.sm, vertical = MakSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)
    ) {
        Text(side, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
