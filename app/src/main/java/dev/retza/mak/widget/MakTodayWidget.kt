package dev.retza.mak.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.retza.mak.MainActivity
import dev.retza.mak.MakApplication
import java.time.Clock

class MakTodayWidget(
    private val clock: Clock = Clock.systemDefaultZone()
) : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            MakWidgetSizes.small,
            MakWidgetSizes.large
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val application = context.applicationContext as MakApplication
        val state = WidgetPlanLoader(
            repository = application.repository,
            clock = clock
        ).load()

        provideContent {
            GlanceTheme {
                MakTodayWidgetContent(context, state)
            }
        }
    }
}
class MakTodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MakTodayWidget()
}

@Composable
private fun MakTodayWidgetContent(context: Context, state: WidgetUiState) {
    val compact = MakWidgetSizes.isCompact(LocalSize.current.width, LocalSize.current.height)
    val openTodayAction = actionStartActivity(
        Intent(context, MainActivity::class.java).putExtra(
            MainActivity.EXTRA_OPEN_TODAY,
            true
        )
    )
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(16.dp)
            .clickable(openTodayAction),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = state.dateLabel,
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.height(6.dp))
        when (state) {
            is WidgetUiState.NoActiveSemester -> WidgetMessage("Brak aktywnego semestru")
            is WidgetUiState.OutsideSemester -> WidgetMessage("Poza zakresem semestru")
            is WidgetUiState.EmptyDay -> {
                WidgetWeekLabel(state.weekLabel)
                WidgetMessage("Brak zajęć na dziś")
            }
            is WidgetUiState.Error -> WidgetMessage(state.message)
            is WidgetUiState.Ready -> {
                WidgetWeekLabel(state.weekLabel)
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = widgetCountLabel(state.items.size),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
                Spacer(GlanceModifier.height(8.dp))
                val visibleItems = state.items.take(widgetItemLimit(LocalSize.current.height.value, compact))
                visibleItems.forEach { item ->
                    WidgetOccurrenceRow(item, openTodayAction)
                    Spacer(GlanceModifier.height(6.dp))
                }
                widgetOverflowLabel(state.items.size, visibleItems.size)?.let { overflowLabel ->
                    Text(
                        text = overflowLabel,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetWeekLabel(label: String) {
    Text(
        text = label,
        style = TextStyle(
            color = GlanceTheme.colors.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    )
}

@Composable
private fun WidgetMessage(message: String) {
    Text(
        text = message,
        style = TextStyle(
            color = GlanceTheme.colors.onBackground,
            fontSize = 13.sp
        ),
        maxLines = 2
    )
}

@Composable
private fun WidgetOccurrenceRow(
    item: WidgetOccurrenceUi,
    openTodayAction: androidx.glance.action.Action
) {
    Row(
        modifier = GlanceModifier.clickable(openTodayAction),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "${item.startTime}-${item.endTime}",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.width(6.dp))
        Column {
            Text(
                text = item.name,
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Text(
                text = listOfNotNull(
                    item.courseName.takeIf(String::isNotBlank),
                    item.roomLabel,
                    item.teacherName,
                    item.conflictLabel?.let { "Kolizja" },
                    item.hasNote.takeIf { it }?.let { "Notatka" }
                ).joinToString(", "),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp
                ),
                maxLines = 1
            )
        }
    }
}

