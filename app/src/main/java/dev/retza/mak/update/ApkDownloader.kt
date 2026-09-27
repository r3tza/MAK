package dev.retza.mak.update

import android.content.Context
import android.os.StatFs
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import kotlin.coroutines.coroutineContext

sealed interface ApkDownloadResult {
    data class Success(val file: File) : ApkDownloadResult
    data object NotEnoughSpace : ApkDownloadResult
    data object NetworkError : ApkDownloadResult
}

fun interface ApkDownloader {
    suspend fun download(info: UpdateInfo, onProgress: (Long, Long?) -> Unit): ApkDownloadResult
}

@Single(binds = [ApkDownloader::class])
class HttpApkDownloader(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ApkDownloader {
    override suspend fun download(
        info: UpdateInfo,
        onProgress: (Long, Long?) -> Unit
    ): ApkDownloadResult = withContext(dispatcher) {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        directory.listFiles()?.filter { it.extension == "part" }?.forEach(File::delete)
        val partial = File(directory, "update-${info.versionCode}.apk.part")
        val ready = File(directory, "update-${info.versionCode}.apk")
        partial.delete()
        ready.delete()
        var connection: HttpURLConnection? = null
        try {
            connection = URL(info.apkUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.requestMethod = "GET"
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext ApkDownloadResult.NetworkError
            val expected = connection.contentLengthLong.takeIf { it >= 0 }
            if (expected != null && availableBytes(directory) < expected + SPACE_RESERVE_BYTES) {
                return@withContext ApkDownloadResult.NotEnoughSpace
            }
            var total = 0L
            connection.inputStream.use { input ->
                partial.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (availableBytes(directory) < count + SPACE_RESERVE_BYTES) {
                            return@withContext ApkDownloadResult.NotEnoughSpace
                        }
                        output.write(buffer, 0, count)
                        total += count
                        onProgress(total, expected)
                    }
                }
            }
            if (!partial.renameTo(ready)) throw IOException("Cannot finalize downloaded APK")
            ApkDownloadResult.Success(ready)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            ApkDownloadResult.NetworkError
        } finally {
            connection?.disconnect()
            if (!ready.exists()) partial.delete()
        }
    }

    private fun availableBytes(directory: File): Long = StatFs(directory.path).availableBytes

    private companion object {
        const val SPACE_RESERVE_BYTES = 8L * 1024L * 1024L
    }
}
