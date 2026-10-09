package dev.retza.mak.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.retza.mak.ui.components.MakBannerAction
import dev.retza.mak.ui.components.MakNoteBanner
import dev.retza.mak.ui.components.MakNoteRole
import dev.retza.mak.ui.components.MakSpacing

/** „Ukryty kierunek: Zarządzanie” or „Ukryte kierunki: …” under the semester name (`FEATURES.md`, I-79). */
@Composable
fun HiddenProgramsHint(names: List<String>, modifier: Modifier = Modifier) {
    val label = if (names.size == 1) "Ukryty kierunek" else "Ukryte kierunki"
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
        Icon(
            imageVector = Icons.Outlined.VisibilityOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp).size(16.dp)
        )
        Text(
            text = "$label: ${names.joinToString(", ")}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Shown on „Dzisiaj” and „Plan” instead of the classes when every study program is hidden. */
@Composable
fun AllProgramsHiddenNote(onOpenPrograms: () -> Unit, modifier: Modifier = Modifier) {
    MakNoteBanner(
        title = "Wszystkie kierunki są ukryte na tym telefonie.",
        subtitle = null,
        role = MakNoteRole.Neutral,
        modifier = modifier,
        action = MakBannerAction("Kierunki", onOpenPrograms)
    )
}
