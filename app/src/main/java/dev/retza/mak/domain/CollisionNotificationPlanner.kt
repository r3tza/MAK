package dev.retza.mak.domain

import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class CollisionNotificationKind {
    EVENING,
    BEFORE_CLASS,
    GAP_EVENING,
    GAP_BEFORE_CLASS
}

/** The evening and before-class kinds of one notification topic: collisions or gaps. */
data class NotificationTopicSettings(
    val eveningEnabled: Boolean,
    val beforeClassEnabled: Boolean,
    val eveningHour: LocalTime = LocalTime.of(20, 0),
    val leadMinutes: Long = 30
)

data class CollisionNotificationSettings(
    val enabled: Boolean,
    val collisions: NotificationTopicSettings,
    val gaps: NotificationTopicSettings = NotificationTopicSettings(eveningEnabled = false, beforeClassEnabled = false),
    val horizonDays: Long = 14,
    val eveningWindowMinutes: Long = 15,
    val beforeClassWindowMinutes: Long = 15
)

data class CollisionNotificationGroup(
    val occurrenceIds: List<String>,
    val earliestStart: LocalTime,
    /** „10:00-11:30” for overlapping classes, otherwise „bez przerwy o 09:45” or „przerwa 5 min o 09:45”. */
    val label: String
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

/**
 * Checks whether a planned notification is still valid. Gap kinds take the gap groups as [currentGroups],
 * so they are checked against the current gaps the same way collision kinds are checked against collisions.
 */
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
        CollisionNotificationKind.EVENING, CollisionNotificationKind.GAP_EVENING -> {
            if (now.toLocalDate() != date.minusDays(1)) return false
            val currentIds = currentGroups.flatMap { it.occurrenceIds }.toSet()
            currentIds.containsAll(requestedOccurrenceIds)
        }

        CollisionNotificationKind.BEFORE_CLASS, CollisionNotificationKind.GAP_BEFORE_CLASS -> {
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
        inputs: ActivePlanInputs,
        settings: CollisionNotificationSettings
    ): List<PlannedCollisionNotification> {
        if (!settings.enabled) return emptyList()
        val today = LocalDate.now(clock)
        val now = LocalDateTime.now(clock)
        val notifications = mutableListOf<PlannedCollisionNotification>()
        val visible = inputs.data.visibleTo(inputs.display)
        for (offset in 0 until settings.horizonDays) {
            val date = today.plusDays(offset)
            val plan = activePlanProvider.resolve(visible, date, inputs.display)
            notifications += planTopic(
                date, collisionNotificationGroups(plan.collisions), settings.collisions, settings,
                CollisionNotificationKind.EVENING, CollisionNotificationKind.BEFORE_CLASS, now
            )
            if (settings.gaps.eveningEnabled || settings.gaps.beforeClassEnabled) {
                notifications += planTopic(
                    date, gapNotificationGroups(plan, inputs.display), settings.gaps, settings,
                    CollisionNotificationKind.GAP_EVENING, CollisionNotificationKind.GAP_BEFORE_CLASS, now
                )
            }
        }
        return notifications
    }

    private fun planTopic(
        date: LocalDate,
        groups: List<CollisionNotificationGroup>,
        topic: NotificationTopicSettings,
        settings: CollisionNotificationSettings,
        eveningKind: CollisionNotificationKind,
        beforeKind: CollisionNotificationKind,
        now: LocalDateTime
    ): List<PlannedCollisionNotification> = buildList {
        if (groups.isEmpty()) return@buildList
        if (topic.eveningEnabled) {
            val from = date.minusDays(1).atTime(topic.eveningHour)
            val until = from.plusMinutes(settings.eveningWindowMinutes)
            if (until.isAfter(now)) add(notification(eveningKind, date, from, until, groups))
        }
        if (topic.beforeClassEnabled) {
            groups.forEach { group ->
                val until = date.atTime(group.earliestStart).minusMinutes(topic.leadMinutes)
                val from = until.minusMinutes(settings.beforeClassWindowMinutes)
                if (until.isAfter(now)) add(notification(beforeKind, date, from, until, listOf(group)))
            }
        }
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
            label = groupLabel(groupCollisions)
        )
    }
}

/**
 * One group per gap: its occurrences are the classes before the gap, labelled with the gap's range.
 * Gap groups reuse [CollisionNotificationGroup] so the same alarm payload and guard apply to both kinds.
 */
fun gapNotificationGroups(
    occurrences: Collection<PlannedOccurrence>,
    gaps: List<Gap>
): List<CollisionNotificationGroup> {
    if (gaps.isEmpty()) return emptyList()
    val startById = occurrences.associate { it.id to it.startTime }
    return gaps.map { gap ->
        CollisionNotificationGroup(
            occurrenceIds = gap.beforeOccurrenceIds,
            earliestStart = gap.beforeOccurrenceIds.minOf { id ->
                checkNotNull(startById[id]) { "Gap refers to an occurrence that is not planned: $id" }
            },
            label = rangeLabel(gap.start, gap.end)
        )
    }
}

/** Gap groups of a resolved day with the user's gap threshold; the planner and the alarm receiver share it. */
fun gapNotificationGroups(plan: ActivePlan, display: PlanDisplaySettings): List<CollisionNotificationGroup> =
    gapNotificationGroups(
        plan.schedule.occurrences,
        findGaps(plan.schedule.occurrences, display.gapThresholdMinutes.toLong())
    )

private fun groupLabel(collisions: List<Collision>): String {
    val overlaps = collisions.filterIsInstance<Collision.Overlap>()
    if (overlaps.isEmpty()) {
        return breakLabel(collisions.filterIsInstance<Collision.NoBreak>().minWith(compareBy<Collision> { it.start }.thenBy { it.end }))
    }
    return rangeLabel(overlaps.minOf { it.start }, overlaps.maxOf { it.end })
}
