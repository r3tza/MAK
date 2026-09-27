package dev.retza.mak.update

import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun returnsAvailableForNewerValidVersion() = runTest {
        val source = UpdateSource { validUpdateBytes() }

        val result = UpdateChecker(source, installedVersionCode = 100, deviceSdk = 36).check()

        assertTrue(result is UpdateCheckResult.Checked)
        assertTrue((result as UpdateCheckResult.Checked).availability is UpdateAvailability.Available)
    }

    @Test
    fun mapsInvalidAndIoFailure() = runTest {
        val invalid = UpdateChecker(UpdateSource { "bad".encodeToByteArray() }, 100, 36).check()
        val failed = UpdateChecker(UpdateSource { throw IOException("offline") }, 100, 36).check()

        assertEquals(UpdateCheckResult.InvalidFile, invalid)
        assertEquals(UpdateCheckResult.NetworkError, failed)
    }

    @Test
    fun propagatesCancellation() = runTest {
        val checker = UpdateChecker(UpdateSource { throw CancellationException("cancel") }, 100, 36)

        assertSuspendThrows<CancellationException> { checker.check() }
    }

    @Test
    fun httpSourceReadsResponseAndConfiguresConnection() = runTest {
        val connection = FakeHttpConnection(validUpdateBytes())
        val source = HttpUpdateSource(
            url = URL("https://github.com/update.json"),
            dispatcher = Dispatchers.Unconfined,
            openConnection = { connection }
        )

        assertTrue(source.fetch(MAX_UPDATE_JSON_BYTES).isNotEmpty())
        assertEquals("GET", connection.requestMethod)
        assertEquals(10_000, connection.connectTimeout)
        assertEquals(10_000, connection.readTimeout)
        assertTrue(connection.instanceFollowRedirects)
        assertTrue(connection.wasDisconnected)
    }

    @Test
    fun httpSourceRejectsStatusAndOversizedBody() = runTest {
        val statusSource = HttpUpdateSource(
            URL("https://github.com/update.json"),
            Dispatchers.Unconfined
        ) { FakeHttpConnection(ByteArray(0), response = 404) }
        val largeSource = HttpUpdateSource(
            URL("https://github.com/update.json"),
            Dispatchers.Unconfined
        ) { FakeHttpConnection(ByteArray(5)) }

        assertSuspendThrows<IOException> { statusSource.fetch(10) }
        assertSuspendThrows<IOException> { largeSource.fetch(4) }
    }
}

private suspend inline fun <reified T : Throwable> assertSuspendThrows(
    crossinline block: suspend () -> Unit
) {
    val thrown = try {
        block()
        null
    } catch (error: Throwable) {
        error
    }
    assertTrue("Expected ${T::class.java.simpleName}, got ${thrown?.javaClass?.simpleName}", thrown is T)
}

private fun validUpdateBytes() = """
    {
      "versionCode": 200,
      "versionName": "0.2.0",
      "apkUrl": "https://github.com/r3tza/MAK/releases/download/v0.2.0/MAK-0.2.0.apk",
      "sha256": "${"a".repeat(64)}",
      "minSdk": 31
    }
""".trimIndent().encodeToByteArray()

private class FakeHttpConnection(
    body: ByteArray,
    private val response: Int = HTTP_OK
) : HttpURLConnection(URL("https://github.com/update.json")) {
    private val bodyStream = ByteArrayInputStream(body)
    var wasDisconnected = false

    override fun getResponseCode(): Int = response
    override fun getInputStream() = bodyStream
    override fun connect() = Unit
    override fun usingProxy(): Boolean = false
    override fun disconnect() {
        wasDisconnected = true
    }
}
