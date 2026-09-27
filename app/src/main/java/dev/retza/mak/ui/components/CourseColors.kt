package dev.retza.mak.ui.components

import kotlin.math.abs
import kotlin.math.pow

/**
 * Course colors picked by hue and shade. Every picked color keeps a WCAG contrast of at least
 * 3:1 for the course bar against the light surfaces (#FFFFFF, #F5F7FB) and the dark cards
 * (#202B40, #19243A) of the app theme, so the relative luminance is limited to
 * [MIN_COURSE_LUMINANCE, MAX_COURSE_LUMINANCE]. Colors are ARGB [Int] values, without Compose.
 */
const val MIN_COURSE_LUMINANCE = 0.18
const val MAX_COURSE_LUMINANCE = 0.27
private const val COURSE_SATURATION = 0.7

data class HueAndShade(val hue: Float, val shade: Float)

/** [hue] in degrees (0 to 360), [shade] from 0 (darkest allowed) to 1 (lightest allowed). */
fun courseColorFrom(hue: Float, shade: Float): Int {
    val target = MIN_COURSE_LUMINANCE + shade.coerceIn(0f, 1f) * (MAX_COURSE_LUMINANCE - MIN_COURSE_LUMINANCE)
    var low = 0.0
    var high = 1.0
    // Luminance grows with HSL lightness for a fixed hue and saturation.
    repeat(40) {
        val mid = (low + high) / 2
        if (relativeLuminance(hslToArgb(hue.toDouble(), COURSE_SATURATION, mid)) < target) low = mid else high = mid
    }
    return hslToArgb(hue.toDouble(), COURSE_SATURATION, (low + high) / 2)
}

fun hueAndShadeOf(color: Int): HueAndShade {
    val shade = (relativeLuminance(color) - MIN_COURSE_LUMINANCE) / (MAX_COURSE_LUMINANCE - MIN_COURSE_LUMINANCE)
    return HueAndShade(hueOf(color), shade.coerceIn(0.0, 1.0).toFloat())
}

fun isReadableCourseColor(color: Int): Boolean =
    relativeLuminance(color) in (MIN_COURSE_LUMINANCE - 0.005)..(MAX_COURSE_LUMINANCE + 0.005)

/** Parses "#RRGGBB" (the hash is optional); returns null for anything else. */
fun parseCourseHex(text: String): Int? {
    val value = text.trim().removePrefix("#")
    if (value.length != 6 || !value.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    return (0xFF000000L or value.toLong(16)).toInt()
}

fun courseHex(color: Int): String = "#%06X".format(color and 0xFFFFFF)

fun relativeLuminance(color: Int): Double {
    fun channel(shift: Int): Double {
        val value = ((color shr shift) and 0xFF) / 255.0
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}

fun contrastRatio(first: Int, second: Int): Double {
    val a = relativeLuminance(first)
    val b = relativeLuminance(second)
    return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
}

private fun hueOf(color: Int): Float {
    val r = ((color shr 16) and 0xFF) / 255.0
    val g = ((color shr 8) and 0xFF) / 255.0
    val b = (color and 0xFF) / 255.0
    val max = maxOf(r, g, b)
    val delta = max - minOf(r, g, b)
    if (delta == 0.0) return 0f
    val hue = when (max) {
        r -> 60 * (((g - b) / delta).mod(6.0))
        g -> 60 * ((b - r) / delta + 2)
        else -> 60 * ((r - g) / delta + 4)
    }
    return hue.toFloat()
}

private fun hslToArgb(hue: Double, saturation: Double, lightness: Double): Int {
    val chroma = (1 - abs(2 * lightness - 1)) * saturation
    val h = (hue.mod(360.0)) / 60
    val x = chroma * (1 - abs(h.mod(2.0) - 1))
    val (r1, g1, b1) = when {
        h < 1 -> Triple(chroma, x, 0.0)
        h < 2 -> Triple(x, chroma, 0.0)
        h < 3 -> Triple(0.0, chroma, x)
        h < 4 -> Triple(0.0, x, chroma)
        h < 5 -> Triple(x, 0.0, chroma)
        else -> Triple(chroma, 0.0, x)
    }
    val m = lightness - chroma / 2
    fun byte(value: Double) = ((value + m) * 255).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (byte(r1) shl 16) or (byte(g1) shl 8) or byte(b1)
}

/** Teal in the middle of the allowed shades; used for a new study program. */
val DefaultCourseColor: String = courseHex(courseColorFrom(174f, 0.5f))

// Hues tried in order for a further program; neighbours on the wheel are far apart.
private val SuggestedHues = listOf(174f, 30f, 220f, 330f, 100f, 270f, 0f, 60f, 190f, 300f)
private const val MIN_HUE_DISTANCE = 40f

/** First readable color whose hue is far from every color in [used]; the default when nothing is used. */
fun suggestedCourseColor(used: List<String>): String {
    val usedHues = used.mapNotNull { parseCourseHex(it) }.map { hueAndShadeOf(it).hue }
    val hue = SuggestedHues.firstOrNull { candidate ->
        usedHues.none { hueDistance(it, candidate) < MIN_HUE_DISTANCE }
    } ?: SuggestedHues[usedHues.size % SuggestedHues.size]
    return courseHex(courseColorFrom(hue, 0.5f))
}

private fun hueDistance(first: Float, second: Float): Float {
    val difference = kotlin.math.abs(first - second) % 360f
    return if (difference > 180f) 360f - difference else difference
}

private const val TEXT_CONTRAST = 4.5

/**
 * The course color for text on [surface]: same hue and saturation, darker on a light surface and
 * lighter on a dark one, until it reaches a 4.5:1 contrast. A color that already has it is kept.
 */
fun courseTextColor(color: Int, surface: Int): Int {
    if (contrastRatio(color, surface) >= TEXT_CONTRAST) return color
    val (hue, saturation, lightness) = hslOf(color)
    val darken = relativeLuminance(surface) > 0.18
    var current = lightness
    repeat(100) {
        current = (if (darken) current - 0.01 else current + 0.01).coerceIn(0.0, 1.0)
        val candidate = hslToArgb(hue, saturation, current)
        if (contrastRatio(candidate, surface) >= TEXT_CONTRAST) return candidate
    }
    return if (darken) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
}

private fun hslOf(color: Int): Triple<Double, Double, Double> {
    val r = ((color shr 16) and 0xFF) / 255.0
    val g = ((color shr 8) and 0xFF) / 255.0
    val b = (color and 0xFF) / 255.0
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val lightness = (max + min) / 2
    val delta = max - min
    val saturation = if (delta == 0.0) 0.0 else delta / (1 - abs(2 * lightness - 1))
    return Triple(hueOf(color).toDouble(), saturation.coerceIn(0.0, 1.0), lightness)
}
