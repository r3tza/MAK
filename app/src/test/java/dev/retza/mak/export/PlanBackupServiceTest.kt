package dev.retza.mak.export

import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.PlanBackupGateway
import dev.retza.mak.ui.settings.InMemorySettingsPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanBackupServiceTest {
    private val preferences = InMemorySettingsPreferences()
    private val service = PlanBackupService(
        object : PlanBackupGateway {
            override suspend fun snapshot() = BackupData(emptyList(), emptyList())

            override suspend fun replaceAll(data: BackupData): Long? = null
        },
        preferences
    )

    @Test
    fun importShowsTheProgramsHiddenOnThisPhoneAgain() = runTest {
        preferences.setStudyProgramHidden("1", hidden = true)
        val ready = service.prepareImport("""{"schemaVersion":3,"studyPrograms":[],"semesters":[]}""".toByteArray())

        service.confirmImport((ready as ImportPreparation.Ready).handle)

        assertTrue(preferences.planDisplay.first().hiddenProgramIds.isEmpty())
    }

    @Test
    fun fileFromANewerVersionAsksToUpdateTheApp() {
        val newer = """{"schemaVersion":4,"studyPrograms":[],"semesters":[],"daysOff":[]}"""

        val result = service.prepareImport(newer.toByteArray())

        assertEquals(
            ImportPreparation.Invalid(listOf("Plik pochodzi z nowszej wersji MAK. Zaktualizuj aplikację.")),
            result
        )
    }

    @Test
    fun damagedFileStillCannotBeRead() {
        val result = service.prepareImport("{".toByteArray())

        assertEquals(ImportPreparation.Invalid(listOf("Nie udało się odczytać pliku.")), result)
    }
}
