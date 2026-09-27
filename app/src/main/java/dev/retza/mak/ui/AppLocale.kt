package dev.retza.mak.ui

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** MAK is Polish only (`ARCHITECTURE.md`), including texts of Material 3 components. */
val AppLocale: Locale = Locale.forLanguageTag("pl-PL")

/**
 * Returns this context with Polish resources, so pickers, their headlines and accessibility labels do
 * not switch to the phone language.
 */
fun Context.withAppLocale(): Context {
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(AppLocale)
    return createConfigurationContext(configuration)
}
