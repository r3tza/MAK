package dev.retza.mak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.retza.mak.ui.theme.MakMarkBlue
import dev.retza.mak.ui.theme.MakMarkGold
import dev.retza.mak.ui.theme.MakMarkLilac
import dev.retza.mak.ui.theme.MakMarkTeal
import dev.retza.mak.ui.theme.MakModified
import dev.retza.mak.ui.theme.MakModifiedSoft
import dev.retza.mak.ui.theme.MakOnce
import dev.retza.mak.ui.theme.MakOnceSoft
import dev.retza.mak.ui.theme.MakOrangeMark
import dev.retza.mak.ui.theme.MakPillNeutral
import dev.retza.mak.ui.theme.MakPillNeutralBg
import dev.retza.mak.ui.theme.MakAccent
import dev.retza.mak.ui.theme.MakAccentSoft
import dev.retza.mak.ui.theme.MakInk
import dev.retza.mak.ui.theme.MakSummaryEnd
import dev.retza.mak.ui.theme.MakSummaryStart
import dev.retza.mak.ui.theme.MakTeal

@Composable
fun MakScreenContent(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = MakSpacing.lg, end = MakSpacing.lg, top = MakSpacing.xs, bottom = MakSpacing.xl),
        verticalArrangement = verticalArrangement,
        content = content
    )
}

@Composable
fun MakSectionHeader(
    eyebrow: String?,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (eyebrow != null) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = MakSpacing.xs, top = MakSpacing.xs, bottom = MakSpacing.sm)
            )
        } else {
            Spacer(modifier = Modifier.height(MakSpacing.sm))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = MakSpacing.xs)
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = MakSpacing.xs, end = MakSpacing.xs, top = MakSpacing.sm, bottom = MakSpacing.lg)
            )
        } else {
            Spacer(modifier = Modifier.height(MakSpacing.lg))
        }
    }
}

/**
 * Places two related fields side by side, or one under the other on narrow screens
 * where half of the width cannot fit a label or a date.
 */
@Composable
fun MakFieldPair(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < MakFieldPairMinRowWidth) {
            Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
                first()
                second()
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)) {
                Box(modifier = Modifier.weight(1f)) { first() }
                Box(modifier = Modifier.weight(1f)) { second() }
            }
        }
    }
}

private val MakFieldPairMinRowWidth = 340.dp

@Composable
fun MakScreenIntro(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = MakSpacing.xs, end = MakSpacing.xs, top = MakSpacing.md, bottom = MakSpacing.lg)
    )
}

@Composable
fun MakBrandMark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .size(31.dp)
            .rotate(-9f),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(Modifier.size(14.dp).clip(RoundedCornerShape(5.dp)).background(MakMarkBlue))
            Box(Modifier.size(14.dp).clip(RoundedCornerShape(5.dp)).background(MakMarkTeal))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(Modifier.size(14.dp).clip(RoundedCornerShape(5.dp)).background(MakMarkGold))
            Box(Modifier.size(14.dp).clip(RoundedCornerShape(5.dp)).background(MakMarkLilac))
        }
    }
}

@Composable
fun MakIconButton(
    label: String,
    symbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                2.dp,
                if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(12.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun MakIconButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                2.dp,
                if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(12.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun MakRoundButton(
    label: String,
    symbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                2.dp,
                if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(10.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .focusable(enabled = enabled)
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            fontSize = 19.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.35f)
        )
    }
}

@Composable
fun MakRoundButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                2.dp,
                if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(10.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .focusable(enabled = enabled)
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.35f)
        )
    }
}

@Composable
fun MakSummaryCard(
    title: String,
    classCount: Int,
    collisionCount: Int,
    gapCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(11.dp, RoundedCornerShape(22.dp), spotColor = Color(0x332D46A4))
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(MakSummaryStart, MakSummaryEnd)))
            .padding(horizontal = MakSpacing.lg, vertical = MakSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.semantics { heading() }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryColumn(
                label = "Zajęcia",
                value = classCount,
                valueColor = Color.White,
                modifier = Modifier.weight(1f)
            )
            SummaryDivider()
            SummaryColumn(
                label = "Kolizje",
                value = collisionCount,
                valueColor = if (collisionCount > 0) SummaryAlert else SummaryOk,
                modifier = Modifier.weight(1f)
            )
            SummaryDivider()
            SummaryColumn(
                label = "Okienka",
                value = gapCount,
                valueColor = Color.White,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryColumn(
    label: String,
    value: Int,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label: $value"
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)
    ) {
        Text(label, color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp)
        Text(value.toString(), color = valueColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(Color.White.copy(alpha = 0.5f))
    )
}

private val SummaryAlert = Color(0xFFFFB4AB)
private val SummaryOk = Color(0xFF7FE0A6)

@Composable
fun MakRowTitle(
    title: String,
    meta: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 2.dp, end = 2.dp, bottom = 11.dp, top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ClassCard(
    item: ClassItemUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val accent = classAccentColor(item)
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
            .alpha(if (item.isCancelled) 0.68f else 1f)
            .shadow(4.dp, shape, spotColor = Color(0x091C2A46))
            .clip(shape)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = shape
            )
            .background(MaterialTheme.colorScheme.surface)
            .then(clickable)
            .onFocusChanged { focused = it.isFocused }
            .focusable(enabled = onClick != null)
            .semantics { contentDescription = classCardDescription(item) }
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(if (item.isCancelled) MaterialTheme.colorScheme.error else accent)
        )
        Row(
            modifier = Modifier
                .weight(1f)
            .padding(start = MakSpacing.sm, end = MakSpacing.lg, top = MakSpacing.md, bottom = MakSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(MakSpacing.md)
        ) {
            Column(modifier = Modifier.width(48.dp)) {
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
            }
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
                    CoursePill(name = item.courseName, accent = accent)
                    val badge = item.statusBadge
                    if (badge != null) {
                        StatusBadge(
                            text = badge,
                            cancelled = item.isCancelled,
                            modified = item.isModified,
                            oneOff = item.isOneOff
                        )
                    }
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
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = MakSpacing.sm, vertical = MakSpacing.xs),
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
                            text = item.conflictLabel,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
                if (item.conflictLabel != null && hasNotes) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                if (classNote != null) {
                    ClassNoteRow(
                        label = "Notatka do zajęć",
                        text = classNote,
                        background = MakAccentSoft,
                        foreground = MakAccent
                    )
                }
                if (occurrenceNote != null) {
                    ClassNoteRow(
                        label = "Notatka na dziś",
                        text = occurrenceNote,
                        background = MakModifiedSoft,
                        foreground = MakModified
                    )
                }
            }
        }
    }
}

@Composable
internal fun CoursePill(
    name: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val dark = isDarkSurface()
    val fill = if (dark) lerp(accent, MaterialTheme.colorScheme.surface, 0.55f) else lerp(accent, Color.White, 0.72f)
    val content = when {
        dark -> lerp(accent, Color.White, 0.85f)
        fill.luminance() > 0.5f -> MakInk
        else -> Color.White
    }
    Text(
        text = name,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(fill)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = content,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ClassNoteRow(
    label: String,
    text: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier
) {
    val dark = isDarkSurface()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)
    ) {
        Text(
            text = label,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (dark) lerp(foreground, MaterialTheme.colorScheme.surface, 0.7f) else background)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            color = if (dark) lerp(foreground, Color.White, 0.7f) else foreground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusBadge(text: String, cancelled: Boolean, modified: Boolean, oneOff: Boolean) {
    val dark = isDarkSurface()
    val base = when {
        cancelled -> MaterialTheme.colorScheme.error
        modified -> MakModified
        oneOff -> MakOnce
        else -> MakPillNeutral
    }
    val bg = when {
        dark -> lerp(base, MaterialTheme.colorScheme.surface, 0.7f)
        cancelled -> MaterialTheme.colorScheme.errorContainer
        modified -> MakModifiedSoft
        oneOff -> MakOnceSoft
        else -> MakPillNeutralBg
    }
    val fg = if (dark) lerp(base, Color.White, 0.7f) else base
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun MakEmptyState(message: String, modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
                drawRoundRect(
                    color = outline,
                    style = stroke,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx())
                )
            }
            .padding(horizontal = MakSpacing.xl, vertical = MakSpacing.xxl),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MakStateMessage(
    status: ScreenStatus,
    modifier: Modifier = Modifier,
    emptyTitle: String? = null,
    emptyMessage: String? = null,
    onRetry: (() -> Unit)? = null
) {
    when (status) {
        ScreenStatus.Loading -> Column(
            modifier = modifier.fillMaxWidth().padding(MakSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
        ) {
            CircularProgressIndicator()
            Text("Ładowanie", style = MaterialTheme.typography.bodyMedium)
        }

        is ScreenStatus.Error -> Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(MakSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)
        ) {
            Text("Nie udało się wczytać danych", fontWeight = FontWeight.SemiBold)
            Text(status.message, color = MaterialTheme.colorScheme.onErrorContainer)
            if (onRetry != null) {
                MakSecondaryAction(text = "Spróbuj ponownie", onClick = onRetry)
            }
        }

        ScreenStatus.Ready -> if (emptyMessage != null) {
            MakEmptyState(emptyMessage, modifier)
        }
    }
}

@Composable
fun MakPrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = 13.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun MakSecondaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false
) {
    val content = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = content),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (destructive) MaterialTheme.colorScheme.error.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline
        ),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun MakTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(
            text = text,
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun MakNoteBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = MakSpacing.lg, vertical = MakSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)
        ) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        if (actionLabel != null && onAction != null) {
            MakTextAction(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun MakViewSwitch(
    firstLabel: String,
    secondLabel: String,
    firstSelected: Boolean,
    onFirst: () -> Unit,
    onSecond: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ViewSwitchButton(firstLabel, firstSelected, onFirst, Modifier.weight(1f))
        ViewSwitchButton(secondLabel, !firstSelected, onSecond, Modifier.weight(1f))
    }
}

@Composable
private fun ViewSwitchButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .shadow(if (selected) 2.dp else 0.dp, RoundedCornerShape(10.dp), spotColor = Color(0x141F325B))
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    !selected -> Color.Transparent
                    isDarkSurface() -> MaterialTheme.colorScheme.surfaceContainerHigh
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .then(
                if (selected) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                }
            )
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = when {
                !selected -> MaterialTheme.colorScheme.onSurfaceVariant
                isDarkSurface() -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.primary
            }
        )
    }
}

@Composable
private fun isDarkSurface(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

@Composable
fun MakNavBar(
    todaySelected: Boolean,
    planSelected: Boolean,
    addSelected: Boolean,
    onToday: () -> Unit,
    onPlan: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outline = MaterialTheme.colorScheme.outline
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.surface)
            .drawBehind {
                drawLine(
                    color = outline,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MakNavButton("Dzisiaj", Icons.Outlined.Today, todaySelected, onToday, Modifier.weight(1f))
        MakNavButton("Plan", Icons.Outlined.CalendarMonth, planSelected, onPlan, Modifier.weight(1f))
        MakNavButton("Dodaj", Icons.Outlined.Add, addSelected, onAdd, Modifier.weight(1f))
    }
}

@Composable
private fun RowScope.MakNavButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                2.dp,
                if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MakField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        enabled = enabled,
        isError = isError,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        keyboardOptions = keyboardOptions,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } }
    )
}

@Composable
fun MakReadOnlyField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        readOnly = true,
        isError = isError,
        label = { Text(label) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
fun MakExpandableSection(
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 1.dp,
                color = if (focused) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = shape
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickable(role = Role.Button, onClick = { onExpandedChange(!expanded) })
                .focusable()
                .onFocusChanged { focused = it.isFocused }
                .heightIn(min = 48.dp)
                .padding(horizontal = MakSpacing.md, vertical = MakSpacing.sm)
                .semantics {
                    contentDescription = label
                    stateDescription = if (expanded) "Rozwinięte" else "Zwinięte"
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (expanded) "Ukryj $label" else "Pokaż $label",
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = MakSpacing.lg, vertical = MakSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(MakSpacing.md),
                content = content
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MakSelectField(
    label: String,
    value: String,
    options: List<T>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    optionLabel: (T) -> String = { it.toString() },
    optionLeading: (@Composable (T) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            singleLine = true,
            isError = isError,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option), style = MaterialTheme.typography.bodyLarge) },
                    leadingIcon = optionLeading?.let { leading -> { leading(option) } },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun MakChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                2.dp,
                if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 48.dp)
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .border(
                    1.5.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Text(label, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun MakCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Checkbox, onClick = { onCheckedChange(!checked) })
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .semantics {
                stateDescription = if (checked) "Zaznaczone" else "Niezaznaczone"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        val shape = RoundedCornerShape(4.dp)
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(shape)
                .border(
                    1.dp,
                    if (focused || checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape
                )
                .background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun MakDialog(
    title: String,
    description: String? = null,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(MakSpacing.xl)
                .semantics { paneTitle = title },
            verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontSize = 20.sp)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
fun MakHelperText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 16.sp
    )
}

@Composable
fun FieldError(
    error: FieldErrorUi?,
    modifier: Modifier = Modifier
) {
    if (error != null) {
        Text(
            text = error.message,
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun MakTag(
    text: String,
    modifier: Modifier = Modifier
) {
    val dark = isDarkSurface()
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (dark) MaterialTheme.colorScheme.surfaceContainerHigh else MakPillNeutralBg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelMedium,
        color = if (dark) MaterialTheme.colorScheme.onSurface else MakPillNeutral
    )
}

@Composable
fun MakDot(color: Color, modifier: Modifier = Modifier, size: Dp = 4.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun MakFactRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.width(88.dp),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun classMeta(item: ClassItemUi): String = listOfNotNull(
    item.room ?: "Sala niepodana",
    item.building,
    item.teacherName
).joinToString(", ")

private fun classCardDescription(item: ClassItemUi): String = buildList {
    add("${item.startTime}-${item.endTime}, ${item.name}")
    add("${item.courseName}, ${item.type}")
    add(classMeta(item))
    item.conflictLabel?.let(::add)
    item.classNote?.takeIf(String::isNotBlank)?.let { add("Notatka do zajęć: $it") }
    item.occurrenceNote?.takeIf(String::isNotBlank)?.let { add("Notatka na dziś: $it") }
}.joinToString(", ")

@Composable
private fun classAccentColor(item: ClassItemUi): Color {
    val parsed = item.courseColor?.let(::parseHexColor)
    if (parsed != null) return parsed
    return if (item.courseName.hashCode() % 2 == 0) MakTeal else MakOrangeMark
}


fun parseHexColor(hex: String): Color? = runCatching {
    val value = hex.trim().removePrefix("#")
    val color = when (value.length) {
        6 -> value.toLong(16) or 0xFF000000L
        8 -> value.toLong(16)
        else -> return null
    }
    Color(color.toInt())
}.getOrNull()

