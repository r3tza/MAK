package dev.retza.mak.export

import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.PlanBackupGateway
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanBackupServiceTest {
    private val service = PlanBackupService(
        object : PlanBackupGateway {
            override suspend fun snapshot() = BackupData(emptyList(), emptyList())

            override suspend fun replaceAll(data: BackupData): Long? = error("Import is not confirmed in these tests")
        }
    )

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
