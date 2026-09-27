package dev.retza.mak.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.PI
import kotlin.math.cos

// Geometry of the poppy logo (variant M) in the 108-unit grid of the launcher icon. The same path
// is used in ic_launcher_foreground.xml and splash_logo_animated.xml.
private const val LOGO_VIEWPORT = 108f
private const val LOGO_CENTER = 54f
private const val FLOWER_SCALE = 1.12f
private const val PETAL_PATH =
    "M50.8,50.2C46,44.5 40.2,38 40.6,32.2Q41.5,27 46,27.5Q50,24.2 54,26Q58,24.2 62,27.5" +
        "Q66.5,27 67.4,32.2C67.8,38 62,44.5 57.2,50.2C55.4,52.3 52.6,52.3 50.8,50.2Z"
private const val PETAL_GAP_WIDTH = 2f
private const val SEED_HEAD_RADIUS = 8.5f

// Brand colours of the logo. They are not interface colours and do not follow the theme.
private val PoppyRed = Color(0xFFE5402A)
private val PoppySeedHead = Color(0xFF1B2236)
private val PoppyIconCircle = Color.White

internal const val POPPY_PETAL_COUNT = 5
internal const val POPPY_WAVE_PERIOD_MILLIS = 1_500
private const val POPPY_WAVE_DEPTH = 0.35f

/**
 * Petal opacity in the waiting loop. A dimmer wave runs around the flower once per period:
 * each petal reaches 0.3 half a period after it starts, and the next petal lags by a fifth.
 */
internal fun waitingPetalAlpha(phase: Float, index: Int): Float {
    val t = phase - index.toFloat() / POPPY_PETAL_COUNT
    return 1f - POPPY_WAVE_DEPTH * (1f - cos(2.0 * PI * t).toFloat())
}

/**
 * Poppy logo on the white icon circle, drawn like the splash screen icon: the square is the 240 dp
 * icon and the circle takes the middle two thirds. With [animateWaiting] a dimmer wave runs around
 * the petals; pass false when system animations are off.
 */
@Composable
fun MakPoppyLogo(
    modifier: Modifier = Modifier,
    animateWaiting: Boolean = false
) {
    val petal = remember { PathParser().parsePathString(PETAL_PATH).toPath() }
    val phase: State<Float>? = if (animateWaiting) {
        rememberInfiniteTransition(label = "poppyWaiting").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(POPPY_WAVE_PERIOD_MILLIS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "poppyWavePhase"
        )
    } else {
        null
    }
    Canvas(modifier = modifier) {
        val side = size.minDimension
        drawCircle(color = PoppyIconCircle, radius = side / 3f)
        // Read the phase here, so the wave redraws the canvas without recomposing.
        val wavePhase = phase?.value
        translate(left = (size.width - side) / 2f, top = (size.height - side) / 2f) {
            scale(scale = side / LOGO_VIEWPORT, pivot = Offset.Zero) {
                scale(scale = FLOWER_SCALE, pivot = Offset(LOGO_CENTER, LOGO_CENTER)) {
                    repeat(POPPY_PETAL_COUNT) { index ->
                        rotate(degrees = 360f / POPPY_PETAL_COUNT * index, pivot = Offset(LOGO_CENTER, LOGO_CENTER)) {
                            val alpha = wavePhase?.let { waitingPetalAlpha(it, index) } ?: 1f
                            drawPath(path = petal, color = PoppyRed.copy(alpha = alpha))
                            drawPath(
                                path = petal,
                                color = PoppyIconCircle,
                                style = Stroke(width = PETAL_GAP_WIDTH, join = StrokeJoin.Round)
                            )
                        }
                    }
                    drawCircle(
                        color = PoppySeedHead,
                        radius = SEED_HEAD_RADIUS,
                        center = Offset(LOGO_CENTER, LOGO_CENTER)
                    )
                }
            }
        }
    }
}
