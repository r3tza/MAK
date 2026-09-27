package dev.retza.mak.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

/** Temporary loading screen with the full name; a logo or its animation will replace the text. */
@Composable
fun MakLoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(LOADING_SCREEN_TAG)
            .semantics { contentDescription = "$MAK_FULL_NAME, ładowanie" },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = MAK_FULL_NAME,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = MakSpacing.xl)
        )
    }
}
