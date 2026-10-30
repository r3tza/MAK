package dev.retza.mak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import dev.retza.mak.ui.MakContentMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.retza.mak.ui.theme.MakSummaryEnd
import dev.retza.mak.ui.theme.MakSummaryEndDark
import dev.retza.mak.ui.theme.MakSummaryStart
import dev.retza.mak.ui.theme.MakSummaryStartDark

@Composable
fun MakScreenContent(
    modifier: Modifier = Modifier,
    maxWidth: Dp = MakContentMaxWidth,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit
) {
    // The caller's modifier (scroll or weight) stays on the full-width box, so a wide window
    // still scrolls anywhere while the content itself is limited and centered.
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .padding(start = MakSpacing.lg, end = MakSpacing.lg, top = MakSpacing.xs, bottom = MakSpacing.xl),
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
}

@Composable
fun MakSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    // A short line under the subtitle, such as the study programs hidden on this phone.
    supportingContent: (@Composable () -> Unit)? = null
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
                modifier = Modifier.padding(start = MakSpacing.xs, end = MakSpacing.xs, top = MakSpacing.sm)
            )
        }
        if (supportingContent != null) {
            Box(modifier = Modifier.padding(start = MakSpacing.xs, end = MakSpacing.xs, top = MakSpacing.sm)) {
                supportingContent()
            }
        }
        Spacer(modifier = Modifier.height(MakSpacing.lg))
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
    val dark = isDarkSurface()
    val shape = RoundedCornerShape(22.dp)
    val cardModifier = if (dark) {
        modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
    } else {
        modifier
            .fillMaxWidth()
            .shadow(11.dp, shape, spotColor = Color(0x332D46A4))
            .clip(shape)
    }
    Column(
        modifier = cardModifier
            .background(Brush.linearGradient(summaryCardGradient(dark)))
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

internal fun summaryCardGradient(dark: Boolean): List<Color> =
    if (dark) listOf(MakSummaryStartDark, MakSummaryEndDark) else listOf(MakSummaryStart, MakSummaryEnd)

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

internal val SummaryAlert = Color(0xFFFFB4AB)
internal val SummaryOk = Color(0xFF7FE0A6)

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
fun MakLoadingMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(MakSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        CircularProgressIndicator()
        Text("Ładowanie", style = MaterialTheme.typography.bodyMedium)
    }
}

enum class MakNoteRole {
    Neutral,
    Warning,
    Error
}

/** Every role shares the neutral surface; only the icon carries the role colour. */
internal data class NoteRoleColors(
    val container: Color,
    val border: Color,
    val title: Color,
    val body: Color,
    val icon: ImageVector?,
    val iconTint: Color?
)

internal fun noteRoleColors(role: MakNoteRole, scheme: ColorScheme): NoteRoleColors {
    val (icon, tint) = when (role) {
        MakNoteRole.Neutral -> null to null
        MakNoteRole.Warning -> Icons.Outlined.WarningAmber to scheme.tertiary
        MakNoteRole.Error -> Icons.Outlined.ErrorOutline to scheme.error
    }
    return NoteRoleColors(
        container = scheme.surfaceContainerLow,
        border = scheme.outlineVariant,
        title = scheme.onSurface,
        body = scheme.onSurfaceVariant,
        icon = icon,
        iconTint = tint
    )
}

/** An action of [MakNoteBanner]; the banner decides how it looks. */
data class MakBannerAction(val text: String, val onClick: () -> Unit, val enabled: Boolean = true)

/**
 * The banner draws its own actions as text buttons on the right, [dismissAction] first; a screen's
 * main action goes below the banner, full width.
 */
@Composable
fun MakNoteBanner(
    title: String?,
    subtitle: String?,
    role: MakNoteRole,
    modifier: Modifier = Modifier,
    action: MakBannerAction? = null,
    dismissAction: MakBannerAction? = null
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
            .border(1.dp, colors.border, RoundedCornerShape(13.dp))
            .semantics(mergeDescendants = true) {
                if (prefix != null) {
                    contentDescription = prefix + listOfNotNull(title, subtitle).joinToString(". ")
                }
            }
            .padding(horizontal = MakSpacing.lg, vertical = MakSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            colors.icon?.let { icon ->
                // 20 dp matches the 20 sp line height of the first line, so the icon sits beside it.
                Icon(icon, contentDescription = null, tint = colors.iconTint ?: colors.body, modifier = Modifier.size(20.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)
            ) {
                title?.let {
                    Text(it, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.title)
                }
                subtitle?.let {
                    // Without a title the message is the banner's only line, so it takes the title colour.
                    Text(
                        it,
                        fontSize = if (title == null) 14.sp else 13.sp,
                        lineHeight = if (title == null) 20.sp else 18.sp,
                        color = if (title == null) colors.title else colors.body
                    )
                }
            }
        }
        if (action != null || dismissAction != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm, Alignment.End)
            ) {
                listOfNotNull(dismissAction, action).forEach {
                    MakTextAction(it.text, it.onClick, enabled = it.enabled)
                }
            }
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
    optionLeading: (@Composable (T) -> Unit)? = null,
    optionTrailing: (@Composable (T) -> Unit)? = null
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
                    trailingIcon = optionTrailing?.let { trailing -> { trailing(option) } },
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

/**
 * A labelled checkbox row. A disabled row is dimmed and ignores taps; [supportingText] is shown under the label.
 */
@Composable
fun MakCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null
) {
    var focused by remember { mutableStateOf(false) }
    // One toggleable row, so a screen reader announces the label, the supporting text and the state once.
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .onFocusChanged { focused = it.isFocused }
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
            .semantics(mergeDescendants = true) {
                stateDescription = if (checked) "Zaznaczone" else "Niezaznaczone"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        MakCheckboxBox(checked = checked, focused = focused)
        if (supportingText == null) {
            Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        } else {
            Column {
                Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** The checkbox alone in a 48 dp target, for a row whose other part has its own action. */
@Composable
fun MakCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(48.dp)
            .onFocusChanged { focused = it.isFocused }
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .semantics {
                this.contentDescription = contentDescription
                stateDescription = if (checked) "Zaznaczone" else "Niezaznaczone"
            },
        contentAlignment = Alignment.Center
    ) {
        MakCheckboxBox(checked = checked, focused = focused)
    }
}

@Composable
internal fun MakCheckboxBox(checked: Boolean, focused: Boolean) {
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
}

/** An action of [MakDialog]; the dialog decides how it looks. */
data class MakDialogAction(
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val destructive: Boolean = false
)

@Composable
fun MakHelperText(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: TextStyle = MakHelperTextStyle
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
        if (icon != null) {
            // Centered on the first line of the text.
            val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = ((lineHeight - 16.dp) / 2).coerceAtLeast(0.dp)).size(16.dp)
            )
        }
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val MakHelperTextStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)

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

fun parseHexColor(hex: String): Color? = runCatching {
    val value = hex.trim().removePrefix("#")
    val color = when (value.length) {
        6 -> value.toLong(16) or 0xFF000000L
        8 -> value.toLong(16)
        else -> return null
    }
    Color(color.toInt())
}.getOrNull()
