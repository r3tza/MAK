package dev.retza.mak.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Screens with an unsaved plan draft or an open confirmation, by key. A downloaded plan waits until
 * none is left, so a form never saves over a plan it did not show.
 *
 * A released key stays for [graceMillis]: recreating the activity (rotation, theme, font) disposes
 * the screen and composes it again with the same saved key, and the edit must not end in between.
 */
class PlanEditTracker(
    private val scope: CoroutineScope,
    private val graceMillis: Long = DEFAULT_GRACE_MILLIS
) {
    private val keys = MutableStateFlow<Set<String>>(emptySet())
    private val pendingRemovals = mutableMapOf<String, Job>()

    val isEditing: Boolean get() = keys.value.isNotEmpty()

    val editing: Flow<Boolean> = keys.map { it.isNotEmpty() }.distinctUntilChanged()

    fun set(key: String, active: Boolean) = synchronized(this) {
        pendingRemovals.remove(key)?.cancel()
        keys.update { if (active) it + key else it - key }
    }

    fun release(key: String) = synchronized(this) {
        if (key !in keys.value) return@synchronized
        pendingRemovals.remove(key)?.cancel()
        pendingRemovals[key] = scope.launch {
            delay(graceMillis)
            synchronized(this@PlanEditTracker) {
                pendingRemovals.remove(key)
                keys.update { it - key }
            }
        }
    }

    companion object {
        const val DEFAULT_GRACE_MILLIS = 5_000L
    }
}
