package dev.retza.mak.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

/** One app-wide stream of plan writes in Room, shared by the widget refresh and synchronization. */
class PlanChanges(changes: Flow<Unit>, scope: CoroutineScope) {
    val changes: Flow<Unit> = changes.shareIn(scope, SharingStarted.WhileSubscribed(), replay = 0)
}
