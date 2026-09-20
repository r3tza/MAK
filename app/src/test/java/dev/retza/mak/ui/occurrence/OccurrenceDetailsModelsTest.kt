package dev.retza.mak.ui.occurrence

import java.lang.reflect.Modifier
import org.junit.Assert.assertTrue
import org.junit.Test

class OccurrenceDetailsModelsTest {
    @Test
    fun uiStateDoesNotExposeDataLayerOrNavigationTypes() {
        val forbidden = listOf(
            "dev.retza.mak.data",
            "SemesterWithData",
            "Entity",
            "MakDestination"
        )
        val types = OccurrenceDetailsUiState::class.java.declaredFields
            .filter { !Modifier.isStatic(it.modifiers) }
            .map { it.type.name }
        assertTrue(
            "OccurrenceDetailsUiState exposes forbidden types: $types",
            types.none { type -> forbidden.any(type::contains) }
        )
    }
}
