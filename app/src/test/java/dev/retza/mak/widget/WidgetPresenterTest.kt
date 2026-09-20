package dev.retza.mak.widget

import dev.retza.mak.domain.ActivePlanData
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.domain.ClassItem
import dev.retza.mak.domain.Course
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
    private val course = Course("course", semester.id, "Informatyka", "#137B71")

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
    fun presenterKeepsProviderOrderAndMapsConflictAndNoteIndicators() {
        val classes = listOf(
            classItem("first", LocalTime.of(9, 0), LocalTime.of(10, 0), "Stała notatka"),
            classItem("second", LocalTime.of(9, 30), LocalTime.of(10, 30)),
            classItem("third", LocalTime.of(12, 0), LocalTime.of(13, 0))
        )
        val plan = ActivePlanProvider().resolve(
            ActivePlanData(semester, classes, courses = listOf(course)),
            date
        )
        val state = WidgetPresenter().present(date, semester.name, plan) as WidgetUiState.Ready

        assertEquals(listOf("first:$date", "second:$date", "third:$date"), state.items.map { it.id })
        assertEquals("Kolizja 09:30-10:00", state.items[0].conflictLabel)
        assertEquals("Kolizja 09:30-10:00", state.items[1].conflictLabel)
        assertTrue(state.items[0].hasNote)
        assertFalse(state.items[1].hasNote)
        assertEquals("Sala niepodana", state.items[0].roomLabel)
    }

    @Test
    fun layoutPolicyClassifiesAllResponsiveSizesOnBothAxes() {
        val sizes = listOf(
            180.dp to 110.dp,
            240.dp to 110.dp,
            180.dp to 175.dp,
            240.dp to 175.dp,
            180.dp to 240.dp,
            240.dp to 240.dp
        )
        val modes = sizes.map { (width, height) -> widgetLayoutMode(width, height) }

        assertEquals(6, MakWidgetSizes.responsiveSizes.size)
        assertTrue(modes.all { it.width == WidgetWidthMode.Narrow || it.width == WidgetWidthMode.Wide })
        assertEquals(WidgetHeightMode.Compact, modes[0].height)
        assertEquals(WidgetHeightMode.Compact, modes[1].height)
        assertEquals(WidgetHeightMode.Medium, modes[2].height)
        assertEquals(WidgetHeightMode.Medium, modes[3].height)
        assertEquals(WidgetHeightMode.Large, modes[4].height)
        assertEquals(WidgetHeightMode.Large, modes[5].height)
        assertEquals(WidgetWidthMode.Narrow, modes[0].width)
        assertEquals(WidgetWidthMode.Wide, modes[1].width)
        assertEquals(WidgetWidthMode.Narrow, modes[2].width)
        assertEquals(WidgetWidthMode.Wide, modes[3].width)
        assertEquals(WidgetWidthMode.Narrow, modes[4].width)
        assertEquals(WidgetWidthMode.Wide, modes[5].width)

        assertEquals(1, widgetItemLimit(modes[0]))
        assertEquals(1, widgetItemLimit(modes[1]))
        assertEquals(2, widgetItemLimit(modes[2]))
        assertEquals(2, widgetItemLimit(modes[3]))
        assertEquals(3, widgetItemLimit(modes[4]))
        assertEquals(3, widgetItemLimit(modes[5]))
        assertEquals(28f, widgetLayoutPolicy(modes[0]).accentHeight.value, 0f)
        assertEquals(42f, widgetLayoutPolicy(modes[2]).accentHeight.value, 0f)
        assertEquals(52f, widgetLayoutPolicy(modes[4]).accentHeight.value, 0f)
        assertEquals(6f, widgetLayoutPolicy(modes[0]).headerSpacing.value, 0f)
        assertEquals(4f, widgetLayoutPolicy(modes[2]).rowSpacing.value, 0f)
        assertEquals(6f, widgetLayoutPolicy(modes[4]).rowSpacing.value, 0f)
        assertEquals(1, widgetNameMaxLines(modes[4]))
        assertEquals(2, widgetNameMaxLines(modes[5]))
        assertEquals(42, widgetNameCharacterLimit(modes[4]))
        assertEquals(56, widgetNameCharacterLimit(modes[5]))
        assertEquals(WidgetStatusMode.Hidden, widgetLayoutPolicy(modes[0]).statusMode)
        assertEquals(WidgetStatusMode.Primary, widgetLayoutPolicy(modes[2]).statusMode)
        assertEquals(WidgetStatusMode.All, widgetLayoutPolicy(modes[4]).statusMode)
        assertEquals(2f, widgetLayoutPolicy(modes[2]).overflowSpacing.value, 0f)
        assertEquals(14f, widgetLayoutPolicy(modes[4]).footerHeight.value, 0f)
        assertEquals("Jeszcze 3", widgetOverflowLabel(total = 5, visible = 2))
        assertEquals("Jeszcze 1", widgetOverflowLabel(total = 4, visible = 3))
        assertEquals(null, widgetOverflowLabel(total = 3, visible = 3))
        assertEquals(null, widgetOverflowLabel(total = 2, visible = 2))
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
            conflictLabel = "Kolizja 09:30-10:00",
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
            conflictLabel = "Kolizja 09:30-10:00",
            hasNote = true
        )

        assertEquals(
            null,
            widgetStatusLabel(
                item,
                widgetLayoutPolicy(widgetLayoutMode(180.dp, 110.dp)).statusMode
            )
        )
        assertEquals(
            "Kolizja 09:30-10:00",
            widgetStatusLabel(
                item,
                widgetLayoutPolicy(widgetLayoutMode(180.dp, 175.dp)).statusMode
            )
        )
        assertEquals(
            "Kolizja 09:30-10:00, Notatka",
            widgetStatusLabel(
                item,
                widgetLayoutPolicy(widgetLayoutMode(180.dp, 240.dp)).statusMode
            )
        )

        val multipleConflicts = item.copy(
            conflictLabel = "Kolizje: 09:30-10:00, 10:15-10:30"
        )
        assertEquals(
            "Kolizje: 09:30-10:00…, Notatka",
            widgetStatusLabel(
                multipleConflicts,
                widgetLayoutPolicy(widgetLayoutMode(240.dp, 240.dp)).statusMode
            )
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
        note: String? = null
    ) = ClassItem(
        id = id,
        semesterId = semester.id,
        name = id,
        type = "Wykład",
        courseId = course.id,
        dayOfWeek = DayOfWeek.MONDAY,
        startTime = start,
        endTime = end,
        classNote = note
    )
}
