package dev.retza.mak.data.entity

enum class WeekType {
    A,
    B
}

enum class WeekOverrideScope {
    ONE_WEEK,
    FROM_WEEK
}

enum class Recurrence {
    EVERY_WEEK,
    A_WEEK,
    B_WEEK,
    ONCE
}

enum class OccurrenceChangeKind {
    CANCELLED,
    MODIFIED
}
