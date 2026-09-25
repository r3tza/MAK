package dev.retza.mak.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.retza.mak.MainActivity
import dev.retza.mak.R
import dev.retza.mak.data.repository.ScheduleRepository
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.domain.CollisionNotificationKind
import dev.retza.mak.domain.collisionNotificationGroups
import dev.retza.mak.domain.shouldShowCollisionNotification
import dev.retza.mak.ui.settings.SettingsPreferences
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CollisionAlarmReceiver : BroadcastReceiver(), KoinComponent {
    private val scheduler: CollisionAlarmScheduler by inject()
    private val semesterRepository: SemesterRepository by inject()
    private val scheduleRepository: ScheduleRepository by inject()
    private val activePlanProvider: ActivePlanProvider by inject()
    private val preferences: SettingsPreferences by inject()
    private val clock: Clock by inject()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            COLLISION_MAINTENANCE_ACTION -> runAsync { scheduler.refresh() }
            COLLISION_ALARM_ACTION -> runAsync { showNotification(context, intent) }
        }
    }

    private fun runAsync(block: suspend () -> Unit) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // A failed alarm must not crash the process; the maintenance alarm reschedules.
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun showNotification(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId < 0) return
        val kind = intent.getStringExtra(EXTRA_KIND)
            ?.let { runCatching { CollisionNotificationKind.valueOf(it) }.getOrNull() }
            ?: return
        val date = intent.getStringExtra(EXTRA_DATE)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return
        val occurrenceIds = intent.getStringArrayExtra(EXTRA_OCCURRENCE_IDS)?.toList().orEmpty()
        if (occurrenceIds.isEmpty()) return

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val settings = preferences.collisionNotifications.first()
        if (!settings.enabled) return
        if (kind == CollisionNotificationKind.EVENING && !settings.eveningEnabled) return
        if (kind == CollisionNotificationKind.BEFORE_CLASS && !settings.beforeClassEnabled) return

        val semester = semesterRepository.observeActiveSemester().first() ?: return
        val planData = scheduleRepository.observeActivePlanData(semester.id).first() ?: return
        val plan = activePlanProvider.resolve(planData, date)
        val groups = collisionNotificationGroups(plan.collisions)
        val occurrences = plan.schedule.occurrences.filter { it.id in occurrenceIds }
        if (occurrences.isEmpty()) return
        val now = LocalDateTime.now(clock)
        val latestEnd = occurrences.maxOf { it.endTime }
        if (!shouldShowCollisionNotification(kind, date, occurrenceIds, groups, now, latestEnd)) return

        val title: String
        val text: String
        if (kind == CollisionNotificationKind.EVENING) {
            title = "Kolizje w planie"
            text = "${groups.size} ${groupLabel(groups.size)}: " +
                groups.joinToString(", ") { "${it.overlapStart}-${it.overlapEnd}" }
        } else {
            val group = groups.first { it.occurrenceIds.containsAll(occurrenceIds) }
            title = "Za chwilę kolizja zajęć"
            text = occurrences.joinToString(" / ") { it.name } + " od ${group.earliestStart}"
        }

        ensureCollisionChannel(context)
        val click = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_PLAN_DATE, date.toString()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, COLLISION_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(click)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notify(context, notificationId, notification)
    }

    @SuppressLint("MissingPermission")
    private fun notify(
        context: Context,
        notificationId: Int,
        notification: android.app.Notification
    ) {
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}

private fun groupLabel(count: Int): String = when {
    count == 1 -> "grupa kolizji"
    count in 2..4 -> "grupy kolizji"
    else -> "grup kolizji"
}

fun ensureCollisionChannel(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    val channel = NotificationChannel(
        COLLISION_NOTIFICATION_CHANNEL_ID,
        "Kolizje w planie",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "Powiadomienia o nakładających się zajęciach."
    }
    manager.createNotificationChannel(channel)
}
