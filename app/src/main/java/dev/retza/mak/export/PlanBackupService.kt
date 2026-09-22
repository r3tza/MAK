package dev.retza.mak.export

import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.PlanBackupGateway

class ImportHandle internal constructor(internal val data: BackupData)

data class ImportSummary(
    val semesterCount: Int,
    val programCount: Int,
    val classCount: Int,
    val overrideCount: Int,
    val noteCount: Int,
    val changeCount: Int,
    val activeSemesterName: String?
)

sealed interface ImportPreparation {
    data class Ready(val summary: ImportSummary, val handle: ImportHandle) : ImportPreparation

    data class Invalid(val errors: List<String>) : ImportPreparation
}

@org.koin.core.annotation.Single
class PlanBackupService(
    private val gateway: PlanBackupGateway
) {
    suspend fun exportJson(): ByteArray =
        JsonExportCodec.encode(ExportSnapshot.from(gateway.snapshot()))

    fun prepareImport(bytes: ByteArray): ImportPreparation {
        val snapshot = try {
            JsonExportCodec.decode(bytes)
        } catch (_: Exception) {
            return ImportPreparation.Invalid(listOf("Nie udało się odczytać pliku."))
        }
        return when (val result = ExportImporter.prepare(snapshot)) {
            is ImportSnapshotResult.Invalid -> ImportPreparation.Invalid(result.errors)
            is ImportSnapshotResult.Ready ->
                ImportPreparation.Ready(result.data.toSummary(), ImportHandle(result.data))
        }
    }

    suspend fun confirmImport(handle: ImportHandle): Long? = gateway.replaceAll(handle.data)
}

private fun BackupData.toSummary(): ImportSummary = ImportSummary(
    semesterCount = semesters.size,
    programCount = studyPrograms.size,
    classCount = semesters.sumOf { it.classes.size },
    overrideCount = semesters.sumOf { it.weekOverrides.size },
    noteCount = semesters.sumOf { it.occurrenceNotes.size },
    changeCount = semesters.sumOf { it.occurrenceChanges.size },
    activeSemesterName = semesters.firstOrNull { it.semester.isActive }?.semester?.name
)
