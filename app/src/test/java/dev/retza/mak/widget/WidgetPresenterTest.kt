package dev.retza.mak.widget

import dev.retza.mak.domain.ActivePlanData
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.domain.ClassItem
import dev.retza.mak.domain.StudyProgram
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPresenterTest {
    private val date = LocalDate.of(2026, 9, 21)
    private val semester = dev.retza.mak.domain.Semester(
        id = "semester",
        name = "Semestr",
        startDate = date,
        endDate = date.plusDays(7),
        firstWeekType = dev.retza.mak.domain.WeekType.A
    )
    private val course = StudyProgram("course", "Informatyka", "#137B71")

    @Test
    fun presenterReturnsEmptyAndOutsideSemesterStates() {
        val provider = ActivePlanProvider()
        val presenter = WidgetPresenter()
        val emptyPlan = provider.resolve(
            ActivePlanData(semester = semester, classes = emptyList(), courses = listOf(course)),
            date
        )
        val outsideDate = date.minusDays(1)
        val outsidePlan = provider.resolve(
            ActivePlanData(semester = semester, classes = emptyList(), courses = listOf(course)),
            outsideDate
        )

        assertTrue(presenter.present(date, semester.name, emptyPlan) is WidgetUiState.EmptyDay)
        assertTrue(
            presenter.present(outsideDate, semester.name, outsidePlan) is WidgetUiState.OutsideSemester
        )
    }

    @Test
    fun presenterShowsPlanWhenActiveCalendarsHaveMixedWeekTypes() {
        val calendarA = dev.retza.mak.domain.AcademicCalendar(
            id = "calendar-a",
            startDate = date,
            endDate = date.plusDays(7),
            firstWeekType = dev.retza.mak.domain.WeekType.A
        )
        val calendarB = dev.retza.mak.domain.AcademicCalendar(
            id = "calendar-b",
            startDate = date,
            endDate = date.plusDays(7),
            firstWeekType = dev.retza.mak.domain.WeekType.B
        )
        val secondCourse = StudyProgram("course-2", "Matematyka", "#2244AA")
        val programs = listOf(
            dev.retza.mak.domain.SemesterProgram("assignment-a", semester.id, course.id, calendarA.id),
            dev.retza.mak.domain.SemesterProgram("assignment-b", semester.id, secondCourse.id, calendarB.id)
        )
        val classes = listOf(
            classItem("first", LocalTime.of(9, 0), LocalTime.of(10, 0)),
            classItem("second", LocalTime.of(9, 30), LocalTime.of(10, 30), courseId = secondCourse.id)
        )
        val plan = ActivePlanProvider().resolve(
            ActivePlanData(
                semester = semester,
                classes = classes,
                courses = listOf(course, secondCourse),
                semesterPrograms = programs,
                calendars = listOf(calendarA, calendarB)
            ),
            date
        )
        assertTrue(plan.schedule.hasMixedWeekTypes)

        val state = WidgetPresenter().present(date, semester.name, plan) as WidgetUiState.Ready

        assertEquals("Różne tygodnie", state.weekLabel)
        assertEquals(2, state.items.size)
    }

    @Test
    fun presenterMapsSortedDeduplicatedConflictsToBothOccurrences() {
        val classes = listOf(
            classItem("first", LocalTime.of(9, 0), LocalTime.of(11, 0), "Stała notatka"),
            classItem("second", LocalTime.of(9, 30), LocalTime.of(10, 0)),
            classItem("third", LocalTime.of(10, 15), LocalTime.of(10, 45))
        )
        val resolved = ActivePlanProvider().resolve(
            ActivePlanData(semester, classes, courses = listOf(course)),
            date
        )
        val duplicatedPlan = resolved.copy(
            collisions = resolved.collisions + resolved.collisions.first()
        )
        val state = WidgetPresenter().present(
            date,
            semester.name,
            duplicatedPlan
        ) as WidgetUiState.Ready

        assertEquals(listOf("first:$date", "second:$date", "third:$date"), state.items.map { it.id })
        assertEquals(2, state.collisionCount)
        assertEquals(
            listOf(
                WidgetConflictUi("09:30-10:00", "second"),
                WidgetConflictUi("10:15-10:45", "third")
            ),
            state.items[0].conflicts
        )
        assertEquals(listOf(WidgetConflictUi("09:30-10:00", "first")), state.items[1].conflicts)
        assertEquals(listOf(WidgetConflictUi("10:15-10:45", "first")), state.items[2].conflicts)
        assertTrue(state.items[0].hasNote)
        assertEquals("Sala niepodana", state.items[0].roomLabel)
    }

    @Test
    fun presenterCountsThreeUniqueCollisionPairsOnce() {
        val classes = listOf(
            classItem("first", LocalTime.of(9, 0), LocalTime.of(11, 0)),
            classItem("second", LocalTime.of(9, 30), LocalTime.of(10, 30)),
            classItem("third", LocalTime.of(10, 0), LocalTime.of(11, 30))
        )
        val plan = ActivePlanProvider().resolve(
            ActivePlanData(semester, classes, courses = listOf(course)),
            date
        )

        val state = WidgetPresenter().present(date, semester.name, plan) as WidgetUiState.Ready

        assertEquals(3, state.collisionCount)
    }

    @Test
    fun layoutPolicyClassifiesResponsiveSizesAndKeepsRowsIndependentOfWidth() {
        val sizes = listOf(
            180.dp to 110.dp,
            240.dp to 110.dp,
            180.dp to 175.dp,
            240.dp to 175.dp,
            180.dp to 240.dp,
            240.dp to 240.dp,
            180.dp to 340.dp,
            240.dp to 340.dp
        )
        val modes = sizes.map { (width, height) -> widgetLayoutMode(width, height) }

        assertEquals(8, MakWidgetSizes.responsiveSizes.size)
        assertEquals(
            listOf(
                WidgetHeightMode.Compact,
                WidgetHeightMode.Compact,
                WidgetHeightMode.Medium,
                WidgetHeightMode.Medium,
                WidgetHeightMode.Large,
                WidgetHeightMode.Large,
                WidgetHeightMode.ExtraLarge,
                WidgetHeightMode.ExtraLarge
            ),
            modes.map { it.height }
        )
        assertEquals(
            listOf(
                WidgetWidthMode.Narrow,
                WidgetWidthMode.Wide,
                WidgetWidthMode.Narrow,
                WidgetWidthMode.Wide,
                WidgetWidthMode.Narrow,
                WidgetWidthMode.Wide,
                WidgetWidthMode.Narrow,
                WidgetWidthMode.Wide
            ),
            modes.map { it.width }
        )
        assertEquals(28f, widgetLayoutPolicy(modes[0]).accentHeight.value, 0f)
        assertEquals(42f, widgetLayoutPolicy(modes[2]).accentHeight.value, 0f)
        assertEquals(52f, widgetLayoutPolicy(modes[4]).accentHeight.value, 0f)
        assertEquals(4, widgetLayoutPolicy(modes[2]).rowSpacing.value.toInt())
        assertEquals(6, widgetLayoutPolicy(modes[4]).rowSpacing.value.toInt())
        assertEquals(1, widgetNameMaxLines(modes[4]))
        assertEquals(2, widgetNameMaxLines(modes[5]))
        assertEquals(2, widgetNameMaxLines(modes[7]))
        assertEquals(WidgetStatusMode.ConflictOnly, widgetLayoutPolicy(modes[0]).statusMode)
        assertEquals(WidgetStatusMode.Primary, widgetLayoutPolicy(modes[2]).statusMode)
        assertEquals(WidgetStatusMode.All, widgetLayoutPolicy(modes[4]).statusMode)
        assertEquals(WidgetStatusMode.All, widgetLayoutPolicy(modes[6]).statusMode)
        assertEquals(
            widgetLayoutPolicy(modes[2]).nameCharacterLimit,
            widgetLayoutPolicy(modes[3]).nameCharacterLimit - 8
        )
        assertEquals(
            widgetLayoutPolicy(modes[2]).metadataCharacterLimit,
            widgetLayoutPolicy(modes[3]).metadataCharacterLimit - 8
        )
    }

    @Test
    fun metadataUsesWidthToShowTeacher() {
        val item = WidgetOccurrenceUi(
            id = "class:date",
            startTime = "09:00",
            endTime = "10:00",
            name = "Algebra",
            courseName = "Matematyka",
            courseColor = "#137B71",
            roomLabel = "Sala 101",
            teacherName = "Jan Kowalski",
            conflicts = emptyList(),
            hasNote = true
        )

        assertEquals(
            "Matematyka, Sala 101",
            widgetMetadataLabel(item, widgetLayoutPolicy(widgetLayoutMode(180.dp, 175.dp)))
        )
        assertEquals(
            "Matematyka, Sala 101, Jan Kowalski",
            widgetMetadataLabel(item, widgetLayoutPolicy(widgetLayoutMode(240.dp, 175.dp)))
        )
        assertEquals(
            "Matematyka, Sala 101",
            widgetMetadataLabel(item, widgetLayoutPolicy(widgetLayoutMode(180.dp, 240.dp)))
        )
        assertEquals(
            "Matematyka, Sala 101, Jan Kowalski",
            widgetMetadataLabel(item, widgetLayoutPolicy(widgetLayoutMode(240.dp, 240.dp)))
        )
    }

    @Test
    fun statusPolicyPrioritizesConflictInMediumAndShowsBothInLarge() {
        val item = WidgetOccurrenceUi(
            id = "class:date",
            startTime = "09:00",
            endTime = "10:00",
            name = "Algebra",
            courseName = "Matematyka",
            courseColor = "#137B71",
            roomLabel = "Sala 101",
            teacherName = "Jan Kowalski",
            conflicts = listOf(WidgetConflictUi("09:30-10:00", "Inny przedmiot")),
            hasNote = true
        )

        val compactPolicy = widgetLayoutPolicy(widgetLayoutMode(180.dp, 110.dp))
        val mediumPolicy = widgetLayoutPolicy(widgetLayoutMode(180.dp, 175.dp))
        val largePolicy = widgetLayoutPolicy(widgetLayoutMode(180.dp, 240.dp))
        assertTrue(widgetShouldShowConflict(item, compactPolicy))
        assertFalse(widgetShouldShowNote(item, compactPolicy))
        assertTrue(widgetShouldShowConflict(item, mediumPolicy))
        assertFalse(widgetShouldShowNote(item, mediumPolicy))
        assertTrue(widgetShouldShowConflict(item, largePolicy))
        assertTrue(widgetShouldShowNote(item, largePolicy))
    }

    @Test
    fun conflictCountUsesCorrectPolishInflection() {
        assertEquals("1 kolizja", widgetConflictCountLabel(1))
        assertEquals("2 kolizje", widgetConflictCountLabel(2))
        assertEquals("5 kolizji", widgetConflictCountLabel(5))
    }

    @Test
    fun stableItemIdIsDerivedFromOccurrenceId() {
        assertEquals(widgetOccurrenceItemId("class:2026-09-21"), widgetOccurrenceItemId("class:2026-09-21"))
        assertTrue(
            widgetOccurrenceItemId("class:2026-09-21") !=
                widgetOccurrenceItemId("class:2026-09-22")
        )
    }

    @Test
    fun invalidColorAndLongTextAreHandledWithoutBreakingWidgetText() {
        assertNull(parseWidgetColor("#not-a-color"))
        assertNotNull(parseWidgetColor("#137B71"))
        assertEquals("abcd…", truncateWidgetText("abcdefgh", 5))
        assertEquals("short", truncateWidgetText("short", 5))
    }

    private fun classItem(
        id: String,
        start: LocalTime,
        end: LocalTime,
        note: String? = null,
        courseId: String = course.id
    ) = ClassItem(
        id = id,
        semesterId = semester.id,
        name = id,
        type = "Wykład",
        courseId = courseId,
        dayOfWeek = DayOfWeek.MONDAY,
        startTime = start,
        endTime = end,
        classNote = note
    )
}
