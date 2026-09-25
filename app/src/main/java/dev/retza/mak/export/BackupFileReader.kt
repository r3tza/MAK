package dev.retza.mak.export

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** A MAK backup is small; anything above this is not a backup and is not loaded into memory. */
const val MAX_BACKUP_FILE_BYTES = 5 * 1024 * 1024

sealed interface BackupFileRead {
    class Loaded(val bytes: ByteArray) : BackupFileRead

    data object TooLarge : BackupFileRead
}

/** Reads at most [maxBytes] and stops as soon as the file turns out to be larger. */
fun readBackupFile(input: InputStream, maxBytes: Int = MAX_BACKUP_FILE_BYTES): BackupFileRead {
    // InputStream.readNBytes needs API 33, so read in chunks up to one byte past the limit.
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var remaining = maxBytes + 1
    while (remaining > 0) {
        val read = input.read(buffer, 0, minOf(buffer.size, remaining))
        if (read < 0) break
        output.write(buffer, 0, read)
        remaining -= read
    }
    val bytes = output.toByteArray()
    return if (bytes.size > maxBytes) BackupFileRead.TooLarge else BackupFileRead.Loaded(bytes)
}
