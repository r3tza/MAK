package dev.retza.mak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.text.TextStyle
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
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.LooksOne
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.EditCalendar
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
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import dev.retza.mak.ui.theme.MakModified
import dev.retza.mak.ui.theme.MakOnce
import dev.retza.mak.ui.theme.MakOrangeMark
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
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(MakSpacing.sm))
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

/** Poppy logo next to the name in the top bar; the petal gaps match the bar surface. */
@Composable
fun MakBrandMark(modifier: Modifier = Modifier) {
    MakPoppyMark(
        gapColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.size(30.dp)
    )
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
                if (focused) 2.dp else 1.dp,
                if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(12.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
            .onFocusChanged { focused = it.isFocused }
            .clickable(role = Role.Button, onClick = onClick)
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
                if (focused) 2.dp else 1.dp,
                if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(12.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
            .onFocusChanged { focused = it.isFocused }
            .clickable(role = Role.Button, onClick = onClick)
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
                if (focused) 2.dp else 1.dp,
                if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(10.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
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
                if (focused) 2.dp else 1.dp,
                if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(10.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
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
        // One line that shrinks down to 12 sp instead of breaking a word at a large font scale.
        BasicText(
            text = label,
            style = TextStyle(color = Color.White, fontSize = 13.sp),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 13.sp)
        )
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
    meta: String? = null,
    modifier: Modifier = Modifier
) {
    // Without room for both, the meta text moves under the title instead of breaking a word.
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 2.dp, end = 2.dp, bottom = 11.dp, top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(MakSpacing.xs),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        meta?.takeIf(String::isNotEmpty)?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
                    else courseShapeColor(accent, MaterialTheme.colorScheme.surface)
                )
        )
        Row(
            modifier = Modifier
                .weight(1f)
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
                classStatusIcon(item)?.let { (icon, tint) ->
                    // The card's own description already reads the status.
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.padding(top = MakSpacing.sm).size(20.dp)
                    )
                }
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
    Text(
        text = name,
        modifier = modifier,
        color = Color(courseTextColor(accent.toArgb(), surface.toArgb())),
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

/** Icon and color for a class state that changes the plan or marks a one-off date; null otherwise. */
@Composable
internal fun classStatusIcon(item: ClassItemUi): Pair<ImageVector, Color>? = when {
    item.isCancelled -> Icons.Outlined.EventBusy to MaterialTheme.colorScheme.error
    item.isModified -> Icons.Outlined.EditCalendar to modifiedTint()
    item.isOneOff -> Icons.Outlined.LooksOne to oneOffTint()
    else -> null
}

@Composable
fun MakEmptyState(message: String, modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.outlineVariant
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
    destructive: Boolean = false,
    icon: ImageVector? = null
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
        if (icon == null) {
            Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(18.dp)
                )
                Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        }
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

enum class MakNoteRole {
    Neutral,
    Warning,
    Error
}

internal data class NoteRoleColors(
    val container: Color,
    val border: Color?,
    val title: Color,
    val body: Color,
    val icon: ImageVector?
)

internal fun noteRoleColors(role: MakNoteRole, scheme: ColorScheme): NoteRoleColors = when (role) {
    MakNoteRole.Neutral -> NoteRoleColors(
        container = scheme.surfaceContainerLow,
        border = scheme.outlineVariant,
        title = scheme.onSurface,
        body = scheme.onSurfaceVariant,
        icon = null
    )
    MakNoteRole.Warning -> NoteRoleColors(
        container = scheme.tertiaryContainer,
        border = null,
        title = scheme.onTertiaryContainer,
        body = scheme.onTertiaryContainer,
        icon = Icons.Outlined.WarningAmber
    )
    MakNoteRole.Error -> NoteRoleColors(
        container = scheme.errorContainer,
        border = null,
        title = scheme.onErrorContainer,
        body = scheme.onErrorContainer,
        icon = Icons.Outlined.ErrorOutline
    )
}

@Composable
fun MakNoteBanner(
    title: String?,
    subtitle: String,
    role: MakNoteRole,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = noteRoleColors(role, MaterialTheme.colorScheme)
    val prefix = when (role) {
        MakNoteRole.Neutral -> null
        MakNoteRole.Warning -> "Ostrzeżenie: "
        MakNoteRole.Error -> "Błąd: "
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(colors.container)
            .then(colors.border?.let { Modifier.border(1.dp, it, RoundedCornerShape(13.dp)) } ?: Modifier)
            .semantics(mergeDescendants = true) {
                if (prefix != null) {
                    contentDescription = prefix + listOfNotNull(title, subtitle).joinToString(". ")
                }
            }
            .padding(horizontal = MakSpacing.lg, vertical = MakSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.icon?.let { icon ->
                Icon(icon, contentDescription = null, tint = colors.body, modifier = Modifier.size(20.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)
            ) {
                title?.let { Text(it, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.title) }
                Text(subtitle, fontSize = 12.sp, lineHeight = 16.sp, color = colors.body)
            }
        }
        actions?.let {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm, Alignment.End),
                content = it
            )
        }
    }
}

private val MakRowFocusShape = RoundedCornerShape(12.dp)

/** Keyboard focus ring for full-width rows; place it before the click modifier. */
@Composable
internal fun Modifier.makRowFocus(): Modifier {
    var focused by remember { mutableStateOf(false) }
    val ring = if (focused) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MakRowFocusShape)
    } else Modifier
    return then(ring).onFocusChanged { focused = it.isFocused }
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
internal fun isDarkSurface(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

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
    val outline = MaterialTheme.colorScheme.outlineVariant
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
        // "Dodaj" opens a form, so it is an action, not a destination tab.
        MakNavButton("Dodaj", Icons.Outlined.Add, addSelected, onAdd, Modifier.weight(1f), role = Role.Button)
    }
}

@Composable
private fun RowScope.MakNavButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.Tab
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
            .onFocusChanged { focused = it.isFocused }
            .clickable(role = role, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics { if (role == Role.Tab) this.selected = selected },
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
                .onFocusChanged { focused = it.isFocused }
                .clickable(role = Role.Button, onClick = { onExpandedChange(!expanded) })
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
                if (focused) 2.dp else 1.dp,
                if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape
            )
            .onFocusChanged { focused = it.isFocused }
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 48.dp)
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
            .onFocusChanged { focused = it.isFocused }
            .clickable(role = Role.Checkbox, onClick = { onCheckedChange(!checked) })
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


fun parseHexColor(hex: String): Color? = runCatching {
    val value = hex.trim().removePrefix("#")
    val color = when (value.length) {
        6 -> value.toLong(16) or 0xFF000000L
        8 -> value.toLong(16)
        else -> return null
    }
    Color(color.toInt())
}.getOrNull()

