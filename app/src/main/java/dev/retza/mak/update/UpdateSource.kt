package dev.retza.mak.update

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface UpdateSource {
    suspend fun fetch(maxBytes: Int): ByteArray
}

class HttpUpdateSource(
    private val url: URL,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val openConnection: (URL) -> HttpURLConnection = { target ->
        target.openConnection() as HttpURLConnection
    }
) : UpdateSource {
    override suspend fun fetch(maxBytes: Int): ByteArray = withContext(dispatcher) {
        require(maxBytes >= 0) { "maxBytes must not be negative" }
        val connection = openConnection(url)
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.instanceFollowRedirects = true
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Unexpected HTTP status: $responseCode")
            }
            readLimited(connection.inputStream, maxBytes)
        } finally {
            connection.disconnect()
        }
    }
}

private fun readLimited(input: java.io.InputStream, maxBytes: Int): ByteArray = input.use { stream ->
    val output = ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val read = stream.read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) throw IOException("Update file exceeds the size limit")
        output.write(buffer, 0, read)
    }
    output.toByteArray()
}

private const val CONNECT_TIMEOUT_MILLIS = 10_000
private const val READ_TIMEOUT_MILLIS = 10_000
