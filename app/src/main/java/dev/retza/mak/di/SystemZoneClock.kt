package dev.retza.mak.di

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * System clock that reads the current default time zone on every use.
 *
 * `Clock.systemDefaultZone()` captures the zone once, so a process-wide singleton
 * would keep computing dates in the old zone after the user changes it.
 */
class SystemZoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()
}
