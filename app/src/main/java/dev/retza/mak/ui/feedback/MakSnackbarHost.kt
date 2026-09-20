package dev.retza.mak.ui.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.retza.mak.ui.components.MakSpacing
import kotlinx.coroutines.flow.Flow

data class MakSnackbarVisuals(
    override val message: String,
    val kind: UiFeedbackKind,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration
) : SnackbarVisuals

fun UiFeedback.toVisuals(): MakSnackbarVisuals = MakSnackbarVisuals(
    message = message,
    kind = kind,
    duration = when (kind) {
        UiFeedbackKind.Success,
        UiFeedbackKind.Info -> SnackbarDuration.Short

        UiFeedbackKind.Error,
        UiFeedbackKind.Warning -> SnackbarDuration.Long
    }
)

@Composable
fun MakSnackbarHost(
    feedback: Flow<UiFeedback>,
    modifier: Modifier = Modifier,
    hostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    LaunchedEffect(feedback) {
        feedback.collect { item ->
            hostState.showSnackbar(item.toVisuals())
        }
    }
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        val kind = (data.visuals as? MakSnackbarVisuals)?.kind ?: UiFeedbackKind.Info
        MakSnackbar(message = data.visuals.message, kind = kind)
    }
}

@Composable
private fun MakSnackbar(message: String, kind: UiFeedbackKind) {
    val container = kind.containerColor()
    val content = kind.contentColor()
    val label = kind.label()
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MakSpacing.lg, vertical = MakSpacing.sm)
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else container,
                shape = RoundedCornerShape(14.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(horizontal = MakSpacing.md, vertical = MakSpacing.md)
            .semantics {
                contentDescription = "$label: $message"
                liveRegion = LiveRegionMode.Polite
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        Icon(
            imageVector = kind.icon(),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = message,
            color = content,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun UiFeedbackKind.containerColor(): Color = when (this) {
    UiFeedbackKind.Success -> MaterialTheme.colorScheme.secondaryContainer
    UiFeedbackKind.Error -> MaterialTheme.colorScheme.errorContainer
    UiFeedbackKind.Warning -> MaterialTheme.colorScheme.tertiaryContainer
    UiFeedbackKind.Info -> MaterialTheme.colorScheme.primaryContainer
}

@Composable
private fun UiFeedbackKind.contentColor(): Color = when (this) {
    UiFeedbackKind.Success -> MaterialTheme.colorScheme.onSecondaryContainer
    UiFeedbackKind.Error -> MaterialTheme.colorScheme.onErrorContainer
    UiFeedbackKind.Warning -> MaterialTheme.colorScheme.onTertiaryContainer
    UiFeedbackKind.Info -> MaterialTheme.colorScheme.onPrimaryContainer
}

private fun UiFeedbackKind.icon(): ImageVector = when (this) {
    UiFeedbackKind.Success -> Icons.Outlined.CheckCircle
    UiFeedbackKind.Error -> Icons.Outlined.ErrorOutline
    UiFeedbackKind.Warning -> Icons.Outlined.WarningAmber
    UiFeedbackKind.Info -> Icons.Outlined.Info
}

private fun UiFeedbackKind.label(): String = when (this) {
    UiFeedbackKind.Success -> "Sukces"
    UiFeedbackKind.Error -> "Błąd"
    UiFeedbackKind.Warning -> "Ostrzeżenie"
    UiFeedbackKind.Info -> "Informacja"
}
