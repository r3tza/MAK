package dev.retza.mak.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Plans the next widget update: a class start or end, the switch to tomorrow, or midnight. */
fun interface WidgetDaySwitchScheduler {
    fun schedule(at: LocalDateTime)
}

class AlarmWidgetDaySwitchScheduler(
    private val context: Context,
    private val clock: Clock
) : WidgetDaySwitchScheduler {
    override fun schedule(at: LocalDateTime) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = PendingIntent.getBroadcast(
            context,
            WIDGET_DAY_SWITCH_REQUEST_CODE,
            Intent(context, WidgetDaySwitchReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Inexact and without waking the phone: the widget is only seen on a lit screen.
        alarmManager.set(AlarmManager.RTC, at.atZone(clock.zone).toInstant().toEpochMilli(), pending)
    }
}

/** The alarm and time changes bump this value so a running Glance session resolves the plan again. */
object WidgetTimeTicks {
    private val mutableTicks = MutableStateFlow(0L)
    val ticks: StateFlow<Long> = mutableTicks

    fun bump() {
        mutableTicks.update { it + 1 }
    }
}

class WidgetDaySwitchReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                WidgetTimeTicks.bump()
                MakTodayWidget().updateAll(context)
            } finally {
                result.finish()
            }
        }
    }
}

private const val WIDGET_DAY_SWITCH_REQUEST_CODE = 7311
