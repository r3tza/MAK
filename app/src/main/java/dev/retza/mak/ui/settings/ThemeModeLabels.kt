package dev.retza.mak.ui.settings

import dev.retza.mak.settings.ThemeMode

fun ThemeMode.label(): String = when (this) {
    ThemeMode.System -> "Systemowy"
    ThemeMode.Light -> "Jasny"
    ThemeMode.Dark -> "Ciemny"
}
