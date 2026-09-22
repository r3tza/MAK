package dev.retza.mak.widget

import android.content.Context
import androidx.room.InvalidationTracker
import androidx.glance.appwidget.updateAll
import dev.retza.mak.data.database.AppDatabase
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
            MakTodayWidget().updateAll(appContext)
        }
    }
}

fun registerMakWidgetRefresh(
    database: AppDatabase,
    requester: WidgetRefreshRequester
) {
    database.invalidationTracker.addObserver(
        object : InvalidationTracker.Observer(MAK_WIDGET_TABLES) {
            override fun onInvalidated(tables: Set<String>) {
                requester.request()
            }
        }
    )
}

private val MAK_WIDGET_TABLES = arrayOf(
    "semesters",
    "study_programs",
    "academic_calendars",
    "semester_programs",
    "classes",
    "week_overrides",
    "occurrence_notes",
    "occurrence_changes"
)
