package dev.retza.mak.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.room.InvalidationTracker
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.data.database.PLAN_TABLES
import dev.retza.mak.domain.PlanDisplaySettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

fun interface WidgetRefreshRequester {
    fun request()
}

class GlanceWidgetRefreshRequester(
    context: Context,
    private val scope: CoroutineScope
) : WidgetRefreshRequester {
    private val appContext = context.applicationContext

    override fun request() {
        scope.launch {
            try {
                MakTodayWidget().updateAll(appContext)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // The periodic widget update retries later.
            }
        }
    }
}

/** Refreshes the widget after a change of the plan in Room or of the settings that change how it resolves. */
fun registerMakWidgetRefresh(
    database: AppDatabase,
    planDisplay: Flow<PlanDisplaySettings>,
    scope: CoroutineScope,
    requester: WidgetRefreshRequester
) {
    database.invalidationTracker.addObserver(
        object : InvalidationTracker.Observer(PLAN_TABLES) {
            override fun onInvalidated(tables: Set<String>) {
                requester.request()
            }
        }
    )
    // The first value is the stored setting, already shown by the widget.
    scope.launch { planDisplay.drop(1).collect { requester.request() } }
}
