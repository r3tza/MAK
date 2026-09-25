package dev.retza.mak.di

import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemZoneClockTest {
    @Test
    fun clockFollowsDefaultTimeZoneChangedAfterCreation() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
            val clock = SystemZoneClock()

            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Pago_Pago"))

            val pagoPago = ZoneId.of("Pacific/Pago_Pago")
            assertEquals(pagoPago, clock.zone)
            assertEquals(LocalDate.now(pagoPago), LocalDate.now(clock))
        } finally {
            TimeZone.setDefault(previous)
        }
    }
}
