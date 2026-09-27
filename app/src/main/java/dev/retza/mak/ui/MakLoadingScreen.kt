package dev.retza.mak.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/** Upper bound for the loading screen, so a stalled read never hides the app. */
internal const val LOADING_SCREEN_TIMEOUT_MILLIS = 2_000L

internal const val LOADING_SCREEN_TAG = "mak_loading_screen"

/**
 * Covers [content] with [MakLoadingScreen] until [isReady] or the timeout. The content is composed
 * underneath, so its view models start loading at once. Once hidden, the loading screen does not
 * come back, also after rotation.
 */
@Composable
fun MakLoadingGate(
    isReady: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var finished by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isReady) {
        if (isReady) finished = true
    }
    LaunchedEffect(Unit) {
        delay(LOADING_SCREEN_TIMEOUT_MILLIS)
        finished = true
    }
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = if (finished) Modifier.fillMaxSize() else Modifier.fillMaxSize().clearAndSetSemantics { }
        ) {
            content()
        }
        if (!finished) {
            MakLoadingScreen()
        }
    }
}

/** Size of the splash screen icon, so the logo stays in place when the splash screen hands over. */
private val SPLASH_ICON_SIZE = 240.dp

/**
 * Loading screen that continues the splash screen: the poppy logo at the size and position of the
 * splash icon, with the full name below. While it waits, a dimmer wave runs around the petals,
 * unless system animations are turned off.
 */
@Composable
fun MakLoadingScreen(modifier: Modifier = Modifier) {
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
            animateWaiting = animationsOn
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
        )
    }
}
