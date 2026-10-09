package dev.retza.mak.domain

/** Phone settings that change which collisions the resolved plan reports. */
data class PlanDisplaySettings(
    val minimumBreakMinutes: Int,
    // Study programs hidden on this phone (`DOMAIN.md`, „Kierunki”); their classes are left out of the plan.
    val hiddenProgramIds: Set<String> = emptySet()
) {
    companion object {
        val DEFAULT = PlanDisplaySettings(minimumBreakMinutes = 0)
    }
}
