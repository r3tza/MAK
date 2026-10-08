package dev.retza.mak.domain

/** Phone settings that change which collisions the resolved plan reports. */
data class PlanDisplaySettings(
    val minimumBreakMinutes: Int
) {
    init {
        require(minimumBreakMinutes >= 0) { "The minimum break cannot be negative." }
    }

    companion object {
        val DEFAULT = PlanDisplaySettings(minimumBreakMinutes = 0)
    }
}
