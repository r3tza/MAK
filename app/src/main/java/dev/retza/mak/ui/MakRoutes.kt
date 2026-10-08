package dev.retza.mak.ui

object MakRoutes {
    const val Today = "today"
    const val Schedule = "schedule"
    const val Edit = "edit?classId={classId}&date={date}"
    const val Occurrence = "occurrence/{classId}/{date}"
    const val Semester = "semester/{semesterId}"
    const val SemesterCourses = "semester/{semesterId}/courses"
    const val SemesterCourseAdd = "semester/{semesterId}/course-new"
    const val SemesterCourseEdit = "semester/{semesterId}/course/{assignmentId}"
    const val SemesterOverrides = "semester/{semesterId}/week-overrides"
    const val SemesterCalendars = "semester/{semesterId}/calendars"
    const val Settings = "settings"
    const val SettingsSemesters = "settings/semesters"
    const val SettingsNotifications = "settings/notifications"
    const val SettingsData = "settings/data"
    const val SettingsSync = "settings/sync"
    const val SettingsAbout = "settings/about"
    const val SettingsUpdate = "settings/update"
    const val StudyPrograms = "settings/programs"
    const val StudyProgramEdit = "settings/programs/{programId}"
    const val ImportPreview = "settings/import"
    const val Setup = "setup"
    const val SetupRoute = "setup?resume={resume}"
}

fun occurrenceRoute(occurrenceId: String): String {
    val parts = occurrenceId.split(":", limit = 2)
    return "occurrence/${parts[0]}/${parts.getOrElse(1) { "" }}"
}

fun editRoute(classId: Long, date: String): String =
    "edit?classId=$classId&date=$date"

fun semesterRoute(id: String): String = "semester/$id"

fun studyProgramEditRoute(id: Long): String = "settings/programs/$id"

fun semesterCoursesRoute(id: String): String = "semester/$id/courses"

fun semesterCourseAddRoute(id: String): String = "semester/$id/course-new"

fun semesterCourseEditRoute(semesterId: String, assignmentId: String): String =
    "semester/$semesterId/course/$assignmentId"

fun semesterOverridesRoute(id: String): String = "semester/$id/week-overrides"

fun semesterCalendarsRoute(id: String): String = "semester/$id/calendars"

fun setupRoute(resumeExisting: Boolean): String = "${MakRoutes.Setup}?resume=$resumeExisting"

internal fun shouldCloseOccurrenceDetails(currentRoute: String?): Boolean =
    currentRoute == MakRoutes.Occurrence

internal fun shouldCloseClassEditor(currentRoute: String?): Boolean =
    currentRoute == MakRoutes.Edit

internal fun shouldCloseSemesterConfiguration(currentRoute: String?): Boolean =
    currentRoute == MakRoutes.Semester

internal fun shouldCloseImportPreview(currentRoute: String?): Boolean =
    currentRoute == MakRoutes.ImportPreview

internal fun shouldHandleSetupEffect(currentRoute: String?): Boolean =
    currentRoute == MakRoutes.Setup || currentRoute == MakRoutes.SetupRoute

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

internal fun titleForRoute(route: String?, editorTitle: String? = null): String = when (route) {
    MakRoutes.Schedule -> "Plan"
    MakRoutes.Edit -> editorTitle ?: "Zajęcia"
    MakRoutes.Occurrence -> "Termin"
    MakRoutes.Semester -> "Semestr"
    MakRoutes.SemesterCourses -> "Kierunki"
    MakRoutes.SemesterCourseAdd -> "Dodaj kierunek"
    MakRoutes.SemesterCourseEdit -> "Edytuj kierunek"
    MakRoutes.SemesterOverrides -> "Korekty tygodni"
    MakRoutes.SemesterCalendars -> "Kalendarze"
    MakRoutes.Settings -> "Ustawienia"
    MakRoutes.SettingsSemesters -> "Semestry"
    MakRoutes.SettingsNotifications -> "Powiadomienia"
    MakRoutes.SettingsData -> "Dane"
    MakRoutes.SettingsSync -> "Synchronizacja Google"
    MakRoutes.SettingsAbout -> "O aplikacji"
    MakRoutes.SettingsUpdate -> "Aktualizacja"
    MakRoutes.StudyPrograms -> "Kierunki"
    MakRoutes.StudyProgramEdit -> "Edytuj kierunek"
    MakRoutes.ImportPreview -> "Import"
    MakRoutes.Setup, MakRoutes.SetupRoute -> "Konfiguracja"
    else -> "Dzisiaj"
}
