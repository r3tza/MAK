package dev.retza.mak.notifications

import dev.retza.mak.domain.CollisionNotificationSettings
import dev.retza.mak.ui.settings.CollisionNotificationPreferences

const val COLLISION_NOTIFICATION_CHANNEL_ID = "collision_notifications"
const val COLLISION_ALARM_ACTION = "dev.retza.mak.action.COLLISION_ALARM"
const val COLLISION_MAINTENANCE_ACTION = "dev.retza.mak.action.COLLISION_MAINTENANCE"

const val EXTRA_NOTIFICATION_ID = "collision_notification_id"
const val EXTRA_KIND = "collision_notification_kind"
const val EXTRA_DATE = "collision_notification_date"
const val EXTRA_OCCURRENCE_IDS = "collision_notification_occurrence_ids"

fun CollisionNotificationPreferences.toPlannerSettings(): CollisionNotificationSettings =
    CollisionNotificationSettings(
        enabled = enabled,
        eveningEnabled = eveningEnabled,
        beforeClassEnabled = beforeClassEnabled,
        eveningHour = eveningHour,
        leadMinutes = leadMinutes
    )
