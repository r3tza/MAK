package dev.retza.mak.update

import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ApkVerifierTest {
    @Test
    fun acceptsMatchingNewerApkAndRejectsAllMismatches() = runTest {
        val file = File.createTempFile("mak-update", ".apk").apply { writeText("apk") }
        val hash = sha256(file)
        val installed = InstalledAppInfoProvider { InstalledAppInfo("0.1.0", 100, "dev.retza.mak", 36) }

        suspend fun verify(info: ApkArchiveInfo, expectedHash: String = hash) =
            AndroidApkVerifier(ApkArchiveInspector { info }, installed).verify(file, expectedHash)

        assertEquals(ApkVerificationResult.Valid, verify(ApkArchiveInfo("dev.retza.mak", 200, setOf("cert"))))
        assertEquals(ApkVerificationResult.Corrupted, verify(ApkArchiveInfo("dev.retza.mak", 200, setOf("cert")), "0".repeat(64)))
        assertEquals(ApkVerificationResult.WrongPackage, verify(ApkArchiveInfo("other", 200, setOf("cert"))))
        assertEquals(ApkVerificationResult.NotNewer, verify(ApkArchiveInfo("dev.retza.mak", 100, setOf("cert"))))
        assertEquals(ApkVerificationResult.WrongSignature, verify(ApkArchiveInfo("dev.retza.mak", 200, emptySet())))
        file.delete()
    }
}
