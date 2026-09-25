package dev.retza.mak.export

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFileReaderTest {
    @Test
    fun fileWithinLimitIsLoadedWhole() {
        val content = ByteArray(10) { it.toByte() }

        val result = readBackupFile(ByteArrayInputStream(content), maxBytes = 10)

        assertTrue(result is BackupFileRead.Loaded)
        assertArrayEquals(content, (result as BackupFileRead.Loaded).bytes)
    }

    @Test
    fun fileAboveLimitIsRejectedWithoutReadingEverything() {
        val input = ByteArrayInputStream(ByteArray(100))

        val result = readBackupFile(input, maxBytes = 10)

        assertEquals(BackupFileRead.TooLarge, result)
        assertEquals(89, input.available())
    }
}
