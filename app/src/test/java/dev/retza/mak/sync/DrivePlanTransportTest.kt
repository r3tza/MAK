package dev.retza.mak.sync

import java.io.ByteArrayInputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrivePlanTransportTest {
    private val account = SyncAccount("sub-1", "ala@example.com", "ala@example.com")
    private val tokens = CountingTokens()
    private val http = ScriptedHttp()
    private val transport = DrivePlanTransport(http, tokens)

    @Test
    fun oneTokenServesManyRequests() = runTest {
        http.respond { _, _ -> DriveHttpResponse(200, LIST_TWO_FILES) }

        transport.find(account)
        transport.find(account)

        assertEquals(1, tokens.requested)
    }

    @Test
    fun rejectedTokenIsRefreshedOnceWithoutLoop() = runTest {
        http.respond { _, headers ->
            if (headers["Authorization"] == "Bearer token-1") DriveHttpResponse(401, ByteArray(0))
            else DriveHttpResponse(200, LIST_TWO_FILES)
        }

        transport.find(account)

        assertEquals(listOf("token-1"), tokens.invalidated)
        assertEquals(2, tokens.requested)
    }

    @Test
    fun unreadableResponseIsANetworkFailureNotAnAuthorizationOne() = runTest {
        http.respond { _, _ -> DriveHttpResponse(200, "<html>Zaloguj się do sieci</html>".toByteArray()) }

        val error = runCatching { transport.find(account) }.exceptionOrNull()

        assertTrue(error is DriveHttpException)
        assertEquals(true, (error as DriveHttpException).isTransient())
    }

    @Test
    fun oversizedPlanResponseIsAnInvalidRemotePlan() = runTest {
        http.respond { _, _ -> throw DriveResponseTooLargeException() }

        val error = runCatching {
            transport.download(account, RemotePlanFile("plan", "unused"))
        }.exceptionOrNull()

        assertEquals(InvalidRemotePlanException::class, error?.let { it::class })
    }

    @Test
    fun oversizedMetadataResponseRemainsTransient() = runTest {
        http.respond { _, _ -> throw DriveResponseTooLargeException() }

        val error = runCatching { transport.find(account) }.exceptionOrNull()

        assertTrue(error is DriveHttpException)
        assertEquals(true, (error as DriveHttpException).isTransient())
    }

    @Test
    fun boundedReaderAllowsResponseExactlyAtLimit() {
        val bytes = byteArrayOf(1, 2, 3, 4)

        assertEquals(bytes.toList(), readLimitedResponse(ByteArrayInputStream(bytes), 4).toList())
    }

    @Test
    fun boundedReaderStopsAsSoonAsResponseExceedsLimit() {
        val error = runCatching {
            readLimitedResponse(ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5)), 4)
        }.exceptionOrNull()

        assertEquals(DriveResponseTooLargeException::class, error?.let { it::class })
    }

    @Test
    fun firstUploadCreatesTheFileWithAMultipartRequest() = runTest {
        val bytes = "{}".toByteArray()
        http.respond { _, _ -> DriveHttpResponse(200, created(bytes)) }

        transport.upload(account, existing = null, bytes = bytes)

        val sent = http.sent.single()
        assertEquals("POST", sent.method)
        assertEquals(true, sent.url.startsWith("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"))
        assertEquals(true, sent.headers["Content-Type"].orEmpty().startsWith("multipart/related; boundary="))
    }

    @Test
    fun laterUploadPatchesTheExistingFile() = runTest {
        val bytes = "{}".toByteArray()
        http.respond { _, _ -> DriveHttpResponse(200, created(bytes)) }

        transport.upload(account, existing = RemotePlanFile("old", "x"), bytes = bytes)

        val sent = http.sent.single()
        assertEquals("POST", sent.method)
        assertEquals(true, sent.url.startsWith("https://www.googleapis.com/upload/drive/v3/files/old?uploadType=media"))
        assertEquals("PATCH", sent.headers["X-HTTP-Method-Override"])
    }

    private fun created(bytes: ByteArray) = "{\"id\":\"old\",\"md5Checksum\":\"${md5(bytes)}\"}".toByteArray()

    @Test
    fun deleteAllRemovesDuplicatePlanFiles() = runTest {
        http.respond { request, _ ->
            if (request.startsWith("GET")) DriveHttpResponse(200, LIST_TWO_FILES) else DriveHttpResponse(204, ByteArray(0))
        }

        transport.deleteAll(account)

        assertEquals(
            listOf("DELETE https://www.googleapis.com/drive/v3/files/new", "DELETE https://www.googleapis.com/drive/v3/files/old"),
            http.requests.filter { it.startsWith("DELETE") }
        )
    }

    @Test
    fun findReadsTheModificationTimeAndToleratesItsAbsence() = runTest {
        http.respond { _, _ ->
            DriveHttpResponse(
                200,
                """{"files":[{"id":"new","md5Checksum":"AA","modifiedTime":"2026-10-08T12:14:00.000Z"},{"id":"old","md5Checksum":"BB"}]}""".toByteArray()
            )
        }

        assertEquals(RemotePlanFile("new", "aa", java.time.Instant.parse("2026-10-08T12:14:00Z").toEpochMilli()), transport.find(account))
        assertEquals(true, http.sent[0].url.contains("modifiedTime"))
    }

    @Test
    fun aFileWithoutModificationTimeHasNone() = runTest {
        http.respond { _, _ -> DriveHttpResponse(200, """{"files":[{"id":"only","md5Checksum":"CC"}]}""".toByteArray()) }

        assertEquals(null, transport.find(account)?.modifiedAtMillis)
    }

    @Test
    fun findReadsTheNextPageBeforeChoosingTheNewestFile() = runTest {
        http.respond { url, _ ->
            when {
                "pageToken=" !in url -> DriveHttpResponse(200, """{"files":[{"id":"new","md5Checksum":"AA"}],"nextPageToken":"next /?"}""".toByteArray())
                else -> DriveHttpResponse(200, """{"files":[{"id":"older","md5Checksum":"BB"}]}""".toByteArray())
            }
        }

        assertEquals(RemotePlanFile("new", "aa"), transport.find(account))
        assertEquals(true, http.sent[0].url.contains("nextPageToken"))
        assertEquals(true, http.sent[1].url.contains("pageToken=next%20%2F%3F"))
    }

    @Test
    fun findContinuesAfterAnEmptyPageWithANextPageToken() = runTest {
        http.respond { url, _ ->
            when {
                "pageToken=" !in url -> DriveHttpResponse(200, """{"files":[],"nextPageToken":"second"}""".toByteArray())
                "pageToken=second" in url -> DriveHttpResponse(200, """{"files":[],"nextPageToken":"third"}""".toByteArray())
                else -> DriveHttpResponse(200, """{"files":[{"id":"last","md5Checksum":"CC"}]}""".toByteArray())
            }
        }

        assertEquals(RemotePlanFile("last", "cc"), transport.find(account))
        assertEquals(3, http.sent.size)
    }

    @Test
    fun deleteAllFailsWhenAContinuationPageFails() = runTest {
        http.respond { url, _ ->
            if ("pageToken=" !in url) DriveHttpResponse(200, """{"files":[{"id":"new","md5Checksum":"AA"}],"nextPageToken":"next"}""".toByteArray())
            else DriveHttpResponse(503, ByteArray(0))
        }

        val error = runCatching { transport.deleteAll(account) }.exceptionOrNull()

        assertEquals(503, (error as DriveHttpException).statusCode)
        assertEquals(emptyList<String>(), http.requests.filter { it.startsWith("DELETE") })
    }

    @Test
    fun repeatedPageTokenFailsAsTransientBeforeDeletingAnything() = runTest {
        http.respond { url, _ ->
            when {
                "pageToken=" !in url -> DriveHttpResponse(200, """{"files":[{"id":"new","md5Checksum":"AA"}],"nextPageToken":"A"}""".toByteArray())
                "pageToken=A" in url -> DriveHttpResponse(200, """{"files":[{"id":"middle","md5Checksum":"BB"}],"nextPageToken":"B"}""".toByteArray())
                "pageToken=B" in url -> DriveHttpResponse(200, """{"files":[{"id":"old","md5Checksum":"CC"}],"nextPageToken":"A"}""".toByteArray())
                else -> throw IllegalStateException("Unexpected repeated page request")
            }
        }

        val error = runCatching { transport.deleteAll(account) }.exceptionOrNull()

        assertTrue(error is DriveHttpException)
        assertEquals(true, (error as DriveHttpException).isTransient())
        assertEquals(emptyList<String>(), http.requests.filter { it.startsWith("DELETE") })
        assertEquals(3, http.sent.size)
    }

    @Test
    fun nullNextPageTokenEndsTheListing() = runTest {
        http.respond { _, _ -> DriveHttpResponse(200, """{"files":[],"nextPageToken":null}""".toByteArray()) }

        assertEquals(null, transport.find(account))
        assertEquals(1, http.sent.size)
    }

    private class CountingTokens : DriveAccessTokenProvider {
        var requested = 0
        val invalidated = mutableListOf<String>()

        override suspend fun tokenFor(account: SyncAccount): String {
            requested += 1
            return "token-$requested"
        }

        override suspend fun invalidate(token: String) {
            invalidated += token
        }
    }

    private data class Sent(val method: String, val url: String, val headers: Map<String, String>)

    private class ScriptedHttp : DriveHttpClient {
        val requests = mutableListOf<String>()
        val sent = mutableListOf<Sent>()
        private var handler: (String, Map<String, String>) -> DriveHttpResponse = { _, _ -> DriveHttpResponse(500, ByteArray(0)) }

        fun respond(handler: (String, Map<String, String>) -> DriveHttpResponse) {
            this.handler = handler
        }

        override suspend fun execute(url: String, method: String, headers: Map<String, String>, body: ByteArray?): DriveHttpResponse {
            val request = "$method ${url.substringBefore('?')}"
            requests += request
            sent += Sent(method, url, headers)
            return handler("$method $url", headers)
        }
    }

    private companion object {
        val LIST_TWO_FILES = """{"files":[{"id":"new","md5Checksum":"AA"},{"id":"old","md5Checksum":"BB"}]}""".toByteArray()
    }
}
