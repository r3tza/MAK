package dev.retza.mak.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing

/**
 * Timeline of the start animation ("bloom", chosen 2026-09-27). The system splash screen is empty
 * (splash_empty.xml); the loading screen then pops in the seed head on the icon circle, grows five
 * petals from the centre with a slight turn and unfolds the full name from the middle, all in one
 * screen.
 */
const val INTRO_BLOOM_MILLIS = 1_600L
private const val SEED_HEAD_POP_MILLIS = 350f
private const val ICON_CIRCLE_GROW_MILLIS = 450f
private const val PETAL_FIRST_START_MILLIS = 250f
private const val PETAL_STAGGER_MILLIS = 130f
private const val PETAL_GROW_MILLIS = 800f
private const val NAME_REVEAL_MILLIS = 1_300f
private const val SEED_HEAD_PEAK_SCALE = 1.12f

// Each petal grows past its size and settles back, like the keyframes of the design.
private const val PETAL_OVERSHOOT_AT = 0.7f
private const val PETAL_START_SCALE = 0.2f
private const val PETAL_PEAK_SCALE = 1.04f
private const val PETAL_START_ROTATION = -25f
private const val PETAL_PEAK_ROTATION = 2f

/** Scale and extra turn of one petal at a moment of the bloom. */
data class PetalPose(val scale: Float, val rotation: Float)

val SettledPetal = PetalPose(scale = 1f, rotation = 0f)

/** Pose of petal [index] (0 to 4, clockwise from the top) [elapsedMillis] after the bloom starts. */
fun bloomPetalPose(elapsedMillis: Float, index: Int): PetalPose {
    val start = PETAL_FIRST_START_MILLIS + PETAL_STAGGER_MILLIS * index
    // Hidden until it starts; from then on the grown seed head covers it while it is small.
    if (elapsedMillis < start) return PetalPose(scale = 0f, rotation = PETAL_START_ROTATION)
    val progress = ((elapsedMillis - start) / PETAL_GROW_MILLIS).coerceIn(0f, 1f)
    return if (progress < PETAL_OVERSHOOT_AT) {
        val eased = FastOutSlowInEasing.transform(progress / PETAL_OVERSHOOT_AT)
        PetalPose(
            scale = lerp(PETAL_START_SCALE, PETAL_PEAK_SCALE, eased),
            rotation = lerp(PETAL_START_ROTATION, PETAL_PEAK_ROTATION, eased)
        )
    } else {
        val eased = FastOutSlowInEasing.transform((progress - PETAL_OVERSHOOT_AT) / (1f - PETAL_OVERSHOOT_AT))
        PetalPose(
            scale = lerp(PETAL_PEAK_SCALE, 1f, eased),
            rotation = lerp(PETAL_PEAK_ROTATION, 0f, eased)
        )
    }
}

/** Scale of the seed head: it pops in first, grows past its size and settles back. */
fun bloomSeedHeadScale(elapsedMillis: Float): Float {
    val progress = (elapsedMillis / SEED_HEAD_POP_MILLIS).coerceIn(0f, 1f)
    return if (progress < PETAL_OVERSHOOT_AT) {
        lerp(0f, SEED_HEAD_PEAK_SCALE, FastOutSlowInEasing.transform(progress / PETAL_OVERSHOOT_AT))
    } else {
        val eased = FastOutSlowInEasing.transform((progress - PETAL_OVERSHOOT_AT) / (1f - PETAL_OVERSHOOT_AT))
        lerp(SEED_HEAD_PEAK_SCALE, 1f, eased)
    }
}

/** Scale of the white icon circle, which grows with the seed head (visible in the dark theme). */
fun bloomIconCircleScale(elapsedMillis: Float): Float =
    FastOutSlowInEasing.transform((elapsedMillis / ICON_CIRCLE_GROW_MILLIS).coerceIn(0f, 1f))

/** Visible part of the full name, from 0 (hidden) to 1, unfolding from the middle. */
fun nameRevealFraction(elapsedMillis: Float): Float =
    FastOutSlowInEasing.transform((elapsedMillis / NAME_REVEAL_MILLIS).coerceIn(0f, 1f))

// This form returns exactly start at 0 and exactly end at 1, so settled petals have scale 1.
private fun lerp(start: Float, end: Float, fraction: Float): Float = (1f - fraction) * start + fraction * end

/** False when the user turned system animations off (animator duration scale 0). */
fun Context.areSystemAnimationsOn(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f

/**
 * The bloom plays once per process, so only a cold start shows it. A new activity in a process
 * that is already running, for example after the back gesture, starts without it.
 */
object StartupBloom {
    private var played = false

    /** True for the first caller in this process, false afterwards. */
    @Synchronized
    fun claim(): Boolean {
        if (played) return false
        played = true
        return true
    }
}
