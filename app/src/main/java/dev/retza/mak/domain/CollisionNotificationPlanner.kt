package dev.retza.mak.domain

import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class CollisionNotificationKind {
    EVENING,
    BEFORE_CLASS
}

data class CollisionNotificationSettings(
    val enabled: Boolean,
    val eveningEnabled: Boolean,
    val beforeClassEnabled: Boolean,
    val eveningHour: LocalTime = LocalTime.of(20, 0),
    val leadMinutes: Long = 30,
    val horizonDays: Long = 14,
    val eveningWindowMinutes: Long = 15,
    val beforeClassWindowMinutes: Long = 15
)

data class CollisionNotificationGroup(
    val occurrenceIds: List<String>,
    val earliestStart: LocalTime,
    val overlapStart: LocalTime,
    val overlapEnd: LocalTime
)

data class PlannedCollisionNotification(
    val id: Int,
    val kind: CollisionNotificationKind,
    val date: LocalDate,
    val triggerFrom: LocalDateTime,
    val triggerUntil: LocalDateTime,
    val groups: List<CollisionNotificationGroup>
)

data class CollisionAlarmPayload(
    val notificationId: Int,
    val kind: CollisionNotificationKind,
    val date: LocalDate,
    val occurrenceIds: List<String>
)

fun PlannedCollisionNotification.toAlarmPayload(): CollisionAlarmPayload =
    CollisionAlarmPayload(
        notificationId = id,
        kind = kind,
        date = date,
        occurrenceIds = groups.flatMap { it.occurrenceIds }
    )

fun shouldShowCollisionNotification(
    kind: CollisionNotificationKind,
    date: LocalDate,
    requestedOccurrenceIds: List<String>,
    currentGroups: List<CollisionNotificationGroup>,
    now: LocalDateTime,
    requestedLatestEnd: LocalTime
): Boolean {
    if (requestedOccurrenceIds.isEmpty()) return false
    return when (kind) {
        CollisionNotificationKind.EVENING -> {
            if (now.toLocalDate() != date.minusDays(1)) return false
            val currentIds = currentGroups.flatMap { it.occurrenceIds }.toSet()
            currentIds.containsAll(requestedOccurrenceIds)
        }

        CollisionNotificationKind.BEFORE_CLASS -> {
            currentGroups.any { it.occurrenceIds.containsAll(requestedOccurrenceIds) } &&
                now.isBefore(date.atTime(requestedLatestEnd))
        }
    }
}

@org.koin.core.annotation.Single
class CollisionNotificationPlanner(
    private val activePlanProvider: ActivePlanProvider,
    private val clock: Clock
) {
    fun plan(
        data: ActivePlanData,
        settings: CollisionNotificationSettings
    ): List<PlannedCollisionNotification> {
        if (!settings.enabled) return emptyList()
        val today = LocalDate.now(clock)
        val now = LocalDateTime.now(clock)
        val notifications = mutableListOf<PlannedCollisionNotification>()
        for (offset in 0 until settings.horizonDays) {
            val date = today.plusDays(offset)
            val groups = collisionNotificationGroups(activePlanProvider.resolve(data, date).collisions)
            if (groups.isEmpty()) continue
            if (settings.eveningEnabled) {
                val from = date.minusDays(1).atTime(settings.eveningHour)
                val until = from.plusMinutes(settings.eveningWindowMinutes)
                if (until.isAfter(now)) {
                    notifications += notification(CollisionNotificationKind.EVENING, date, from, until, groups)
                }
            }
            if (settings.beforeClassEnabled) {
                groups.forEach { group ->
                    val until = date.atTime(group.earliestStart).minusMinutes(settings.leadMinutes)
                    val from = until.minusMinutes(settings.beforeClassWindowMinutes)
                    if (until.isAfter(now)) {
                        notifications += notification(
                            CollisionNotificationKind.BEFORE_CLASS,
                            date,
                            from,
                            until,
                            listOf(group)
                        )
                    }
                }
            }
        }
        return notifications
    }

    private fun notification(
        kind: CollisionNotificationKind,
        date: LocalDate,
        triggerFrom: LocalDateTime,
        triggerUntil: LocalDateTime,
        groups: List<CollisionNotificationGroup>
    ): PlannedCollisionNotification = PlannedCollisionNotification(
        id = stableId(kind, date, groups.flatMap { it.occurrenceIds }),
        kind = kind,
        date = date,
        triggerFrom = triggerFrom,
        triggerUntil = triggerUntil,
        groups = groups
    )

    private fun stableId(
        kind: CollisionNotificationKind,
        date: LocalDate,
        occurrenceIds: List<String>
    ): Int {
        val key = buildString {
            append(kind.name)
            append(':')
            append(date)
            occurrenceIds.sorted().forEach {
                append(':')
                append(it)
            }
        }
        return key.hashCode() and 0x7fffffff
    }
}

fun collisionNotificationGroups(collisions: List<Collision>): List<CollisionNotificationGroup> {
    if (collisions.isEmpty()) return emptyList()
    val parent = mutableMapOf<String, String>()
    fun find(value: String): String {
        val direct = parent.getOrPut(value) { value }
        if (direct == value) return value
        val root = find(direct)
        parent[value] = root
        return root
    }

    fun union(first: String, second: String) {
        val firstRoot = find(first)
        val secondRoot = find(second)
        if (firstRoot != secondRoot) parent[firstRoot] = secondRoot
    }

    collisions.forEach { union(it.first.id, it.second.id) }
    return collisions.groupBy { find(it.first.id) }.values.map { groupCollisions ->
        val occurrences = groupCollisions
            .flatMap { listOf(it.first, it.second) }
            .distinctBy { it.id }
        CollisionNotificationGroup(
            occurrenceIds = occurrences.map { it.id }.sorted(),
            earliestStart = occurrences.minOf { it.startTime },
            overlapStart = groupCollisions.minOf { it.overlapStart },
            overlapEnd = groupCollisions.maxOf { it.overlapEnd }
        )
    }
}
