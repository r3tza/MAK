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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val polishDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("pl-PL"))

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
        label = { Text(label) },
        trailingIcon = {
            IconButton(
                onClick = {
                    hasOpened = true
                    open = true
                },
                enabled = enabled
            ) {
                Text(
                    text = "▣",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = "Wybierz datę"
                    }
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
        label = { Text(label) },
        trailingIcon = {
            IconButton(
                onClick = {
                    hasOpened = true
                    open = true
                },
                enabled = enabled
            ) {
                Text(
                    text = "◷",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = "Wybierz godzinę"
                    }
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
        Dialog(onDismissRequest = { open = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Wybierz godzinę", style = MaterialTheme.typography.titleLarge)
                    TimePicker(state = pickerState)
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

data class MakColorOption(
    val id: String,
    val label: String,
    val color: Color
)

val MakColorPaletteOptions = listOf(
    MakColorOption("teal", "Morski", Color(0xFF137B71)),
    MakColorOption("blue", "Niebieski", Color(0xFF334FCE)),
    MakColorOption("violet", "Fioletowy", Color(0xFF7256AD)),
    MakColorOption("orange", "Pomarańczowy", Color(0xFFA65724)),
    MakColorOption("red", "Czerwony", Color(0xFFAD3E46)),
    MakColorOption("indigo", "Indygo", Color(0xFF315F9F))
)

@Composable
fun MakColorPalette(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        Text("Kolor kierunku", style = MaterialTheme.typography.labelLarge)
        MakColorPaletteOptions.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
            ) {
                row.forEach { option ->
                    var focused by remember(option.id) { mutableStateOf(false) }
                    val selected = selectedColor.equals(option.color.toHex(), ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                        .background(option.color, RoundedCornerShape(MakSpacing.md))
                            .border(
                                BorderStroke(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected || focused) MaterialTheme.colorScheme.onSurface else option.color
                                ),
                                RoundedCornerShape(MakSpacing.md)
                            )
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onColorSelected(option.color.toHex()) }
                            )
                            .focusable()
                            .onFocusChanged { focused = it.isFocused }
                            .semantics {
                                contentDescription = option.label
                                this.selected = selected
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.label,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                repeat(3 - row.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
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
            symbol = "⋮",
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

private fun Color.toHex(): String =
    "#%02X%02X%02X".format(
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
