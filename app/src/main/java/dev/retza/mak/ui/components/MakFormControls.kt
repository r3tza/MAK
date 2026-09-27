package dev.retza.mak.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.foundation.layout.widthIn
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LowWindowHeight = 560.dp

/** The vertical clock does not fit a window lower than 560 dp, e.g. a split screen. */
internal fun useHorizontalTimePicker(windowHeight: Dp): Boolean = windowHeight < LowWindowHeight

private val polishDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("pl-PL"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakDatePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    isError: Boolean = false,
    enabled: Boolean = true
) {
    var open by remember { mutableStateOf(false) }
    var hasOpened by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val selectedDate = value.toLocalDateOrNull()
    val initialDate = selectedDate?.coerceToRange(minDate, maxDate)
    val interactionSource = rememberOpenOnTap(enabled) {
        hasOpened = true
        open = true
    }
    val selectableDates = remember(minDate, maxDate) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return (minDate == null || !date.isBefore(minDate)) &&
                    (maxDate == null || !date.isAfter(maxDate))
            }
        }
    }

    OutlinedTextField(
        value = selectedDate?.format(polishDateFormatter).orEmpty(),
        onValueChange = {},
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .focusRequester(focusRequester)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    hasOpened = true
                    open = true
                }
            )
            .semantics {
                onClick(label = "Wybierz datę") {
                    if (enabled) {
                        hasOpened = true
                        open = true
                        true
                    } else {
                        false
                    }
                }
            },
        enabled = enabled,
        readOnly = true,
        isError = isError,
        interactionSource = interactionSource,
        label = { Text(label) },
        trailingIcon = {
            IconButton(
                onClick = {
                    hasOpened = true
                    open = true
                },
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Outlined.Event,
                    contentDescription = "Wybierz datę",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )

    if (open) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate?.toUtcMillis(),
            selectableDates = selectableDates
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onValueChange(millis.toLocalDate().toString())
                        }
                        open = false
                    }
                ) {
                    Text("Wybierz")
                }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) {
                    Text("Anuluj")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    LaunchedFocusRestore(open = open, hasOpened = hasOpened, focusRequester = focusRequester)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakTimePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true
) {
    var open by remember { mutableStateOf(false) }
    var hasOpened by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val selectedTime = value.toLocalTimeOrNull()
    val interactionSource = rememberOpenOnTap(enabled) {
        hasOpened = true
        open = true
    }

    OutlinedTextField(
        value = selectedTime?.toString()?.take(5).orEmpty(),
        onValueChange = {},
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .focusRequester(focusRequester)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    hasOpened = true
                    open = true
                }
            )
            .semantics {
                onClick(label = "Wybierz godzinę") {
                    if (enabled) {
                        hasOpened = true
                        open = true
                        true
                    } else {
                        false
                    }
                }
            },
        enabled = enabled,
        readOnly = true,
        isError = isError,
        interactionSource = interactionSource,
        label = { Text(label) },
        trailingIcon = {
            IconButton(
                onClick = {
                    hasOpened = true
                    open = true
                },
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = "Wybierz godzinę",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )

    if (open) {
        val pickerState = rememberTimePickerState(
            initialHour = selectedTime?.hour ?: 8,
            initialMinute = selectedTime?.minute ?: 0,
            is24Hour = true
        )
        // A low window (split screen, landscape tablet) needs the side-by-side clock layout.
        val windowHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
        val horizontal = useHorizontalTimePicker(windowHeight)
        Dialog(
            onDismissRequest = { open = false },
            properties = DialogProperties(usePlatformDefaultWidth = !horizontal)
        ) {
            Surface(
                modifier = if (horizontal) Modifier.widthIn(max = 640.dp).padding(horizontal = 24.dp) else Modifier,
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Wybierz godzinę", style = MaterialTheme.typography.titleLarge)
                    TimePicker(
                        state = pickerState,
                        layoutType = if (horizontal) TimePickerLayoutType.Horizontal else TimePickerLayoutType.Vertical
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { open = false }) {
                            Text("Anuluj")
                        }
                        TextButton(
                            onClick = {
                                onValueChange("%02d:%02d".format(pickerState.hour, pickerState.minute))
                                open = false
                            }
                        ) {
                            Text("Wybierz")
                        }
                    }
                }
            }
        }
    }

    LaunchedFocusRestore(open = open, hasOpened = hasOpened, focusRequester = focusRequester)
}

@Composable
private fun LaunchedFocusRestore(
    open: Boolean,
    hasOpened: Boolean,
    focusRequester: FocusRequester
) {
    androidx.compose.runtime.LaunchedEffect(open, hasOpened) {
        if (!open && hasOpened) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
fun MakActionMenu(
    actions: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        MakIconButton(
            label = "Więcej opcji",
            icon = Icons.Outlined.MoreVert,
            onClick = { expanded = true }
        )
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            actions.forEach { (label, action) ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        action()
                    }
                )
            }
        }
    }
}

/**
 * A read-only text field consumes taps itself, so `clickable` on it never fires.
 * Opening the picker on the field's own press release makes the whole field tappable.
 */
@Composable
private fun rememberOpenOnTap(enabled: Boolean, onTap: () -> Unit): MutableInteractionSource {
    val interactionSource = remember { MutableInteractionSource() }
    val currentOnTap by rememberUpdatedState(onTap)
    LaunchedEffect(interactionSource, enabled) {
        interactionSource.interactions.collect { interaction ->
            if (enabled && interaction is PressInteraction.Release) currentOnTap()
        }
    }
    return interactionSource
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

private fun String.toLocalTimeOrNull(): LocalTime? = runCatching { LocalTime.parse(this) }.getOrNull()

private fun LocalDate.coerceToRange(minDate: LocalDate?, maxDate: LocalDate?): LocalDate {
    var result = this
    if (minDate != null && result.isBefore(minDate)) result = minDate
    if (maxDate != null && result.isAfter(maxDate)) result = maxDate
    return result
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

