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
    fun layoutPolicyClassifiesAllResponsiveSizesAndShowsExplicitLimits() {
        val smallMode = widgetLayoutMode(180.dp, 110.dp)
        val mediumMode = widgetLayoutMode(280.dp, 180.dp)
        val largeMode = widgetLayoutMode(360.dp, 260.dp)

        assertEquals(WidgetLayoutMode.Compact, smallMode)
        assertEquals(WidgetLayoutMode.ExpandedMedium, mediumMode)
        assertEquals(WidgetLayoutMode.ExpandedLarge, largeMode)
        assertEquals(1, widgetItemLimit(smallMode))
        assertEquals(2, widgetItemLimit(mediumMode))
        assertEquals(3, widgetItemLimit(largeMode))
        assertEquals(30f, widgetLayoutPolicy(smallMode).accentHeight.value, 0f)
        assertEquals(42f, widgetLayoutPolicy(mediumMode).accentHeight.value, 0f)
        assertEquals(52f, widgetLayoutPolicy(largeMode).accentHeight.value, 0f)
        assertEquals(1, widgetNameMaxLines(mediumMode))
        assertEquals(2, widgetNameMaxLines(largeMode))
        assertEquals(42, widgetNameCharacterLimit(mediumMode))
        assertEquals(56, widgetNameCharacterLimit(largeMode))
        assertEquals(0f, widgetLayoutPolicy(smallMode).rowSpacing.value, 0f)
        assertEquals(4f, widgetLayoutPolicy(mediumMode).rowSpacing.value, 0f)
        assertEquals(6f, widgetLayoutPolicy(largeMode).rowSpacing.value, 0f)
        assertEquals(WidgetStatusMode.Hidden, widgetLayoutPolicy(smallMode).statusMode)
        assertEquals(WidgetStatusMode.Primary, widgetLayoutPolicy(mediumMode).statusMode)
        assertEquals(WidgetStatusMode.All, widgetLayoutPolicy(largeMode).statusMode)
        assertEquals("Jeszcze 3", widgetOverflowLabel(total = 5, visible = 2))
        assertEquals(null, widgetOverflowLabel(total = 2, visible = 2))
    }

    @Test
    fun metadataUsesCommasAndOmitsTeacherOnlyInCompactMode() {
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
            widgetMetadataLabel(item, widgetLayoutPolicy(WidgetLayoutMode.Compact))
        )
        assertEquals(
            "Matematyka, Sala 101, Jan Kowalski",
            widgetMetadataLabel(item, widgetLayoutPolicy(WidgetLayoutMode.ExpandedMedium))
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
            widgetStatusLabel(item, widgetLayoutPolicy(WidgetLayoutMode.Compact).statusMode)
        )
        assertEquals(
            "Kolizja 09:30-10:00",
            widgetStatusLabel(item, widgetLayoutPolicy(WidgetLayoutMode.ExpandedMedium).statusMode)
        )
        assertEquals(
            "Kolizja 09:30-10:00, Notatka",
            widgetStatusLabel(item, widgetLayoutPolicy(WidgetLayoutMode.ExpandedLarge).statusMode)
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
