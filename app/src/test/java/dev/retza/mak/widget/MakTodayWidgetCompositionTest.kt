package dev.retza.mak.widget

import android.content.Intent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxWidth
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
                    conflicts = emptyList(),
                    hasNote = false
                )
            }
            val state = WidgetUiState.Ready(
                dateLabel = "poniedziałek, 21 września",
                weekLabel = "Tydzień A",
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
        }
}
