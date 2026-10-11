package dev.retza.mak.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import dev.retza.mak.domain.WeekOverrideScope
import dev.retza.mak.domain.WeekType

fun WeekType.label(): String = "Tydzień $name"

fun WeekOverrideScope.label(): String = when (this) {
    WeekOverrideScope.ONE_WEEK -> "Tylko ten tydzień"
    WeekOverrideScope.FROM_WEEK -> "Od tego tygodnia w przyszłość"
}

@Composable
fun WeekTypeSelectField(
    label: String,
    value: WeekType,
    onSelected: (WeekType) -> Unit,
    modifier: Modifier = Modifier
) {
    MakSelectField(
        label = label,
        value = value.label(),
        options = WeekType.entries,
        onSelected = onSelected,
        modifier = modifier,
        optionLabel = { it.label() }
    )
}

@Composable
fun WeekOverrideScopeChoice(
    selected: WeekOverrideScope,
    onSelected: (WeekOverrideScope) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)) {
        WeekOverrideScope.entries.forEach { scope ->
            MakChoiceRow(
                label = scope.label(),
                selected = selected == scope,
                onClick = { onSelected(scope) }
            )
        }
    }
}
