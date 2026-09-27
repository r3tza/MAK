package dev.retza.mak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private const val HEX_ERROR = "Ten kolor będzie słabo widoczny. Wybierz inny odcień albo jasność."
private const val HEX_FORMAT_ERROR = "Podaj kod w postaci #RRGGBB."

/**
 * Course color chosen from a continuous hue bar and a shade slider. Every value the sliders
 * produce is readable on the light and dark theme, see [courseColorFrom]. A typed code is
 * accepted only when it is readable too.
 */
@Composable
fun MakCourseColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    previewName: String = "Kierunek"
) {
    val current = parseCourseHex(selectedColor)
    val position = current?.let(::hueAndShadeOf) ?: HueAndShade(174f, 0.5f)
    var hexText by remember(selectedColor) { mutableStateOf(selectedColor.uppercase()) }
    var hexError by remember(selectedColor) { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        Text("Kolor kierunku", style = MaterialTheme.typography.labelLarge)
        ColorPreview(color = current, name = previewName.ifBlank { "Kierunek" })

        GradientSlider(
            label = "Odcień",
            value = position.hue,
            valueRange = 0f..360f,
            stateDescription = "${position.hue.roundToInt()} stopni",
            trackColors = (0..360 step 30).map { Color(courseColorFrom(it.toFloat(), position.shade)) },
            onValueChange = { onColorSelected(courseHex(courseColorFrom(it, position.shade))) }
        )
        GradientSlider(
            label = "Jasność",
            value = position.shade,
            valueRange = 0f..1f,
            stateDescription = "${(position.shade * 100).roundToInt()} procent",
            trackColors = listOf(0f, 0.5f, 1f).map { Color(courseColorFrom(position.hue, it)) },
            onValueChange = { onColorSelected(courseHex(courseColorFrom(position.hue, it))) }
        )

        MakField(
            label = "Kod koloru",
            value = hexText,
            onValueChange = { text ->
                hexText = text.uppercase()
                val parsed = parseCourseHex(text)
                hexError = when {
                    parsed == null -> if (text.trim().removePrefix("#").length >= 6) HEX_FORMAT_ERROR else null
                    !isReadableCourseColor(parsed) -> HEX_ERROR
                    else -> null
                }
                if (parsed != null && isReadableCourseColor(parsed)) onColorSelected(courseHex(parsed))
            },
            placeholder = DefaultCourseColor,
            isError = hexError != null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
        )
        FieldError(hexError?.let(::FieldErrorUi))
    }
}

@Composable
private fun ColorPreview(color: Int?, name: String) {
    val accent = color?.let { Color(it) } ?: MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(MakSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        CourseNameText(name = name, accent = accent)
        Text(
            text = "Podgląd",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Slider whose track shows the colors it produces, so the bar and the control are one element. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradientSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    stateDescription: String,
    trackColors: List<Color>,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            // A neutral thumb stays visible on every track color.
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.onSurface),
            track = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(trackColors))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                )
            },
            modifier = Modifier.semantics {
                contentDescription = label
                this.stateDescription = stateDescription
            }
        )
    }
}
