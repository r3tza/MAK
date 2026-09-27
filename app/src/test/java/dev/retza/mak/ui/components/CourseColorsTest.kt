package dev.retza.mak.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseColorsTest {
    // Light surfaces and dark cards of the app theme.
    private val surfaces = listOf(0xFFFFFFFF, 0xFFF5F7FB, 0xFF202B40, 0xFF19243A, 0xFF10192B).map { it.toInt() }

    @Test
    fun everyPickedColorKeepsBarContrastOnAllSurfaces() {
        for (hue in 0..360 step 15) {
            for (shade in listOf(0f, 0.5f, 1f)) {
                val color = courseColorFrom(hue.toFloat(), shade)
                surfaces.forEach { surface ->
                    val ratio = contrastRatio(color, surface)
                    assertTrue("hue $hue shade $shade on ${courseHex(surface)}: $ratio", ratio >= 3.0)
                }
                assertTrue(isReadableCourseColor(color))
            }
        }
    }

    @Test
    fun hueAndShadeRoundTrip() {
        val color = courseColorFrom(210f, 0.4f)

        val parsed = hueAndShadeOf(color)

        assertEquals(210f, parsed.hue, 2f)
        assertEquals(0.4f, parsed.shade, 0.05f)
    }

    @Test
    fun hueZeroAndFullCircleGiveSameColor() {
        assertEquals(courseColorFrom(0f, 0.5f), courseColorFrom(360f, 0.5f))
    }

    @Test
    fun hexRoundTripAndValidation() {
        val color = parseCourseHex("#137b71")

        assertEquals("#137B71", color?.let(::courseHex))
        assertEquals(color, parseCourseHex("137B71"))
        assertNull(parseCourseHex("#12345"))
        assertNull(parseCourseHex("#GG0000"))
    }

    @Test
    fun tooLightAndTooDarkColorsAreRejected() {
        assertFalse(isReadableCourseColor(parseCourseHex("#FFEB3B")!!))
        assertFalse(isReadableCourseColor(parseCourseHex("#000000")!!))
    }

    @Test
    fun suggestedColorIsDefaultWithoutUsedColors() {
        assertEquals(DefaultCourseColor, suggestedCourseColor(emptyList()))
    }

    @Test
    fun suggestedColorAvoidsHuesAlreadyUsed() {
        val first = suggestedCourseColor(emptyList())
        val second = suggestedCourseColor(listOf(first))
        val third = suggestedCourseColor(listOf(first, second))

        assertTrue(first != second && second != third && first != third)
        listOf(second, third).forEach { assertTrue(isReadableCourseColor(parseCourseHex(it)!!)) }
    }

    @Test
    fun courseTextReachesTextContrastOnBothCardSurfaces() {
        val surfaces = listOf(0xFFFFFFFF.toInt(), 0xFF202B40.toInt())
        for (hue in 0 until 360 step 15) {
            for (shade in listOf(0f, 0.5f, 1f)) {
                val color = courseColorFrom(hue.toFloat(), shade)
                surfaces.forEach { surface ->
                    val text = courseTextColor(color, surface)
                    assertTrue("hue $hue shade $shade", contrastRatio(text, surface) >= 4.5)
                    assertTrue("hue $hue keeps its hue", hueDistanceForTest(hueAndShadeOf(text).hue, hue.toFloat()) < 12f)
                }
            }
        }
    }

    @Test
    fun readableCourseTextIsKept() {
        val dark = 0xFF1B3A8C.toInt()
        assertEquals(dark, courseTextColor(dark, 0xFFFFFFFF.toInt()))
    }

    private fun hueDistanceForTest(first: Float, second: Float): Float {
        val difference = kotlin.math.abs(first - second) % 360f
        return if (difference > 180f) 360f - difference else difference
    }
}
