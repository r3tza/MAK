package dev.retza.mak.data.database

/** Every table that holds the plan; a write to any of them changes the widget and the synced file. */
val PLAN_TABLES = arrayOf(
    "semesters",
    "study_programs",
    "academic_calendars",
    "semester_programs",
    "classes",
    "week_overrides",
    "occurrence_notes",
    "occurrence_changes"
)
