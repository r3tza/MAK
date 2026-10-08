package dev.retza.mak.sync

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

@Serializable
data class SyncAccount(val subject: String, val email: String, val androidAccountName: String)

/** Metadata of the single plan file; [md5] identifies its content, [modifiedAtMillis] is shown to the user. */
data class RemotePlanFile(val id: String, val md5: String, val modifiedAtMillis: Long? = null)

interface PlanFileTransport {
    suspend fun find(account: SyncAccount): RemotePlanFile?
    suspend fun download(account: SyncAccount, file: RemotePlanFile): ByteArray
    suspend fun upload(account: SyncAccount, existing: RemotePlanFile?, bytes: ByteArray): RemotePlanFile
    /** Deletes every copy of the plan file, including a duplicate left by two first uploads at once. */
    suspend fun deleteAll(account: SyncAccount)
}

interface DriveAccessTokenProvider {
    suspend fun tokenFor(account: SyncAccount): String
    suspend fun invalidate(token: String)
}

/** [reason] is the first `errors[].reason` of a Drive error body, when the body has one. */
open class DriveHttpException(
    val statusCode: Int,
    message: String,
    val reason: String? = null
) : IllegalStateException(message) {
    /**
     * Network-side failures a later run may pass; 0 marks a response that failed a local check.
     * Drive also answers 403 when a request limit is exceeded, which passes after a while.
     */
    fun isTransient(): Boolean =
        statusCode == 0 || statusCode == 429 || statusCode in 500..599 || statusCode == 403 && reason in RATE_LIMIT_REASONS

    fun isStorageFull(): Boolean = statusCode == 403 && reason == "storageQuotaExceeded"

    private companion object {
        val RATE_LIMIT_REASONS = setOf("rateLimitExceeded", "userRateLimitExceeded")
    }
}

class DriveResponseTooLargeException : DriveHttpException(0, "Google response is too large")

data class DriveHttpResponse(val status: Int, val body: ByteArray)

fun interface DriveHttpClient {
    suspend fun execute(url: String, method: String, headers: Map<String, String>, body: ByteArray?): DriveHttpResponse
}

/** Drive v3 in `appDataFolder`, which only this app can see. */
class DrivePlanTransport(
    private val http: DriveHttpClient,
    private val tokens: DriveAccessTokenProvider
) : PlanFileTransport {
    private val json = Json { ignoreUnknownKeys = true }

    // Authorization verifies the account on each token request, so a token is reused until Drive rejects it.
    // It lives only in memory.
    @Volatile
    private var cachedToken: Pair<String, String>? = null

    // Two phones creating the file at once leave two copies; the newest one is the plan.
    override suspend fun find(account: SyncAccount): RemotePlanFile? = list(account).firstOrNull()

    private suspend fun list(account: SyncAccount): List<RemotePlanFile> {
        val query = "name = '$FILE_NAME' and 'appDataFolder' in parents and trashed = false"
        val fields = "nextPageToken,files(id,md5Checksum,modifiedTime)"
        val files = mutableListOf<RemotePlanFile>()
        val seenPageTokens = mutableSetOf<String>()
        var pageToken: String? = null
        do {
            if (pageToken != null && !seenPageTokens.add(pageToken)) {
                throw DriveHttpException(0, "Google Drive repeated a page token")
            }
            val pageParameter = pageToken?.let { "&pageToken=${encode(it)}" }.orEmpty()
            val url = "$API/files?spaces=appDataFolder&orderBy=modifiedTime%20desc&pageSize=100" +
                "&q=${encode(query)}&fields=${encode(fields)}$pageParameter"
            val body = request(account, url, "GET")
            val page = readable {
                val root = json.parseToJsonElement(body.decodeToString()).jsonObject
                val pageFiles = root["files"]?.jsonArray.orEmpty().map { entry ->
                    val file = entry.jsonObject
                    RemotePlanFile(
                        id = file.getValue("id").jsonPrimitive.content,
                        md5 = file.getValue("md5Checksum").jsonPrimitive.content.lowercase(),
                        modifiedAtMillis = file.modifiedAtMillis()
                    )
                }
                pageFiles to root["nextPageToken"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotEmpty)
            }
            files += page.first
            pageToken = page.second
        }
        while (pageToken != null)
        return files
    }

    /** A body that is not the expected JSON (a proxy page, a cut response) is a network-side failure. */
    private fun <T> readable(parse: () -> T): T = try {
        parse()
    } catch (error: IllegalArgumentException) {
        throw DriveHttpException(0, "Google Drive returned an unreadable response")
    } catch (error: NoSuchElementException) {
        throw DriveHttpException(0, "Google Drive response is missing a field")
    }

    override suspend fun download(account: SyncAccount, file: RemotePlanFile): ByteArray {
        val bytes = try {
            request(account, "$API/files/${encode(file.id)}?alt=media", "GET")
        } catch (_: DriveResponseTooLargeException) {
            throw InvalidRemotePlanException("Plik planu na Dysku przekracza limit 8 MiB.")
        }
        if (md5(bytes) != file.md5) throw DriveHttpException(0, "Drive returned a file that does not match its checksum")
        return bytes
    }

    override suspend fun upload(account: SyncAccount, existing: RemotePlanFile?, bytes: ByteArray): RemotePlanFile {
        val fields = "fields=id,md5Checksum,modifiedTime"
        val response = if (existing == null) {
            val boundary = "mak-plan-${System.nanoTime()}"
            val metadata = buildJsonObject {
                put("name", FILE_NAME)
                put("parents", JsonArray(listOf(JsonPrimitive("appDataFolder"))))
            }.toString().toByteArray()
            request(
                account, "$UPLOAD/files?uploadType=multipart&$fields", "POST",
                multipart(boundary, metadata, bytes), mapOf("Content-Type" to "multipart/related; boundary=$boundary")
            )
        } else {
            // HttpURLConnection has no PATCH; Google APIs accept the override header.
            request(
                account, "$UPLOAD/files/${encode(existing.id)}?uploadType=media&$fields", "POST", bytes,
                mapOf("Content-Type" to "application/json", "X-HTTP-Method-Override" to "PATCH")
            )
        }
        val result = readable {
            val created = json.parseToJsonElement(response.decodeToString()).jsonObject
            RemotePlanFile(
                id = created.getValue("id").jsonPrimitive.content,
                md5 = created.getValue("md5Checksum").jsonPrimitive.content.lowercase(),
                modifiedAtMillis = created.modifiedAtMillis()
            )
        }
        if (result.md5 != md5(bytes)) throw DriveHttpException(0, "Drive stored different content than sent")
        return result
    }

    override suspend fun deleteAll(account: SyncAccount) {
        list(account).forEach { file ->
            val response = execute(account, "$API/files/${encode(file.id)}", "DELETE", null, emptyMap())
            if (response.status != 404) checkStatus(response)
        }
    }

    private suspend fun request(
        account: SyncAccount,
        url: String,
        method: String,
        body: ByteArray? = null,
        headers: Map<String, String> = emptyMap()
    ): ByteArray = execute(account, url, method, body, headers).also(::checkStatus).body

    /** Retries once with a fresh token after 401, never in a loop. */
    private suspend fun execute(
        account: SyncAccount,
        url: String,
        method: String,
        body: ByteArray?,
        headers: Map<String, String>
    ): DriveHttpResponse {
        var token = token(account)
        var response = http.execute(url, method, headers + ("Authorization" to "Bearer $token"), body)
        if (response.status == 401) {
            cachedToken = null
            tokens.invalidate(token)
            token = token(account)
            response = http.execute(url, method, headers + ("Authorization" to "Bearer $token"), body)
        }
        return response
    }

    private suspend fun token(account: SyncAccount): String =
        cachedToken?.takeIf { it.first == account.subject }?.second
            ?: tokens.tokenFor(account).also { cachedToken = account.subject to it }

    private fun checkStatus(response: DriveHttpResponse) {
        if (response.status in 200..299) return
        throw DriveHttpException(response.status, "Google Drive request failed (${response.status})", errorReason(response.body))
    }

    // An error body that is not Drive JSON leaves the reason unknown; the status still decides.
    private fun errorReason(body: ByteArray): String? = runCatching {
        json.parseToJsonElement(body.decodeToString()).jsonObject["error"]?.jsonObject
            ?.get("errors")?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("reason")?.jsonPrimitive?.contentOrNull
    }.getOrNull()

    private fun multipart(boundary: String, metadata: ByteArray, content: ByteArray): ByteArray =
        ByteArrayOutputStream().apply {
            write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n".toByteArray())
            write(metadata)
            write("\r\n--$boundary\r\nContent-Type: application/json\r\n\r\n".toByteArray())
            write(content)
            write("\r\n--$boundary--\r\n".toByteArray())
        }.toByteArray()

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    // Only shown to the user, so a missing or unreadable time is not an error.
    private fun JsonObject.modifiedAtMillis(): Long? =
        this["modifiedTime"]?.jsonPrimitive?.contentOrNull?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

    companion object {
        const val FILE_NAME = "mak-plan.json"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
    }
}

internal fun md5(bytes: ByteArray): String =
    MessageDigest.getInstance("MD5").digest(bytes).joinToString("") { "%02x".format(it) }

class UrlConnectionDriveHttpClient(
    private val dispatcher: CoroutineDispatcher,
    private val timeoutMillis: Int = 20_000,
    private val maxResponseBytes: Int = SyncPlanFile.MAX_BYTES
) : DriveHttpClient {
    override suspend fun execute(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: ByteArray?
    ): DriveHttpResponse = withContext(dispatcher) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            connection.requestMethod = method
            connection.setRequestProperty("Accept", "application/json")
            headers.forEach(connection::setRequestProperty)
            if (body != null) {
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            val status = connection.responseCode
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            val bytes = try {
                stream?.use { input -> readLimitedResponse(input, maxResponseBytes) } ?: ByteArray(0)
            } catch (_: DriveResponseTooLargeException) {
                if (status >= 400) throw DriveHttpException(status, "Google error response is too large")
                throw DriveResponseTooLargeException()
            }
            DriveHttpResponse(status, bytes)
        } finally {
            connection.disconnect()
        }
    }
}

internal fun readLimitedResponse(input: InputStream, maxBytes: Int): ByteArray {
    require(maxBytes >= 0)
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(minOf(8192, maxBytes.coerceAtLeast(1)))
    while (true) {
        val read = input.read(buffer)
        if (read < 0) return output.toByteArray()
        if (read > maxBytes - output.size()) throw DriveResponseTooLargeException()
        output.write(buffer, 0, read)
    }
}
