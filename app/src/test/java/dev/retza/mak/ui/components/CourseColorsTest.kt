package dev.retza.mak.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseColorsTest {
    // Light surfaces, dark cards and the dark screen background of the app theme.
    private val surfaces = listOf(0xFFFFFFFF, 0xFFF5F7FB, 0xFF202B40, 0xFF19243A, 0xFF10192B).map { it.toInt() }

    @Test
    fun lightnessRunsFromBlackToWhite() {
        assertEquals("#000000", courseHex(courseColorFrom(210f, 0f)))
        assertEquals("#FFFFFF", courseHex(courseColorFrom(210f, 1f)))
    }

    @Test
    fun changingHueKeepsLightness() {
        for (lightness in listOf(0.2f, 0.5f, 0.8f)) {
            for (hue in 0 until 360 step 30) {
                val color = courseColorFrom(hue.toFloat(), lightness)
                assertEquals("hue $hue lightness $lightness", lightness, hueAndLightnessOf(color).lightness, 0.01f)
            }
        }
    }

    @Test
    fun hueAndLightnessRoundTrip() {
        val color = courseColorFrom(210f, 0.4f)

        val parsed = hueAndLightnessOf(color)

        assertEquals(210f, parsed.hue, 2f)
        assertEquals(0.4f, parsed.lightness, 0.01f)
    }

    @Test
    fun greyHasNoSaturation() {
        assertEquals(0f, saturationOf(parseCourseHex("#808080")!!))
        assertTrue(saturationOf(courseColorFrom(30f, 0.5f)) > 0.6f)
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
    fun shapeAndTextColorsReachTheirContrastOnEverySurface() {
        val colors = listOf("#FFFFFF", "#000000", "#F4F0C8", "#137B71", "#334FCE", "#FFEB3B")
            .map { parseCourseHex(it)!! } +
            (0 until 360 step 15).flatMap { hue -> listOf(0.1f, 0.5f, 0.9f).map { courseColorFrom(hue.toFloat(), it) } }
        colors.forEach { color ->
            surfaces.forEach { surface ->
                val shape = contrastRatio(courseShapeColor(color, surface), surface)
                val text = contrastRatio(courseTextColor(color, surface), surface)
                assertTrue("shape ${courseHex(color)} on ${courseHex(surface)}: $shape", shape >= SHAPE_CONTRAST)
                assertTrue("text ${courseHex(color)} on ${courseHex(surface)}: $text", text >= TEXT_CONTRAST)
            }
        }
    }

    @Test
    fun adaptedColorKeepsItsHue() {
        val surfaces = listOf(0xFFFFFFFF.toInt(), 0xFF202B40.toInt())
        for (hue in 0 until 360 step 15) {
            val color = courseColorFrom(hue.toFloat(), 0.5f)
            surfaces.forEach { surface ->
                val text = courseTextColor(color, surface)
                assertTrue("hue $hue keeps its hue", hueDistanceForTest(hueAndLightnessOf(text).hue, hue.toFloat()) < 12f)
            }
        }
    }

    @Test
    fun colorWithEnoughContrastIsKept() {
        val dark = 0xFF1B3A8C.toInt()
        assertEquals(dark, courseTextColor(dark, 0xFFFFFFFF.toInt()))
        assertEquals(dark, courseShapeColor(dark, 0xFFFFFFFF.toInt()))
    }

    @Test
    fun defaultColorIsUnchangedAndReadableOnEverySurfaceWithoutAdaptation() {
        assertEquals("#199286", DefaultCourseColor)
        val color = parseCourseHex(DefaultCourseColor)!!
        surfaces.forEach { assertTrue(contrastRatio(color, it) >= SHAPE_CONTRAST) }
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
        listOf(second, third).forEach { hex ->
            surfaces.forEach { assertTrue(contrastRatio(parseCourseHex(hex)!!, it) >= SHAPE_CONTRAST) }
        }
    }

    private fun hueDistanceForTest(first: Float, second: Float): Float {
        val difference = kotlin.math.abs(first - second) % 360f
        return if (difference > 180f) 360f - difference else difference
    }
}
