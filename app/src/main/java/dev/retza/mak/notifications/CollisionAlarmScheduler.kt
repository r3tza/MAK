package dev.retza.mak.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import dev.retza.mak.data.repository.ScheduleRepository
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.domain.CollisionNotificationPlanner
import dev.retza.mak.domain.PlannedCollisionNotification
import dev.retza.mak.domain.toAlarmPayload
import dev.retza.mak.ui.settings.SettingsPreferences
import java.time.Clock
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val ALARM_PREFERENCES = "collision_alarms"
private const val SCHEDULED_IDS_KEY = "scheduled_ids"
private const val MAINTENANCE_REQUEST_CODE = 0x4d414b01

@org.koin.core.annotation.Single
class CollisionAlarmScheduler(
    private val context: Context,
    private val semesterRepository: SemesterRepository,
    private val scheduleRepository: ScheduleRepository,
    private val planner: CollisionNotificationPlanner,
    private val preferences: SettingsPreferences,
    private val clock: Clock
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val stored = context.getSharedPreferences(ALARM_PREFERENCES, Context.MODE_PRIVATE)

    // Refreshes from the app, the boot receiver and the maintenance alarm may overlap; the lock keeps
    // the stored alarm ids equal to the alarms that are actually scheduled.
    private val mutex = Mutex()

    suspend fun refresh() = mutex.withLock {
        val settings = preferences.collisionNotifications.first()
        if (!settings.enabled) {
            cancelAllLocked()
            return@withLock
        }
        val semester = semesterRepository.observeActiveSemester().first()
        val planData = semester?.let { scheduleRepository.observeActivePlanData(it.id).first() }
        if (planData == null) {
            cancelAllLocked()
            return@withLock
        }
        val planned = planner.plan(planData, settings.toPlannerSettings())
        val plannedIds = planned.map { it.id }.toSet()
        storedIds().subtract(plannedIds).forEach { cancelAlarm(it) }
        planned.forEach { schedule(it) }
        scheduleMaintenance()
        store(plannedIds)
    }

    suspend fun cancelAll() = mutex.withLock { cancelAllLocked() }

    private fun cancelAllLocked() {
        storedIds().forEach { cancelAlarm(it) }
        cancelMaintenance()
        store(emptySet())
    }

    private fun schedule(item: PlannedCollisionNotification) {
        val from = item.triggerFrom.atZone(clock.zone).toInstant().toEpochMilli()
        val until = item.triggerUntil.atZone(clock.zone).toInstant().toEpochMilli()
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            from,
            (until - from).coerceAtLeast(1L),
            alarmPendingIntent(item)
        )
    }

    private fun cancelAlarm(id: Int) {
        val pending = PendingIntent.getBroadcast(
            context,
            id,
            alarmIntent(),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) alarmManager.cancel(pending)
    }

    private fun scheduleMaintenance() {
        val triggerAt = clock.instant()
            .plus(MAINTENANCE_INTERVAL_DAYS, ChronoUnit.DAYS)
            .toEpochMilli()
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            MAINTENANCE_WINDOW_MILLIS,
            maintenancePendingIntent()
        )
    }

    private fun cancelMaintenance() {
        val pending = PendingIntent.getBroadcast(
            context,
            MAINTENANCE_REQUEST_CODE,
            maintenanceIntent(),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) alarmManager.cancel(pending)
    }

    private fun alarmPendingIntent(item: PlannedCollisionNotification): PendingIntent {
        val payload = item.toAlarmPayload()
        val intent = Intent(context, CollisionAlarmReceiver::class.java)
            .setAction(COLLISION_ALARM_ACTION)
            .putExtra(EXTRA_NOTIFICATION_ID, payload.notificationId)
            .putExtra(EXTRA_KIND, payload.kind.name)
            .putExtra(EXTRA_DATE, payload.date.toString())
            .putExtra(EXTRA_OCCURRENCE_IDS, payload.occurrenceIds.toTypedArray())
        return PendingIntent.getBroadcast(
            context,
            payload.notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun alarmIntent(): Intent =
        Intent(context, CollisionAlarmReceiver::class.java).setAction(COLLISION_ALARM_ACTION)

    private fun maintenancePendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        MAINTENANCE_REQUEST_CODE,
        maintenanceIntent(),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun maintenanceIntent(): Intent =
        Intent(context, CollisionAlarmReceiver::class.java).setAction(COLLISION_MAINTENANCE_ACTION)

    private fun storedIds(): Set<Int> =
        stored.getStringSet(SCHEDULED_IDS_KEY, emptySet())
            .orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .toSet()

    private fun store(ids: Set<Int>) {
        stored.edit { putStringSet(SCHEDULED_IDS_KEY, ids.map { it.toString() }.toSet()) }
    }

    private companion object {
        const val MAINTENANCE_INTERVAL_DAYS = 7L
        const val MAINTENANCE_WINDOW_MILLIS = 6L * 60L * 60L * 1000L
    }
}
