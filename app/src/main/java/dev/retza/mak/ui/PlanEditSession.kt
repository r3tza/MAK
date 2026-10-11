package dev.retza.mak.ui

import dev.retza.mak.sync.PlanEditTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Edit admission held by a view model (`ARCHITECTURE.md`, section 7). A sync replacement waits while the key
 * is held. The first action that starts a draft reloads the base data after admission; the key is released
 * when [hasDraft] turns false and when the view model is cleared.
 */
class PlanEditSession(
    private val tracker: PlanEditTracker,
    private val scope: CoroutineScope,
    private val refreshBase: suspend () -> Boolean,
    private val hasDraft: () -> Boolean
) {
    private val key = UUID.randomUUID().toString()
    private val mutex = Mutex()
    private var admitted = false

    /** Admits a screen that is a draft from its start and reloads its base; false when the base is gone. */
    suspend fun admitForm(): Boolean = mutex.withLock {
        tracker.beginEdit(key)
        admitted = true
        refreshBase().also { loaded -> if (!loaded) release() }
    }

    /**
     * Applies a typed draft change at once, so a text field keeps its cursor, then admits the screen. The first
     * admission reloads the base after the change, so [refreshBase] must keep drafts the user already typed.
     */
    fun edit(change: () -> Unit) {
        change()
        run {}
    }

    /** Runs an action that may start a draft or a confirmation. */
    fun run(action: suspend () -> Unit) {
        scope.launch {
            mutex.withLock {
                tracker.beginEdit(key)
                val first = !admitted
                admitted = true
                try {
                    if (first && !refreshBase()) {
                        release()
                        return@withLock
                    }
                    action()
                } finally {
                    if (currentCoroutineContext().isActive && admitted && !hasDraft()) release()
                }
            }
        }
    }

    /** Releases the key once the draft is gone, for example after a save that finished later. */
    suspend fun releaseIfNoDraft() = mutex.withLock { if (admitted && !hasDraft()) release() }

    fun close() {
        if (admitted) tracker.release(key)
    }

    private suspend fun release() {
        tracker.endEdit(key)
        admitted = false
    }
}
