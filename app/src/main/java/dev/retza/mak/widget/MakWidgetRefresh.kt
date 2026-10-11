package dev.retza.mak.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dev.retza.mak.domain.PlanDisplaySettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

private const val WIDGET_REFRESH_DEBOUNCE_MILLIS = 1_000L

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
@OptIn(FlowPreview::class)
fun registerMakWidgetRefresh(
    planChanges: Flow<Unit>,
    planDisplay: Flow<PlanDisplaySettings>,
    scope: CoroutineScope,
    requester: WidgetRefreshRequester
) {
    scope.launch {
        // The first settings value is the stored one, already shown by the widget.
        merge(planChanges, planDisplay.drop(1).map { })
            .debounce(WIDGET_REFRESH_DEBOUNCE_MILLIS)
            .collect { requester.request() }
    }
}
