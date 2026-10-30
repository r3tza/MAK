package dev.retza.mak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.retza.mak.ui.theme.MakModified
import dev.retza.mak.ui.theme.MakOnce
import dev.retza.mak.ui.theme.MakOrangeMark
import dev.retza.mak.ui.theme.MakTeal

@Composable
fun ClassCard(
    item: ClassItemUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val accent = classAccentColor(item)
    val surface = MaterialTheme.colorScheme.surface
    val barColor = remember(accent, surface) {
        courseShapeColor(accent, surface)
    }
    val shape = RoundedCornerShape(16.dp)
    var focused by remember { mutableStateOf(false) }
    val clickable = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .shadow(4.dp, shape, spotColor = Color(0x091C2A46))
            .clip(shape)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .background(MaterialTheme.colorScheme.surface)
            .onFocusChanged { focused = it.isFocused }
            .then(clickable)
            .semantics { contentDescription = classCardDescription(item) }
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(
                    if (item.isCancelled) MaterialTheme.colorScheme.error
                    else barColor
                )
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = MakSpacing.sm, end = MakSpacing.lg, top = MakSpacing.md, bottom = MakSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(MakSpacing.md)
        ) {
            // At least 48 dp, wider with a large font scale, so the start time is never cut.
            Column(modifier = Modifier.widthIn(min = 48.dp)) {
                Text(
                    text = item.startTime,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = item.endTime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                occurrenceStatusIcon(item.status)?.let { (icon, tint) ->
                    // The card's own description already reads the status.
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.padding(top = MakSpacing.sm).size(20.dp)
                    )
                }
            }
            VerticalDivider(
                modifier = Modifier.fillMaxHeight().testTag("class-card-divider"),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (item.isCancelled) TextDecoration.LineThrough else null,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MakSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MakSpacing.xs),
                    itemVerticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CourseNameText(name = item.courseName, accent = accent)
                    if (item.type.isNotBlank()) {
                        Text(
                            text = item.type,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = classMeta(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val classNote = item.classNote?.takeIf { it.isNotBlank() }
                val occurrenceNote = item.occurrenceNote?.takeIf { it.isNotBlank() }
                val hasNotes = classNote != null || occurrenceNote != null
                if (item.conflictLabel != null || hasNotes) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                if (item.conflictLabel != null) {
                    MakConflictNote(item.conflictLabel, item.conflictWith)
                }
                if (item.conflictLabel != null && hasNotes) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                if (classNote != null) {
                    ClassNoteLine(
                        icon = Icons.Outlined.StickyNote2,
                        tint = MaterialTheme.colorScheme.primary,
                        text = classNote
                    )
                }
                if (occurrenceNote != null) {
                    ClassNoteLine(
                        icon = Icons.Outlined.Today,
                        tint = modifiedTint(),
                        text = occurrenceNote
                    )
                }
            }
        }
    }
}

@Composable
internal fun MakConflictNote(label: String, partners: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = MakSpacing.sm, vertical = MakSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MakSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
        partners?.let {
            Text(
                text = "Z: $it",
                modifier = Modifier.padding(start = 20.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontSize = 12.sp
            )
        }
    }
}

/** Course name in the course hue with text contrast on the current surface, as on the class card. */
@Composable
internal fun CourseNameText(
    name: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val surface = MaterialTheme.colorScheme.surface
    val textColor = remember(accent, surface) { Color(courseTextColor(accent.toArgb(), surface.toArgb())) }
    Text(
        text = name,
        modifier = modifier,
        color = textColor,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

// The screen reader gets the note kind from the card description, the eye from the icon.
@Composable
private fun ClassNoteLine(
    icon: ImageVector,
    tint: Color,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.padding(top = 1.dp).size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Color of the "changed" state and of occurrence notes, readable on both themes. */
@Composable
internal fun modifiedTint(): Color =
    if (isDarkSurface()) lerp(MakModified, Color.White, 0.55f) else MakModified

/** Color of the "one-off" state, readable on both themes. */
@Composable
internal fun oneOffTint(): Color =
    if (isDarkSurface()) lerp(MakOnce, Color.White, 0.55f) else MakOnce

private fun classMeta(item: ClassItemUi): String = listOfNotNull(
    item.room ?: "Sala niepodana",
    item.building,
    item.teacherName
).joinToString(", ")

/** What TalkBack reads for a class card; empty parts are skipped. */
internal fun classCardDescription(item: ClassItemUi): String = buildList {
    add("${item.startTime}-${item.endTime}, ${item.name}")
    item.statusBadge?.takeIf(String::isNotBlank)?.let(::add)
    item.courseName.takeIf(String::isNotBlank)?.let(::add)
    item.type.takeIf(String::isNotBlank)?.let(::add)
    // "Jednorazowe" is already read as the status badge.
    item.weekLabel?.takeIf { it.isNotBlank() && it != item.statusBadge }?.let(::add)
    add(classMeta(item))
    item.conflictLabel?.let { label ->
        add(label)
        item.conflictWith?.let { add("z: $it") }
    }
    item.classNote?.takeIf(String::isNotBlank)?.let { add("Notatka do zajęć: $it") }
    item.occurrenceNote?.takeIf(String::isNotBlank)?.let { add("Notatka do terminu: $it") }
}.joinToString(", ")

@Composable
private fun classAccentColor(item: ClassItemUi): Color {
    val parsed = item.courseColor?.let(::parseHexColor)
    if (parsed != null) return parsed
    return if (item.courseName.hashCode() % 2 == 0) MakTeal else MakOrangeMark
}
