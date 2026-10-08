package dev.retza.mak.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.room.InvalidationTracker
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.data.database.PLAN_TABLES
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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

fun registerMakWidgetRefresh(
    database: AppDatabase,
    requester: WidgetRefreshRequester
) {
    database.invalidationTracker.addObserver(
        object : InvalidationTracker.Observer(PLAN_TABLES) {
            override fun onInvalidated(tables: Set<String>) {
                requester.request()
            }
        }
    )
}
