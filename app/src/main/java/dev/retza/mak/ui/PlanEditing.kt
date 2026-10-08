package dev.retza.mak.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.CircularProgressIndicator
import dev.retza.mak.sync.PlanEditTracker
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Controls admission to plan edits before a form reads or changes its base data. */
internal class PlanEditingController(
    private val key: String,
    private val tracker: PlanEditTracker,
    private val scope: CoroutineScope,
    private val isAdmitted: () -> Boolean,
    private val setAdmitted: (Boolean) -> Unit,
    private val setReady: (Boolean) -> Unit,
    private val isActiveNow: () -> Boolean = isAdmitted
) {
    private val actionMutex = Mutex()
    var ready by mutableStateOf(false)
        private set

    internal fun updateReady(value: Boolean) {
        ready = value
        setReady(value)
    }

    suspend fun setActive(active: Boolean) {
        actionMutex.withLock {
            if (active || isActiveNow()) {
                tracker.beginEdit(key)
                setAdmitted(true)
                updateReady(true)
            } else if (isAdmitted()) {
                tracker.endEdit(key)
                setAdmitted(false)
                updateReady(true)
            }
        }
    }

    fun releaseOnDispose() {
        if (isAdmitted()) tracker.release(key)
    }

    fun run(
        refreshBeforeFirstEdit: suspend () -> Boolean = { true },
        holdIfInactive: Boolean = false,
        action: suspend () -> Unit
    ) {
        scope.launch {
            actionMutex.withLock {
                tracker.beginEdit(key)
                val firstAdmission = !isAdmitted()
                setAdmitted(true)
                updateReady(true)
                try {
                    if (firstAdmission && !refreshBeforeFirstEdit()) {
                        tracker.endEdit(key)
                        setAdmitted(false)
                        return@withLock
                    }
                    action()
                } finally {
                    if (currentCoroutineContext().isActive && !holdIfInactive && !isActiveNow() && isAdmitted()) {
                        tracker.endEdit(key)
                        setAdmitted(false)
                    }
                }
            }
        }
    }

    fun callback(
        refreshBeforeFirstEdit: suspend () -> Boolean = { true },
        holdIfInactive: Boolean = false,
        action: () -> Unit
    ): () -> Unit = { run(refreshBeforeFirstEdit, holdIfInactive) { action() } }

    fun <T> callback(
        refreshBeforeFirstEdit: suspend () -> Boolean = { true },
        holdIfInactive: Boolean = false,
        action: (T) -> Unit
    ): (T) -> Unit = { value -> run(refreshBeforeFirstEdit, holdIfInactive) { action(value) } }
}

/**
 * Registers always-active forms before exposing their contents. On preview screens, call [PlanEditingController.run]
 * before every callback that creates a draft or opens a plan-changing confirmation.
 */
@Composable
internal fun TrackPlanEditing(
    active: Boolean = true,
    isActiveNow: () -> Boolean = { active },
    tracker: PlanEditTracker = koinInject()
): PlanEditingController {
    val key = rememberSaveable { UUID.randomUUID().toString() }
    var admitted by remember(key, tracker) { mutableStateOf(false) }
    var ready by remember(key, tracker) { mutableStateOf(!active) }
    val scope = rememberCoroutineScope()
    val latestIsActiveNow = rememberUpdatedState(isActiveNow)
    val controller = remember(key, tracker, scope) {
        PlanEditingController(
            key,
            tracker,
            scope,
            { admitted },
            { admitted = it },
            { ready = it },
            { latestIsActiveNow.value() }
        )
    }
    LaunchedEffect(tracker, key, active) {
        controller.setActive(active)
    }
    DisposableEffect(tracker, key) {
        onDispose(controller::releaseOnDispose)
    }
    controller.updateReady(ready)
    return controller
}

@Composable
internal fun PlanEditingWait() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
