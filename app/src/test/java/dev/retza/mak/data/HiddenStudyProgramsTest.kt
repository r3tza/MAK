package dev.retza.mak.data

import dev.retza.mak.settings.InMemorySettingsPreferences
import dev.retza.mak.data.repository.StudyProgramRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HiddenStudyProgramsTest {
    @Test
    fun deletedProgramIsNoLongerHidden() = runTest(UnconfinedTestDispatcher()) {
        val preferences = InMemorySettingsPreferences()
        preferences.setStudyProgramHidden(1L, hidden = true)
        preferences.setStudyProgramHidden(2L, hidden = true)
        val programs = MutableStateFlow(listOf(program(1), program(2)))
        backgroundScope.launch { retainExistingHiddenPrograms(programs, preferences) }
        assertEquals(setOf(1L, 2L), preferences.planDisplay.first().hiddenProgramIds)

        programs.value = listOf(program(1))

        assertEquals(setOf(1L), preferences.planDisplay.first().hiddenProgramIds)
    }

    private fun program(id: Long) = StudyProgramRecord(id = id, name = "Kierunek $id", color = "#137B71")
}
