package dev.retza.mak.ui.settings

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
        preferences.setStudyProgramHidden("1", hidden = true)
        preferences.setStudyProgramHidden("2", hidden = true)
        val programs = MutableStateFlow(listOf(program(1), program(2)))
        backgroundScope.launch { retainExistingHiddenPrograms(programs, preferences) }
        assertEquals(setOf("1", "2"), preferences.planDisplay.first().hiddenProgramIds)

        programs.value = listOf(program(1))

        assertEquals(setOf("1"), preferences.planDisplay.first().hiddenProgramIds)
    }

    private fun program(id: Long) = StudyProgramRecord(id = id, name = "Kierunek $id", color = "#137B71")
}
