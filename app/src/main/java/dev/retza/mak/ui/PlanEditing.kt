package dev.retza.mak.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import dev.retza.mak.sync.PlanEditTracker
import java.util.UUID
import org.koin.compose.koinInject

/**
 * Marks the screen as editing the plan while [active], so a downloaded plan waits. The key survives
 * activity recreation, which lets the tracker keep the edit across it.
 */
@Composable
internal fun TrackPlanEditing(active: Boolean = true, tracker: PlanEditTracker = koinInject()) {
    val key = rememberSaveable { UUID.randomUUID().toString() }
    DisposableEffect(tracker, key, active) {
        tracker.set(key, active)
        onDispose { tracker.release(key) }
    }
}
