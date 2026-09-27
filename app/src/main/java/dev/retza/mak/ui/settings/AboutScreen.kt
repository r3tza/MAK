package dev.retza.mak.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.retza.mak.update.ReleaseHistoryEntry
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSpacing

@Composable
fun AboutScreen(
    installedVersionName: String,
    releaseHistory: List<ReleaseHistoryEntry>,
    modifier: Modifier = Modifier
) {
    MakScreenContent(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.lg)
    ) {
        AboutSection("Mój Akademicki Kalendarz") {
            Text(
                text = "Wersja $installedVersionName",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Plan zajęć z kilku kierunków, z tygodniami A/B, kolizjami i notatkami. " +
                    "Plan zostaje na telefonie i działa bez konta."
            )
            Text("Autor: r3tza", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (releaseHistory.isNotEmpty()) {
            AboutSection("Ostatnie zmiany") {
                releaseHistory.forEach { entry -> ReleaseHistory(entry) }
            }
        }
    }
}

@Composable
internal fun AboutSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(14.dp))
                .padding(MakSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(MakSpacing.md),
            content = content
        )
    }
}

@Composable
private fun ReleaseHistory(entry: ReleaseHistoryEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
        Text(
            text = entry.date?.let { "${entry.versionName} ($it)" } ?: entry.versionName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        entry.changes.forEach { change ->
            Text(change, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
