package dev.retza.mak.widget

import android.content.Intent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxWidth
import androidx.glance.testing.unit.hasContentDescription
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasText
import org.junit.Test

class MakTodayWidgetCompositionTest {
    @Test
    fun readyContentKeepsHeaderSeparateAndRendersAllLazyItems() =
        runGlanceAppWidgetUnitTest {
            setAppWidgetSize(DpSize(240.dp, 340.dp))
            val items = listOf("first", "second", "third", "fourth").map { name ->
                WidgetOccurrenceUi(
                    id = "$name:2026-09-21",
                    startTime = "09:00",
                    endTime = "10:00",
                    name = name,
                    courseName = "Informatyka",
                    courseColor = "#137B71",
                    roomLabel = "Sala 101",
                    teacherName = "Jan Kowalski",
                    conflictLabel = null
                )
            }
            val state = WidgetUiState.Ready(
                dateLabel = "poniedziałek, 21 września",
                weekLabel = "Tydzień A",
                collisionCount = 0,
                items = items
            )
            val action = actionStartActivity(Intent("dev.retza.mak.OPEN_TODAY"))

            provideComposable {
                Column {
                    WidgetHeader(state)
                    WidgetOccurrenceList(
                        items = items,
                        openTodayAction = action,
                        layoutPolicy = widgetLayoutPolicy(
                            widgetLayoutMode(240.dp, 340.dp)
                        ),
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                }
            }

            onNode(hasTestTag("widget-header")).assertExists()
            onNode(hasTestTag("widget-list")).assertExists()
            items.forEach { item ->
                onNode(hasText(item.name)).assertExists()
            }
            onNode(hasTestTag("widget-separator-0")).assertExists()
            onNode(hasTestTag("widget-separator-1")).assertExists()
            onNode(hasTestTag("widget-separator-2")).assertExists()
            onAllNodes(hasTestTag("widget-separator-3")).assertCountEquals(0)
            onAllNodes(hasText("Jeszcze 1")).assertCountEquals(0)
            onAllNodes(hasText("kolizja")).assertCountEquals(0)
        }

    @Test
    fun headerShowsCollisionCountWhenItIsPositive() =
        runGlanceAppWidgetUnitTest {
            val state = WidgetUiState.Ready(
                dateLabel = "poniedziałek, 21 września",
                weekLabel = "Tydzień A",
                collisionCount = 1,
                items = emptyList()
            )

            provideComposable { WidgetHeader(state) }

            onNode(hasText("0 zajęć")).assertExists()
            onNode(hasText("1 kolizja")).assertExists()
        }

    @Test
    fun notesUseIconsAndContentWhilePhaseAndWeekArePlainText() =
        runGlanceAppWidgetUnitTest {
            setAppWidgetSize(DpSize(240.dp, 340.dp))
            val item = WidgetOccurrenceUi(
                id = "notes:2026-09-21",
                startTime = "09:00",
                endTime = "10:00",
                name = "Bazy danych",
                courseName = "Informatyka",
                courseColor = "#137B71",
                roomLabel = "Sala 101",
                teacherName = null,
                conflictLabel = null,
                classNote = "Przeczytać rozdział 4",
                occurrenceNote = "Przynieść materiały",
                phase = WidgetOccurrencePhase.Current
            )
            val state = WidgetUiState.Ready(
                dateLabel = "poniedziałek, 21 września",
                weekLabel = "Tydzień A",
                collisionCount = 0,
                items = listOf(item)
            )
            val action = actionStartActivity(Intent("dev.retza.mak.OPEN_TODAY"))

            provideComposable {
                Column {
                    WidgetHeader(state)
                    WidgetOccurrenceList(
                        items = listOf(item),
                        openTodayAction = action,
                        layoutPolicy = widgetLayoutPolicy(widgetLayoutMode(240.dp, 340.dp)),
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                }
            }

            onNode(hasContentDescription("Notatka do zajęć")).assertExists()
            onNode(hasText("Przeczytać rozdział 4")).assertExists()
            onNode(hasContentDescription("Notatka do terminu")).assertExists()
            onNode(hasText("Przynieść materiały")).assertExists()
            onNode(hasText("Teraz")).assertExists()
            onNode(hasText("Tydzień A,")).assertExists()
        }

    @Test
    fun collisionAlertIsOneRangeLineWithoutPartnerNames() =
        runGlanceAppWidgetUnitTest {
            setAppWidgetSize(DpSize(240.dp, 340.dp))
            val item = WidgetOccurrenceUi(
                id = "collision:2026-09-21",
                startTime = "09:00",
                endTime = "11:00",
                name = "Programowanie",
                courseName = "Informatyka",
                courseColor = "#137B71",
                roomLabel = "Sala 101",
                teacherName = null,
                conflictLabel = "Kolizje: 09:30-10:00, 10:15-10:45"
            )
            val action = actionStartActivity(Intent("dev.retza.mak.OPEN_TODAY"))

            provideComposable {
                WidgetOccurrenceList(
                    items = listOf(item),
                    openTodayAction = action,
                    layoutPolicy = widgetLayoutPolicy(widgetLayoutMode(240.dp, 340.dp)),
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }

            onNode(hasText("Kolizje: 09:30-10:00, 10:15-10:45")).assertExists()
            onAllNodes(hasText("Z:")).assertCountEquals(0)
            onAllNodes(hasText("Jeszcze")).assertCountEquals(0)
        }
}
