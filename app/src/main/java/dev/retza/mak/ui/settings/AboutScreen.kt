package dev.retza.mak.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.retza.mak.update.ReleaseHistoryEntry
import dev.retza.mak.ui.components.MakDot
import dev.retza.mak.ui.components.MakNoteBanner
import dev.retza.mak.ui.components.MakNoteRole
import dev.retza.mak.ui.components.MakPoppyMark
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val appFeatures = listOf(
    "plan kilku kierunków i semestrów",
    "tygodnie A/B oraz własne kalendarze tygodni",
    "widok „Dzisiaj”, lista tygodnia, kalendarz i widget",
    "wykrywanie kolizji oraz liczenie okienek",
    "odwołanie, zmiana lub przeniesienie pojedynczego terminu",
    "osobne notatki do zajęć i konkretnego terminu",
    "opcjonalne powiadomienia",
    "eksport i import kopii zapasowej w JSON",
    "aktualizacje z GitHub Releases"
)

// Matches res/xml/data_extraction_rules.xml: plan and settings only, encrypted cloud backup.
internal const val BACKUP_NOTICE =
    "Jeśli w telefonie jest włączona kopia zapasowa Google i ustawiona blokada ekranu, Android " +
        "dołącza plan i ustawienia do zaszyfrowanej kopii i przywraca je po ponownej instalacji aplikacji."

@Composable
fun AboutScreen(
    installedVersionName: String,
    releaseHistory: List<ReleaseHistoryEntry>,
    modifier: Modifier = Modifier
) {
    MakScreenContent(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.xl)
    ) {
        Column(
            modifier = Modifier.padding(top = MakSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
        ) {
            AppHeader(installedVersionName)
            Text(
                "Aplikacja na Androida do zarządzania planem zajęć. Powstała na potrzeby prywatnego " +
                    "użytku i jest rozwijana pod rzeczywisty plan autora oraz małej grupy znajomych.",
                style = MaterialTheme.typography.bodyMedium
            )
            if (isPreRelease(installedVersionName)) {
                MakNoteBanner(
                    title = "Wersja przed pełnym wydaniem",
                    subtitle = "MAK jest nadal rozwijany. Mogą pojawiać się błędy, dlatego regularnie " +
                        "rób kopię zapasową planu w ustawieniach, w sekcji „Dane”.",
                    role = MakNoteRole.Warning
                )
            }
        }

        AboutGroup("Możliwości") {
            BulletList(appFeatures)
        }

        AboutGroup("Dane i prywatność", spacing = MakSpacing.md) {
            Text(
                "Plan i notatki są przechowywane na telefonie. Podstawowe funkcje działają " +
                    "bez konta i połączenia z siecią.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(BACKUP_NOTICE, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Sieć służy do sprawdzania i pobierania aktualizacji z GitHuba. Plan trafia do sieci " +
                    "tylko po włączeniu synchronizacji Google: aplikacja zapisuje wtedy jego kopię " +
                    "w ukrytym folderze aplikacji na Twoim Dysku Google.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (releaseHistory.isNotEmpty()) {
            val installedRelease = installedVersionName.removeSuffix("-debug")
            AboutGroup("Ostatnie zmiany") {
                AboutCard {
                    releaseHistory.forEachIndexed { index, entry ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ReleaseEntry(entry, isInstalled = entry.versionName == installedRelease)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppHeader(installedVersionName: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        MakPoppyMark(
            // The mark no longer stands on a card, so the petal gaps match the screen background.
            gapColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.size(44.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
            Text(
                text = "Mój Akademicki Kalendarz",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Autor: r3tza",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Wersja $installedVersionName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Section heading with its content underneath, without a container. */
@Composable
internal fun AboutGroup(
    title: String,
    spacing: Dp = MakSpacing.sm,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

/** Shared border for the release history, the only list that needs a common boundary. */
@Composable
private fun AboutCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, shape),
        content = content
    )
}

@Composable
private fun ReleaseEntry(entry: ReleaseHistoryEntry, isInstalled: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MakSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        // Without room for both, the tag moves under the version instead of breaking the date.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(MakSpacing.xs),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Wersja ${entry.versionName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                entry.date?.let { date ->
                    Text(
                        text = formatReleaseDate(date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isInstalled) StatusText(text = "Zainstalowana")
        }
        BulletList(entry.changes)
    }
}

@Composable
internal fun BulletList(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)) {
        items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
                MakDot(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                    size = 6.dp
                )
                Text(item, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private val releaseDateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("pl"))

internal fun formatReleaseDate(isoDate: String): String = try {
    LocalDate.parse(isoDate).format(releaseDateFormat)
} catch (_: DateTimeParseException) {
    isoDate
}

/** Versions before 1.0.0 are not a full release yet. */
internal fun isPreRelease(versionName: String): Boolean =
    versionName.substringBefore('.').toIntOrNull()?.let { it < 1 } ?: false

/** A positive state next to a title: check icon and word in the accent color, without a container. */
@Composable
internal fun StatusText(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
