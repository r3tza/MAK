package dev.retza.mak.sync

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.koin.core.annotation.Single

data class AuthorizedGoogleAccount(val subject: String, val email: String, val androidAccountName: String)

sealed interface AuthorizationAttempt {
    data class Granted(val account: AuthorizedGoogleAccount) : AuthorizationAttempt
    data class NeedsResolution(val pendingIntent: PendingIntent) : AuthorizationAttempt
}

class UserActionRequiredException(val resolution: PendingIntent?) : IllegalStateException("Google authorization needs user action")

object GoogleScopeValidator {
    private const val DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
    private const val EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email"

    fun requireAll(grantedScopes: Collection<String>) {
        val normalized = grantedScopes.map(::normalize).toSet()
        require(DRIVE_APPDATA in normalized && "openid" in normalized && "email" in normalized) {
            "Google did not grant all required synchronization permissions"
        }
    }

    private fun normalize(scope: String): String = when (scope.lowercase()) {
        "openid", "https://www.googleapis.com/auth/openid" -> "openid"
        "email", EMAIL_SCOPE -> "email"
        else -> scope
    }
}

internal object GoogleUserInfoStatus {
    fun requireSuccess(statusCode: Int) {
        if (statusCode !in 200..299) {
            throw DriveHttpException(statusCode, "Google identity verification failed")
        }
    }
}

@Single(binds = [DriveAccessTokenProvider::class])
class GoogleAccountAuthorization(
    context: Context,
    private val backgroundDispatcher: CoroutineDispatcher
) : DriveAccessTokenProvider {
    private val appContext = context.applicationContext
    private val client = Identity.getAuthorizationClient(appContext)

    suspend fun beginAccountSelection(): AuthorizationAttempt = withContext(backgroundDispatcher) {
        val result = authorize(accountName = null, selectAccount = true)
        result.toAttempt(expectedSubject = null, accountName = null, selectAccount = true)
    }

    suspend fun refreshPinnedAccount(account: SyncAccount): AuthorizationAttempt = withContext(backgroundDispatcher) {
        authorize(account.androidAccountName, selectAccount = false).toAttempt(account.subject, account.androidAccountName, false)
    }

    suspend fun completeAccountSelection(resultIntent: Intent): AuthorizationAttempt = withContext(backgroundDispatcher) {
        client.getAuthorizationResultFromIntent(resultIntent).toAttempt(expectedSubject = null, accountName = null, selectAccount = true)
    }

    suspend fun completePinnedAuthorization(resultIntent: Intent, account: SyncAccount): AuthorizationAttempt =
        withContext(backgroundDispatcher) {
            client.getAuthorizationResultFromIntent(resultIntent).toAttempt(account.subject, account.androidAccountName, false)
        }

    override suspend fun tokenFor(account: SyncAccount): String = withContext(backgroundDispatcher) {
        var result = authorize(account.androidAccountName, selectAccount = false)
        repeat(2) { attempt ->
            if (result.hasResolution()) throw UserActionRequiredException(result.pendingIntent)
            GoogleScopeValidator.requireAll(result.grantedScopes)
            val token = result.accessToken?.takeIf(String::isNotBlank) ?: throw UserActionRequiredException(null)
            try {
                val identity = userInfo(token)
                require(identity.subject == account.subject) { "Google account changed; reconnect before synchronizing" }
                return@withContext token
            } catch (error: DriveHttpException) {
                if (error.statusCode != 401) throw error
                clearToken(token)
                if (attempt == 1) throw UserActionRequiredException(null)
                result = authorize(account.androidAccountName, selectAccount = false)
            }
        }
        error("Google authorization retry was exhausted")
    }

    override suspend fun invalidate(token: String) = withContext(backgroundDispatcher) { clearToken(token) }

    private suspend fun AuthorizationResult.toAttempt(
        expectedSubject: String?,
        accountName: String?,
        selectAccount: Boolean
    ): AuthorizationAttempt {
        var result = this
        repeat(2) { attempt ->
            if (result.hasResolution()) return AuthorizationAttempt.NeedsResolution(requireNotNull(result.pendingIntent))
            GoogleScopeValidator.requireAll(result.grantedScopes)
            val token = result.accessToken?.takeIf(String::isNotBlank)
                ?: throw IllegalStateException("Google did not return an access token")
            try {
                val identity = userInfo(token)
                if (expectedSubject != null) require(identity.subject == expectedSubject) {
                    "Google account changed; reconnect before synchronizing"
                }
                return AuthorizationAttempt.Granted(AuthorizedGoogleAccount(identity.subject, identity.email, identity.email))
            } catch (error: DriveHttpException) {
                if (error.statusCode != 401) throw error
                clearToken(token)
                if (attempt == 1) throw UserActionRequiredException(null)
                result = authorize(accountName, selectAccount)
            }
        }
        error("Google authorization retry was exhausted")
    }

    private suspend fun authorize(accountName: String?, selectAccount: Boolean): AuthorizationResult {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(
                Scope("https://www.googleapis.com/auth/drive.appdata"),
                Scope("openid"),
                Scope("email")
            ))
            .setOptOutIncludingGrantedScopes(true)
            .also { builder ->
                if (selectAccount) builder.setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT)
                else if (accountName != null) builder.setAccount(Account(accountName, "com.google"))
            }
            .build()
        return suspendCancellableCoroutine { continuation ->
            client.authorize(request)
                .addOnSuccessListener { result -> if (continuation.isActive) continuation.resume(result) }
                .addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
        }
    }

    private suspend fun userInfo(token: String): UserInfo = withContext(backgroundDispatcher) {
        val connection = URL("https://openidconnect.googleapis.com/v1/userinfo").openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = HTTP_TIMEOUT_MILLIS
            connection.readTimeout = HTTP_TIMEOUT_MILLIS
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            val responseCode = connection.responseCode
            GoogleUserInfoStatus.requireSuccess(responseCode)
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                var size = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    size += count
                    require(size <= USERINFO_LIMIT_BYTES) { "Google identity response is too large" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val body = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
            val root = Json.parseToJsonElement(body).jsonObject
            val subject = root["sub"]?.jsonPrimitive?.content?.takeIf(String::isNotBlank)
                ?: error("Google identity response has no subject")
            val email = root["email"]?.jsonPrimitive?.content?.takeIf(String::isNotBlank)
                ?: error("Google identity response has no email")
            require(root["email_verified"]?.jsonPrimitive?.content == "true") { "Google account email is not verified" }
            UserInfo(subject, email)
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun clearToken(token: String) {
        suspendCancellableCoroutine<Unit> { continuation ->
            client.clearToken(ClearTokenRequest.builder().setToken(token).build())
                .addOnSuccessListener { if (continuation.isActive) continuation.resume(Unit) }
                .addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
        }
    }

    private data class UserInfo(val subject: String, val email: String)

    companion object {
        private const val HTTP_TIMEOUT_MILLIS = 15_000
        private const val USERINFO_LIMIT_BYTES = 16 * 1024
    }
}
