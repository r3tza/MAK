package dev.retza.mak.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

data class ApkArchiveInfo(
    val packageName: String,
    val versionCode: Long,
    val signerSha256: Set<String>
)

fun interface ApkArchiveInspector {
    fun inspect(file: File): ApkArchiveInfo?
}

sealed interface ApkVerificationResult {
    data object Valid : ApkVerificationResult
    data object Corrupted : ApkVerificationResult
    data object WrongPackage : ApkVerificationResult
    data object NotNewer : ApkVerificationResult
    data object WrongSignature : ApkVerificationResult
}

@Single(binds = [ApkArchiveInspector::class])
class AndroidApkArchiveInspector(private val context: Context) : ApkArchiveInspector {
    override fun inspect(file: File): ApkArchiveInfo? {
        val archive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val flags = PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
            context.packageManager.getPackageArchiveInfo(file.path, flags)
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageArchiveInfo(file.path, PackageManager.GET_SIGNING_CERTIFICATES)
        } ?: return null
        val installed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        }
        return ApkArchiveInfo(
            packageName = archive.packageName,
            versionCode = PackageInfoCompat.getLongVersionCode(archive),
            signerSha256 = archive.signingInfo?.apkContentsSigners.orEmpty().map(::certificateDigest).toSet()
        ).takeIf {
            it.signerSha256 == installed.signingInfo?.apkContentsSigners.orEmpty().map(::certificateDigest).toSet()
        } ?: ApkArchiveInfo(archive.packageName, PackageInfoCompat.getLongVersionCode(archive), emptySet())
    }

    private fun certificateDigest(signature: android.content.pm.Signature): String =
        MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).toHex()
}

fun interface ApkVerifier {
    suspend fun verify(file: File, expectedSha256: String): ApkVerificationResult
}

@Single(binds = [ApkVerifier::class])
class AndroidApkVerifier(
    private val inspector: ApkArchiveInspector,
    private val appInfoProvider: InstalledAppInfoProvider,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ApkVerifier {
    override suspend fun verify(file: File, expectedSha256: String): ApkVerificationResult = withContext(dispatcher) {
        if (sha256(file) != expectedSha256.lowercase()) return@withContext ApkVerificationResult.Corrupted
        val archive = inspector.inspect(file) ?: return@withContext ApkVerificationResult.Corrupted
        val installed = appInfoProvider.get()
        when {
            archive.packageName != installed.packageName -> ApkVerificationResult.WrongPackage
            archive.versionCode <= installed.versionCode -> ApkVerificationResult.NotNewer
            archive.signerSha256.isEmpty() -> ApkVerificationResult.WrongSignature
            else -> ApkVerificationResult.Valid
        }
    }
}

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().toHex()
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
