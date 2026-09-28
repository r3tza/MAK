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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.retza.mak.ui.theme.MakLine
import dev.retza.mak.ui.theme.MakLineDark
import dev.retza.mak.ui.theme.MakMuted
import dev.retza.mak.ui.theme.MakMutedDark
import dev.retza.mak.ui.theme.MakPaper
import dev.retza.mak.ui.theme.MakPaperDark
import kotlin.math.roundToInt

private const val HEX_FORMAT_ERROR = "Podaj kod w postaci #RRGGBB."
private const val LIGHT_THEME_NOTE = "W motywie jasnym kolor będzie ciemniejszy, aby był czytelny."
private const val DARK_THEME_NOTE = "W motywie ciemnym kolor będzie jaśniejszy, aby był czytelny."
private const val DEFAULT_HUE = 174f

// Greys have no hue; below this saturation the hue slider keeps its last position.
private const val GREY_SATURATION = 0.02f

/**
 * Course color chosen with a hue slider and a lightness slider from black to white. The picker
 * stores the exact color; the app adapts it to each theme when drawing, which the two previews
 * show. A typed code keeps its own saturation until a slider moves.
 */
@Composable
fun MakCourseColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    previewName: String = "Kierunek"
) {
    val current = parseCourseHex(selectedColor)
    val defaultLightness = remember { hueAndLightnessOf(parseCourseHex(DefaultCourseColor)!!).lightness }
    var lastHue by rememberSaveable { mutableFloatStateOf(DEFAULT_HUE) }
    val hue = current
        ?.takeIf { saturationOf(it) > GREY_SATURATION }
        ?.let { hueAndLightnessOf(it).hue }
        ?: lastHue
    val lightness = current?.let { hueAndLightnessOf(it).lightness } ?: defaultLightness
    var hexText by remember(selectedColor) { mutableStateOf(selectedColor.uppercase()) }
    var hexError by remember(selectedColor) { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        Text("Kolor kierunku", style = MaterialTheme.typography.labelLarge)
        val name = previewName.ifBlank { "Kierunek" }
        MakFieldPair(
            first = { ThemePreview(color = current, name = name, dark = false) },
            second = { ThemePreview(color = current, name = name, dark = true) }
        )
        current?.let { color ->
            if (contrastRatio(color, MakPaper.toArgb()) < SHAPE_CONTRAST) MakHelperText(LIGHT_THEME_NOTE)
            if (contrastRatio(color, MakPaperDark.toArgb()) < SHAPE_CONTRAST) MakHelperText(DARK_THEME_NOTE)
        }

        GradientSlider(
            label = "Odcień",
            value = hue,
            valueRange = 0f..360f,
            stateDescription = "${hue.roundToInt()} stopni",
            // Pure hues keep the track readable also for a very light or dark color.
            trackColors = (0..360 step 30).map { Color(courseColorFrom(it.toFloat(), 0.5f)) },
            onValueChange = {
                lastHue = it
                onColorSelected(courseHex(courseColorFrom(it, lightness)))
            }
        )
        GradientSlider(
            label = "Jasność",
            value = lightness,
            valueRange = 0f..1f,
            stateDescription = "${(lightness * 100).roundToInt()} procent",
            trackColors = listOf(0f, 0.5f, 1f).map { Color(courseColorFrom(hue, it)) },
            onValueChange = {
                lastHue = hue
                onColorSelected(courseHex(courseColorFrom(hue, it)))
            }
        )

        MakField(
            label = "Kod koloru",
            value = hexText,
            onValueChange = { text ->
                hexText = text.uppercase()
                val parsed = parseCourseHex(text)
                hexError = if (parsed == null && text.trim().removePrefix("#").length >= 6) HEX_FORMAT_ERROR else null
                if (parsed != null) onColorSelected(courseHex(parsed))
            },
            placeholder = DefaultCourseColor,
            isError = hexError != null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
        )
        FieldError(hexError?.let(::FieldErrorUi))
    }
}

/** Bar and course name as the app draws them on the card of one theme, independent of the current theme. */
@Composable
private fun ThemePreview(color: Int?, name: String, dark: Boolean) {
    val surface = if (dark) MakPaperDark else MakPaper
    val muted = if (dark) MakMutedDark else MakMuted
    val shape = RoundedCornerShape(12.dp)
    val bar = color?.let { Color(courseShapeColor(it, surface.toArgb())) } ?: muted
    val text = color?.let { Color(courseTextColor(it, surface.toArgb())) } ?: muted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surface)
            .border(1.dp, if (dark) MakLineDark else MakLine, shape)
            .padding(MakSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(bar)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (dark) "Motyw ciemny" else "Motyw jasny",
                color = muted,
                style = MaterialTheme.typography.bodySmall
            )
        }
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
