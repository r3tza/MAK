package dev.retza.mak.export

import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.CourseEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceChangeKind
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.Recurrence
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.TeacherEntity
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
    fun snapshotRoundTripPreservesAllSemesterData() {
        val semester = SemesterEntity(
            id = 1,
            name = "Semestr zimowy",
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2027, 2, 15),
            firstWeekType = WeekType.B,
            isActive = true
        )
        val course = CourseEntity(
            id = 2,
            semesterId = semester.id,
            name = "Informatyka",
            color = "#3366FF"
        )
        val teacher = TeacherEntity(
            id = 3,
            semesterId = semester.id,
            name = "Jan Kowalski"
        )
        val classEntity = ClassEntity(
            id = 4,
            semesterId = semester.id,
            name = "Programowanie",
            type = "wykład",
            courseId = course.id,
            teacherId = teacher.id,
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
            id = 5,
            semesterId = semester.id,
            weekStartDate = LocalDate.of(2026, 10, 5),
            weekType = WeekType.A,
            scope = WeekOverrideScope.FROM_WEEK
        )
        val occurrenceNote = OccurrenceNoteEntity(
            id = 6,
            semesterId = semester.id,
            classId = classEntity.id,
            occurrenceDate = LocalDate.of(2026, 10, 6),
            body = "Kolokwium"
        )
        val occurrenceChange = OccurrenceChangeEntity(
            id = 7,
            semesterId = semester.id,
            classId = classEntity.id,
            originalDate = LocalDate.of(2026, 10, 13),
            kind = OccurrenceChangeKind.MODIFIED,
            targetDate = LocalDate.of(2026, 10, 14),
            newStartTime = LocalTime.of(8, 0),
            newEndTime = LocalTime.of(9, 45),
            newRoom = "B-202",
            newBuilding = "Nowy",
            newTeacherId = teacher.id,
            newNote = "Zajęcia przeniesione"
        )
        val source = ExportSnapshot.from(
            listOf(
                SemesterWithData(
                    semester = semester,
                    courses = listOf(course),
                    teachers = listOf(teacher),
                    classes = listOf(classEntity),
                    weekOverrides = listOf(weekOverride),
                    occurrenceNotes = listOf(occurrenceNote),
                    occurrenceChanges = listOf(occurrenceChange)
                )
            )
        )

        val restored = JsonExportCodec.decode(JsonExportCodec.encode(source))

        assertEquals(source, restored)
    }
}
