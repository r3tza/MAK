package dev.retza.mak.domain

/** Phone settings that change which collisions the resolved plan reports. */
data class PlanDisplaySettings(
    val minimumBreakMinutes: Int
) {
    companion object {
        val DEFAULT = PlanDisplaySettings(minimumBreakMinutes = 0)
    }
}
