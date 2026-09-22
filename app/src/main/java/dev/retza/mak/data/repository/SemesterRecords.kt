package dev.retza.mak.data.repository

import dev.retza.mak.domain.WeekOverrideScope
import dev.retza.mak.domain.WeekType
import java.time.LocalDate

data class SemesterRecord(
    val id: Long = 0,
    val name: String,
    val isActive: Boolean = false
)

data class StudyProgramRecord(
    val id: Long = 0,
    val name: String,
    val color: String
)

data class AcademicCalendarRecord(
    val id: Long = 0,
    val semesterId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val firstWeekType: WeekType
)

data class SemesterProgramRecord(
    val id: Long = 0,
    val semesterId: Long,
    val studyProgramId: Long,
    val academicCalendarId: Long
)

data class WeekOverrideRecord(
    val id: Long = 0,
    val semesterId: Long,
    val academicCalendarId: Long,
    val weekStartDate: LocalDate,
    val weekType: WeekType,
    val scope: WeekOverrideScope
)
