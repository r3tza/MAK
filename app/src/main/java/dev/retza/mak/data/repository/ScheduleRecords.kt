package dev.retza.mak.data.repository

import dev.retza.mak.domain.OccurrenceChangeKind
import dev.retza.mak.domain.Recurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class ClassRecord(
    val id: Long = 0,
    val semesterId: Long,
    val semesterProgramId: Long,
    val name: String,
    val type: String,
    val teacherName: String?,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val room: String?,
    val building: String?,
    val group: String?,
    val recurrence: Recurrence,
    val date: LocalDate?,
    val classNote: String?
)

data class OccurrenceNoteRecord(
    val id: Long = 0,
    val semesterId: Long,
    val classId: Long,
    val occurrenceDate: LocalDate,
    val body: String
)

data class OccurrenceChangeRecord(
    val id: Long = 0,
    val semesterId: Long,
    val classId: Long,
    val originalDate: LocalDate,
    val kind: OccurrenceChangeKind,
    val targetDate: LocalDate?,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val room: String?,
    val building: String?,
    val teacherName: String?,
    val note: String?
)
