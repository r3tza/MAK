package dev.retza.mak.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import dev.retza.mak.ui.semester.WeekOverrideScopeUi
import dev.retza.mak.ui.semester.WeekTypeUi

fun WeekTypeUi.label(): String = "Tydzień $name"

fun WeekOverrideScopeUi.label(): String = when (this) {
    WeekOverrideScopeUi.ONE_WEEK -> "Tylko ten tydzień"
    WeekOverrideScopeUi.FROM_WEEK -> "Od tego tygodnia w przyszłość"
}

@Composable
fun WeekTypeSelectField(
    label: String,
    value: WeekTypeUi,
    onSelected: (WeekTypeUi) -> Unit,
    modifier: Modifier = Modifier
) {
    MakSelectField(
        label = label,
        value = value.label(),
        options = WeekTypeUi.entries,
        onSelected = onSelected,
        modifier = modifier,
        optionLabel = { it.label() }
    )
}

@Composable
fun WeekOverrideScopeChoice(
    selected: WeekOverrideScopeUi,
    onSelected: (WeekOverrideScopeUi) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)) {
        WeekOverrideScopeUi.entries.forEach { scope ->
            MakChoiceRow(
                label = scope.label(),
                selected = selected == scope,
                onClick = { onSelected(scope) }
            )
        }
    }
}
