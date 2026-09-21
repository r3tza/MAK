package dev.retza.mak.ui

object MakRoutes {
    const val Today = "today"
    const val Schedule = "schedule"
    const val Edit = "edit?classId={classId}&date={date}"
    const val Occurrence = "occurrence/{classId}/{date}"
    const val Semester = "semester/{semesterId}"
    const val SemesterCourses = "semester/{semesterId}/courses"
    const val SemesterOverrides = "semester/{semesterId}/week-overrides"
    const val SemesterCalendars = "semester/{semesterId}/calendars"
    const val Settings = "settings"
    const val ImportPreview = "settings/import"
    const val Setup = "setup"
}

fun occurrenceRoute(occurrenceId: String): String {
    val parts = occurrenceId.split(":", limit = 2)
    return "occurrence/${parts[0]}/${parts.getOrElse(1) { "" }}"
}

fun editRoute(classId: Long, date: String): String =
    "edit?classId=$classId&date=$date"

fun semesterRoute(id: String): String = "semester/$id"

fun semesterCoursesRoute(id: String): String = "semester/$id/courses"

fun semesterOverridesRoute(id: String): String = "semester/$id/week-overrides"

fun semesterCalendarsRoute(id: String): String = "semester/$id/calendars"

internal fun shouldCloseOccurrenceDetails(currentRoute: String?): Boolean =
    currentRoute == MakRoutes.Occurrence

internal fun shouldCloseClassEditor(currentRoute: String?): Boolean =
    currentRoute == MakRoutes.Edit

internal fun shouldCloseSemesterConfiguration(currentRoute: String?): Boolean =
    currentRoute == MakRoutes.Semester

internal fun shouldCloseImportPreview(currentRoute: String?): Boolean =
    currentRoute == MakRoutes.ImportPreview

internal fun shouldHandleSetupEffect(currentRoute: String?): Boolean =
    currentRoute == MakRoutes.Setup

internal enum class AddAction {
    None,
    Setup,
    Editor
}

internal fun addAction(hasLoadedData: Boolean, requiresSetup: Boolean): AddAction = when {
    !hasLoadedData -> AddAction.None
    requiresSetup -> AddAction.Setup
    else -> AddAction.Editor
}

internal fun titleForRoute(route: String?): String = when (route) {
    MakRoutes.Schedule -> "Plan"
    MakRoutes.Edit -> "Zajęcia"
    MakRoutes.Occurrence -> "Termin"
    MakRoutes.Semester -> "Semestr"
    MakRoutes.SemesterCourses -> "Kierunki"
    MakRoutes.SemesterOverrides -> "Korekty tygodni"
    MakRoutes.SemesterCalendars -> "Kalendarze"
    MakRoutes.Settings -> "Ustawienia"
    MakRoutes.ImportPreview -> "Import"
    MakRoutes.Setup -> "Konfiguracja"
    else -> "Dzisiaj"
}
