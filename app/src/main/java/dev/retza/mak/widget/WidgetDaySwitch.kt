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
import kotlinx.coroutines.launch

/** Plans the single widget update at the moment it should show tomorrow; null cancels it. */
fun interface WidgetDaySwitchScheduler {
    fun schedule(at: LocalDateTime?)
}

class AlarmWidgetDaySwitchScheduler(
    private val context: Context,
    private val clock: Clock
) : WidgetDaySwitchScheduler {
    override fun schedule(at: LocalDateTime?) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = PendingIntent.getBroadcast(
            context,
            WIDGET_DAY_SWITCH_REQUEST_CODE,
            Intent(context, WidgetDaySwitchReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (at == null) {
            alarmManager.cancel(pending)
        } else {
            // Inexact and without waking the phone: the widget is only seen on a lit screen.
            alarmManager.set(AlarmManager.RTC, at.atZone(clock.zone).toInstant().toEpochMilli(), pending)
        }
    }
}

class WidgetDaySwitchReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                MakTodayWidget().updateAll(context)
            } finally {
                result.finish()
            }
        }
    }
}

private const val WIDGET_DAY_SWITCH_REQUEST_CODE = 7311
