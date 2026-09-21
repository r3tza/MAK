package dev.retza.mak.export

import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceChangeKind
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.Recurrence
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekOverrideEntity
import dev.retza.mak.data.entity.WeekOverrideScope
import dev.retza.mak.data.entity.WeekType
import java.nio.charset.StandardCharsets
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonExportCodecTest {
    @Test
    fun encodeIncludesSchemaVersion() {
        val bytes = JsonExportCodec.encode(ExportSnapshot())
        val json = bytes.toString(StandardCharsets.UTF_8)

        assertTrue(json.contains("\"schemaVersion\":${ExportSchema.VERSION}"))
        assertEquals(ExportSchema.VERSION, JsonExportCodec.decode(bytes).schemaVersion)
    }

    @Test
    fun snapshotKeepsStudyProgramsWithoutAssignments() {
        val program = StudyProgramEntity(id = 9, name = "Fizyka", color = "#ABCDEF")

        val snapshot = ExportSnapshot.from(emptyList(), listOf(program))

        assertEquals(1, snapshot.studyPrograms.size)
        assertEquals(9L, snapshot.studyPrograms.single().id)
    }

    @Test
    fun snapshotRoundTripPreservesAllSemesterData() {
        val semester = SemesterEntity(id = 1, name = "Semestr zimowy", isActive = true)
        val calendar = AcademicCalendarEntity(
            id = 2,
            semesterId = semester.id,
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2027, 2, 15),
            firstWeekType = WeekType.B
        )
        val studyProgram = StudyProgramEntity(
            id = 3,
            name = "Informatyka",
            color = "#3366FF"
        )
        val assignment = SemesterProgramEntity(
            id = 4,
            semesterId = semester.id,
            studyProgramId = studyProgram.id,
            academicCalendarId = calendar.id
        )
        val classEntity = ClassEntity(
            id = 5,
            semesterId = semester.id,
            semesterProgramId = assignment.id,
            name = "Programowanie",
            type = "wykład",
            teacherName = "Jan Kowalski",
            dayOfWeek = DayOfWeek.TUESDAY,
            startTime = LocalTime.of(10, 15),
            endTime = LocalTime.of(12, 0),
            room = "A-101",
            building = "Główny",
            group = "grupa 1",
            recurrence = Recurrence.A_WEEK,
            date = null,
            classNote = "Przynieść projektor"
        )
        val weekOverride = WeekOverrideEntity(
            id = 6,
            semesterId = semester.id,
            academicCalendarId = calendar.id,
            weekStartDate = LocalDate.of(2026, 10, 5),
            weekType = WeekType.A,
            scope = WeekOverrideScope.FROM_WEEK
        )
        val occurrenceNote = OccurrenceNoteEntity(
            id = 7,
            semesterId = semester.id,
            classId = classEntity.id,
            occurrenceDate = LocalDate.of(2026, 10, 6),
            body = "Kolokwium"
        )
        val occurrenceChange = OccurrenceChangeEntity(
            id = 8,
            semesterId = semester.id,
            classId = classEntity.id,
            originalDate = LocalDate.of(2026, 10, 13),
            kind = OccurrenceChangeKind.MODIFIED,
            targetDate = LocalDate.of(2026, 10, 14),
            newStartTime = LocalTime.of(8, 0),
            newEndTime = LocalTime.of(9, 45),
            newRoom = "B-202",
            newBuilding = "Nowy",
            newTeacherName = "Jan Kowalski",
            newNote = "Zajęcia przeniesione"
        )
        val source = ExportSnapshot.from(
            semesters = listOf(
                SemesterWithData(
                    semester = semester,
                    semesterPrograms = listOf(assignment),
                    academicCalendars = listOf(calendar),
                    studyPrograms = listOf(studyProgram),
                    classes = listOf(classEntity),
                    weekOverrides = listOf(weekOverride),
                    occurrenceNotes = listOf(occurrenceNote),
                    occurrenceChanges = listOf(occurrenceChange)
                )
            ),
            studyPrograms = listOf(studyProgram)
        )

        val restored = JsonExportCodec.decode(JsonExportCodec.encode(source))

        assertEquals(source, restored)
    }
}
