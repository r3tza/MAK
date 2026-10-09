package dev.retza.mak.ui.settings

import dev.retza.mak.data.repository.StudyProgramRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Forgets hidden study programs that no longer exist, whatever removed them: a delete on this phone or
 * a plan downloaded from Drive. A program created later with the same number must not start hidden.
 */
suspend fun retainExistingHiddenPrograms(programs: Flow<List<StudyProgramRecord>>, preferences: SettingsPreferences) {
    programs
        .map { records -> records.mapTo(mutableSetOf()) { it.id.toString() } }
        .distinctUntilChanged()
        .collect { ids -> preferences.retainHiddenStudyPrograms(ids) }
}
