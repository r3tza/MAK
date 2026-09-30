package dev.retza.mak.sync

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    private class ScriptedHttp : DriveHttpClient {
        val requests = mutableListOf<String>()
        private var handler: (String, Map<String, String>) -> DriveHttpResponse = { _, _ -> DriveHttpResponse(500, ByteArray(0)) }

        fun respond(handler: (String, Map<String, String>) -> DriveHttpResponse) {
            this.handler = handler
        }

        override suspend fun execute(url: String, method: String, headers: Map<String, String>, body: ByteArray?): DriveHttpResponse {
            val request = "$method ${url.substringBefore('?')}"
            requests += request
            return handler(request, headers)
        }
    }

    private companion object {
        val LIST_TWO_FILES = """{"files":[{"id":"new","md5Checksum":"AA"},{"id":"old","md5Checksum":"BB"}]}""".toByteArray()
    }
}
