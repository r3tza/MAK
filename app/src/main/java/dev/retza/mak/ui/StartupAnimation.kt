package dev.retza.mak.ui

import android.content.Context
import android.provider.Settings

/**
 * Length of the "bloom" splash animation in splash_logo_animated.xml: the seed head takes 350 ms,
 * then five petals grow for 800 ms each, starting 130 ms apart from 250 ms.
 */
const val SPLASH_BLOOM_MILLIS = 1_600L

/** False when the user turned system animations off (animator duration scale 0). */
fun Context.areSystemAnimationsOn(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f

/**
 * Uptime until which the splash screen stays so the bloom can finish. The splash screen appears
 * with the process, so the time counts from the process start: a cold start waits for the rest of
 * the animation, while a start in a process that is already running does not wait at all.
 */
fun splashBloomEndsAt(processStartUptimeMillis: Long, animationsOn: Boolean): Long =
    if (animationsOn) processStartUptimeMillis + SPLASH_BLOOM_MILLIS else processStartUptimeMillis
