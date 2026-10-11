package dev.retza.mak.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.retza.mak.ui.components.MakBannerAction
import dev.retza.mak.ui.components.MakHelperText
import dev.retza.mak.ui.components.MakNoteBanner
import dev.retza.mak.ui.components.MakNoteRole

/** „Ukryty kierunek: Zarządzanie” or „Ukryte kierunki: …” under the semester name (`FEATURES.md`, I-79). */
@Composable
fun HiddenProgramsHint(names: List<String>, modifier: Modifier = Modifier) {
    val label = if (names.size == 1) "Ukryty kierunek" else "Ukryte kierunki"
    MakHelperText(
        text = "$label: ${names.joinToString(", ")}",
        modifier = modifier,
        icon = Icons.Outlined.VisibilityOff,
        style = MaterialTheme.typography.bodyMedium
    )
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
