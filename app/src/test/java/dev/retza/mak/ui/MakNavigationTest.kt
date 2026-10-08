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
        assertEquals("setup?resume=true", setupRoute(resumeExisting = true))
        assertEquals("setup?resume=false", setupRoute(resumeExisting = false))
    }

    @Test
    fun editorRouteUsesDynamicEditorTitle() {
        assertEquals("Dodaj zajęcia", titleForRoute(MakRoutes.Edit, "Dodaj zajęcia"))
        assertEquals("Edytuj zajęcia", titleForRoute(MakRoutes.Edit, "Edytuj zajęcia"))
        assertEquals("Plan", titleForRoute(MakRoutes.Schedule, "Dodaj zajęcia"))
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
        assertTrue(shouldHandleSetupEffect(MakRoutes.SetupRoute))
        assertTrue(isSetupRoute(MakRoutes.SetupRoute))
        assertEquals("Konfiguracja", titleForRoute(MakRoutes.SetupRoute))
        assertFalse(shouldHandleSetupEffect(MakRoutes.Today))
        assertFalse(shouldHandleSetupEffect(MakRoutes.Settings))
        assertFalse(shouldHandleSetupEffect(null))
        assertTrue(isSetupRoute(MakRoutes.Setup))
        assertFalse(isSetupRoute(MakRoutes.Today))
    }
}
