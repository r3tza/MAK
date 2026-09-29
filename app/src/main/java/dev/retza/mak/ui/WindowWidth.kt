package dev.retza.mak.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class MakWidthClass { Compact, Medium, Expanded }

private val MediumWidthBreakpoint = 600.dp
private val ExpandedWidthBreakpoint = 840.dp
private const val TABLET_MIN_SMALLEST_WIDTH_DP = 600

internal fun makWidthClassFor(width: Dp): MakWidthClass = when {
    width < MediumWidthBreakpoint -> MakWidthClass.Compact
    width < ExpandedWidthBreakpoint -> MakWidthClass.Medium
    else -> MakWidthClass.Expanded
}

val LocalMakWidthClass = staticCompositionLocalOf { MakWidthClass.Compact }

internal fun shouldLockPortrait(smallestScreenWidthDp: Int): Boolean =
    smallestScreenWidthDp < TABLET_MIN_SMALLEST_WIDTH_DP

enum class MakNavigationLayout { BottomBar, Rail, None }

internal fun makNavigationLayout(widthClass: MakWidthClass, isRootRoute: Boolean): MakNavigationLayout = when {
    !isRootRoute -> MakNavigationLayout.None
    widthClass == MakWidthClass.Compact -> MakNavigationLayout.BottomBar
    else -> MakNavigationLayout.Rail
}

/** Widest screen content; wider windows center it. */
val MakContentMaxWidth = 640.dp

/** Widest content of a two-column screen. */
val MakTwoColumnMaxWidth = 1040.dp

val MakDialogMaxWidth = 560.dp
