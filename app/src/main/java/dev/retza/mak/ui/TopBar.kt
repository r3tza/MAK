package dev.retza.mak.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** What a destination adds to the shared top bar while it is shown. */
internal data class TopBarSpec(
    val route: String,
    val title: String? = null,
    val actions: List<Pair<String, () -> Unit>> = emptyList(),
    val onBack: (() -> Unit)? = null
)

/** Holds the spec of the destination on screen; a leaving destination clears only its own spec. */
@Stable
internal class TopBarState {
    private var spec by mutableStateOf<TopBarSpec?>(null)

    fun show(spec: TopBarSpec) {
        this.spec = spec
    }

    fun clear(route: String) {
        if (spec?.route == route) spec = null
    }

    fun specFor(route: String?): TopBarSpec? = spec?.takeIf { it.route == route }
}

/** Declares the top bar of the calling destination for as long as it stays composed. */
@Composable
internal fun DeclareTopBar(state: TopBarState, spec: TopBarSpec) {
    SideEffect { state.show(spec) }
    DisposableEffect(state, spec.route) { onDispose { state.clear(spec.route) } }
}
