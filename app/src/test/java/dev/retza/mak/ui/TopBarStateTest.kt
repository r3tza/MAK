package dev.retza.mak.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TopBarStateTest {
    @Test
    fun leavingDestinationDoesNotClearTheNextOne() {
        val state = TopBarState()
        state.show(TopBarSpec(MakRoutes.Edit, title = "Edytuj zajęcia"))

        state.clear(MakRoutes.Occurrence)

        assertEquals("Edytuj zajęcia", state.specFor(MakRoutes.Edit)?.title)
    }

    @Test
    fun specForOtherRouteIsNull() {
        val state = TopBarState()
        state.show(TopBarSpec(MakRoutes.Edit, title = "Edytuj zajęcia"))

        assertNull(state.specFor(MakRoutes.Today))
    }
}
