package dev.retza.mak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MakNavBar(
    todaySelected: Boolean,
    planSelected: Boolean,
    addSelected: Boolean,
    onToday: () -> Unit,
    onPlan: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.surface)
            .drawBehind {
                drawLine(
                    color = outline,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MakNavButton("Dzisiaj", Icons.Outlined.Today, todaySelected, onToday, Modifier.weight(1f))
        MakNavButton("Plan", Icons.Outlined.CalendarMonth, planSelected, onPlan, Modifier.weight(1f))
        // "Dodaj" opens a form, so it is an action, not a destination tab.
        MakNavButton("Dodaj", Icons.Outlined.Add, addSelected, onAdd, Modifier.weight(1f), role = Role.Button)
    }
}

@Composable
fun MakNavRail(
    todaySelected: Boolean,
    planSelected: Boolean,
    onToday: () -> Unit,
    onPlan: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = modifier
            .width(88.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .drawBehind {
                drawLine(
                    color = outline,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MakNavButton("Dzisiaj", Icons.Outlined.Today, todaySelected, onToday, Modifier.width(72.dp))
        MakNavButton("Plan", Icons.Outlined.CalendarMonth, planSelected, onPlan, Modifier.width(72.dp))
        MakNavButton("Dodaj", Icons.Outlined.Add, false, onAdd, Modifier.width(72.dp), role = Role.Button)
    }
}

@Composable
private fun MakNavButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.Tab
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                2.dp,
                if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .clickable(role = role, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics { if (role == Role.Tab) this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
