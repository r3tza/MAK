package dev.retza.mak.domain

/** Phone settings that change which collisions the resolved plan reports and how gaps are counted. */
data class PlanDisplaySettings(
    val minimumBreakMinutes: Int,
    val gapThresholdMinutes: Int = PlanDisplayLimits.DEFAULT_GAP_THRESHOLD_MINUTES,
    // Study programs hidden on this phone (`DOMAIN.md`, „Kierunki”); their classes are left out of the plan.
    val hiddenProgramIds: Set<String> = emptySet()
) {
    companion object {
        val DEFAULT = PlanDisplaySettings(minimumBreakMinutes = 0)
    }
}

/**
 * Choices for the two settings. A gap is longer than the threshold and a collision break is not longer
 * than the minimum break, so the smallest threshold is at least the largest minimum break and no break
 * is both (decision of 2026-10-09).
 */
object PlanDisplayLimits {
    val minimumBreakOptions = listOf(0, 5, 10, 15, 20)
    val gapThresholdOptions = listOf(20, 30, 45, 60)
    const val DEFAULT_GAP_THRESHOLD_MINUTES = 30
    const val MAX_GAP_THRESHOLD_MINUTES = 180
    val maxMinimumBreakMinutes: Int get() = minimumBreakOptions.last()
    val minGapThresholdMinutes: Int get() = gapThresholdOptions.first()

    init {
        check(minGapThresholdMinutes >= maxMinimumBreakMinutes) {
            "The smallest gap threshold must not be below the largest minimum break"
        }
    }
}
