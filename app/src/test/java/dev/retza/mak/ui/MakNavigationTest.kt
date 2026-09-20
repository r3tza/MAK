package dev.retza.mak.ui

import org.junit.Assert.assertEquals
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
    fun backStackRoutesMapToTheirScreenDestinations() {
        assertEquals(MakDestination.Schedule, destinationForRoute(MakRoutes.Schedule))
        assertEquals(MakDestination.EditClass, destinationForRoute(MakRoutes.Edit))
        assertEquals(MakDestination.OccurrenceDetails, destinationForRoute(MakRoutes.Occurrence))
        assertEquals(MakDestination.Semester, destinationForRoute(MakRoutes.Semester))
        assertEquals(MakDestination.Semester, destinationForRoute(MakRoutes.SemesterCourses))
        assertEquals(MakDestination.Semester, destinationForRoute(MakRoutes.SemesterOverrides))
        assertTrue(destinationMatchesRoute(MakDestination.Semester, MakRoutes.SemesterCourses))
        assertTrue(destinationMatchesRoute(MakDestination.Settings, MakRoutes.Settings))
    }
}
