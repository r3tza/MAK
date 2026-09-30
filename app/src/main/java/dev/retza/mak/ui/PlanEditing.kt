package dev.retza.mak.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import dev.retza.mak.sync.PlanEditTracker
import org.koin.compose.koinInject

/** Marks a screen with an unsaved plan form; a downloaded plan waits until it closes. */
@Composable
internal fun TrackPlanEditing() {
    val tracker = koinInject<PlanEditTracker>()
    DisposableEffect(tracker) {
        tracker.open()
        onDispose { tracker.close() }
    }
}
