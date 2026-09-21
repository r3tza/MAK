package dev.retza.mak.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MakNavigationTest {
    @Test
    fun routeArgumentsPreserveOccurrenceAndEditorContext() {
        assertEquals("occurrence/42/2026-09-19", occurrenceRoute("42:2026-09-19"))
        assertEquals("edit?classId=42&date=2026-09-19", editRoute(42, "2026-09-19"))
        assertEquals("semester/7", semesterRoute("7"))
        assertEquals("semester/7/courses", semesterCoursesRoute("7"))
        assertEquals("semester/7/week-overrides", semesterOverridesRoute("7"))
    }

    @Test
    fun semesterSubRoutesKeepTheirSemesterIdWhenRestored() {
        val courses = semesterCoursesRoute("9")
        val overrides = semesterOverridesRoute("9")
        assertTrue(courses.startsWith("semester/9/"))
        assertTrue(overrides.startsWith("semester/9/"))
        assertTrue(courses.endsWith("/courses"))
        assertTrue(overrides.endsWith("/week-overrides"))
        assertTrue(courses != overrides)
    }

    @Test
    fun closeEffectAppliesOnlyOnOccurrenceRoute() {
        assertTrue(shouldCloseOccurrenceDetails(MakRoutes.Occurrence))
        assertFalse(shouldCloseOccurrenceDetails(MakRoutes.Today))
        assertFalse(shouldCloseOccurrenceDetails(null))
    }

    @Test
    fun semesterCloseEffectAppliesOnlyOnConfigurationRoute() {
        assertTrue(shouldCloseSemesterConfiguration(MakRoutes.Semester))
        assertFalse(shouldCloseSemesterConfiguration(MakRoutes.SemesterCourses))
        assertFalse(shouldCloseSemesterConfiguration(MakRoutes.SemesterOverrides))
        assertFalse(shouldCloseSemesterConfiguration(MakRoutes.Today))
        assertFalse(shouldCloseSemesterConfiguration(null))
    }

    @Test
    fun addActionWaitsForDataBeforeOpeningEditor() {
        assertEquals(AddAction.None, addAction(hasLoadedData = false, requiresSetup = false))
        assertEquals(AddAction.None, addAction(hasLoadedData = false, requiresSetup = true))
        assertEquals(AddAction.Setup, addAction(hasLoadedData = true, requiresSetup = true))
        assertEquals(AddAction.Editor, addAction(hasLoadedData = true, requiresSetup = false))
    }

    @Test
    fun setupEffectAppliesOnlyOnWizardRoute() {
        assertTrue(shouldHandleSetupEffect(MakRoutes.Setup))
        assertFalse(shouldHandleSetupEffect(MakRoutes.Today))
        assertFalse(shouldHandleSetupEffect(MakRoutes.Settings))
        assertFalse(shouldHandleSetupEffect(null))
        assertTrue(isSetupRoute(MakRoutes.Setup))
        assertFalse(isSetupRoute(MakRoutes.Today))
    }
}
