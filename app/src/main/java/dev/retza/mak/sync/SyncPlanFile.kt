package dev.retza.mak.sync

import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.export.ExportImporter
import dev.retza.mak.export.ExportSnapshot
import dev.retza.mak.export.ImportSnapshotResult
import dev.retza.mak.export.JsonExportCodec
import dev.retza.mak.export.NewerExportVersionException
import java.security.MessageDigest

/** The plan file on Drive: the manual JSON export with the local active semester removed. */
object SyncPlanFile {
    const val MAX_BYTES = 8 * 1024 * 1024

    fun encode(data: BackupData): ByteArray = JsonExportCodec.encode(shared(data))

    /** Validates like a manual import; a file that fails is never applied. */
    fun decode(bytes: ByteArray): BackupData {
        if (bytes.size > MAX_BYTES) throw InvalidRemotePlanException("Plik planu na Dysku jest za duży.")
        val snapshot = try {
            JsonExportCodec.decode(bytes)
        } catch (error: NewerExportVersionException) {
            // Not an invalid plan: only an update of the app lets this phone read it.
            throw error
        } catch (_: Exception) {
            throw InvalidRemotePlanException("Nie udało się odczytać pliku planu z Dysku.")
        }
        return when (val result = ExportImporter.prepare(snapshot)) {
            is ImportSnapshotResult.Ready -> result.data
            is ImportSnapshotResult.Invalid -> throw InvalidRemotePlanException(
                "Plik planu na Dysku jest niepoprawny: ${result.errors.first()}"
            )
        }
    }

    /** Equal for plans that differ only in the active semester. */
    fun fingerprint(data: BackupData): String =
        fingerprint(encode(data))

    fun fingerprint(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    fun isEmpty(data: BackupData): Boolean = data.semesters.isEmpty() && data.studyPrograms.isEmpty()

    private fun shared(data: BackupData): ExportSnapshot {
        val snapshot = ExportSnapshot.from(data)
        return snapshot.copy(semesters = snapshot.semesters.map { it.copy(isActive = false) })
    }
}

class InvalidRemotePlanException(message: String) : IllegalStateException(message)
