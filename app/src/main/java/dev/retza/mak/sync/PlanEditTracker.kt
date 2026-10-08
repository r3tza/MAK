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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** Screens with an unsaved plan draft or an open confirmation, tracked by stable key. */
class PlanEditTracker(
    private val scope: CoroutineScope,
    private val graceMillis: Long = DEFAULT_GRACE_MILLIS
) {
    private val keys = MutableStateFlow<Set<String>>(emptySet())
    private val pendingRemovals = mutableMapOf<String, Job>()
    private val generations = ConcurrentHashMap<String, AtomicLong>()
    private val gate = Mutex()

    val isEditing: Boolean get() = keys.value.isNotEmpty()

    val editing: Flow<Boolean> = keys.map { it.isNotEmpty() }.distinctUntilChanged()

    /** Acquires an edit key before the caller reads or changes plan data. */
    suspend fun beginEdit(key: String) {
        gate.withLock {
            nextGeneration(key)
            pendingRemovals.remove(key)?.cancel()
            keys.update { it + key }
        }
    }

    /** Ends a draft or confirmation immediately after its save or cancellation completes. */
    suspend fun endEdit(key: String) {
        gate.withLock {
            nextGeneration(key)
            pendingRemovals.remove(key)?.cancel()
            keys.update { it - key }
        }
    }

    /** Runs a local replacement only when no editor has a key, serialized with [beginEdit]. */
    suspend fun <T> withReplacement(block: suspend () -> T): PlanReplacementResult<T> = gate.withLock {
        if (keys.value.isNotEmpty()) {
            PlanReplacementResult.Editing
        } else {
            PlanReplacementResult.Applied(block())
        }
    }

    /** Keeps an edit key for the activity recreation grace period after its screen is disposed. */
    fun release(key: String): Job {
        val generation = currentGeneration(key)
        return scope.launch {
        val removal = gate.withLock {
            if (currentGeneration(key) != generation || key !in keys.value) return@withLock null
            pendingRemovals.remove(key)?.cancel()
            scope.launch {
                delay(graceMillis)
                gate.withLock {
                    if (currentGeneration(key) == generation &&
                        pendingRemovals[key] === kotlinx.coroutines.currentCoroutineContext()[Job]
                    ) {
                        pendingRemovals.remove(key)
                        keys.update { it - key }
                    }
                }
            }
                .also { pendingRemovals[key] = it }
        }
        removal?.join()
        }
    }

    private fun nextGeneration(key: String): Long =
        generations.computeIfAbsent(key) { AtomicLong() }.incrementAndGet()

    private fun currentGeneration(key: String): Long = generations[key]?.get() ?: 0

    companion object {
        const val DEFAULT_GRACE_MILLIS = 5_000L
    }
}

sealed interface PlanReplacementResult<out T> {
    data object Editing : PlanReplacementResult<Nothing>
    data class Applied<T>(val value: T) : PlanReplacementResult<T>
}
