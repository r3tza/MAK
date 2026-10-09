package dev.retza.mak.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import dev.retza.mak.R
import androidx.glance.layout.size
import androidx.glance.ImageProvider
import androidx.glance.Image
import androidx.glance.ColorFilter
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
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
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.retza.mak.MainActivity
import dev.retza.mak.ui.components.courseShapeColor
import dev.retza.mak.ui.theme.MakOrange
import dev.retza.mak.ui.theme.MakOrangeDark
import dev.retza.mak.ui.theme.MakOrangeSoft
import dev.retza.mak.ui.theme.MakOrangeSoftDark
import org.koin.core.context.GlobalContext

class MakTodayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(MakWidgetSizes.responsiveSizes)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val koin = GlobalContext.get()
        val state = WidgetPlanLoader(
            semesterRepository = koin.get(),
            activePlanSource = koin.get(),
            activePlanProvider = koin.get(),
            clock = koin.get()
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
    val layoutMode = widgetLayoutMode(LocalSize.current.width, LocalSize.current.height)
    val layoutPolicy = widgetLayoutPolicy(layoutMode)
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
            .padding(12.dp)
            .clickable(openTodayAction),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        WidgetHeader(state)
        Spacer(GlanceModifier.height(layoutPolicy.headerSpacing))
        when (state) {
            is WidgetUiState.NoActiveSemester -> WidgetMessage("Brak aktywnego semestru")
            is WidgetUiState.OutsideSemester -> WidgetMessage("Poza zakresem semestru")
            is WidgetUiState.EmptyDay -> {
                WidgetMessage("Brak zajęć na dziś")
            }
            is WidgetUiState.Error -> WidgetMessage(state.message)
            is WidgetUiState.Ready -> {
                WidgetOccurrenceList(
                    items = state.items,
                    openTodayAction = openTodayAction,
                    layoutPolicy = layoutPolicy,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                )
            }
        }
    }
}

@Composable
internal fun WidgetOccurrenceList(
    items: List<WidgetOccurrenceUi>,
    openTodayAction: androidx.glance.action.Action,
    layoutPolicy: WidgetLayoutPolicy,
    modifier: GlanceModifier
) {
    LazyColumn(
        modifier = modifier.semantics { testTag = "widget-list" }
    ) {
        itemsIndexed(
            items = items,
            itemId = { _, item -> widgetOccurrenceItemId(item.id) }
        ) { index, item ->
            WidgetOccurrenceListItem(
                item = item,
                openTodayAction = openTodayAction,
                layoutPolicy = layoutPolicy,
                showSeparator = index < items.lastIndex,
                separatorIndex = index
            )
        }
    }
}

@Composable
private fun WidgetOccurrenceListItem(
    item: WidgetOccurrenceUi,
    openTodayAction: androidx.glance.action.Action,
    layoutPolicy: WidgetLayoutPolicy,
    showSeparator: Boolean,
    separatorIndex: Int
) {
    Column {
        WidgetOccurrenceRow(item, openTodayAction, layoutPolicy)
        if (showSeparator) {
            Spacer(GlanceModifier.height(2.dp))
            Spacer(
                GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GlanceTheme.colors.surfaceVariant)
                    .semantics { testTag = "widget-separator-$separatorIndex" }
            )
            Spacer(GlanceModifier.height(2.dp))
        }
    }
}

@Composable
internal fun WidgetHeader(state: WidgetUiState) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .semantics { testTag = "widget-header" },
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
                is WidgetUiState.EmptyDay -> WidgetHeaderDetails(state.weekLabel, 0, 0)
                is WidgetUiState.Ready -> WidgetHeaderDetails(
                    state.weekLabel,
                    state.items.size,
                    state.collisionCount
                )
                else -> Unit
            }
        }
    }
}

@Composable
private fun WidgetHeaderDetails(weekLabel: String, count: Int, collisionCount: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        WidgetWeekLabel(weekLabel)
        Spacer(GlanceModifier.width(4.dp))
        Text(
            text = widgetCountLabel(count),
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 11.sp
            ),
            maxLines = 1
        )
        if (collisionCount > 0) {
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = widgetConflictCountLabel(collisionCount),
                style = TextStyle(
                    color = GlanceTheme.colors.error,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WidgetWeekLabel(label: String) {
    Text(
        text = "$label,",
        style = TextStyle(
            color = GlanceTheme.colors.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        ),
        maxLines = 1
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
    openTodayAction: androidx.glance.action.Action,
    layoutPolicy: WidgetLayoutPolicy
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
                .height(layoutPolicy.accentHeight)
                .then(
                    parseWidgetColor(item.courseColor)
                        ?.let { GlanceModifier.background(widgetCourseColor(it)) }
                        ?: GlanceModifier.background(Color.Transparent)
                )
                .cornerRadius(2.dp)
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        val past = item.phase == WidgetOccurrencePhase.Past
        Text(
            text = "${item.startTime}-${item.endTime}",
            modifier = GlanceModifier.width(62.dp),
            style = TextStyle(
                // The accent stays on the "Teraz" and "Następne" labels only.
                color = if (past) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onBackground,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Spacer(GlanceModifier.width(6.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = truncateWidgetText(item.name, layoutPolicy.nameCharacterLimit),
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        color = if (past) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onBackground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = layoutPolicy.nameMaxLines
                )
                if (item.phase == WidgetOccurrencePhase.Current || item.phase == WidgetOccurrencePhase.Next) {
                    Spacer(GlanceModifier.width(4.dp))
                    WidgetPhaseLabel(item.phase)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = widgetMetadataLabel(item, layoutPolicy),
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }
            if (widgetShouldShowConflict(item, layoutPolicy)) {
                WidgetConflictAlert(item)
            }
            if (widgetShouldShowNotes(item, layoutPolicy)) {
                item.classNote?.let {
                    WidgetNoteLine(
                        icon = R.drawable.ic_widget_note,
                        contentDescription = "Notatka do zajęć",
                        text = it,
                        layoutPolicy = layoutPolicy,
                        occurrenceNote = false
                    )
                }
                item.occurrenceNote?.let {
                    WidgetNoteLine(
                        icon = R.drawable.ic_widget_calendar,
                        contentDescription = "Notatka do terminu",
                        text = it,
                        layoutPolicy = layoutPolicy,
                        occurrenceNote = true
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetConflictAlert(item: WidgetOccurrenceUi) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(day = MakOrangeSoft, night = MakOrangeSoftDark))
            .cornerRadius(4.dp)
            .padding(4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = GlanceModifier
                .width(3.dp)
                .height(14.dp)
                .background(ColorProvider(day = MakOrange, night = MakOrangeDark))
        ) {}
        Spacer(GlanceModifier.width(4.dp))
        Text(
            text = item.conflictLabel.orEmpty(),
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = ColorProvider(day = MakOrange, night = MakOrangeDark),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun WidgetPhaseLabel(phase: WidgetOccurrencePhase) {
    Text(
        text = if (phase == WidgetOccurrencePhase.Current) "Teraz" else "Następne",
        style = TextStyle(
            color = GlanceTheme.colors.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        ),
        maxLines = 1
    )
}

@Composable
private fun WidgetNoteLine(
    icon: Int,
    contentDescription: String,
    text: String,
    layoutPolicy: WidgetLayoutPolicy,
    occurrenceNote: Boolean
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(top = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(12.dp),
            colorFilter = ColorFilter.tint(
                if (occurrenceNote) GlanceTheme.colors.tertiary else GlanceTheme.colors.primary
            )
        )
        Spacer(GlanceModifier.width(4.dp))
        Text(
            text = truncateWidgetText(text, layoutPolicy.noteCharacterLimit),
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontSize = 11.sp
            ),
            maxLines = 1
        )
    }
}

internal fun widgetMetadataLabel(
    item: WidgetOccurrenceUi,
    layoutPolicy: WidgetLayoutPolicy
): String {
    return listOfNotNull(
        item.courseName.takeIf(String::isNotBlank),
        item.roomLabel.takeIf(String::isNotBlank),
        item.teacherName
            ?.takeIf { layoutPolicy.includeTeacher && it.isNotBlank() }
            ?.let { truncateWidgetText(it, 28) }
    ).joinToString(", ") {
        truncateWidgetText(it, layoutPolicy.metadataCharacterLimit)
    }
}

// The widget follows the system theme, so white and the app's dark card approximate its background.
private fun widgetCourseColor(color: Color): androidx.glance.unit.ColorProvider = ColorProvider(
    day = Color(courseShapeColor(color.toArgb(), WidgetDayBackground)),
    night = Color(courseShapeColor(color.toArgb(), WidgetNightBackground))
)

private val WidgetDayBackground = 0xFFFFFFFF.toInt()
private val WidgetNightBackground = 0xFF202B40.toInt()

internal fun parseWidgetColor(value: String?): Color? = runCatching {
    val hex = value?.trim()?.removePrefix("#") ?: return null
    val argb = when (hex.length) {
        6 -> hex.toLong(16) or 0xFF000000L
        8 -> hex.toLong(16)
        else -> return null
    }
    Color(argb.toInt())
}.getOrNull()

