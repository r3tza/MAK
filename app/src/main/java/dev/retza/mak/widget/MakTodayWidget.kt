package dev.retza.mak.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
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
            .background(GlanceTheme.colors.widgetBackground)
            .appWidgetBackground()
            .cornerRadius(16.dp)
            .padding(16.dp)
            .clickable(openTodayAction),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        WidgetHeader(state)
        Spacer(GlanceModifier.height(8.dp))
        when (state) {
            is WidgetUiState.NoActiveSemester -> WidgetMessage("Brak aktywnego semestru")
            is WidgetUiState.OutsideSemester -> WidgetMessage("Poza zakresem semestru")
            is WidgetUiState.EmptyDay -> {
                WidgetMessage("Brak zajęć na dziś")
            }
            is WidgetUiState.Error -> WidgetMessage(state.message)
            is WidgetUiState.Ready -> {
                val visibleItems = state.items.take(widgetItemLimit(LocalSize.current.height.value, compact))
                visibleItems.forEach { item ->
                    WidgetOccurrenceRow(item, openTodayAction, compact)
                    if (item != visibleItems.last()) {
                        Spacer(
                            GlanceModifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(GlanceTheme.colors.surfaceVariant)
                        )
                        Spacer(GlanceModifier.height(if (compact) 6.dp else 10.dp))
                    }
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
private fun WidgetHeader(state: WidgetUiState) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = state.dateLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            when (state) {
                is WidgetUiState.EmptyDay -> WidgetHeaderDetails(state.weekLabel, 0)
                is WidgetUiState.Ready -> WidgetHeaderDetails(
                    state.weekLabel,
                    state.items.size
                )
                else -> Unit
            }
        }
    }
}

@Composable
private fun WidgetHeaderDetails(weekLabel: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        WidgetWeekLabel(weekLabel)
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = widgetCountLabel(count),
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 11.sp
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun WidgetWeekLabel(label: String) {
    Box(
        modifier = GlanceModifier
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius(6.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = GlanceTheme.colors.onSecondaryContainer,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
    }
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
    openTodayAction: androidx.glance.action.Action,
    compact: Boolean
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(openTodayAction),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = GlanceModifier
                .width(4.dp)
                .height(if (compact) 34.dp else 50.dp)
                .background(parseWidgetColor(item.courseColor) ?: Color.Transparent)
                .cornerRadius(2.dp)
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = "${item.startTime}-${item.endTime}",
            modifier = GlanceModifier.width(62.dp),
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.width(6.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = truncateWidgetText(item.name, if (compact) 28 else 42),
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = if (compact) 1 else 2
            )
            Text(
                text = widgetMetadataLabel(item, compact),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp
                ),
                maxLines = 1
            )
            val statusLabel = listOfNotNull(
                item.conflictLabel?.let { truncateWidgetText(it, 30) },
                item.hasNote.takeIf { it }?.let { "Notatka" }
            ).joinToString(" · ")
            if (statusLabel.isNotBlank()) {
                Box(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.tertiaryContainer)
                        .cornerRadius(4.dp)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = TextStyle(
                            color = GlanceTheme.colors.onTertiaryContainer,
                            fontSize = 10.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun widgetMetadataLabel(item: WidgetOccurrenceUi, compact: Boolean): String =
    listOfNotNull(
        item.courseName.takeIf(String::isNotBlank),
        item.roomLabel.takeIf(String::isNotBlank),
        item.teacherName
            ?.takeIf { !compact && it.isNotBlank() }
            ?.let { truncateWidgetText(it, 28) }
    ).joinToString(" · ") { truncateWidgetText(it, if (compact) 28 else 34) }

private fun parseWidgetColor(value: String?): Color? = runCatching {
    val hex = value?.trim()?.removePrefix("#") ?: return null
    val argb = when (hex.length) {
        6 -> hex.toLong(16) or 0xFF000000L
        8 -> hex.toLong(16)
        else -> return null
    }
    Color(argb.toInt())
}.getOrNull()

