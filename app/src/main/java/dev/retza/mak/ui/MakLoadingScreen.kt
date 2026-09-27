package dev.retza.mak.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.retza.mak.ui.components.MakPoppyLogo
import dev.retza.mak.ui.components.MakSpacing
import kotlinx.coroutines.delay

const val MAK_FULL_NAME = "Mój Akademicki Kalendarz"

/** Upper bound for waiting on data, so a stalled read never hides the app. */
internal const val LOADING_SCREEN_TIMEOUT_MILLIS = 2_000L

/** Fade of the loading screen into the app. */
internal const val LOADING_SCREEN_FADE_MILLIS = 250

internal const val LOADING_SCREEN_TAG = "mak_loading_screen"

/**
 * Covers [content] with [MakLoadingScreen] until [isReady] or the timeout, then fades it out. The
 * content is composed underneath, so its view models start loading at once. With [playIntro] the
 * screen first plays the start animation, which begins once [introMayStart] is true (when the
 * system splash screen is gone) and always runs to the end. Once hidden, the loading screen does
 * not come back, also after rotation.
 */
@Composable
fun MakLoadingGate(
    isReady: Boolean,
    modifier: Modifier = Modifier,
    playIntro: Boolean = false,
    introMayStart: Boolean = true,
    content: @Composable () -> Unit
) {
    var finished by rememberSaveable { mutableStateOf(false) }
    var introDone by rememberSaveable { mutableStateOf(!playIntro) }
    var timedOut by remember { mutableStateOf(false) }
    val introElapsed = remember { Animatable(if (introDone) INTRO_BLOOM_MILLIS.toFloat() else 0f) }
    LaunchedEffect(introMayStart) {
        if (introMayStart && !introDone) {
            introElapsed.animateTo(
                targetValue = INTRO_BLOOM_MILLIS.toFloat(),
                animationSpec = tween(INTRO_BLOOM_MILLIS.toInt(), easing = LinearEasing)
            )
            introDone = true
        }
    }
    LaunchedEffect(Unit) {
        delay(LOADING_SCREEN_TIMEOUT_MILLIS)
        timedOut = true
    }
    LaunchedEffect(isReady, introDone, timedOut) {
        if (introDone && (isReady || timedOut)) finished = true
    }
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = if (finished) Modifier.fillMaxSize() else Modifier.fillMaxSize().clearAndSetSemantics { }
        ) {
            content()
        }
        AnimatedVisibility(
            visible = !finished,
            enter = EnterTransition.None,
            exit = fadeOut(tween(LOADING_SCREEN_FADE_MILLIS))
        ) {
            MakLoadingScreen(
                introElapsed = if (playIntro) introElapsed.asState() else null,
                waiting = introDone
            )
        }
    }
}

/** Size of the splash screen icon, so the logo stays in place when the splash screen hands over. */
private val SPLASH_ICON_SIZE = 240.dp

/**
 * Loading screen that follows the empty splash screen: the poppy logo at the size and position of
 * a splash icon, with the full name below. [introElapsed] drives the start animation: the seed
 * head pops in and the petals grow around it while the name unfolds from the middle. When
 * [waiting] after it, a dimmer wave runs around the petals, unless system animations are off.
 */
@Composable
fun MakLoadingScreen(
    modifier: Modifier = Modifier,
    introElapsed: State<Float>? = null,
    waiting: Boolean = true
) {
    val context = LocalContext.current
    val animationsOn = remember(context) { context.areSystemAnimationsOn() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(LOADING_SCREEN_TAG)
            .semantics { contentDescription = "$MAK_FULL_NAME, ładowanie" },
        contentAlignment = Alignment.Center
    ) {
        MakPoppyLogo(
            modifier = Modifier.size(SPLASH_ICON_SIZE),
            bloomElapsed = introElapsed,
            animateWaiting = animationsOn && waiting
        )
        Text(
            text = MAK_FULL_NAME,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            // The logo circle ends 80 dp below the centre, so the name starts clearly under it.
            modifier = Modifier
                .offset(y = SPLASH_ICON_SIZE / 2 + MakSpacing.xl)
                .padding(horizontal = MakSpacing.xl)
                .drawWithContent {
                    // Read the animation here, so the name unfolds without recomposing.
                    val shown = introElapsed?.let { nameRevealFraction(it.value) } ?: 1f
                    val half = size.width * shown / 2f
                    clipRect(left = size.width / 2f - half, right = size.width / 2f + half) {
                        this@drawWithContent.drawContent()
                    }
                }
        )
    }
}
