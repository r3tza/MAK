package dev.retza.mak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.retza.mak.ui.theme.MakFieldLabel
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
import dev.retza.mak.ui.theme.MakSummaryEnd
import dev.retza.mak.ui.theme.MakSummaryStart
import dev.retza.mak.ui.theme.MakTeal

@Composable
fun MakScreenContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = MakSpacing.lg, end = MakSpacing.lg, top = MakSpacing.xs, bottom = MakSpacing.xl),
        content = content
    )
}

@Composable
fun MakSectionHeader(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = eyebrow.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = MakSpacing.xs, top = MakSpacing.xs, bottom = MakSpacing.sm)
        )
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
    caption: String,
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
        Text(caption, color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp)
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
        if (classCount == 0) {
            Text("Dziś bez zajęć", color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp)
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
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)
    ) {
        Text(label, color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp)
        Text(value.toString(), color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(Color.White.copy(alpha = 0.25f))
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
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
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
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = item.endTime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        textDecoration = if (item.isCancelled) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    val badge = item.statusBadge
                    if (badge != null) {
                        StatusBadge(
                            text = badge,
                            cancelled = item.isCancelled,
                            modified = item.isModified,
                            oneOff = item.isOneOff
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MakSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CoursePill(
                        name = item.courseName,
                        accent = accent,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = item.type,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = classMeta(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
                            fontSize = 11.sp
                        )
                    }
                }
                if (!item.note.isNullOrBlank()) {
                    Text(item.note, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun CoursePill(
    name: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val warm = isWarm(accent)
    Text(
        text = name,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (warm) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.secondaryContainer
            )
            .padding(horizontal = 7.dp, vertical = 3.dp),
        color = if (warm) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun StatusBadge(text: String, cancelled: Boolean, modified: Boolean, oneOff: Boolean) {
    val bg = when {
        cancelled -> MaterialTheme.colorScheme.errorContainer
        modified -> MakModifiedSoft
        oneOff -> MakOnceSoft
        else -> MakPillNeutralBg
    }
    val fg = when {
        cancelled -> MaterialTheme.colorScheme.error
        modified -> MakModified
        oneOff -> MakOnce
        else -> MakPillNeutral
    }
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        color = fg,
        fontSize = 9.sp,
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
    emptyTitle: String? = null,
    emptyMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
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
        Text(text, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun MakTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp
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
            .padding(horizontal = 11.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 1.dp)
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
            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


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
            fontSize = 10.sp,
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
        trailingIcon = { Text("▾", color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
                color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = shape
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (expanded) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                )
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (expanded) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = if (expanded) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = MakSpacing.md, vertical = MakSpacing.md),
                verticalArrangement = Arrangement.spacedBy(MakSpacing.md),
                content = content
            )
        }
    }
}

@Composable
fun <T> MakSelectField(
    label: String,
    value: String,
    options: List<T>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    optionLabel: (T) -> String = { it.toString() }
) {
    var expanded by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        FieldLabel(label)
        val border = when {
            isError -> MaterialTheme.colorScheme.error
            focused -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outlineVariant
        }
        val shape = RoundedCornerShape(11.dp)
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(shape)
                    .border(1.dp, border, shape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(role = Role.Button, onClick = { expanded = true })
                    .focusable()
                    .onFocusChanged { focused = it.isFocused }
                    .padding(horizontal = 11.dp, vertical = 9.dp)
                    .semantics {
                        contentDescription = "$label: ${value.ifBlank { "brak wyboru" }}"
                        stateDescription = if (expanded) "Rozwinięte" else "Zwinięte"
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value.ifBlank { " " },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text("▾", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option), fontSize = 13.sp) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(label: String) {
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = if (MaterialTheme.colorScheme.background.luminanceOrInk()) {
            MakFieldLabel
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
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
        Text(label, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
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
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val shape = RoundedCornerShape(4.dp)
        Box(
            modifier = Modifier
                .size(16.dp)
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
                Text("✓", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        fontSize = 11.sp,
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
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MakPillNeutralBg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MakPillNeutral
    )
}

@Composable
fun MakDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(4.dp)
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
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
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
    item.note?.takeIf(String::isNotBlank)?.let(::add)
}.joinToString(", ")

@Composable
private fun classAccentColor(item: ClassItemUi): Color {
    val parsed = item.courseColor?.let(::parseHexColor)
    if (parsed != null) return parsed
    return if (item.courseName.hashCode() % 2 == 0) MakTeal else MakOrangeMark
}

private fun isWarm(color: Color): Boolean = color.red > color.blue && color.red >= color.green * 0.55f

fun parseHexColor(hex: String): Color? = runCatching {
    val value = hex.trim().removePrefix("#")
    val color = when (value.length) {
        6 -> value.toLong(16) or 0xFF000000L
        8 -> value.toLong(16)
        else -> return null
    }
    Color(color.toInt())
}.getOrNull()

private fun Color.luminanceOrInk(): Boolean {
    val r = red
    val g = green
    val b = blue
    return (0.299f * r + 0.587f * g + 0.114f * b) > 0.5f
}
