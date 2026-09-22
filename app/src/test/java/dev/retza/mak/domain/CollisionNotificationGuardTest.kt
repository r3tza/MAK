package dev.retza.mak.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionNotificationGuardTest {
    private val date = LocalDate.of(2026, 9, 22)
    private val groupA = group("a", "b", LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0))
    private val groupB = group("c", "d", LocalTime.of(12, 0), LocalTime.of(12, 30), LocalTime.of(13, 0))

    @Test
    fun eveningShowsOnlyTheEveningBefore() {
        val requested = listOf("a", "b")

        assertTrue(
            shouldShowCollisionNotification(
                CollisionNotificationKind.EVENING,
                date,
                requested,
                listOf(groupA),
                date.minusDays(1).atTime(20, 0),
                LocalTime.of(18, 0)
            )
        )
        assertFalse(
            shouldShowCollisionNotification(
                CollisionNotificationKind.EVENING,
                date,
                requested,
                listOf(groupA),
                date.atTime(8, 0),
                LocalTime.of(18, 0)
            )
        )
    }

    @Test
    fun eveningAcceptsAllGroupsOfTheDay() {
        val requested = listOf("a", "b", "c", "d")

        assertTrue(
            shouldShowCollisionNotification(
                CollisionNotificationKind.EVENING,
                date,
                requested,
                listOf(groupA, groupB),
                date.minusDays(1).atTime(20, 0),
                LocalTime.of(18, 0)
            )
        )
        assertFalse(
            shouldShowCollisionNotification(
                CollisionNotificationKind.EVENING,
                date,
                requested,
                listOf(groupA),
                date.minusDays(1).atTime(20, 0),
                LocalTime.of(18, 0)
            )
        )
    }

    @Test
    fun beforeClassAcceptedUntilGroupEnd() {
        assertTrue(
            shouldShowCollisionNotification(
                CollisionNotificationKind.BEFORE_CLASS,
                date,
                listOf("a", "b"),
                listOf(groupA),
                date.atTime(8, 30),
                LocalTime.of(10, 0)
            )
        )
        assertFalse(
            shouldShowCollisionNotification(
                CollisionNotificationKind.BEFORE_CLASS,
                date,
                listOf("a", "b"),
                listOf(groupA),
                date.atTime(10, 30),
                LocalTime.of(10, 0)
            )
        )
    }

    @Test
    fun beforeClassRejectsIdsNotInAnyGroup() {
        assertFalse(
            shouldShowCollisionNotification(
                CollisionNotificationKind.BEFORE_CLASS,
                date,
                listOf("a", "c"),
                listOf(groupA, groupB),
                date.atTime(8, 30),
                LocalTime.of(10, 0)
            )
        )
    }

    @Test
    fun payloadCarriesAllGroupIds() {
        val item = PlannedCollisionNotification(
            id = 1,
            kind = CollisionNotificationKind.EVENING,
            date = date,
            triggerFrom = date.minusDays(1).atTime(20, 0),
            triggerUntil = date.minusDays(1).atTime(20, 15),
            groups = listOf(groupA, groupB)
        )

        val payload = item.toAlarmPayload()

        assertEquals(1, payload.notificationId)
        assertEquals(CollisionNotificationKind.EVENING, payload.kind)
        assertEquals(date, payload.date)
        assertEquals(listOf("a", "b", "c", "d"), payload.occurrenceIds)
    }

    private fun group(
        first: String,
        second: String,
        earliestStart: LocalTime,
        overlapStart: LocalTime,
        overlapEnd: LocalTime
    ) = CollisionNotificationGroup(
        occurrenceIds = listOf(first, second),
        earliestStart = earliestStart,
        overlapStart = overlapStart,
        overlapEnd = overlapEnd
    )
}
